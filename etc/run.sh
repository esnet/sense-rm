#! /bin/bash -x

export HOME=.
export TRUSTSTORE=./config/truststore.jks
export KEYSTORE=./config/keystore.jks
export PASSWORD="changeit"

/usr/bin/java \
        -Xmx1024m -Djava.net.preferIPv4Stack=true  \
        -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
        -Dbasedir="$HOME" \
        -Djavax.net.ssl.trustStore=$TRUSTSTORE \
        -Djavax.net.ssl.trustStorePassword=$PASSWORD \
        -Djavax.net.ssl.keyStore=$KEYSTORE \
        -Djavax.net.ssl.keyStorePassword=$PASSWORD \
	      -Dlogging.config="file:$HOME/config/logback.xml" \
	      -XX:+StartAttachListener \
        -jar "$HOME/sense-n-rm.jar" \
	--spring.config.location="file:$HOME/config/" 

