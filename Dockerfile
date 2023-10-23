FROM maven:3-openjdk-17 AS MAVEN_BUILD
MAINTAINER John MacAuley <macauley@es.net>
ENV BUILD_HOME /home/sense
ENV HOME /sense-rm
WORKDIR $HOME
COPY . .
RUN mvn clean install -Dmaven.test.skip=true -Ddocker.nocache

FROM openjdk:17

ENV HOME /sense-rm
ENV TRUSTSTORE $HOME/pkcs/truststore.p12
ENV KEYSTORE $HOME/pkcs/keystore.p12
ENV STORETYPE "PKCS12"
ENV PASSWORD "changeit"
USER 1000:1000

WORKDIR $HOME
COPY --from=MAVEN_BUILD $HOME/rm/target/rm-0.1.0.jar ./sense-rm.jar
COPY --from=MAVEN_BUILD $HOME/config ./config

EXPOSE 8080/tcp
CMD java \
        -Xmx1024m -Djava.net.preferIPv4Stack=true  \
        -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \
        -Dbasedir="$HOME" \
        -logback.configurationFile="file:$HOME/config/logback.xml" \
        -Djavax.net.ssl.trustStore=$TRUSTSTORE \
        -Djavax.net.ssl.trustStorePassword=$PASSWORD \
        -Djavax.net.ssl.trustStoreType=$STORETYPE \
        -Djavax.net.ssl.keyStore=$KEYSTORE \
        -Djavax.net.ssl.keyStorePassword=$PASSWORD \
        -Djavax.net.ssl.keyStoreType=$STORETYPE \
        -XX:+StartAttachListener \
        -jar "$HOME/sense-rm.jar" \
        --spring.config.location="file:$HOME/config/"
