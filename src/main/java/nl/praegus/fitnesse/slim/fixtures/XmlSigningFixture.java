package nl.praegus.fitnesse.slim.fixtures;

import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.apache.wss4j.common.WSEncryptionPart;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.SOAPConstants;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecEncrypt;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.util.WSSecurityUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;


public class XmlSigningFixture extends SlimFixture {

    static {
        org.apache.xml.security.Init.init();
    }

    private Properties signatureProperties = new Properties();


    public void setKeyStore(String keyStore) {
        signatureProperties.put("org.apache.ws.security.crypto.provider", "org.apache.ws.security.components.crypto.Merlin");
        signatureProperties.put("org.apache.ws.security.crypto.merlin.keystore.file", getFilePathFromWikiUrl(keyStore));
        signatureProperties.put("org.apache.ws.security.crypto.merlin.keystore.type", "jks");
    }

    public void setKeyStorePassword(String keyStorePassword) {
        signatureProperties.put("org.apache.ws.security.crypto.merlin.keystore.password", keyStorePassword);
    }

    public void setKeyAlias(String keyAlias) {
        signatureProperties.put("org.apache.ws.security.crypto.merlin.keystore.alias", keyAlias);
    }

    public void setKeyPassword(String keyPassword) {
        signatureProperties.put("privatekeypassword", keyPassword);
    }


    public String signMessage(String message) {
        String ret;
        try {
            InputStream msg = new ByteArrayInputStream(message.getBytes());
            SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null, msg);

            SOAPPart soapPart = soapMessage.getSOAPPart();
            SOAPEnvelope soapEnvelope = soapPart.getEnvelope();

            Name name = soapEnvelope.createName("id", "soapsec",
                    "http://schemas.xmlsoap.org/soap/security/2000-12");

            SOAPBody soapBody = soapEnvelope.getBody();
            soapBody.addAttribute(name, "Body");

            Document doc = soapBody.getOwnerDocument();
            Crypto crypto = CryptoFactory.getInstance(signatureProperties); //File

            WSSecHeader secHeader = new WSSecHeader(doc);
            secHeader.insertSecurityHeader();

            WSSecSignature sign = new WSSecSignature(secHeader);
            sign.setUserInfo(signatureProperties.getProperty("org.apache.ws.security.crypto.merlin.keystore.alias"), signatureProperties.getProperty("privatekeypassword"));
            sign.setKeyIdentifierType(WSConstants.BST_DIRECT_REFERENCE); // Binary Security Token - SecurityTokenReference
            sign.setUseSingleCertificate(true);
            sign.setDigestAlgo(DigestMethod.SHA256);

            Document signedDoc = sign.build(crypto);

            ret = org.apache.wss4j.common.util.XMLUtils.prettyDocumentToString(signedDoc);

        } catch (Exception e) {
            throw new SlimFixtureException(true, "ERR", e);
        }
        return ret;
    }
}

