#!/usr/bin/env bash

set -e

REVISION=`cat VERSION_TEMPLATE`

echo ${REVISION} > resources/version
mvn versions:set -DgenerateBackupPoms=false -DnewVersion=${REVISION}
