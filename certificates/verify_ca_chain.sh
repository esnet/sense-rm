#!/bin/bash -v

#
# A simple script to validate certificate chains are valid.
#

for file in `ls *.pem`
do
  if [ "$file" != "ca-chain.pem" ]; then
	echo "==== Processing file $file ===="
	openssl verify -verbose -CAfile ca-chain.pem $file 
  fi
done

