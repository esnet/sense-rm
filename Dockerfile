FROM maven:3.6.3-jdk-8 AS MAVEN_TOOL_CHAIN
MAINTAINER John MacAuley <macauley@es.net>

Volume [ "maven-repo:/root/.m2" ]

# speed up Maven JVM a bit
# ENV MAVEN_OPTS="-XX:+TieredCompilation -XX:TieredStopAtLevel=1"

RUN mkdir -p /usr/src/sense
ADD . /usr/src/sense/
WORKDIR /usr/src/sense/
RUN ls -las
RUN mvn clean install -DskipTests=true
RUN mvn maven-antrun-plugin:run@package-runtime

#WORKDIR /usr/src/sense
#ONBUILD ADD . /usr/src/app
#ONBUILD RUN mvn install

#COPY * /usr/
#RUN mvn clean install 
# -DskipTests=true

#FROM openjdk:8-jre-alpine

#COPY --from=MAVEN_TOOL_CHAIN target/dist/* /usr/bin/sense

#$docker run -it --rm --name sense-nsi-rm -v "$PWD":/usr/src/app -v "$HOME"/.m2:/root/.m2 -w /usr/src/app maven:3.3-jdk-8 mvn clean install
