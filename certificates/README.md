# SENSE-NSI-RM #

The SENSE-NSI-RM acts in the roll of an uRA in the NSI architecture.  It uses the NSI DDS protocol to communicate with a nsi-dds server to retrieve NSA and topology files for use in building a topological view of the local NSI uPA.  It also uses the NSI CS protocol to communicate with the local NSI uPA to reserve assoicated network resources, and inventory existing reservation for completing topology.  For these reasons the SENSE-NSI-RM requires X.509 certificates to participate in the TLS secured NSA infrestructure.

## Certificates ##

This directory contains the current set of certificates for NSI networking devices participating on the SENSE network.  There are two shell scripts provided to create Java key and trust stores for the SENSE-NSI-RM.

There is an assumption that any leaf X.509 certificate allocated to an NSA must be present here, in PEM format, with the subject DN contained in the file using the following string format:
``
 0 s:/OU=Domain Control Validated/CN=nsi-aggr-west.es.net
``

### Prerequisites ###

The two scripts provided here require ``openssl`` and the java ``keytool`` to be installed.  The ``keytool`` is installed as part of the Java JDK and will require having the ``$JAVA_HOME`` environment variable set to the proper Java installation location.

### Keystore ###
The SENSE-NSI-RM is implemented in Java and therefore requires a Java keystore for secure TLS communications.  We can created the required keystore using the ``build_keystore.sh`` shell script contained in this directory.

``Usage: build_keystore.sh <keystorefile> <passwd> <keyfile> <certfile> <ca-file>``

This command takes the following parameters:

  - ``<keystorefile>`` - filename of the keystore you would like to create (such as ``keystore.jks``).
  - ``<passwd>`` - the keystore password used to secure access (such as ``changeit``).
  - ``<keyfile>`` - the X.509 private key file for the SENSE-NSI-RM server.
  - ``<certfile>`` - the X.509 public certificate for the SENSE-NSI-RM server.
  - ``<ca-file>`` - the signing Certificate Authority file for the X.509 private key.

As an example:
  
``./build_keystore.sh keystore.jks changeit nsi-host.key nsi-host.crt nsi-cachain.crt``

It is suggested that you do not use the default Java "``changeit``" password.

### Truststore ###
Similar to the keystore we also require a truststore containing the Certificate Authorities of the peer servers the SENSE-NSI-RM will communicate with.  This will include the nsi-dds server, the NSI uPA server, and the SENSE-O server.  We can created the required truststore using the ``build_truststore.sh`` shell script contained in this directory.

``Usage: build_truststore.sh <truststorefile> <password>``

This command takes the following parameters:

  - ``<truststorefile>`` - filename of the truststore you would like to create (such as ``truststore.jks``).
  - ``<passwd>`` - the truststore password used to secure access (such as ``changeit``).  

The script will import any public certificate (``*.pem``) files in the local directory.  For example:

``./build_truststore.sh truststore.jks changeit``
  
It is suggested that you do not use the default Java "``changeit``" password.

Once you have created both the keystore and truststore files they can be copied to a location of your choice for use by the SENSE-NSI-RM.  Change the ``TRUSTSTORE`` and ``KEYSTORE`` environment variable the ``run.sh`` file to point to the new location.  You will also need to modify the ``keyStore`` and ``trustStore`` parameters in the ``config/application.yml`` file similarly.
