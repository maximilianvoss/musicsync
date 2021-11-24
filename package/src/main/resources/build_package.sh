#!/bin/bash

target="$1/target"
source="$1/.."

mkdir -p $target
mkdir -p $target/libs

find $source -name '*.jar' '!' -path '*libs*' -exec  cp {} $target \;
find $source -path '*libs*' -name '*.jar' -exec cp {} $target/libs \;
cp $1/src/main/resources/musicsync $target
chmod +x $target/musicsync
