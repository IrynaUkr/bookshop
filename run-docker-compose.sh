#!/usr/bin/env bash
if [[ $(uname -p) == 'arm' ]]
then
  dockerComposeFile="${BASH_SOURCE%/*}/docker-compose.yml"
else
  dockerComposeFile="${BASH_SOURCE%/*}/docker-compose.yml"
fi

#registry=registry.fluidcloud.bskyb.com/cimdb
#username=svc-app-vclwsro
#password=('+=)7!Y%B!G(s"Q')
#
#
#echo "NOTE: stopping running containers"
#docker-compose -f $dockerComposeFile down --remove-orphans
#
#echo "NOTE: Logging in to $registry as $username"
#echo "${password[0]}" | docker login $registry -u $username --password-stdin
#
#echo "NOTE: running docker compose up"
#
#docker-compose -f $dockerComposeFile up -d
#
#echo "NOTE: Checking kafka container is ready"
#
#COUNT=0
#while [[ $(docker logs docker_kafka_1 2>&1 | grep -c "started") -eq 0 ]]; do
#  if [[ ${COUNT} -gt 30 ]]; then exit 1; fi
#  echo "NOTE: Waiting kafka to start..."
#  COUNT=$((COUNT + 1))
#  sleep 3
#done
#
#echo "NOTE: creating topics on kafka"
#
#docker exec docker_kafka_1 /apps/was/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 2 --topic KAFKA_LOCAL_CARE_ORCHESTRATION_EVENTS
#docker exec docker_kafka_1 /apps/was/kafka/bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 2 --topic KAFKA_LOCAL_CARE_ORCHESTRATION_EVENTS_DLT
#
#echo "NOTE: logging out of $registry"
#docker logout $registry
