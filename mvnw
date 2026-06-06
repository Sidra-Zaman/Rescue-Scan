#!/bin/sh
# Maven Wrapper script

M2_HOME="$(cd "$(dirname "$0")" && pwd)"
WRAPPER_JAR="$M2_HOME/.mvn/wrapper/maven-wrapper.jar"
WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if command -v java >/dev/null 2>&1; then
    JAVA_EXE=java
elif [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_EXE="$JAVA_HOME/bin/java"
else
    echo "ERROR: java not found. Please install Java JDK 17 or higher."
    exit 1
fi

exec "$JAVA_EXE" -cp "$WRAPPER_JAR" -Dmaven.multiModuleProjectDirectory="$M2_HOME" $WRAPPER_LAUNCHER "$@"
