/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib.constants;

/**
 *
 * @author hacksaw
 */
public class Properties {
    // System properties.
    public static final String SYSTEM_PROPERTY_BASEDIR = "basedir";
    public static final String SYSTEM_PROPERTY_CONFIGDIR = "configdir";
    public static final String SYSTEM_PROPERTY_CONFIGFILE = "ddsConfigFile";

    public static final String SYSTEM_PROPERTY_DEBUG = "debug";
    public static final String SYSTEM_PROPERTY_LOG4J = "log4j.configuration";
    public static final String SYSTEM_PROPERTY_SSL_KEYSTORE = "javax.net.ssl.keyStore";
    public static final String SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    public static final String SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";
    public static final String SYSTEM_PROPERTY_SSL_TRUSTSTORE = "javax.net.ssl.trustStore";
    public static final String SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    public static final String SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

    public static final String DEFAULT_SSL_KEYSTORE = "config/keystore.jks";
    public static final String DEFAULT_SSL_KEYSTORE_PASSWORD = "changeit";
    public static final String DEFAULT_SSL_KEYSTORE_TYPE = "JKS";
    public static final String DEFAULT_SSL_TRUSTSTORE = "config/truststore.jks";
    public static final String DEFAULT_SSL_TRUSTSTORE_PASSWORD = "changeit";
    public static final String DEFAULT_SSL_TRUSTSTORE_TYPE = "JKS";
}
