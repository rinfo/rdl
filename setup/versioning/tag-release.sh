#!/bin/bash

TAG=$1
REPO_BASE=https://source.verva.se/svn/rinfo
REPO_BASE_TAGS=$REPO_BASE/tags
TAG_MESSAGE="'Tagging $TAG.'"
TAG_COMMAND="svn copy $REPO_BASE/trunk $REPO_BASE_TAGS/$TAG -m $TAG_MESSAGE"

TAG_FORMAT="^[a-z-_0-9\.]\+-[0-9]\+\(\.[0-9]\+\)\?$"

if [[ $(echo -n $TAG | grep ' ') != "" ]]; then
    echo "Error - you cannot use spaces in tag name ($TAG)."
    exit 1
elif [[ $TAG == "" ]]; then
    echo "Usage: $0 <tag-name>"
    echo "Available tags in $REPO_BASE_TAGS:"
    svn ls $REPO_BASE_TAGS
    echo
    exit 1
elif [[ $(echo -n $TAG | grep $TAG_FORMAT) == "" ]]; then
    echo "Error - malformed tag '$TAG'; must match '$TAG_FORMAT'."
    exit 1
fi

echo "Running: $TAG_COMMAND"
echo $TAG_COMMAND | bash
echo "Done."

