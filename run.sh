#! /bin/bash -x

export USER=sense
export GROUP=sense
export HOME=.
export TRUSTSTORE=./config/truststore.nsi-aggr-west.jks
export KEYSTORE=./config/keystore.nsi-aggr-west.jks
export PASSWORD="changeit"

/usr/bin/java \
        -Xmx1024m -Djava.net.preferIPv4Stack=true  \
        -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
        -Dbasedir="$HOME" \
        -Djavax.net.ssl.trustStore=$TRUSTSTORE \
        -Djavax.net.ssl.trustStorePassword=$PASSWORD \
        -Djavax.net.ssl.keyStore=$KEYSTORE \
        -Djavax.net.ssl.keyStorePassword=$PASSWORD \
        -Dlogging.config="file:config/logback.xml" \
	-XX:+StartAttachListener \
        -jar "$HOME/rm-0.1.0.jar" 

