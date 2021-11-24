FROM maximilianvoss/musicsyncbase

# Latest Git Code
RUN git clone https://github.com/maximilianvoss/musicsync.git musicsync-git
RUN (cd musicsync-git; git pull; git checkout develop; mvn clean install)
RUN mv /musicsync-git/application/target/* /musicsync/

# tidy up
#RUN rm -rf /musicsync

# Setup config
RUN ln -s /config/musicsync.properties /musicsync/
RUN ln -s /config/log4j.properties /musicsync/
RUN mkdir /musicsync/cache

ENTRYPOINT bash /config/start.sh
