#!/bin/bash

set -e

if [ -z "$1" ]; then
  echo "Usage: $0 <new-version>"
  exit 1
fi

NEW_VERSION=$1

mvn versions:set -DnewVersion="$NEW_VERSION" -DprocessAllModules

echo "Version updated to $NEW_VERSION for parent and all submodules."