@echo off
REM *******************************************
REM Set classpath in Windows environment
REM *******************************************

break=ON 

REM Get package directory from command parameter or use current directory
SET pkgdir=%1
IF X%pkgdir%.\==X.\ SET pkgdir=%CD%

REM Go to package dir and get classpath from maven dump
SET outfile=%TMP%.\classpath.txt

PUSHD %pkgdir%
CALL mvn dependency:build-classpath -Dmdep.outputFile=%outfile%
POPD

REM Add default dirs to outfile
ECHO ;%pkgdir%\target\classes\;%pkgdir%\src\main\groovy\;%pkgdir%\src\main\resources\ >> %outfile%

SET /P CLASSPATH= < %outfile%
