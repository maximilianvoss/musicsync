#!/usr/bin/perl
use File::Basename;
use Time::HiRes qw(time);
use Getopt::Long;
use Data::Dumper;
use File::Temp;

##############
## SETTINGS ##
##############
my $options = CheckOps();
###################
## PREREQUISITES ##
###################
## check binaries
CheckBinaries(qw/sox spotify sp pacmd pactl ffmpeg lame qdbus/);
## spotify running ?
StartSpotify($0);


#############
## PREPARE ##
#############
our $tmpdir = File::Temp->newdir(CLEANUP => 0);
print "using tmpdir : $tmpdir\n";
## get main sink.
my $main_out = GetMainSink();
my $all_sinks = GetAllSinks();
############
## process.#
############
if (-f $options->{'uri'}) {
	# is a file. loop contents
	open URIS, $options->{'uri'};
	while (my $uri = <URIS>) {
		chomp($uri);
		# skip commented
		if ($uri =~ m/^#/) {
			next;
		}
		if ($uri !~ m/^spotify/) {
			print "Invalid URI (should start with 'spotify:' : $uri\n";
			next;
		}
		RecordStream($uri,$main_out,$options,$all_sinks);
	}
	close URIS;
}
else {
	# a single uri.
	RecordStream($options->{'uri'},$main_out,$options,$all_sinks);
}
## clean up tmpdir
print "Cleaning up tmpdir\n";
system("rm -Rf $tmpdir");
exit;


##################
## main wrapper.##
##################
sub RecordStream {
	my ($uri,$main_out,$options) = @_;
	if ($uri eq '') {
		return;
	}
	## load spotify stream
	my ($sink_name,$sink_idx) = LoadStream($uri,$all_sinks,$options->{'silent'});
	## RECORD ##
	print "Recording...\n";
	# fork recorder.
	my $recorder_pid = fork();
	die if not defined($recorder_pid);
	if (not $recorder_pid) {
		# in child.
		setpgrp(0, 0); ## needed to make the child-killing actually kill the recording exec.

		my $cmd = "parec --rate=44100 --monitor-stream=$sink_idx --file-format=wav $tmpdir/full.wav >$tmpdir/parec.log 2>&1"; #| ".$options->{'encoder'}." ".$options->{'encoder_opts'}." > $tmpdir/full.".$options->{'format'};
		#print "\nRecording command:\n$cmd\n";
		exec($cmd) ;
		exit;
	}
	# fork time tracker
	my $timer_pid = fork();
	die if not defined($timer_pid);
	if (not $timer_pid) {
		# in child.
		ChildOfTimer();
		exit;

	}

	# (re)start player.
	system("sp open '$uri' >>$tmpdir/run.log 2>&1") == 0 or SpeakEnglishAndDie("Spotify stream could not be loaded\n$!",$?,"sp");
	# wait until paused (end op playlist)
	WaitAndBleed($recorder_pid,$timer_pid);
	# proforma : wait for forks to end.
	for (1 .. 2) {
		wait();
	}
	# clean up
	CleanUp($options->{'silent'},$main_out,$sink_name);

	## split the recorded stream. #
	print "Splitting the stream into tracks...\n";
	SplitSongs();
}




sub CheckOps {
	my $silent = 0;  	# dump spotify stream to the null device (silent recording)
	my $format = 'mp3'; 	# can be mp3 or flac
	my $outdir = './Results'; # where should we create the artist/album/track structure
	my $uri = '';
	my $filename = '';
	GetOptions (	'silent' => \$silent,
			'format=s' => \$format,
			'outdir=s' => \$outdir,
			'uri=s' => \$uri,
			'filename=s' => \$filename
		   );
	my %options = (silent => $silent, 'format' => $format, 'outdir' => $outdir, 'uri' => $uri, 'filename' => $filename);
	if ($format eq 'mp3') {
		$options{'encoder_opts'} = '-vn -y -codec:a libmp3lame -q:a 0';#"-r -V0 -b 192  - -";
		$options{'encoder'} = 'ffmpeg'; #'lame';
	}
	elsif ($format eq 'flac') {
		$options{'encoder_opts'} = '-y -vn -codec:a flac';
		$options{'encoder'} = 'ffmpeg' ; #'flac';
	}
	else {
		print "Encoder format '$format' is not supported. Please use mp3 or flac\n";
		exit;
	}
	CheckBinaries($options{'encoder'});
	return(\%options);

}

sub CheckBinaries {
	my $failed = '';
	foreach my $bin (@_) {
		my $which = `which $bin`;
		chomp($which);
		if ($which eq '') {
			$failed .= "  - $bin\n";
		}
	}
	if ($failed ne '') {
		print "Missing Binaries: \n$failed";
		if ($failed =~ m/sp\n/) {
			print "\n\nNote: sp (command line controller) can be downloaded from : https://gist.github.com/wandernauta/6800547\n";
		}
		exit;
	}
}
sub PrintHelp {
	# todo.

}

