FROM ubuntu:20.04

RUN apt-get update
RUN apt-get install -y gnupg2 curl
RUN curl -sS https://download.spotify.com/debian/pubkey_5E3C45D7B312C643.gpg | apt-key add -
RUN echo "deb http://repository.spotify.com stable non-free" | tee /etc/apt/sources.list.d/spotify.list
RUN apt-get update
RUN DEBIAN_FRONTEND="noninteractive" apt-get install -y x11vnc xvfb sox lame dbus ffmpeg pulseaudio dbus-x11 xinit spotify-client maven git qdbus-qt5

COPY jdk-11*.tar.gz /usr/local
RUN tar -xvzf /usr/local/jdk-11*.tar.gz -C /usr/local
RUN rm /usr/local/*.tar.gz
RUN ls /usr/local | grep jdk-11 | awk '{ printf("ln -s %s /usr/local/jdk\n", $1); }' | sh

RUN ln -s /usr/local/jdk/bin/* /usr/local/bin/
