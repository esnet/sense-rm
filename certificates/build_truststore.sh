#!/bin/bash

if [[ "$#" -ne 2 ]]; then
    echo "ERROR: Illegal number of parameters"
    echo "Usage: $0 <truststorefile> <password>"
    exit 2
fi

if [[ -f "$1" ]]; then
    echo "ERROR: $1 exist"
    exit 2
fi

for file in `ls *.pem`
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

