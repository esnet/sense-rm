package net.es.nsi.cs.lib;

import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.WebApplicationException;

/**
 *
 * @author hacksaw
 */
public class SimpleLabel {
    public static final String NSI_EVTS_LABEL_TYPE = "vlan";

    public final static Set<String> LABELS = new HashSet<>();
    static {
        LABELS.add(NSI_EVTS_LABEL_TYPE);
    };

    public final static String HASH = "#";
    public final static String QUESTION = "?";
    public final static String EQUALS = "=";
    public final static String HYPHEN = "-";
    public final static String COMMA = ",";
    public final static String LABELTYPE_SEPARATOR = ";";

    private String type;
    private String value;

    public SimpleLabel() {

    }

    public SimpleLabel(String type, String value) throws IllegalArgumentException {
        if (LABELS.contains(type)) {
            this.type = type;
            this.value = value;
        }
        else {
            throw new IllegalArgumentException("STP unknown label type " + type);
        }
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) throws WebApplicationException {
        if (LABELS.contains(type)) {
            this.type = type;
        }
        else {
            throw new IllegalArgumentException("STP unknown label type " + type);
        }
    }

    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return type + "=" + value;
    }

    @Override
    public boolean equals(Object object){
        if (object == this) {
            return true;
        }

        if((object == null) || (object.getClass() != this.getClass())) {
            return false;
        }

        SimpleLabel that = (SimpleLabel) object;
        if (this.type == null && that.getType() == null) {
            return true;
        }

        if (this.type == null && that.getType() != null) {
            return false;
        }

        if (this.type != null && that.getType() == null) {
            return false;
        }

        if (!this.type.equalsIgnoreCase(this.getType())) {
            return false;
        }

        if (this.value == null && that.getValue() == null) {
            return true;
        }

        if (this.value == null && that.getValue() != null) {
            return false;
        }

        if (this.value != null && that.getValue() == null) {
            return false;
        }

        if (!this.value.equalsIgnoreCase(this.getValue())) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((type == null) ? 0 : type.hashCode());
        result = prime * result
                + ((value == null) ? 0 : value.hashCode());
        return result;
    }
}
