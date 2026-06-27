#!/bin/bash
#
# Pre-commit-hook (spotless): VERIFISERER formatering – endrer ALDRI koden din.
#
# Spotless skal formatere når du BYGGER (./lint_and_build.sh kjører
# `gradlew spotlessApply build`), ikke når du committer. Derfor kjører denne
# hooken kun `spotlessCheck` og feiler hvis noe ikke er riktig formatert.
# Den gjør INGEN `git add`, og kan derfor aldri dra med seg endringer du ikke
# har staget selv.

set -uo pipefail

echo "*********************************************************"
echo "Pre-commit: verifiserer formatering (spotlessCheck)..."
echo "*********************************************************"

# Sjekk bare hvis noe i det hele tatt er staget.
stagedFiles=$(git diff --staged --name-only --diff-filter=ACM)
if [ -z "$stagedFiles" ]; then
    exit 0
fi

# Guard: legg vekk alt som IKKE er staget, slik at spotless kun ser den stagede
# tilstanden – ikke unstaged endringer i de samme filene. Sikrer at sjekken
# gjenspeiler nøyaktig det som committes, og at ingenting uventet kan dras med
# selv om noen senere skulle bytte spotlessCheck -> spotlessApply her.
needStash=0
if ! git diff --quiet || git ls-files --others --exclude-standard | grep -q .; then
    git stash push --keep-index --include-untracked --quiet --message "pre-commit-spotless"
    needStash=1
fi

./gradlew spotlessCheck
status=$?

# Gjenopprett arbeidskopien uansett utfall.
if [ "$needStash" = 1 ]; then
    git stash pop --quiet
fi

if [ "$status" != 0 ]; then
    echo "*********************************************************"
    echo 1>&2 "Spotless fant formateringsbrudd i det du prøver å committe."
    echo "Kjør './gradlew spotlessApply' (eller ./lint_and_build.sh) og"
    echo "stage endringene selv før du committer på nytt."
    echo "*********************************************************"
    exit 1
fi

echo "Formatering OK."
exit 0
