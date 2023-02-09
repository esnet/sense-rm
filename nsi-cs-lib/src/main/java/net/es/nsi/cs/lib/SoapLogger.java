package net.es.nsi.cs.lib;

import java.io.ByteArrayOutputStream;
//import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
//import javax.xml.soap.SOAPEnvelope;
//import javax.xml.soap.SOAPException;
//import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;

public class SoapLogger implements SOAPHandler<SOAPMessageContext> {
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());

    QName NsiHeader_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/framework/headers", "nsiHeader");

    @Override
    public Set getHeaders() {
        logger.info("SoapLogger.getHeaders: entering...");
        Set headers = new HashSet<>();
        headers.add(NsiHeader_QNAME);
        return headers;
    }

    @Override
    public void close(MessageContext arg0) {
        logger.info("SoapLogger.close: entering...");
    }

    @Override
    public boolean handleFault(SOAPMessageContext context) {
        logger.info("SoapLogger.handleFault: entering...");
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
        logger.info("SoapLogger.handleMessage: entering...");
        SOAPMessage soapMessage = context.getMessage();
        if (soapMessage != null) {
            try {
                //SOAPEnvelope soapEnv = soapMessage.getSOAPPart().getEnvelope();
                //SOAPHeader soapHeader = soapEnv.getHeader();

                boolean isOutboundMessage = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (isOutboundMessage) {
                    logger.info("SoapLogger.handleMessage: OUTBOUND MESSAGE");

                } else {
                    logger.info("SoapLogger.handleMessage: INBOUND MESSAGE");
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                soapMessage.writeTo(out);
                logger.info(out.toString());
                System.out.println(out);
                out.close();
            } catch (Exception ex) {
                logger.error("SoapLogger: Failed to process SOAP message", ex);
            }
        }
        return true;
    }
}
