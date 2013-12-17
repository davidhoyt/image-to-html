#!/bin/sh

CURR_DIR=`pwd`
TOP=$(dirname $0)/.
ROOT=$( (cd "$TOP" && pwd) )

cd "$ROOT"



java -jar image-to-html-assembly-0.0.1-SNAPSHOT.jar $@



cd "$CURR_DIR"
