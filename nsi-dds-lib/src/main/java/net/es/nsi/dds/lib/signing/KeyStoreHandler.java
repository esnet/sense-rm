package net.es.nsi.dds.lib.signing;

import com.google.common.base.Strings;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import net.es.nsi.dds.lib.constants.Properties;


/**
 *
 * @author hacksaw
 */
public class KeyStoreHandler {

  private final String keyStore;
  private final String keyStorePassword;
  private final String keyStoreType;
  private KeyStore ks;

  public KeyStoreHandler() throws KeyStoreException, FileNotFoundException, IOException,
          NoSuchAlgorithmException, CertificateException {
    keyStore = System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE,
            Properties.DEFAULT_SSL_KEYSTORE);
    keyStorePassword = System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD,
            Properties.DEFAULT_SSL_KEYSTORE_PASSWORD);
    keyStoreType = System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE,
            Properties.DEFAULT_SSL_KEYSTORE_TYPE);

    if (Strings.isNullOrEmpty(keyStore)) {
      throw new KeyStoreException("Keystore file name not provided (javax.net.ssl.keyStore).");
    }

    // Load the KeyStore and get the signing key and certificate.
    ks = KeyStore.getInstance(keyStoreType);
    ks.load(new FileInputStream(keyStore), keyStorePassword.toCharArray());
  }

  public KeyStoreHandler(String keyStore, String keyStorePassword, String keyStoreType)
          throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException,
          CertificateException {
    this.keyStore = keyStore;
    this.keyStorePassword = keyStorePassword;
    this.keyStoreType = keyStoreType;

    if (Strings.isNullOrEmpty(keyStore)) {
      throw new KeyStoreException("Keystore file name not provided (javax.net.ssl.keyStore).");
    }

    // Load the KeyStore and get the signing key and certificate.
    ks = KeyStore.getInstance(keyStoreType);
    ks.load(new FileInputStream(keyStore), keyStorePassword.toCharArray());
  }

  public KeyStore.PrivateKeyEntry getPrivateKeyEntry(String alias)
          throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
    return (KeyStore.PrivateKeyEntry) ks.getEntry(alias, new KeyStore.PasswordProtection(keyStorePassword.toCharArray()));
  }

  public X509Certificate getX509Certificate(String alias)
          throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
    return (X509Certificate) getPrivateKeyEntry(alias).getCertificate();
  }

  public KeyInfo getKeyInfo(String alias, XMLSignatureFactory fac)
          throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
    // Create the KeyInfo containing the X509Data.
    KeyInfoFactory kif = fac.getKeyInfoFactory();
    X509Certificate x509Certificate = getX509Certificate(alias);
    List x509Content = new ArrayList();
    x509Content.add(x509Certificate.getSubjectX500Principal().getName());
    x509Content.add(x509Certificate);
    X509Data xd = kif.newX509Data(x509Content);
    KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));
    return ki;
  }
}
