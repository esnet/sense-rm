package net.es.nsi.cs.lib;

import oasis.names.tc.saml._2_0.assertion.AttributeStatementType;
import oasis.names.tc.saml._2_0.assertion.AttributeType;

/**
 *
 * @author hacksaw
 */
public class SessionSecurityAttr {
    private static final String GLOBAL_USERNAME = "globalUserName";
    private static final String USER_ROLE = "role";
    private static final String NAME_FORMAT = "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";

    public static AttributeStatementType getAttributeStatementType(String globalUserName, String userRole) {
        // Set the globalUserName into the sessionSecurityAttr element.
        AttributeStatementType ass = new AttributeStatementType();
        if (globalUserName != null || !globalUserName.isEmpty()) {
            AttributeType attr = new AttributeType();
            attr.setName(GLOBAL_USERNAME);
            attr.setNameFormat(NAME_FORMAT);
            attr.getAttributeValue().add(globalUserName);
            ass.getAttributeOrEncryptedAttribute().add(attr);
        }
        if (userRole != null || !userRole.isEmpty()) {
            AttributeType attr = new AttributeType();
            attr.setName(USER_ROLE);
            attr.setNameFormat(NAME_FORMAT);
            attr.getAttributeValue().add(userRole);
            ass.getAttributeOrEncryptedAttribute().add(attr);
        }

        return ass;
    }
}
