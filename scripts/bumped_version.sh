#!/usr/bin/env bash

# Returns the version with the patch number bumped up

set -e

REVISION=`cat VERSION_TEMPLATE`
REGEX="([0-9]\.[0-9]\.)([0-9])+$"

if [[ $REVISION =~ $REGEX ]]
then
  MAJOR_MINOR="${BASH_REMATCH[1]}"
  PATCH="${BASH_REMATCH[2]}"
else
  MAJOR_MINOR="0.1."
  PATCH="0"
fi

echo ${MAJOR_MINOR}$((PATCH+1))
