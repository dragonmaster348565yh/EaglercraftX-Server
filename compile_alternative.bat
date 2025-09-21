@echo off
echo Trying alternative compilation methods...

REM Method 1: Try using the server JAR directly
echo Method 1: Using server.jar...
javac -cp "..\server\server.jar" -d target\classes src\main\java\com\economy\*.java
if %ERRORLEVEL% EQU 0 (
    echo SUCCESS with server.jar!
    goto :create_jar
)

REM Method 2: Try using paper JAR
echo Method 2: Using paper-1.8.8.jar...
javac -cp "..\misc\paper-1.8.8.jar" -d target\classes src\main\java\com\economy\*.java
if %ERRORLEVEL% EQU 0 (
    echo SUCCESS with paper JAR!
    goto :create_jar
)

REM Method 3: Try using multiple JARs
echo Method 3: Using multiple JARs...
javac -cp "..\server\server.jar;..\misc\paper-1.8.8.jar" -d target\classes src\main\java\com\economy\*.java
if %ERRORLEVEL% EQU 0 (
    echo SUCCESS with multiple JARs!
    goto :create_jar
)

echo All compilation methods failed!
echo You need to download the Spigot 1.8.8 API JAR file.
echo Download from: https://hub.spigotmc.org/nexus/content/repositories/snapshots/org/spigotmc/spigot-api/1.8.8-R0.1-SNAPSHOT/
echo Place it in the misc folder and update the compile.bat file.
pause
exit /b 1

:create_jar
echo Creating JAR file...
cd target\classes
jar cf ..\EconomyPlugin-1.0.0.jar com\economy\*.class
cd ..\..

REM Copy resources
cd target\classes
xcopy /E /I ..\..\src\main\resources\* .
cd ..\..

REM Update JAR with resources
cd target\classes
jar uf ..\EconomyPlugin-1.0.0.jar *.yml
cd ..\..

echo Plugin compiled successfully: target\EconomyPlugin-1.0.0.jar
echo Copy this JAR to your server's plugins folder!
pause
