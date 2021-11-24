#!/bin/bash

resources="$1/src/main/resources"
(cd $resources/docker/base; ./build.sh)
(cd $resources/docker/application; ./build.sh)


