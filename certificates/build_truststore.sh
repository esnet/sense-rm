#!/bin/bash

#
# A simple script to build a truststore from PEM files in the current directory.
#

# Verify the correct number of parameters.
if [[ "$#" -ne 4 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <truststoretype> <truststorefile> <password> <directory>"
    exit 2
fi

# Verify this is a supported truststore type.
if [[ "$1" != "JKS" && "$1" != "PKCS12" ]]; then
    echo "Error: <storetype> must be one of JKS or PKCS12"
    exit 2
fi

# Remove the truststore file if it already exists.
if [[ -f "$2" ]]; then
    echo "Deleting $2"
    rm -f $2
fi

# If the target director does not exist then create it.
target="$(dirname "$2")"
if [[ ! -d "$target" ]]; then
    echo "Creating directory $target"
    mkdir -p "$target"
fi

if [[ ! -d "$4" ]]; then
    echo "ERROR: $4 not a directory"
    exit 2
fi

for file in `ls $4/*.pem`
do
  filename="${file##*/}"
  alias="${filename%.*}"

  echo "Adding $file as alias $alias"

  openssl x509 -in $file -text | grep -i "Subject: " | cut -d':' -f2-

  $JAVA_HOME/bin/keytool -importcert \
        -keystore $2 \
        -storepass "$3" \
        -alias $alias \
        -trustcacerts \
        -file $file \
        -noprompt \
        -deststoretype $1
        # -deststoretype PKCS12
        # -deststoretype JKS

done

if [[ -f "$2" ]]; then
    echo "COMPLETED: created truststore $2"
else
    echo "Error: no PEM files present in directory $4"
    exit 2
fi

