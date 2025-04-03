#!/usr/bin/env bash

set -e  # Exit immediately if a command exits with a non-zero status.

dockerComposeFile="${BASH_SOURCE%/*}/docker-compose.yml"

echo "Starting Docker Compose using file: $dockerComposeFile"

docker-compose down  # Stop and remove existing containers

docker-compose up --build -d  # Build and start containers in detached mode

echo "Docker Compose setup started successfully."
