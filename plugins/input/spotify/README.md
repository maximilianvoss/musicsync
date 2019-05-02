# Spotify Input Plugin
This plugin is able to download any song and playlist from Spotify. All downloaded data will be cached.

**Note: This plugin requires Spotify Premium**

## Configuration
These are the necessary properites for the Spotify communication and for caching the audio files. 
```properties
# Setup to run with Spotify
spotify.clientId=<clientId>
spotify.clientSecret=<clientSecret>
spotify.redirectUri=<redirectUri>
spotify.refreshToken=<refreshToken>
spotify.cachePath=<pathForCaching>
``` 

An initial App setting can be defined at https://developer.spotify.com. You will set and get `spotify.clientId`, `spotify.clientSecret` and `spotify.redirectUri` there.  
`spotify.redirectUri` can be selected randomly by you, it must be a valid URI but nothing has to be hosted there. You have to use the same here as in the ClientId

## Command line arguments
Fill out `spotify.clientId`, `spotify.clientSecret` and `spotify.redirectUri`.  

```bash
musicsync --spotify-apicode
``` 
The call will result in some output like
```text
URI: https://accounts.spotify.com:443/authorize?client_id=e26995898f9f4e4d9bf45b3001e6701d&response_type=code&redirect_uri=https%3A%2F%2Fmaximilian.voss.rocks%2Fspotify-redirect&state=x4xkmn9pu3j6ukrs8n&scope=playlist-read-private%2Cplaylist-read-collaborative%2Cplaylist-modify-private%2Cplaylist-modify-public&show_dialog=true
```

Open this URI in your web-browser, enter your credentials, agree and see that you are going to be redirected to `spotify.redirectUri`.  
The URI has a  parameter `data` appended. Take the value (after the data parameter is a state parameter, make sure you don't copy this as well).
Take the `data` parameter and execute:
```bash
musicsync --spotify-code "[DATA-PARAMETER]"
```
The call will result in some output like
```text
Refresh Token: AQBStXZ6GhKpU-eGSN2GS13d0kITqUBgqeXWgMLlFOSacOpjSQ-nB9F-Y3KOT4dGRB_aAV5bX1T8hDoKQP04hGqJkhhiVonKZgWBFVuWSqoHDEm-eiKKg0nT3qY0nOxEX_zNXg
```
Take the refresh token and fill `spotify.refreshToken` in the properties file.

As for `spotify.cachePath`, select any temp directory where you want to cache the Spotify songs.

## Input Argument
As input argument takes this plugin a Spotify URI, starting always with **spotify:**  
Example:
```
spotify:playlist:2Cswd6UwqB0F60PBsyjyga
```