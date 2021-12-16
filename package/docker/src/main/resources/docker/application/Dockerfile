FROM maximilianvoss/musicsyncbase

# Latest Git Code
RUN git clone https://github.com/maximilianvoss/musicsync.git musicsync-git
RUN (cd musicsync-git; git pull; mvn clean install)
RUN mkdir /musicsync
RUN mv /musicsync-git/package/application/target/* /musicsync/
RUN ln -s /musicsync/sp /usr/local/bin/
RUN ln -s /musicsync/stream_recorder.pl /usr/local/bin/

# tidy up
RUN rm -rf /musicsync-git

# Setup config
RUN ln -s /config/musicsync.json /musicsync/
RUN ln -s /config/log4j2.xml /musicsync/
RUN mkdir /musicsync/cache

ENTRYPOINT bash /config/start.sh
