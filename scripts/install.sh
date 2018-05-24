#!/usr/bin/env bash

set -e

BIN_PATH="/usr/local/bin"
UMLAUT_BIN="${BIN_PATH}/umlaut"

API_ENDPOINT="https://api.github.com/repos/workco/umlaut/releases/latest"

DOWNLOAD_URL=`curl -s ${API_ENDPOINT} | grep 'browser_' | cut -d\" -f4`

echo "Downloading ${DOWNLOAD_URL} to ${BIN_PATH}..."

curl -sL $DOWNLOAD_URL -o $UMLAUT_BIN
chmod +x $UMLAUT_BIN

echo "Done."
