#!/bin/bash

#
# A simple script to build a list of client DN based on PEM file containing "0 s:" entry.
#

if [[ "$#" -ne 1 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <directory>"
    exit 2
fi

if [[ ! -d "$1" ]]; then
    echo "ERROR: $1 not a directory"
    exit 2
fi

for file in `ls $1/*.pem`
do
  cat $file | grep "0 s:" | cut -f2- -d':'
done

