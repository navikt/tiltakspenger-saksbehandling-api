#!/bin/bash
./gradlew spotlessApply && ./gradlew build --configuration-cache "$@"
