#!/bin/bash
echo "*********************************************************"
echo "Running git pre-commit hook. Running Flyway Migration Names Check... "
echo "*********************************************************"
./gradlew checkFlywayMigrationNames
flywaystatus=$?
if [ "$flywaystatus" = 0 ] ; then
    echo "Flyway migration names check passed."
else
    echo "*********************************************************"
    echo "       ********************************************      "
    echo 1>&2 "Flyway migration names check failed."
    echo "Please fix the migration names before trying to commit again."
    echo "       ********************************************      "
    echo "*********************************************************"
    exit 1
fi