sub StartSpotify {

	my $script = basename(shift(@_));
	my $wc = `ps aux | grep spotify | grep -v '$script' | grep -v grep | wc -l`;
	chomp($wc);
	if ($wc == 0) {
		print "Starting Spotify\n";
		system("spotify \$SPOTIFY_OPTIONS>/dev/null 2>&1&");
		print "  ... waiting for spotify to start\n";
		sleep 5;
	}
}

sub GetMainSink {
	my @sinks = `pacmd list-sinks`;
	chomp(@sinks);
	my $main_out = '';
	my $ok = 0;
	foreach my $line (@sinks) {
		if ($line =~ m/index: (\d+)/) {
			$main_out = $1;
			next;
		}
		if ($line =~ m/name: <alsa_output/) {
			last;
		}
	}
	return($main_out);

}
sub GetAllSinks {
	my %return = (); # id => muted
	my @sinks = `pacmd list-sinks`;
	chomp(@sinks);
	my $idx = $muted = '';
	my $ok = 0;
	foreach my $line (@sinks) {
		if ($line =~ m/index: (\d+)/) {
			$idx = $1;
			next;
		}
		#if ($line =~ m/name: <alsa_output/) {
		#	last;
		#}
		if ($line =~ m/muted: (.*)/) {
			if ($1 =~ m/no/i) {
				$return{$idx} = 1;
			}
			elsif ($1 =~ m/yes/i) {
				$return{$idx} = 0;
			}
		}
	}
	return(\%return);

}


sub FindSpotify {
	# get sink input
	my @inputs = `pacmd list-sink-inputs`;
	chomp(@inputs);
	my $sink_idx = '';
	my $current_out = '';
	my $ok = 0;
	foreach my $line (@inputs) {
		if ($line =~ m/index:\s+(\d+)/) {
			$sink_idx = $1;
			next;
		}
		if ($line =~ m/sink:\s(\d+)\s/) {
			$current_out = $1;
			next;
		}
		if ($sink_idx ne '' && ($line =~ m/client.*Spotify/ || $line =~ m/application\.name.*spotify/i)) {
			$ok = 1;
			last;
		}
	}
	if ($ok != 1) {
		print "Could not find spotify sink : This was the output from pacmd list-sink-inputs:\n";
		print join("\n",@inputs);
		print "\nPlease report this\n";
		exit;
	}
	return($current_out,$sink_idx);
}
sub LoadStream {
	#my ($uri,$main_out,$silent) = @_;
	my ($uri,$all_sinks,$silent) = @_;
	print "Muting speakers to load spotify stream...\n";
	foreach my $idx (keys(%$all_sinks)) {
		if ($all_sinks->{$idx} == 1) {
			my $cmd = "pacmd set-sink-mute $idx 1  >>$tmpdir/run.log 2>&1";
			system($cmd) == 0 or SpeakEnglishAndDie("Could not mute speakers on channel $idx\n$!", $?,"pacmd");
		}
	}
	# prepare spotify
	print "Loading spotify URI\n";
	#$cmd = "sp open '$uri' ; sleep 0.5; sp pause";
	$cmd = "sp open '$uri'  >>$tmpdir/run.log 2>&1 ; sleep 0.5  >>$tmpdir/run.log 2>&1 ; sp pause  >>$tmpdir/run.log 2>&1";
	system($cmd) == 0 or SpeakEnglishAndDie("Spotify stream could not be loaded\n$!",$?,"sp");
	sleep 0.5;
	my ($current_out,$sink_idx) = FindSpotify();
	# unmute speakers
	foreach my $idx (keys(%$all_sinks)) {
		if ($all_sinks->{$idx} == 1) {
			my $cmd = "pacmd set-sink-mute $idx 0  >>$tmpdir/run.log 2>&1";
			system($cmd) == 0 or SpeakEnglishAndDie("Could not unmute speakers on channel $idx\n$!",$?,"pacmd");
		}
	}

	# reroute
	my $sink_name = '';
	my ($current_out,$sink_idx) = FindSpotify();
	if($silent) {
		$sink_name = "sr-".int(time());
		print "rerouting spotify stream to a black hole (you won't hear it)\n";#$sink_idx from $current_out to $sink_name\n";
		system("pactl load-module module-null-sink sink_name=$sink_name  >>$tmpdir/run.log 2>&1") == 0 or SpeakEnglishAndDie("Error generating the black hole sink\n$!",$?,"pactl");
		system("pactl move-sink-input $sink_idx $sink_name  >>$tmpdir/run.log 2>&1") == 0 or SpeakEnglishAndDie("Error moving spotify stream to the black hole\n$!", $?,"pactl");
	}
	# put stream at start position (this starts playback)
	#$cmd = "sp prev";
	#system($cmd) == 0  or die("Could not reset the stream: $?\n");
	return($sink_name,$sink_idx);

}

