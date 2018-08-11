#!/usr/bin/env bash

# Returns the number of commits made since the v0.0 tag

set -e

NEW_REVISION=`scripts/bumped_version.sh`

echo $NEW_REVISION > VERSION_TEMPLATE
