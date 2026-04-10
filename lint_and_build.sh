#!/bin/bash
./gradlew spotlessApply && ./gradlew build "$@"
