# sense-n-rm

SENSE Network Resource Manager

This is prototype software supporting the SENSE-RM API over an NSI network.

## Contents

- How-to configure and run the SENSE-N-RM application
- How-to natively build the SENSE-N-RM
- How-to install Java on CentOS 7
- How-to install Maven on CentOS 7
- How-to install Docker on CentOS 7
- Using Docker tools to build the SENSE-N-RM

## How-to configure and run the SENSE-N-RM application
The Java 1.8 runtime and Postgres database is required to run the SENSE-N-RM naitively on a server.  In addition, maven is required to build the SENSE-N-RM from source.

### Pre-configuration checklist
1. Install the prerequisite runtime software
 - java 1.8 for runtime and build of source;
 - maven for building the source;
 - postgresql for runtime.
2. Create a non-root user account to run the SENSE-N-RM.
3. Download and build the SENSE-N-RM source code (will require java and maven).
4. Create a Postgres account for use by the SENSE-N-RM.
5. 


## Create a non-root user account
The SENSE-N-RM application should be run as a low-privilege user on the target production server. Ideally it should be run as a user created only for the purpose of running the set of software associated with the SENSE-N-RM application.  However, an existing account can be used if available (i.e. opennsa).  However, if an account other than "sense" is used to run the application make sure to create the SENSE-N-RM database user using the utilized id.

As an example, we create the following new user and user group for the SENSE-N-RM application if you have not already done so (for example, below we create a "sense" user and user group).

```
$ sudo groupadd sense
$ sudo useradd sense -g sense
$ sudo su - sense
```

## Download and build the SENSE-N-RM source code
The SENSE-N-RM is a Java Spring-boot application the will require a Java 1.8 runtime and Maven to build.  Please follow the "How-To" sections on installing both Java and Maven if not on your system.

We recommend pulling the current version of the main branch from github.  Create a location to download and build the source.  This can be done in the context of the application specific user.

```
$ mkdir src
$ cd src
$ git clone https://github.com/esnet/sense-rm.git
```

Into the `sense-rm` directory we go and build the source (skipping test cases).

```
$ cd sense-rm
$ mvn clean package -DskipTests=true
```

Now we want to collect the pieces in `target/dist` directory:

```
$ mvn antrun:run@package-runtime
```

Copy the contents of target/dist to the location you would like to use as the runtime directory for the SENSE-N-RM.  For example, if you are running under a dedicated sense user id then place in a location in the home directory `/home/sense/sense-rm`.  It is recommended that you do not run from the build directory as any new builds will overwrite the existing jar and configuration.

## Create a Postgres application account
The SENSE-N-RM requires a dedicated Postgres user and database to be created for storage of MRML related model information, and other runtime data needed to map requests from the SENSE-O through to underlying resources.

First we create new database user called "sense" using the Postgres interactive tools.  You can use a different user name but remember it for when you configure the SENSE-N-RM runtime.

```
$ sudo -u postgres createuser --interactive --pwprompt
```

Here is an example output:

```
$ sudo -u postgres createuser --interactive --pwprompt
Enter name of role to add: sense
Enter password for new role: ******
Enter it again: *****
Shall the new role be a superuser? (y/n) n
Shall the new role be allowed to create databases? (y/n) y
Shall the new role be allowed to create more new roles? (y/n) n
```

Now we create the database "sense-n-rm" and assign ownership to the "sense" user we just created.

```
$ sudo -u postgres createdb -O sense sense-n-rm
```

The creation of database tables will be done dynamically by the SENSE-N-RM.

## Creating Java key and trust stores
The SENSE-N-RM source tree contains a `/certificates` directory holding a set of common certificates and scripts writen to create your Java keystore and trustore needed to communicate with systems in the SENSE ecosystem.

To build the truststore we can use the `build_truststore.sh` script:

```
Usage: build_truststore.sh <filename> <password>
```

Here is an example where we create the a `truststore.jks` file with a default password of `changeit`.  Once created move the your SENSE-N-RM runtime `/config` directory, or somewhere it can be accessed.

```
$ cd certificates
$ ./build_truststore.sh truststore.p12 changeit
Adding 179-132_research_maxgigapop_net.pem as alias 179-132_research_maxgigapop_net
Certificate was added to keystore
Adding agg_netherlight_net.pem as alias agg_netherlight_net
Certificate was added to keystore
Adding nsi-aggr-west_es_net.pem as alias nsi-aggr-west_es_net
Certificate was added to keystore
Adding nsi-am-sl_northwestern_edu.pem as alias nsi-am-sl_northwestern_edu
Certificate was added to keystore
Adding nsi0_lsanca_pacificwave_net.pem as alias nsi0_lsanca_pacificwave_net
Certificate was added to keystore
Adding nsi0_snvaca_pacificwave_net.pem as alias nsi0_snvaca_pacificwave_net
Certificate was added to keystore
Adding nsi0_sttlwa_pacificwave_net.pem as alias nsi0_sttlwa_pacificwave_net
Certificate was added to keystore
Adding nsi_ampath_net.pem as alias nsi_ampath_net
Certificate was added to keystore
Adding pmri061_it_northwestern_edu.pem as alias pmri061_it_northwestern_edu
Certificate was added to keystore
Adding southernlight_net_br.pem as alias southernlight_net_br
Certificate was added to keystore
```

