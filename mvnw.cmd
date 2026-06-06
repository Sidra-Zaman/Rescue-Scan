@echo off
set MAVEN_VERSION=3.9.9
set MAVEN_DIST_DIR=%USERPROFILE%\.m2\wrapper\dists
set MAVEN_HOME=%MAVEN_DIST_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_BIN=%MAVEN_HOME%\apache-maven-%MAVEN_VERSION%\bin\mvn.cmd

if not defined JAVA_HOME (
    for %%I in (java.exe) do set FOUND_JAVA=%%~$PATH:I
    if not defined FOUND_JAVA (
        echo ERROR: Java not found. Please install Java JDK 17 or higher.
        exit /b 1
    )
)

if not exist "%MAVEN_BIN%" (
    echo Installing Maven %MAVEN_VERSION%...

    powershell -NoProfile -File "%~dp0.mvn\wrapper\download-maven.ps1" "%MAVEN_VERSION%" "%MAVEN_HOME%"
    if errorlevel 1 (
        echo Failed to install Maven.
        exit /b 1
    )

    if not exist "%MAVEN_BIN%" (
        echo Maven installation failed.
        exit /b 1
    )
)

call "%MAVEN_BIN%" %*
exit /b %errorlevel%
