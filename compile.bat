@echo off
echo Compiling EconomyPlugin...

REM Create target directory
if not exist "target\classes" mkdir target\classes

REM Compile Java files
javac -cp "..\misc\spigot-api-1.8.8.jar" -d target\classes src\main\java\com\economy\*.java

if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Compilation successful!

REM Create JAR file
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
pause
