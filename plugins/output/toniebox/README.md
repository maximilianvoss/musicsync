# Toniebox Output Plugin

This plugin is capable of uploading tracks to your Creative Tonie.

## Plugin Configuration

These are the general plugin configurations:

```json
"plugins": [
{
"plugin": "toniebox",
"config": {
"username": "<TONIEBOX USER>", // username for the login
"password": "<TONIEBOX PASSWORD>"       // password for the login
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
"plugin": "toniebox", // always "toniebox" for the Toniebox plugin
"config": {
"household": "<HOUSEHOLD>", // Household of the creative Tonie
"tonie": "<CREATIVE TONIE ID>"    // ID of the Creative Tonie
}
}
}
]
```

The easiest way to get the `household` and `tonie` is looking at the URL when you open one of your creative tonies
at http://meine.tonies.de. Example given:

```text
https://meine.tonies.de/tonies/[HOUSEHOLD]/[TONIE]
https://meine.tonies.de/tonies/b2ca3ceb-cc94-42fc-ab5f-54af67d074c8/F969A20D500304E2
```

Results to:
Results to:

```json
"household": "b2ca3ceb-cc94-42fc-ab5f-54af67d074c8"
"tonie": "F969A20D500304E2"
```
