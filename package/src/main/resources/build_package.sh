#!/bin/bash

target="$1/target"
source="$1/.."

libs="$target/libs"
modules="$target/modules"

mkdir -p $target
mkdir -p $libs
mkdir -p $modules

find $source -name '*.jar' '!' -path '*libs*' -exec  cp {} $target \;
find $source -path '*libs*' -name '*.jar' -exec cp {} $libs \;

find $libs \
       -name '*commons-lang3*'  -exec mv {} $modules \; \
    -o -name '*lombok*'  -exec mv {} $modules \; \
    -o -name '*reflections*' -exec mv {} $modules \; \
    -o -name '*log4j*' -exec mv {} $modules \;

cp $1/src/main/resources/musicsync $target
chmod +x $target/musicsync

