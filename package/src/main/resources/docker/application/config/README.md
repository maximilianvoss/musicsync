# Configuration folder

This configuration folder is going to be shared with the Docker container and can be used to change easily configuration
without having to rebuild the image.

File list:

* log4j.properties: Can be used to adapt the log levels for easier debugging
* musicsync.properties: Your audio connections for the musicsync
* spotify: Your start parameters for Spotify (e.g. username & password)
* start.sh: script which is actually executed upon start of the container
* vnc: Your VNC settings (e.g. username & password)
