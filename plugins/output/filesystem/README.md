# Filesystem Output Plugin
This plugin is capable of putting tracks into the filesystem

## Plugin Configuration
These are the general plugin configurations:
```json
"plugins": [
    {
        "plugin": "filesystem",
        "config": {
          "directory": "<DEFAULT FOLDER>"       // if connection has no folder defined, this will be the fallback
    }
]
``` 

## Connection Configuration
For the connection configuration:
```json
 "connections": [
    {
        "name": "<CONNECTION NAME>",
        "in": {
            "plugin": "spotify",
            "config": {
              "uri": "<SPOTIFY URI>"
            }
        },
        "out": {
            "plugin": "filesystem",                       // always "filesystem" for the Filesystem plugin
            "config": {
              "directory": "<OPTIONAL OUTPUT FOLDER>"     // folder to store tracks to
            }
        }
    }
]
```
