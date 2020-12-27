# Music Sync
I have 2 wonderful children and for Xmas they received a Toniebox [1]. 
There are many many incredbile Tonie's out there, but we were struggeling with what we can fill our Creative Tonies.
After a while we found many wonderful stories for kids on Spotify [2] and we wondered why not having these stories on a Creative Tonie.

In the end, this handy tool was created to sync music between different services.

**Remark: To use this tool you must have a Spotify premium abo!**     
**Remark: Only JDK 8 is supported**

**My special thanks to:** 
* Geert Vandeweyer for stream_recorder.pl [3]
* Wander Nauta for SP [4]

Links:  
[1]: https://www.tonies.com/  
[2]: https://www.spotify.com/  
[3]: https://bitbucket.org/geertvandeweyer/spotify_recorder/src    
[4]: https://gist.github.com/wandernauta/6800547  

## Build
To build the entire project
```bash
mvn clean install
```
The end result will be packaged in package/target/output. 

### Docker Build
`docker build . -t [IMAGE-TAG]`

## Execution
Run it as a daemon
```bash
nohup ./musicsync --daemon &
```

Run it for a single time
```bash
./musicsync --input [INPUT-URI] --output [OUTPUT-URI]
```

### Docker execution for a single time
`docker run [IMAGE-TAG] [ARGUMENTS]`
where _ARGUMENTS_ is e.g. `--input [INPUT-URI] --output [OUTPUT-URI]`

*Note: Parameters may change with the available plugins. Each plugin defines additional parameters if applicable.*

## Plugins
All plugins with their documentation can be found [here](plugins/README.md).

## Configuration
Either you use the command line arguments to define settings or you put it in a `musicsync.properties` file.
The command line options are described above and in the plugin documentations.  
For using the Daemon mode the `musicsync.properties` file has to be used.  

To define all properties while build time use the [musicsync.properties](application/src/main/resources/musicsync.properties) file from the application project.  
If you want to make changes before runtime copy the [musicsync.properties](application/src/main/resources/musicsync.properties) to the execution path of musicsync (by default: package/target/output/)

### Daemon Mapping Properties
You can setup multiple mappings source -> destiny when you run in daemon mode.  
The schema is always: mapping\[X\]=InputURI;OutputURI, whereby X starts at 0 and has to be increased with each mappping.  
For example:
```properties
mapping[0]=spotify:playlist:2Cswd6UwqB0F60PBsyjyga;toniebox:b2ca3ceb-cc94-42fc-ab5f-54af67d074c8:F969A20D500304E2
mapping[1]=spotify:playlist:0fc1f6yGwDYHwirzup42df;toniebox:b2ca3ceb-cc94-42fc-ab5g-54af67d074c8:3E141A0D500304E5
``` 

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
nohup musicsync --daemon & 
```
