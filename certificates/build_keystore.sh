#!/bin/bash 

if [[ "$#" -ne 5 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <keystorefile> <passwd> <keyfile> <certfile> <ca-file>"
    exit 2
fi

if [[ -f "$1" ]]; then
    echo "ERROR: $1 already exists"
    exit 2
fi

if [[ ! -f "$3" ]]; then
    echo "ERROR: $2 does not exist"
    exit 2
fi

if [[ ! -f "$4" ]]; then
    echo "ERROR: $3 does not exist"
    exit 2
fi

if [[ ! -f "$5" ]]; then
    echo "ERROR: $4 does not exist"
    exit 2
fi

#openssl pkcs12 -export \
    #-out $1 \
    #-inkey $3 \
    #-in $4 \
    #-certfile $5 \
    #-passout pass:$2

tfile=$(mktemp /tmp/foo.XXXXXXXXX)

openssl pkcs12 -export \
    -out $tfile \
    -inkey $3 \
    -in $4 \
    -certfile $5 \
    -passout pass:"$2"

keytool -v -importkeystore \
    -noprompt \
    -srcstorepass "$2" \
    -deststorepass "$2"  \
    -srckeystore $tfile \
    -srcstoretype PKCS12 \
    -deststoretype JKS \
    -destkeystore $1

rm -f $tfile

