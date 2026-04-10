#!/bin/bash
./gradlew clean spotlessApply && ./gradlew build installDist "$@"
