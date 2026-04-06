#!/bin/bash
./gradlew clean spotlessApply build --no-build-cache "$@"