For creating the Java keystore you will need the host SSL key, certificate, and signing CA file.  To build the keystore we can use the `build_keystore.sh` script:

```
Usage: build_keystore.sh <keystorefile> <passwd> <keyfile> <certfile> <ca-file>
```

Here is an example where we create the a `keystore.p12` file with a password of `changeit`.  Once created move the your SENSE-N-RM runtime `/config` directory, or somewhere it can be accessed.

```
$ ./build_keystore.sh keystore.p12 changeit test.key test.crt test-ca.crt 
Entry for alias 1 successfully imported.
Import command completed:  1 entries successfully imported, 0 entries failed or cancelled
[Storing keystore.p12]
```
 
## Configuring the SENSE-N-RM

'sdjsjf';sldjfsd;fjsd';fjsfjsdjFK

## How-to install Java on CentOS 7
If you decide to use the java runtime on CentOS 7 then we will need to install the Java 8 runtime to execute our SENSE-N-RM jar file.

Reference: [https://www.liquidweb.com/kb/install-java-8-on-centos-7/](Install Java 8 on CentoOS 7)

### Step 1: As root up-to-date OS before we install.

```
$ sudo yum -y update
```

### Step 2: As root install Java 8 OpenJDK development kit.

```
$ sudo yum install java-1.8.0-openjdk java-1.8.0-openjdk-devel
```

### Step 3: Verify Java is installed

```
$ java -version
```

Example output:

```
$ java -version
openjdk version "1.8.0_232"
OpenJDK Runtime Environment (build 1.8.0_232-b09)
OpenJDK 64-Bit Server VM (build 25.232-b09, mixed mode)
```

### Step 4: Set up your Java environment

Determine the JAVA_HOME location:

```
$ sudo update-alternatives --config java
```

Output will look like:

```
$ update-alternatives --config java

There is 1 program that provides 'java'.

  Selection    Command
-----------------------------------------------
*+ 1           java-1.8.0-openjdk.x86_64 (/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.232.b09-0.el7_7.x86_64/jre/bin/java)

Enter to keep the current selection[+], or type selection number: 
```

Now copy this location into your `.bash_profile` using your favorite text editor.

```
$ vim ~/.bash_profile
```

Add the following line:

```
$ export JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.232.b09-0.el7_7.x86_64/jre/bin/java
```

Load the new value into your environment"

```
$ source ~/.bash_profile
```

Now we should see the following:

```
$ echo $JAVA_HOME
/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.232.b09-0.el7_7.x86_64/jre/bin/java
```

Finally, setup the JAVA_HOME environment variable for the system.  If you would rather restrict this to a single user then install in user's `.bash_profile`.

```
$ echo "JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")" | sudo tee -a /etc/profile
$ source /etc/profile
```

## How-to install Maven on CentOS 7
The SENSE-N-RM requires Maven to download, build, and package all dependencies.  See "Using Docker tools" if you would rather use a Docker image for Maven instead of a loacl install.

### Step 1: Download Apache Maven 3.6.2
First, download and extract the Apache Maven 3.6.2 archive.

```
$ cd /tmp
$ wget http://www-us.apache.org/dist/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.tar.gz
$ tar -zxvf apache-maven-3.6.2-bin.tar.gz
```

### Step 2: Install Maven in a common location
Move all Apache Maven 3.6.2 files to a reasonable location and change their ownership to `root:root`.

```
$ sudo mv /tmp/apache-maven-3.6.2 /opt
$ sudo chown -R root:root /opt/apache-maven-3.6.2
```

Create a version-irrelevant symbolic link pointing to the original Apache Maven 3.6.2 directory.

```
$ sudo ln -s /opt/apache-maven-3.6.2 /opt/apache-maven
```

Add the path /opt/apache-maven to the PATH environment variable.

```
$ echo 'export PATH=$PATH:/opt/apache-maven/bin' | sudo tee -a /etc/profile
$ source /etc/profile
```

Finally, use the command below to verify the installation.

```
$ mvn --version
```

The output should resemble the following.

```
$ mvn --version
Apache Maven 3.5.0 (ff8f5e7444045639af65f6095c62210b5713f426; 2017-04-03T19:39:06Z)
Maven home: /opt/maven
Java version: 1.8.0_141, vendor: Oracle Corporation
Java home: /usr/lib/jvm/java-1.8.0-openjdk-1.8.0.141-1.b16.el7_3.x86_64/jre
Default locale: en_US, platform encoding: UTF-8
OS name: "linux", version: "3.10.0-514.26.2.el7.x86_64", arch: "amd64", family: "unix"
```

Finally we clean up.

```
$ rm /tmp/apache-maven-3.6.2-bin.tar.gz
```
## How-to install Postrges on CentOS 7
The default CentOS repository contain a postgresql package we can install using the yum package system.  If you require a specific version then follow published instructions.

Reference: [https://www.digitalocean.com/community/tutorials/how-to-install-and-use-postgresql-on-centos-7](How To Install and Use PostgreSQL on CentOS 7))

If we have not yet updated the yum package database do that now:

```
$ sudo yum check-update
```

Install the postgresql-server package and the “contrib” package, that adds some additional utilities and functionality:

```
$ sudo yum install postgresql-server postgresql-contrib
```

Accept the prompt, by responding with a `y`.

Now that our software is installed, we have to perform a few steps before we can use it.

Create a new PostgreSQL database cluster:

```
$ sudo postgresql-setup initdb
```

Now we will need to configure Postgres authentication of the SENSE-N-RM.  There are two options for this depending on how you have configured the runtime environment.  If you have created a dedicated OS user id (say "sense") and have created a dedicated postgres user that matches the OS user id (i.e. "sense") then the default Postgres "ident" authentication mechanism should work out of the box.  However, if this does not work, or if you are running the SENSE-N-RM under a different user id than the Postgres user you created, then you should change the Postrges authentication mechanism from "ident" to the password authentication mechanism "md5".  We can change the authentication mechanism by editing the Postgres host-based authentication (HBA) configuration.

Open the HBA configuration with your favorite text editor. We will use vi:

```
$ sudo vi /var/lib/pgsql/data/pg_hba.conf
```

Find the lines that looks like this, near the bottom of the file:

**pg_hba.conf excerpt (original)**

```
host    all             all             127.0.0.1/32            ident
host    all             all             ::1/128                 ident
```

Then replace “ident” with “md5”, so they look like this:

**pg_hba.conf excerpt (updated)**

```
host    all             all             127.0.0.1/32            md5
host    all             all             ::1/128                 md5
```

Save and exit. PostgreSQL is now configured to allow password authentication.

Now start and enable PostgreSQL:

```
$ sudo systemctl start postgresql
$ sudo systemctl enable postgresql
```

PostgreSQL is now ready to be used.

## How-to install Docker on CentOS 7

For this section we download the CentOS 7 installationb package from the Docker repository directly to get the most recent version.  (Liberated from https://www.digitalocean.com/community/tutorials/how-to-install-and-use-docker-on-centos-7)

If we have not yet updated the yum package database do that now:

```
$ sudo yum check-update
```

Now run this command to add the official Docker repository, download the latest version of Docker, and install it:

```
$ curl -fsSL https://get.docker.com/ | sh
```

After installation has completed, start the Docker daemon:

```
$ sudo systemctl start docker
```

Verify that it’s running:

```
$ sudo systemctl status docker
```

The output should be similar to the following, showing that the service is active and running:

```
$ sudo systemctl status docker
● docker.service - Docker Application Container Engine
   Loaded: loaded (/usr/lib/systemd/system/docker.service; disabled; vendor preset: disabled)
   Active: active (running) since Tue 2019-10-29 15:00:46 PDT; 7s ago
     Docs: https://docs.docker.com
 Main PID: 29476 (dockerd)
    Tasks: 8
   Memory: 42.1M
   CGroup: /system.slice/docker.service
           └─29476 /usr/bin/dockerd -H fd:// --containerd=/run/containerd/containerd.sock
```

Lastly, make sure it starts at every server reboot:

```
$ sudo systemctl enable docker
```
## Using Docker tools to build the SENSE-N-RM
To build the SENSE-N-RM java runtime image using Docker tools we can do the following...

First we create a maven-repo to reuse during our build phases:

```
$ docker volume create --name maven-repo
```

Then we build the SENSE-N-RM jar file:

```
$ docker run -it --rm --name sense-rm -v "$(pwd)":/usr/src/mymaven -v "$HOME/.m2":/root/.m2  -w /usr/src/mymaven maven:3.6.2-jdk-8 mvn clean install
```

Now collect the components together under `${builddir}/target/dist`:

```
$ docker run -it --rm --name sense-rm -v "$(pwd)":/usr/src/mymaven -v "$HOME/.m2":/root/.m2  -w /usr/src/mymaven maven:3.6.2-jdk-8 mvn antrun:run@package-runtime
```

## Useful debug commands

```
$ openssl s_client -debug -connect nsi0.snvaca.pacificwave.net:9443 -no_ssl2 -no_ssl3 -no_tls1
```