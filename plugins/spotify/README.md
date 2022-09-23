# Spotify Input Plugin
This plugin is able to download any song and playlist from Spotify. All downloaded data will be cached.

**My special thanks to:**
* Geert Vandeweyer for stream_recorder.pl [1]
* Wander Nauta for SP [2]

[1]: https://bitbucket.org/geertvandeweyer/spotify_recorder/src
[2]: https://gist.github.com/wandernauta/6800547

**Note: This plugin requires Spotify Premium**

## Setup
To make this Spotify Input Plugin work, following steps have to be performed.

1. Create initial app settings at [https://developer.spotify.com/dashboard/](https://developer.spotify.com/dashboard/)
   1. Go to [https://developer.spotify.com/dashboard/](https://developer.spotify.com/dashboard/). 
   2. Accept the service.
   3. Create new app, to get  `clientId` & `clientSecret`,
   2. Define `redirectUri` - can be randomly chosen, must just be a valid URI 
   3. Copy `clientId` & `clientSecret`, can be put directly into `musicsync.json` see below
   4. Save same `redirectUri` to `musicsync.json`
2. Get an API Code by running `musicsync --spotify-apicode` after storing values to `musicsync.json`  
This will return a link, e.g.
```text
   URI: https://accounts.spotify.com:443/authorize?client_id=e26995898f9f4e4d9bf45b3001e8701d&response_type=code&redirect_uri=https%3A%2F%2Fmaximilian.voss.rocks%2Fspotify-redirect&state=x4xkmn9pu3j6ukrs8n&scope=playlist-read-private%2Cplaylist-read-collaborative%2Cplaylist-modify-private%2Cplaylist-modify-public&show_dialog=true
```
3. Open this link in a browser, login with your user credentials and approve
4. After saving you will be redirected to the `redirectUri` 
5. Copy the URL from the browser  
It might look like following:
```text
https://maximilian.voss.rocks/spotify-redirect?code=BQC8Dw4FxXv18d7HbA0w0ZngGNs31SyPSgbAfNvxBKPM6CZckWJZIcSlgu9JT3sygs2RNJHFZ1F9Y6bnX8XGZehPZzGpRWiZsmD4N-45qHUo9sBuw44QYW1A2O55ev76nBuYQfTumFl0xUPh5mFyg2t_e2PZBPiJT4Sxix0dyqTJZiEJ31z_6rR5zLn1PDq5ikm6VTpenxHjJZ7_S24Sqt5jBFkyOq2hkDva2BCO6fKcyiB7Ig5_sFtmzl344utaKvArAlYaXMeLDxp0cvvEVlo8kUCoRe2OA0kuYOYTe8iaXGsu9kVEoWqRaA7hajBBaZIgFQOWgw&state=x4xkmn9pu3j6ukrs8n
```
6. Copy the query parameter value for `?code=` and run `musicsync --spotify-code [WHATEVER YOUR CODE IS]`  
This will return the Refresh token looking like:
```text
Refresh Token: AQCYtXZ6GhKpU-eGSN2GS13d0kITqUBgqeXWgMLlFOSacOpjSQ-nB9F-Y3KOT4dGRB_aAV5bX1T8hDoKQP04hGqJkhhiVonKZgWBFVuWSqoHDEm-eiKKg0nT3qY0nOxEX_zNXg
```
7. Copy Refresh token into `musicsync.json`

## Plugin Configuration
These are the general plugin configurations: 
```json
"plugins": [
  {
    "plugin": "spotify",
    "config": {
      "clientId": "<CLIENT ID>",            // coming from the Spotify Account Setup
      "clientSecret": "<CLIENT SECRET>",    // coming from the Spotify Account Setup
      "redirectUri": "<REDIRECT URL>",      // coming from the Spotify Account Setup
      "refreshToken": "<REFRESH TOKEN>",    // Must be generated, see command line arguments
      "cachePath": "<CACHE PATH>",          // for docker file /musicsync/cache else to your convenience
      "trackThreshold": 2000                // threshold of milliseconds to define if a track is valid or not
    }
  }
]
``` 

## Connection Configuration
For the connection configuration:
```json
 "connections": [
    {
      "name": "Spotify Example",
      "in": {
        "plugin": "spotify",                                // always "spotify" for the Spotify plugin
        "config": {
          "uri": "spotify:playlist:2Cswd6UwqB0F60PBsyjygu"  // Spotify URI to download tracks from
        }
      },
      "out": {
        "plugin": "filesytem",
        "config": {}
      }
    }
]
```