sub ChildOfTimer {
	my $cmd = "sp metadata";
	my $current = '';
	open TIME, ">$tmpdir/Track.timings.txt";
	print TIME "TimeStamp\tTrack\n";
	my $start = time();
	open START, ">$tmpdir/start.time.txt";
	print START $start;
	close START;
	while ( 1 ) { # loop until killed.
		my $now = time(); # this is high-res timing.
		my @lines = `$cmd`;
		chomp(@lines);
		my %meta = ();
		foreach(@lines) {
			my ($k,$v) = split(/\|/,$_);
			$meta{$k} = $v;
		}
		my $track = '';
		if ($meta{'discNumber'} > 1) {
			$track = $meta{'albumArtist'}."/".$meta{'album'}." - CD".$meta{'discNumber'}."/".sprintf("%02d",$meta{'trackNumber'})." - ".$meta{'title'};

		}
		else {
			$track = $meta{'albumArtist'}."/".$meta{'album'}."/".sprintf("%02d",$meta{'trackNumber'})." - ".$meta{'title'};
		}
		# new track : write out startpoint.webapps/ua-pintra_shib_translate_module-BBLEARN/module/sbmt_index.jsp
		if ($track ne $current) {
			my $diff = sprintf("%.4f",($now - $start));
			print TIME "$diff\t$track\n";
			print "  $diff"."s\t$track\n";
			$current = $track;
		}
		select(undef,undef,undef, .25); # run 4 times per second.
	}
	close TIME;

}

sub WaitAndBleed {
	my ($rpid, $tpid) = @_;
	sleep 2; # initial startup ; prevent race condition
	my $cmd = "qdbus org.mpris.MediaPlayer2.spotify /org/mpris/MediaPlayer2 org.freedesktop.DBus.Properties.Get org.mpris.MediaPlayer2.Player PlaybackStatus";
	my $status = `$cmd`;
	chomp($status);
	while ($status eq 'Playing') {
		select(undef,undef,undef,0.25);
		$status = `$cmd`;
		chomp($status);
	}
	open END, ">$tmpdir/end.time.txt";
	print END time();
	close END;
	sleep(4); # give time to pick up the 'start song'

	# Playlist ended : kill children.
	# stop recorder.
	kill 9, $rpid;
	# stop timer
	kill 9, $tpid;
}

sub CleanUp {
	my ($silent,$main_out,$sink_name) = @_;
	if ($silent) {
		# get spotify stream.
		my ($current_out,$sink_idx) = FindSpotify();

		# move spotify back to speakers
		$cmd = "pactl move-sink-input $sink_idx $main_out  >>$tmpdir/run.log 2>&1";
		system($cmd) == 0 or warn("Error retrieving spotify stream from the black hole: $?\n");
		# remove the temporary sink.
		my $module = `pactl list short modules | grep '$sink_name'`;
		chomp($module);
		$module =~ s/^(\d+)\s+.*/$1/;
		$cmd = "pactl unload-module $module  >>$tmpdir/run.log 2>&1";
		system($cmd) == 0 or SpeakEnglishAndDie("Error closing the black hole\n$!",$?,"pactl");
	}
}

