#!/usr/bin/env bash

set -ex

target=$(dirname $(realpath $0))/../target
uberjar=$(ls ${target}/scala-3.*/syncere-assembly*.jar | grep -e '[0-9]\.jar' | sort |tail -1)

exec java -jar $uberjar "$@"
