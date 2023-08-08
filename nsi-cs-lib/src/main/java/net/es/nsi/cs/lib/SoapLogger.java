package net.es.nsi.cs.lib;

import javax.xml.namespace.QName;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;

public class SoapLogger implements SOAPHandler<SOAPMessageContext> {
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    QName NsiHeader_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/framework/headers", "nsiHeader");

    @Override
    public Set getHeaders() {
        logger.debug("SoapLogger.getHeaders: entering...");
        Set headers = new HashSet<>();
        headers.add(NsiHeader_QNAME);
        return headers;
    }

    @Override
    public void close(MessageContext arg0) {
        logger.debug("SoapLogger.close: entering...");
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        logger.debug("SoapLogger.handleFault: entering...");
        SOAPMessage soapMessage = context.getMessage();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            soapMessage.writeTo(out);
            logger.debug(out.toString());
            out.close();
        } catch (Exception ex) {
            logger.error("SoapLogger: Failed to process SOAP message", ex);
        }
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        logger.debug("SoapLogger.handleMessage: entering...");
        SOAPMessage soapMessage = context.getMessage();
        if (soapMessage != null) {
            try {
                //SOAPEnvelope soapEnv = soapMessage.getSOAPPart().getEnvelope();
                //SOAPHeader soapHeader = soapEnv.getHeader();

                boolean isOutboundMessage = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (isOutboundMessage) {
                    logger.debug("SoapLogger.handleMessage: OUTBOUND MESSAGE");

                } else {
                    logger.debug("SoapLogger.handleMessage: INBOUND MESSAGE");
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                soapMessage.writeTo(out);
                logger.debug(out.toString());
                System.out.println(out);
                out.close();
            } catch (Exception ex) {
                logger.debug("SoapLogger: Failed to process SOAP message", ex);
            }
        }
        return true;
    }
}