sub SplitSongs {
	open IN, "$tmpdir/Track.timings.txt";
	my $head = <IN>;
	my $prev_start = 0;
	my $prev_title = '';
	my $first_title = '';
	my $nr_songs = 0;
	my $commercial = 0;
	while (<IN>) {
		chomp;
		$nr_songs++;
		($this_start,$this_title) = split(/\t/,$_);
		if ($first_title eq '') {
			$first_title = $this_title;
		}
		if ($prev_title eq '') {
			$prev_title = $this_title;
			$prev_start = $this_start;
			next;
		}
		# commercials lead to strange timings.
		my @pp = split(/\//,$this_title);
		if ($pp[0] eq '') {
			# this_ is a commercial.
			# extend previous.
			$this_start += 1.5;
			$commercial = 2;
		}
		elsif ($commercial == 2) {
			# prev_ was a commercial, need to adjust the next round.
			$commercial = 1;
		}
		elsif ($commercial == 1) {
			# writing song after commercial.
			$prev_start += 1.5;
			$commercial = 0;
		}
		if ($prev_title ne '') {
			WriteSong($prev_start,$this_start,$prev_title,$options->{'outdir'},$options->{'format'});
		}
		$prev_title = $this_title;
		$prev_start = $this_start;


	}
	close IN;
	# last == first (due to playlist ending & waiting (except for single song uris)
	if ($prev_title eq $first_title && $nr_songs > 1) {
		return;
	}
	WriteSong($prev_start,0,$prev_title,$options->{'outdir'},$options->{'format'});

}

sub WriteSong {
	my ($prev_start,$this_start,$prev_title,$outdir,$format) = @_;
	my $prev_length = $this_start - $prev_start;
	my @pp = split(/\//,$prev_title);
	my $nr_title = pop(@pp);
	if ($pp[0] eq '') {
		$pp[0] = 'Commercials';
	}
	my $path = join("/",@pp);
	my $artist = $pp[0];
	my $album = $pp[1];
	$nr_title =~ m/(\d+) - (.*)/;
	my $nr = $1;
	my $title = $2;
	$artist =~ s/'/'"'"'/g;
	$album =~ s/'/'"'"'/g;
	$nr =~ s/'/'"'"'/g;
	$title =~ s/'/'"'"'/g;
	$path =~ s/'/'"'"'/g;
	my $cmd ;
	$cmd = "mkdir -p '$outdir/$path'  >>$tmpdir/run.log 2>&1" if ( $options->{'filename'} eq '' );
	$cmd = "mkdir -p '$outdir'  >>$tmpdir/run.log 2>&1" unless ( $options->{'filename'} eq '' );
	system($cmd) == 0 or SpeakEnglishAndDie("Could not create output directory $outdir/$path\n$!",$?,"mkdir");

	# rrebuild file path
	my $song_file;
	$song_file = "$outdir/$path/$nr - $title.$format" if ( $options->{'filename'} eq '' );
	$song_file = "$outdir/$options->{'filename'}" unless ( $options->{'filename'} eq '' );

	my $split_cmd = '';
	# sox complains if using "trim 0 length", so increment by one millisec.
	if ($prev_start == 0) {
		$prev_start = 0.001;
	}
	if ($this_start > 0) {
		$split_cmd = "sox '$tmpdir/full.wav' '$tmpdir/$nr.wav' trim $prev_start $prev_length";
	}
	else {
		# get time
		open IN, "$tmpdir/end.time.txt";
		my $end = <IN>;
		chomp($end);
		close IN;
		open IN, "$tmpdir/start.time.txt";
		my $start = <IN>;
		chomp($start);
		close IN;
		my $duration = $end - $start;
		$split_cmd = "sox '$tmpdir/full.wav' '$tmpdir/$nr.wav' trim 0.001 $duration";

	}
	my $convert = "ffmpeg -i $tmpdir/$nr.wav ".$options->{'encoder_opts'}. " -metadata title='$title' -metadata artist='$artist' -metadata album='$album' -metadata track='$nr' '$song_file'";

	print "  Creating $prev_title\n";
	system("$split_cmd >>$tmpdir/run.log 2>&1") == 0 or SpeakEnglishAndDie("Sox could not split the streamfile\n$!", $?,"sox");
	system("$convert >$tmpdir/run.log 2>&1 ")  == 0 or SpeakEnglishAndDie("ffmpeg could not convert the splitted stream:\n$!", $?, "ffmpeg");
	unlink("$tmpdir/$nr.wav");
}

sub SpeakEnglishAndDie {
	my ($message,$exitcode,$binary) = @_;

	## get binary version.
	my $version = '';
	if ($binary =~ m/ffmpeg/) {
		$version = `ffmpeg 2>&1 | head -n 1`;
	}
	elsif ($binary eq 'sp') {
		$version = `sp version | head -n 1`;
	}
	elsif ($binary =~ m/^pa/ || $binary eq 'sox' || $binary eq 'mkdir') {
		$version = `$binary --version | head -n 1`;
	}
	chomp($version);
	open OUT, ">>$tmpdir/run.log";
	print OUT "\n\nFailing program : $binary\n";
	print OUT "$version\n";
	close OUT;
	print "Spotify StreamRecorder encountered a problem with $binary:\n";
	print " (version info: $version)\n";
	print "\n";
	print "$message\n";
	print "\n";
	print "The exit code was $exitcode\n";
	print "\n";
	print "The temporary folder is retained, so make sure to delete it:\n";
	print "  - $tmpdir\n";
	print "\n";
	print "Please provide the contents of the logs when reporting this issue:\n";
	print "  - General Log : $tmpdir/run.log\n";
	print "  - parec Log   : $tmpdir/parec.log\n";
	print "  - Share on : https://pastebin.com/\n";
	print "\n";
	exit(1);


}
