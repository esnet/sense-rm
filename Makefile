# These files need to be under the sense-rm build context so they
# are available under the docker mount.
KEYFILE="certificates/server.key"
CERTFILE="certificates/server.cer"
CAFILE="certificates/server.crt"

all:	build package certificates

.PHONY: build package certificates clean
build:
	docker run -it --rm --name sense-build \
		-v "$(PWD)":/usr/src/mymaven \
		-v "$(HOME)/.m2":/root/.m2  \
		-w /usr/src/mymaven \
		maven:3.9.3-eclipse-temurin-11 mvn clean install

package:
	docker run -it --rm --name sense-package \
		-v "$(PWD)":/usr/src/mymaven \
		-v "$(HOME)/.m2":/root/.m2  \
		-w /usr/src/mymaven \
		maven:3.9.3-eclipse-temurin-11 mvn antrun:run@package-runtime

certificates:
	docker run -it --rm --name sense-certs \
		-v "$(PWD)":/usr/src/mymaven \
		-v "$(HOME)/.m2":/root/.m2  \
		-w /usr/src/mymaven \
		maven:3.9.3-eclipse-temurin-11 mvn antrun:run@build-keystores \
		-Dhost.key=$(KEYFILE) -Dhost.cer=$(CERTFILE) -Dhost.ca=$(CAFILE)

docker:

clean:
	docker run -it --rm --name sense-clean \
                -v "$(PWD)":/usr/src/mymaven \
                -v "$(HOME)/.m2":/root/.m2  \
                -w /usr/src/mymaven \
                maven:3.9.3-eclipse-temurin-11 mvn clean


