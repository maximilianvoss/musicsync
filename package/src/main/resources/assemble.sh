#!/bin/bash

projectRoot="$1/.."
resources="$1/src/main/resources"

target="$1/target"
targetDocker="$target/dockerbaseimage"
targetApp="$target/musicsync"

libs="$targetApp/libs"
modules="$targetApp/modules"

mkdir -p $targetApp
mkdir -p $libs
mkdir -p $modules

echo "Application Libraries"
find $projectRoot -name '*.jar' '!' -path '*libs*'
find $projectRoot -name '*.jar' '!' -path '*libs*' -exec  cp {} $targetApp \;

echo ""
echo "Dependency Libraries"
find $projectRoot -path '*libs*' -name '*.jar'
find $projectRoot -path '*libs*' -name '*.jar' -exec cp {} $libs \;

find $libs \
       -name '*commons-lang3*'  -exec mv {} $modules \; \
    -o -name '*commons-io*'  -exec mv {} $modules \; \
    -o -name '*lombok*'  -exec mv {} $modules \; \
    -o -name '*reflections*' -exec mv {} $modules \; \
    -o -name '*log4j*' -exec mv {} $modules \; \
    -o -name '*jackson*2.13.0*' -exec mv {} $modules \; \
    -o -name '*jsonhelper*' -exec mv {} $modules \;

cp $resources/musicsync $targetApp
chmod +x $targetApp/musicsync

cp $resources/sp $targetApp
chmod +x $targetApp/sp

cp $resources/stream_recorder.pl $targetApp
chmod +x $targetApp/stream_recorder.pl

if [ "$2" == "docker" ]; then
    echo ""
    echo "Docker"

    mkdir -p $targetDocker

    cp $resources/docker/base/* $targetDocker
    cp -R $resources/docker/application/* $target

    $target/image_build.sh
fi
