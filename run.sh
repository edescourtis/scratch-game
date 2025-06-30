#!/bin/bash

# Set JAVA_HOME to avoid asdf issues
#export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

# Run the scratch game with provided arguments
java -jar target/scratch-game.jar "$@"

