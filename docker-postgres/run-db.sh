#!/bin/bash

echo "Building Docker image..."
docker build -t my-postgres .

echo "Running PostgreSQL container..."
docker run -d --name my-db-container -p 5433:5432 \
  -e POSTGRES_DB=bookshop \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  my-postgres

echo "PostgreSQL is now running and should be accessible on localhost:5433"

