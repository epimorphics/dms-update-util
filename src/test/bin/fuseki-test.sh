#!/bin/bash
# Start a fuseki server, only for test/debug scripts not for real use

readonly LOC=/home/der/tmp/dms-update

cd $LOC
nohup fuseki-server --mgtPort=3131 --loc=DS-DB /ds > fuseki.log &
