#!/bin/bash
#

. /config/spotify
. /config/vnc

service dbus start
nohup Xvfb :99 -screen 0 1024x768x16 &
DISPLAY=:99; export DISPLAY

export NO_AT_BRIDGE=1
eval $(dbus-launch --sh-syntax)
export DBUS_SESSION_BUS_ADDRESS
export DBUS_SESSION_BUS_PID
export DBUS_SESSION_BUS_WINDOWID

sleep 10

nohup pulseaudio --system &
nohup pulseaudio --start &
x11vnc -forever -shared -passwd $VNCPASSWORD &
sleep 10
spotify $SPOTIFY_OPTIONS &

sleep 10
cd /musicsync
./musicsync
