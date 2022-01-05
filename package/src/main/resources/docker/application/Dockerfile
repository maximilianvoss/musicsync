FROM maximilianvoss/musicsyncbase

RUN mkdir /musicsync
COPY musicsync /musicsync

RUN ln -s /musicsync/sp /usr/local/bin/
RUN ln -s /musicsync/stream_recorder.pl /usr/local/bin/

# Setup config
RUN ln -s /config/musicsync.json /musicsync/
RUN ln -s /config/log4j2.xml /musicsync/
RUN mkdir /musicsync/cache

ENTRYPOINT bash /config/start.sh
