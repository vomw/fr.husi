#!/usr/bin/env bash

set -euo pipefail

if [ "$#" -ne 1 ] || [ "$1" != "--android" ]; then
  echo "Usage: ./run lib core --android" >&2
  exit 1
fi

source buildScript/init/env.sh
cd libcore
./build.sh --android
