# Spotify Toniebox Sync
I have 2 wonderful children and for Xmas they received a Toniebox [1]. 
There are many many incredbile Tonie's out there, but we were struggeling with what we can fill our Creative Tonies.
After a while we found many wonderful stories for kids on Spotify [2] and we wondered why not having these stories on a Creative Tonie.

So this handy tool is to sync Spotify playlists with your Creative Tonies.

**Remark: To use this tool you must have a Spotify premium abo!**   

**My special thanks to:** 
* Geert Vandeweyer for stream_recorder.pl [3]
* Wander Nauta for SP [4]

Links:  
[1]: https://www.tonies.com/  
[2]: https://www.spotify.com/  
[3]: https://bitbucket.org/geertvandeweyer/spotify_recorder/src    
[4]: https://gist.github.com/wandernauta/6800547  

## Build
To build a single package including all dependencies:
```bash
mvn clean package assembly:single install
```

## Execution
Generate URI to accept all terms & conditions for your clientId at Spotify
```bash
java -jar spotify-toniebox-sync-[VERSION]-jar-with-dependencies.jar --apicode
```
Generate your `refreshToken` based on the `data` parameter
```bash
java -jar spotify-toniebox-sync-[VERSION]-jar-with-dependencies.jar --code "[DATA PARAMETER]"
```
Sync one playlist to a Tonie. No daemon which keeps syncing, just a one-shot
```bash
java -jar spotify-toniebox-sync-[VERSION]-jar-with-dependencies.jar --playlist "[PLAYLIST NAME ON SPOTIFY]" --tonie "[NAME OF TONIE]"
``` 
Run Spotify Toniebox Sync as daemon which keeps polling & syncing
```bash
java -jar spotify-toniebox-sync-[VERSION]-jar-with-dependencies.jar --daemon &
```

## Configuration
There are 3 properties-file you have to adapt for your personal needs.

### Spotify Properties
This properties file contains everything which is necessary for the Spotify communication and caching audio files which are downloaded from Spotify.
```properties
# Setup to run with Spotify
spotify.clientId=<clientId>
spotify.clientSecret=<clientSecret>
spotify.redirectUri=<redirectUri>
spotify.refreshToken=<refreshToken>
spotify.cachePath=<pathForCaching>
```

You have to setup a ClientId at https://developer.spotify.com for `spotify.clientId`, `spotify.clientSecret` and `spotify.redirectUri`.  
`spotify.redirectUri` can be selected randomly by you, it must be a valid URI but nothing has to be hosted there. You have to use the same here as in the ClientId

**To get the refreshToken**
Fill out `spotify.clientId`, `spotify.clientSecret` and `spotify.redirectUri`.  
Buid the software like described above.
Run:
```bash
java -jar spotify-toniebox-sync-[VERSION]-jar-with-dependencies.jar --apicode
``` 
The call will result in some output like
```text
URI: https://accounts.spotify.com:443/authorize?client_id=e26995898f9f4e4d9bf45b3001e6701d&response_type=code&redirect_uri=https%3A%2F%2Fmaximilian.voss.rocks%2Fspotify-redirect&state=x4xkmn9pu3j6ukrs8n&scope=playlist-read-private%2Cplaylist-read-collaborative%2Cplaylist-modify-private%2Cplaylist-modify-public&show_dialog=true
```

Open this URI in your web-browser, enter your credentials, agree and see that you are going to be redirected to `spotify.redirectUri`.  
The URI has a  parameter `data` appended. Take the value (after the data parameter is a state parameter, make sure you don't copy this as well).
Take the `data` parameter and execute:
```bash
java -jar spotify-toniebox-sync-[VERSION]-jar-with-dependencies.jar --code "[DATA-PARAMETER]"
```
The call will result in some output like
```text
Refresh Token: AQBStXZ6GhKpU-eGSN2GS13d0kITqUBgqeXWgMLlFOSacOpjSQ-nB9F-Y3KOT4dGRB_aAV5bX1T8hDoKQP04hGqJkhhiVonKZgWBFVuWSqoHDEm-eiKKg0nT3qY0nOxEX_zNXg
```
Take the refresh token and fill `spotify.refreshToken` in the properties file.

As for `spotify.cachePath`, select any temp directory where you want to cache the Spotify songs.

### Toniebox Properties 
Insert your Toniebox credentials here:
```properties
# Toniebox credentials
toniebox.username=<username>
toniebox.password=<password>
```

### Daemon Mapping Properties
If you want to run Spotify Toniebox Sync as a daemon you have to fill this properties file.
Each mapping[X] is a set of `Tonie Id`;`Spotify Playlist URI`.
```properties
mapping[0]=3E141A0D500314E9;spotify:user:dummyuser:playlist:1mXl7MwD1BGILtZVW4af4f
mapping[1]=F969A20D500384E9;spotify:user:dummyuser:playlist:5A52aoIXdtWS20WmZ9FVGR
``` 

`TonieId` can be found in the URL of your creative Tonie.  
`Spotify Playlist URI`: context menu in Spotify -> Share -> Copy Spotify URI.

## Running Spotify Toniebox Sync on your local environment/server
Installation based on vanilla Ubuntu 64-bit Server 18.04.1

### Ubuntu Server installing additional software
```bash
apt-get install -y x11vnc xvfb sox lame qdbus ffmpeg pulseaudio dbus-x11 xinit
```

### Install Spotify
```bash
echo deb http://repository.spotify.com stable non-free | sudo tee /etc/apt/sources.list.d/spotify.list 
sudo apt-key adv --recv-keys --keyserver keyserver.ubuntu.com 931FF8E79F0876134EDDBDCCA87FF9DF48BF1C90 
sudo apt-get update 
sudo apt-get install -y spotify-client 
```

### Clone repository and link scripts
```bash
git clone git@git.voss.rocks:toniebox/spotify-download-service.git
ln -sf $PWD/spotify-toniebox-sync/bin/sp /usr/local/bin
ln -sf $PWD/spotify-toniebox-sync/bin/stream_recorder.pl /usr/local/bin/
```

### Verfication
To check if Spotify is really running and maybe to also adjust there some settings.
```bash
x11vnc -display :99 -bg -nopw -xkb
```

### Scripts
Everything you need to make it run:
```bash 
nohup Xvfb :99 -screen 0 800x600x16 &
DISPLAY=:99; export DISPLAY
nohup pulseaudio --start &
nohup spotify $SPOTIFY_OPTIONS &
nohup java -jar spotify-toniebox-sync-[VERSION]-SNAPSHOT-jar-with-dependencies.jar --daemon & 
```