#!/bin/bash

#
# A simple script to build the Java Keystore.
#

# Verify the correct number of parameters.
if [[ "$#" -ne 7 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <keystoretype> <keystorefile> <passwd> <name> <keyfile> <certfile> <ca-file>"
    exit 2
fi

# Verify this is a supported keystore type.
if [[ "$1" != "JKS" && "$1" != "PKCS12" ]]; then
    echo "Error: <storetype> must be one of JKS or PKCS12"
    exit 2
fi

# Remove the keystore file if it already exists.
if [[ -f "$2" ]]; then
    echo "Deleting $2"
    rm -f $2
fi

# Check if a vaild key file was specified.
if [[ ! -f "$5" ]]; then
    echo "ERROR: $5 does not exist"
    exit 2
fi

# Check if a vaild certificate file was specified.
if [[ ! -f "$6" ]]; then
    echo "ERROR: $6 does not exist"
    exit 2
fi

# Check if a vaild CA file was specified.
if [[ ! -f "$7" ]]; then
    echo "ERROR: $7 does not exist"
    exit 2
fi

# We need to generate a temporary file name for this process.
tfile=$(mktemp /tmp/foo.XXXXXXXXX)

# Create a temporary pkcs12 keystore.
openssl pkcs12 -export \
    -out $tfile \
    -inkey $5 \
    -in $6 \
    -certfile $7 \
    -name $4 \
    -passout pass:"$3"

if [[ "$1" == "JKS" ]]; then
  # Convert the temporary pkcs12 keystore to JKS.
  $JAVA_HOME/bin/keytool -v -importkeystore \
      -noprompt \
      -srcstorepass "$3" \
      -deststorepass "$3"  \
      -srckeystore $tfile \
      -srcstoretype PKCS12 \
      -deststoretype JKS \
      -destkeystore $2
  rm -f $tfile
else
    mv -f $tfile $2
fi

#
# We are done
#
if [[ -f "$2" ]]; then
    echo "COMPLETED: created keystore $2"
else
    echo "Error: failed to create keystore $2"
    exit 2
fi

