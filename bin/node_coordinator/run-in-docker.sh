#!/bin/bash

########################################################################################################
# Usage: use this script to build or run in a docker container.
# 
# BUILD
# To build, use the command 'build' and then any paramters that you want passed to build.
# This will use bin/node_coordinator/build.sh
# e.g. ./run-in-docker.sh build [params].       (at the moment, no params are necessary)
# 
# RUN
# To run, just pass parameters. This will use /bin/node_coordinator/run.sh
# e.g. ./run-in-docker.sh [params]
# 
########################################################################################################

variables_file=${VARIABLES_FILE:-"variables.sh"}
echo "Using variables file = \"$variables_file\""
source $(dirname $0)/../$variables_file

set -e                                  # stops the execution of a script if a command or pipeline has an error 
cd "$(dirname $BASH_SOURCE)"            # change to directory of the script

maven_cache_repo="$HOME/.m2/repository"
myname="$(basename $BASH_SOURCE)"

if [ "$1" = "build" ]; then
        cmd="./build.sh"
        shift
        args="$@"
else
        run_script="run.sh"
        
        # Check if project is built
        if [ ! -f "$run_script" ]; then
                echo "ERROR File not found: $run_script"
                # echo "ERROR Did you forget to './$myname mvn package'?"
                exit 1
        fi
        
        cmd="./$run_script"
        args="$@"
fi

mkdir -p "$maven_cache_repo"

if [ "`tty`" != "not a tty" ]; then
        switch="-it"
else
        switch="-i"
fi

set -x


# should use the script /docker/run.sh,  but thinking of deprecating, not worth maintaining another script that isn't that useful
docker run "$switch" \
        -w /root/dev/agi/bin/node_coordinator \
        -e VARIABLES_FILE="variables-docker.sh" \
        -v $AGI_HOME:/root/dev/agi \
        -v $AGI_RUN_HOME:/root/dev/run \
        -v "${maven_cache_repo}:/root/.m2/repository" \
        -p 8491:8491 -p 5432:5432 \
        gkowadlo/agief:latest $cmd $args