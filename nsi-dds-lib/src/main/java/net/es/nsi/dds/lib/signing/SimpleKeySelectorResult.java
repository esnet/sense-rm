/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.lib.signing;

import java.security.Key;
import java.security.PublicKey;
import javax.xml.crypto.KeySelectorResult;

/**
 *
 * @author hacksaw
 */
public class SimpleKeySelectorResult implements KeySelectorResult {

    private final PublicKey pk;
    SimpleKeySelectorResult(PublicKey pk) {
        this.pk = pk;
    }

    @Override
    public Key getKey() { return pk; }
 }
