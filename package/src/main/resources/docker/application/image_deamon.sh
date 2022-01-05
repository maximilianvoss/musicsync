#!/bin/bash
#

while true; do
    ./run.sh
    sleep 2h
    containerId=$(docker ps -f ancestor=maximilianvoss/musicsycapplication -f status=running -q)
    docker kill $containerId
    docker container rm $containerId
    sleep 5
done
