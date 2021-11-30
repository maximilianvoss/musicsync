# Music Sync
I have 2 wonderful children and for Xmas they received a Toniebox [1]. There are many incredbile Tonie's out there, but
we were struggling with what we can fill our Creative Tonies. After a while we found many wonderful stories for kids on
Spotify [2] and we wondered why not having these stories on a Creative Tonie.

In the end, this handy tool was created to sync music between different services. It supports plugins which helps in
extending it for other input & output services.

Links:  
[1]: https://www.tonies.com/  
[2]: https://www.spotify.com/

## Build

... To build the application to: [package/application/target](package/application/target)
```bash
mvn clean install
```

... To build a docker image whose execution and configuration files can be found
at: [package/docker/src/main/resources/docker/application](package/docker/src/main/resources/docker/application)

```bash
mvn clean install -Pdocker
```

## Configuration

The configuration is provided in a JSON format for easier read- and accessibility. There for, it is split in 3 major
parts.  
Full example file can be found
at: [package/docker/src/main/resources/docker/application/config/musicsync.json](package/docker/src/main/resources/docker/application/config/musicsync.json)

1. General - defines global configurations which are valid for the whole application   
   An example of this is:

```json
"general": {
"timeout": 60, // nap time for the loop
"bulk": true                      // upload all tracks as a bulk or do it one by one
} 
```

2. Plugins - configures global plugin settings   
   An example of this is - see details on plugin documentations:

```json
"plugins":
[
{
"plugin": "<PLUGIN NAME>", // name of the plugin
"config": {}                    // configuration of plugin
}
]
```

3. Connections - configures the synchronization connection (from where to where the music shall be synced)  
   An example of this is:

```json
"connections":
[
{
"name": "<CONNECTION NAME>", // name of configuration
"in": {
"plugin": "<PLUGIN NAME>", // input plugin to source tracks
"config": {}                // input plugin configuration to source tracks
},
"out": {
"plugin": "<PLUGIN NAME>", // output plugin to which the tracks shall be copied to
"config": {}                // output plugin configuration to upload tracks
}
}
]
```

## Execution

The application can be executed as standalone application or within a docker image which is provided by the maven build

### Application

The standard maven build copies the full build output to: [package/application/target](package/application/target).  
This output will consist out of:

* libs - containing all necessary JAR files for the classpath
* modules - contains all necessary JAR files for the Java modules
* musicsync - BASH script file to start musicsync
* sp - file for stream recording. Must be copied to /usr/bin or /usr/local/bin. Chmod 755 is required
* stream_recorder.pl file for stream recording. Most be copied to /usr/bin or /usr/local/bin. Chmod 755 is required
* musicsync-*.jar files - the actual Java code of musicsync

If you move the directory's content to other folders and want to run the application with relative or absolute paths you
have to update [package/application/target/musicsync](package/application/target/musicsync).

```bash
#!/bin/bash 

execdir=.     # THIS PATH HAS TO BE SPECIFIED/ADAPTED

classpath=$(find $execdir -name '*.jar' | awk '{ printf("%s:", $1); } END { printf("%s", $1); }')
executable='rocks.voss.musicsync.application/rocks.voss.musicsync.application.Application'

java -classpath $classpath -p $execdir:$execdir/modules -m $executable  $@
```

### Docker Image

While using the docker profile of maven 2 docker images will be created.

1. Base image (maximilianvoss/musicsyncbase)  
   This base image will base on Ubuntu 20.04 and will install Spotify and Oracle's JDK11.   
   Please review the documentation
   at [package/docker/src/main/resources/docker/base/README.md](package/docker/src/main/resources/docker/base/README.md)
   to ensure the docker image can be build.
2. Application image (maximilianvoss/musicsyncapplication)  
   This image is based on maximilianvoss/musicsyncbase and will compile and install musicsync within the image.  
   During the image execution following folder is shared from the host with the
   image: [package/docker/src/main/resources/docker/application/config](package/docker/src/main/resources/docker/application/config)

**Steps which must be performed to create a proper Docker image**

1. Download JDK 11 from Oracle
2. Copy JDK 11 to [package/docker/src/main/resources/docker/base](package/docker/src/main/resources/docker/base)
3. Build project with `mvn clean install -Pdocker`
4. Change dir
   to [package/docker/src/main/resources/docker/application/config](package/docker/src/main/resources/docker/application/config)
   1. Update musicsync.json (maybe you have to run musicsync locally first to update Spotify settings)
   2. Update spotify
   3. Update vnc
5. Run docker image using `./run.sh` in
   folder [package/docker/src/main/resources/docker/application](package/docker/src/main/resources/docker/application)

## Plugins

All plugins with their documentation can be found at [plugins/README.md](plugins/README.md).
