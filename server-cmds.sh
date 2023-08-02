#!/usr/bin/env bash

export IMAGE=$1
export USER=$2
export PASS=$3
export DOCKER_REPO=$4
echo $PASS | docker login -u $USER --password-stdin
docker-compose -f docker-compose.yaml up --detach
echo "success"
