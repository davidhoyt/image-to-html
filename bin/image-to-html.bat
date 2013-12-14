@echo off

REM **********************************************************************
REM * Converts images to HTML
REM **********************************************************************

set CURR=%CD%
set DIR=%~dp0.

:start
cd /d "%DIR%"



java -jar image-to-html-0.0.1-SNAPSHOT.jar



goto exit

:exit
cd /d "%CURR%"