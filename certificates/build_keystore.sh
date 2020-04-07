#!/bin/bash 

#
# A simple script to build the Java Keystore.
#

# Verify the correct number of parameters.
if [[ "$#" -ne 5 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <keystorefile> <passwd> <keyfile> <certfile> <ca-file>"
    exit 2
fi

# Make sure we do not overwrite the target file if it exists.
if [[ -f "$1" ]]; then
    echo "ERROR: $1 already exists"
    exit 2
fi

# Check if a vaild key file was specified.
if [[ ! -f "$3" ]]; then
    echo "ERROR: $2 does not exist"
    exit 2
fi

# Check if a vaild certificate file was specified.
if [[ ! -f "$4" ]]; then
    echo "ERROR: $3 does not exist"
    exit 2
fi

# Check if a vaild CA file was specified.
if [[ ! -f "$5" ]]; then
    echo "ERROR: $5 does not exist"
    exit 2
fi

#openssl pkcs12 -export \
    #-out $1 \
    #-inkey $3 \
    #-in $4 \
    #-certfile $5 \
    #-passout pass:$2

# We need to generate a temporary file name for this process.
tfile=$(mktemp /tmp/foo.XXXXXXXXX)

# Create a temporary pkcs12 keystore. 
openssl pkcs12 -export \
    -out $tfile \
    -inkey $3 \
    -in $4 \
    -certfile $5 \
    -passout pass:"$2"

# Conver the temporary pkcs12 keystore to JKS.
$JAVA_HOME/bin/keytool -v -importkeystore \
    -noprompt \
    -srcstorepass "$2" \
    -deststorepass "$2"  \
    -srckeystore $tfile \
    -srcstoretype PKCS12 \
    -deststoretype JKS \
    -destkeystore $1

# Cleanup.
rm -f $tfile

#
# We are done
#
