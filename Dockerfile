FROM openjdk:8-jdk-alpine AS build

## Set the working directory
WORKDIR /build

## Copy files from your host to current location.
COPY . .

RUN apk add maven

# fresh build
RUN mvn clean install

# Build final image
FROM openjdk:8-jdk-alpine as runtime
RUN ["mkdir", "/bin/spotify-toniebox-sync"]
COPY --from=build /build/package/target/output/musicsync /bin/spotify-toniebox-sync/
COPY --from=build /build/package/target/output/*.jar /bin/spotify-toniebox-sync/

WORKDIR /bin/spotify-toniebox-sync

# mark script as executable
RUN ["chmod", "+x", "musicsync"]
RUN ["touch", "musicsync.log"]
RUN ["chown", "-R", "nobody:", "/bin/spotify-toniebox-sync"]
RUN ["chmod", "-R",  "u+w", "/bin/spotify-toniebox-sync"]

USER nobody
ENTRYPOINT ["sh", "musicsync"]