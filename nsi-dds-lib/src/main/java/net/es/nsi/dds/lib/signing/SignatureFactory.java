package net.es.nsi.dds.lib.signing;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Collections;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

/**
 *
 * @author hacksaw
 */
public class SignatureFactory {
    private final Logger log = LoggerFactory.getLogger(SignatureFactory.class);
    private final XMLSignatureFactory fac;
    private final KeyStoreHandler keyStoreHandler;

    public SignatureFactory() throws KeyStoreException, RuntimeException {
        // We are going to use the jsr105 provider to generate our digital signature.
        String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");

        // Create a DOM XMLSignatureFactory that will be used to
        // generate the enveloped signature.
        try {
            fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
        } catch (ClassNotFoundException | InstantiationException| IllegalAccessException ex) {
            log.error("SignatureFactory: could not instantiate a jsr105Provider", ex);
            throw new RuntimeException(ex);
        }

        try {
            keyStoreHandler = new KeyStoreHandler();
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | CertificateException ex) {
            log.error("SignatureFactory: could not create default keystore handler", ex);
            throw new KeyStoreException(ex);
        }
    }

    public SignatureFactory(KeyStoreHandler keyStoreHandler) throws RuntimeException {
        // We are going to use the jsr105 provider to generate our digital signature.
        String providerName = System.getProperty("jsr105Provider", "org.jcp.xml.dsig.internal.dom.XMLDSigRI");

        // Create a DOM XMLSignatureFactory that will be used to
        // generate the enveloped signature.
        try {
            fac = XMLSignatureFactory.getInstance("DOM", (Provider) Class.forName(providerName).newInstance());
        } catch (ClassNotFoundException | InstantiationException| IllegalAccessException ex) {
            log.error("SignatureFactory: could not instantiate a jsr105Provider", ex);
            throw new RuntimeException(ex);
        }

        this.keyStoreHandler = keyStoreHandler;
    }

