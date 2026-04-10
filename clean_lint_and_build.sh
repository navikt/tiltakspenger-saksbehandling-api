#!/bin/bash
./gradlew clean spotlessApply --no-build-cache && ./gradlew build --no-build-cache "$@"
