package nl.praegus.fitnesse.slim.fixtures;

import nl.hsac.fitnesse.fixture.Environment;
import nl.hsac.fitnesse.fixture.slim.SlimFixture;
import nl.hsac.fitnesse.fixture.slim.SlimFixtureException;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.message.WSSecHeader;
import org.apache.wss4j.dom.message.WSSecSignature;
import org.apache.wss4j.dom.message.WSSecTimestamp;
import org.apache.wss4j.dom.message.WSSecUsernameToken;
import org.w3c.dom.Document;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.soap.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Properties;


public class XmlSigningFixture extends SlimFixture {

    static {
        org.apache.xml.security.Init.init();
    }

    private Properties signatureProperties = new Properties();
    private String user;
    private String password;

    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

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
            doc = addTimestamp(doc);
            doc = addUsernameToken(doc);
            doc = addSignature(doc);

            return Environment.getInstance().getHtmlForXml(org.apache.wss4j.common.util.XMLUtils.prettyDocumentToString(doc));

        } catch (Exception e) {
            throw new SlimFixtureException(true, "ERR", e);
        }
    }

    private Document addSignature(Document doc) {
        try {
            Crypto crypto = CryptoFactory.getInstance(signatureProperties);

            WSSecHeader secHeader = new WSSecHeader(doc);
            secHeader.insertSecurityHeader();

            WSSecSignature sign = new WSSecSignature(secHeader);
            sign.setUserInfo(signatureProperties.getProperty("org.apache.ws.security.crypto.merlin.keystore.alias"), signatureProperties.getProperty("privatekeypassword"));
            sign.setKeyIdentifierType(WSConstants.ISSUER_SERIAL);
            sign.setUseSingleCertificate(false);
            sign.setSigCanonicalization(CanonicalizationMethod.EXCLUSIVE);
            sign.setDigestAlgo(DigestMethod.SHA1);



            return sign.build(crypto);
        } catch (Exception e) {
            throw new SlimFixtureException(true, "ERR", e);
        }
    }

    private Document addTimestamp(Document doc) {
        try {
            WSSecHeader secHeader = new WSSecHeader(doc);
            secHeader.insertSecurityHeader();
            WSSecTimestamp timestamp = new WSSecTimestamp(secHeader);
            timestamp.setTimeToLive(300);
            timestamp.setPrecisionInMilliSeconds(true);
            return timestamp.build();
        } catch (Exception e) {
            throw new SlimFixtureException(true, "ERR", e);
        }
    }

    private Document addUsernameToken(Document doc) {
        try{
            WSSecHeader secHeader = new WSSecHeader(doc);
            secHeader.insertSecurityHeader();

            WSSecUsernameToken builder = new WSSecUsernameToken(secHeader);
            builder.setUserInfo(user, password);
            builder.addNonce();
            builder.addCreated();
            return builder.build();

        } catch (Exception e) {
            throw new SlimFixtureException(true, "ERR", e);
        }
    }
}

