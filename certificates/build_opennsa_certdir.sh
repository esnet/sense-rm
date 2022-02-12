#!/bin/bash

#
# A simple script to build a truststore from PEM files in the current directory.
#

# Verify the correct number of parameters.
if [[ "$#" -ne 2 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <source PEM dir> <dest dir>"
    exit 2
fi

# Verify this is a supported truststore type.
if [[ ! -d "$1" ]]; then
    echo "Error: <source PEM dir> must be a directory containing PEM files"
    echo "Usage: $0 <source PEM dir> <dest dir>"
    exit 2
fi


# If the target director does not exist then create it.
target="$(dirname "$2")"
if [[ ! -d "$target" ]]; then
    echo "Creating directory $target"
    mkdir -p "$target"
fi

for file in `ls $1/*.pem`
do
  filename="${file##*/}"
  alias="${filename%.*}"

  echo "Copying $file to $target/$alias.0"

  cp "$file" "$target/$alias.0"
done