    public Document generateEnvelopedSignature(Document doc, String alias) throws KeyStoreException, XMLSignatureException, RuntimeException {
        // Create a Reference to the enveloped document (in this case,
        // you are signing the whole document, so a URI of "" signifies
        // that, and also specify the SHA1 digest algorithm and
        // the ENVELOPED Transform.
        Reference ref;
        try {
            ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA512, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (XMLStructure) null)), null, null);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
            log.error("generateEnvelopedSignature: could not create reference to the enveloped document", ex);
            throw new RuntimeException(ex);
        }

        // Open the keystore and load the private key corresponding to alias.
        KeyStore.PrivateKeyEntry keyEntry;
        try {
            keyEntry = keyStoreHandler.getPrivateKeyEntry(alias);
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("generateEnvelopedSignature: could not locate private key for alias=\"" + alias + "\"", ex);
            throw new KeyStoreException(ex);
        }

        // Determine the signature method we will use.
        String algorithm = keyEntry.getPrivateKey().getAlgorithm();
        String method = SignatureMethod.HMAC_SHA1;
        if (algorithm.equalsIgnoreCase("RSA")) {
            method = SignatureMethod.RSA_SHA1;
        }
        else if (algorithm.equalsIgnoreCase("DSA")) {
            method = SignatureMethod.DSA_SHA1;
        }

        // Create the SignedInfo.
        SignedInfo si;
        try {
            si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null), fac.newSignatureMethod(method, null), Collections.singletonList(ref));
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
            log.error("generateEnvelopedSignature: configured signature algorithms not supported", ex);
            throw new RuntimeException(ex);
        }

        // Create a KeyInfo and add the KeyValue to it.
        KeyInfo ki;
        try {
            ki = keyStoreHandler.getKeyInfo(alias, fac);
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("generateEnvelopedSignature: could not create keyInfo", ex);
            throw new RuntimeException(ex);
        }

        // Create a DOMSignContext and set the signing Key to the DSA
        // PrivateKey and specify where the XMLSignature should be inserted
        // in the target document (in this case, the document root)
        DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());

        // Marshal, generate (and sign) the eveloped XMLSignature. The DOM
        // Document will contain the XML Signature if this method returns
        // successfully.
        XMLSignature signature = fac.newXMLSignature(si, ki);
        try {
            signature.sign(dsc);
        } catch (MarshalException | XMLSignatureException ex) {
            log.error("generateEnvelopedSignature: could not create signature", ex);
            throw new XMLSignatureException(ex);
        }

        // Validate document before returning.
        try {
            Validate.validateEnveloped(doc);
        }
        catch (Exception ex) {
            log.error("Failed to validate enveloped signature", ex);
            throw new RuntimeException(ex);
        }

        return doc;
    }

    public Document generateExternalSignature(Document dom, String alias) throws KeyStoreException, XMLSignatureException, RuntimeException {
        // Create a Reference to the enveloped document (in this case,
        // you are signing the whole document, so a URI of "" signifies
        // that, and also specify the SHA1 digest algorithm and
        // the ENVELOPED Transform.
        Reference ref;
        try {
            ref = fac.newReference("http://www.w3.org/TR/xml-stylesheet", fac.newDigestMethod(DigestMethod.SHA512, null));
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
            log.error("generateExternalSignature: could not create reference to the enveloped document", ex);
            throw new RuntimeException(ex);
        }

        // Open the keystore and load the private key corresponding to alias.
        KeyStore.PrivateKeyEntry keyEntry;
        try {
            keyEntry = keyStoreHandler.getPrivateKeyEntry(alias);
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("generateEnvelopedSignature: could not locate private key for alias=\"" + alias + "\"", ex);
            throw new KeyStoreException(ex);
        }

        // Determine the signature method we will use.
        String algorithm = keyEntry.getPrivateKey().getAlgorithm();
        String method = SignatureMethod.HMAC_SHA1;
        if (algorithm.equalsIgnoreCase("RSA")) {
            method = SignatureMethod.RSA_SHA1;
        }
        else if (algorithm.equalsIgnoreCase("DSA")) {
            method = SignatureMethod.DSA_SHA1;
        }

        // Create the SignedInfo.
        SignedInfo si;
        try {
            si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null), fac.newSignatureMethod(method, null), Collections.singletonList(ref));
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
            log.error("generateExternalSignature: configured signature algorithms not supported", ex);
            throw new RuntimeException(ex);
        }

        // Create a KeyInfo and add the KeyValue to it.
        KeyInfo ki;
        try {
            ki = keyStoreHandler.getKeyInfo(alias, fac);
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException | KeyStoreException ex) {
            log.error("generateExternalSignature: could not create keyInfo", ex);
            throw new RuntimeException(ex);
        }

        // Create the XMLSignature (but don't sign it yet)
        XMLSignature signature = fac.newXMLSignature(si, ki);

        // Create the Document that will hold the resulting XMLSignature
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true); // must be set
        dbf.setValidating(true);
        dbf.setExpandEntityReferences(true);
        dbf.setXIncludeAware(true);
        Document doc;
        try {
            doc = dbf.newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            log.error("generateExternalSignature: could not create document to hold signature", ex);
            throw new RuntimeException(ex);
        }

        // Create a DOMSignContext and set the signing Key to the DSA
        // PrivateKey and specify where the XMLSignature should be inserted
        // in the target document (in this case, the document root)
        DOMSignContext signContext = new DOMSignContext(keyEntry.getPrivateKey(), doc);

        try {
            // Marshal, generate (and sign) the detached XMLSignature. The DOM
            // Document will contain the XML Signature if this method returns
            // successfully.
            signature.sign(signContext);
        } catch (MarshalException | XMLSignatureException ex) {
            log.error("generateExternalSignature: could not create signature", ex);
            throw new XMLSignatureException(ex);
        }

        // Validate document before returning.
        try {
            Validate.validateEnveloped(doc);
        }
        catch (Exception ex) {
            log.error("Failed to validate enveloped signature", ex);
            throw new RuntimeException(ex);
        }

        return doc;
    }
}
