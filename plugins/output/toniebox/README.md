# Toniebox Output Plugin
This plugin is capable of uploading tracks to your Creative Tonie.

## Configuration
```properties
# Toniebox credentials
toniebox.username=<username>
toniebox.password=<password>
```

`<username>` and `<password>` are your Toniebox credentials.

## Command line arguments

--toniebox-username to override the property's username.  
--toniebox-password to override the property's password. 


## Output Argument
The output argument has following schema: toniebox:\[householdId\]:\[creativeTonieId\].  
For example:  
`toniebox:b2ca3ceb-cc94-42fc-ab5f-54af67d074c8:F969A20D500304E2`

The easiest way to get the values is looking at the URL when you open one of your creative tonies.

https://meine.tonies.de/tonies/b2ca3ceb-cc94-42fc-ab5f-54af67d074c8/F969A20D500304E2

As you can see, the path after /tonies/ is starting with the household and then ending with the creativeTonieId.
