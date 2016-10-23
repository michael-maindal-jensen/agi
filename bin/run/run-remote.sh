#!/bin/bash


if [ "$1" == "-h" -o "$1" == "--help" -o "$1" == "" ]; then
  echo "Usage: `basename $0` HOST KEY_FILE (default = ~/.ssh/ecs-key.pem)"
  exit 0
fi

host=$1
keyfile=${2:-$HOME/.ssh/ecs-key.pem}


echo "Using keyfile = " $keyfile
echo "Using host = " $host


# WARNING: hardcoded path on remote machine in shell commands below (to be run on remote host via ssh)

ssh -i $keyfile ec2-user@${host} 'bash -s' <<'ENDSSH' 
	export VARIABLES_FILE="variables-ec2.sh"
	cd /home/ec2-user/agief-project/agi/bin/node_coordinator
	./run-in-docker.sh
ENDSSH


exit
ssh -i ~/.ssh/nextpair.pem ec2-user@52.63.242.158 "bash -c \"export VARIABLES_FILE=\"variables-ec2.sh\" && cd /home/ec2-user/agief-project/agi/bin/node_coordinator && ./run-in-docker.sh\""