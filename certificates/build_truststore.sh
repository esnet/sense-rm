#!/bin/bash

#
# A simple script to build a Java truststore from PEM files in the current directory.
#

if [[ "$#" -ne 3 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <truststorefile> <password> <directory>"
    exit 2
fi

if [[ -f "$1" ]]; then
    echo "Deleting $1"
    rm -f $1
fi

target="$(dirname "$1")"
if [[ ! -d "$target" ]]; then
    echo "Creating directory $target"
    mkdir -p "$target"
fi

if [[ ! -d "$3" ]]; then
    echo "ERROR: $3 not a directory"
    exit 2
fi

for file in `ls $3/*.pem`
do
  filename="${file##*/}"
  alias="${filename%.*}"

  echo "Adding $file as alias $alias"

  openssl x509 -in $file -text | grep -i "Subject: " | cut -d':' -f2- 

  $JAVA_HOME/bin/keytool -importcert \
        -keystore $1 \
        -storepass "$2" \
        -alias $alias \
        -trustcacerts \
        -file $file \
        -noprompt \
        -deststoretype JKS
        #-deststoretype PKCS12

done

echo "COMPLETED: created truststore $1"

