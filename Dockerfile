FROM maven:3-openjdk-17 AS MAVEN_BUILD
MAINTAINER John MacAuley <macauley@es.net>
ENV BUILD_HOME /home/sense
ENV HOME /sense-rm
WORKDIR $HOME
COPY . .
RUN mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

FROM openjdk:17

ENV HOME /sense-rm
ENV LOGBACK "file:$HOME/config/logback.xml"
ENV CONFIG_DIR "file:$HOME/config/"

# SSL options.
ENV TRUSTSTORE $HOME/pkcs/truststore.p12
ENV KEYSTORE $HOME/pkcs/keystore.p12
ENV STORETYPE "PKCS12"
ENV PASSWORD "changeit"

# We add these SSL options to a separate variable so they can be overridden if needed.
ENV SSL_OPTS "-Djavax.net.ssl.trustStore=$TRUSTSTORE -Djavax.net.ssl.trustStorePassword=$PASSWORD \
               -Djavax.net.ssl.trustStoreType=$STORETYPE -Djavax.net.ssl.keyStore=$KEYSTORE \
               -Djavax.net.ssl.keyStorePassword=$PASSWORD -Djavax.net.ssl.keyStoreType=$STORETYPE"
USER 1000:1000

WORKDIR $HOME
COPY --from=MAVEN_BUILD $HOME/rm/target/rm-0.1.0.jar ./sense-rm.jar
COPY --from=MAVEN_BUILD $HOME/config ./config

EXPOSE 8080/tcp
CMD java \
        -Xmx1024m -Djava.net.preferIPv4Stack=true  \
        -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
        -Dbasedir="$HOME" \
        -Dlogback.configurationFile=$LOGBACK \
        -Dlogging.config=$LOGBACK \
        $SSL_OPTS \
        -XX:+StartAttachListener \
        -jar "$HOME/sense-rm.jar" \
        --spring.config.location=$CONFIG_DIR
