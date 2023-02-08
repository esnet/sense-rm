package net.es.nsi.cs.lib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;
//import javax.xml.soap.SOAPEnvelope;
import javax.xml.soap.SOAPException;
//import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SoapLogger implements SOAPHandler<SOAPMessageContext> {
    QName NsiHeader_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/framework/headers", "nsiHeader");

    @Override
    public Set getHeaders() {
        Set headers = new HashSet<>();
        headers.add(NsiHeader_QNAME);
        return headers;
    }

    @Override
    public void close(MessageContext arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public boolean handleFault(SOAPMessageContext arg0) {
        SOAPMessage message= arg0.getMessage();
        try {
            message.writeTo(System.out);
        } catch (SOAPException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public boolean handleMessage(SOAPMessageContext context) {
        SOAPMessage soapMessage = context.getMessage();
        if (soapMessage != null) {
            try {
                //SOAPEnvelope soapEnv = soapMessage.getSOAPPart().getEnvelope();
                //SOAPHeader soapHeader = soapEnv.getHeader();

                boolean isOutboundMessage = (Boolean) context.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);
                if (isOutboundMessage) {
                    log.debug("SoapLogger: OUTBOUND MESSAGE\n");

                } else {
                    log.debug("SoapLogger: INBOUND MESSAGE\n");
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                soapMessage.writeTo(out);
                log.debug(out.toString());
                out.close();
            } catch (Exception ex) {
               log.error("SoapLogger: Failed to process SOAP message", ex);
            }
        }
        return true;
    }
}
