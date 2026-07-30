#!/bin/bash
./gradlew spotlessApply build --configuration-cache "$@"
