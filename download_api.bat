@echo off
echo Downloading Spigot 1.8.8 API...

REM Create misc directory if it doesn't exist
if not exist "..\misc" mkdir "..\misc"

REM Download the API using PowerShell
powershell -Command "& {Invoke-WebRequest -Uri 'https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/spigot-api-1.8.8-R0.1-SNAPSHOT.jar' -OutFile '..\misc\spigot-api-1.8.8.jar'}"

if exist "..\misc\spigot-api-1.8.8.jar" (
    echo API downloaded successfully!
    echo Now running compilation...
    call compile.bat
) else (
    echo Download failed! Please download manually from:
    echo https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/
    pause
)
