package net.es.nsi.cs.lib;

import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class SimpleLabels {
    private static final Pattern EQUALSPATTERN = Pattern.compile(SimpleLabel.EQUALS);
    private static final Pattern COMMAPATTERN = Pattern.compile(SimpleLabel.COMMA);
    private static final Pattern HYPHENPATTERN = Pattern.compile(SimpleLabel.HYPHEN);

    public static Set<SimpleLabel> fromString(String labels) throws IllegalArgumentException {
        Set<SimpleLabel> results = new HashSet<>();

        if (Strings.isNullOrEmpty(labels)) {
            return results;
        }

        // Split the vlan first by comma, then by hyphen.
        String[] equals = EQUALSPATTERN.split(labels);
        if (equals.length == 1 && !equals[0].isEmpty()) {
            SimpleLabel label = new SimpleLabel();
            label.setType(equals[0].trim());
            results.add(label);
        }
        else if (equals.length > 1 && !equals[1].isEmpty()) {
            // Split the vlan first by comma, then by hyphen.
            String type = equals[0].trim();
            String[] comma = COMMAPATTERN.split(equals[1]);
            for (int i = 0; i < comma.length; i++) {
                // Now by hyphen.
                String[] hyphen = HYPHENPATTERN.split(comma[i]);

                // Just a single vlan.
                if (hyphen.length == 1) {
                    SimpleLabel label = new SimpleLabel();
                    label.setType(type);
                    label.setValue(hyphen[0].trim());
                    results.add(label);
                }
                // Two vlans in a range.
                else if (hyphen.length > 1 && hyphen.length < 3) {
                    int min = Integer.parseInt(hyphen[0].trim());
                    int max = Integer.parseInt(hyphen[1].trim());

                    if (max <= min) {
                        throw new IllegalArgumentException("Invalid label range: min=" + min + ", max=" + max);
                    }
                    for (int j = min; j < max + 1; j++) {
                        SimpleLabel label = new SimpleLabel();
                        label.setType(type);
                        label.setValue(Integer.toString(j));
                        results.add(label);
                    }
                }
                // This is unsupported.
                else {
                    throw new IllegalArgumentException("Invalid label format: " + labels);
                }
            }
        }

        return results;
    }

    public static String toString(Set<SimpleLabel> labels) {
        StringBuilder sb = new StringBuilder();

        // Get a sorted version of the labels so we can build sequences.
        List<SimpleLabel> sorted = sortLabels(labels);

        // Track the current label type we are serializing.
        String currentType = null;

        boolean closed = true;
        int lastValue = -69;
        int sequenceStart = -69;

        for (SimpleLabel label : sorted) {
            // Add the label type when a new label type is encountered.
            if (currentType == null || !label.getType().equalsIgnoreCase(currentType)) {
                if (currentType != null) {
                    sb.append(SimpleLabel.LABELTYPE_SEPARATOR);
                }
                currentType = label.getType();
                sb.append(currentType);
                sb.append(SimpleLabel.EQUALS);
            }

            // Get the current label value as an integer.  Will need to abstract
            // when non-integer labels need to be supported.
            int currentValue = Integer.parseInt(label.getValue());

            // First time on this sequence.
            if (lastValue < 0) {
                sequenceStart = currentValue;
                lastValue = currentValue;
            }
            // Is this a continuing sequence?
            else if (lastValue == currentValue - 1) {
                lastValue = currentValue;
                closed = false;
            }
            // Sequence has ended.
            else {
                if (closed) {
                    sb.append(lastValue);
                    sb.append(SimpleLabel.COMMA);
                }
                else {
                    sb.append(sequenceStart);
                    sb.append(SimpleLabel.HYPHEN);
                    sb.append(lastValue);
                    sb.append(SimpleLabel.COMMA);
                }

                lastValue = currentValue;
                sequenceStart = currentValue;
                closed = true;
            }
        }

        if (!closed) {
            sb.append(sequenceStart);
            sb.append(SimpleLabel.HYPHEN);
            sb.append(lastValue);
        }
        else {
            sb.append(lastValue);
        }

        return sb.toString();
    }

    public static List<SimpleLabel> sortLabels(Set<SimpleLabel> labels) {
        return Ordering.from(compareSimpleLabel).sortedCopy(labels);
    }

    /**
     * Compares two ordered set of labels for equality.
     *
     * @param l1
     * @param l2
     * @return
     */
    public static boolean equals(List<SimpleLabel> l1, List<SimpleLabel> l2) {
        // Do the number of labels match?
        if (l1.size() != l2.size()) {
            return false;
        }

        // For each label type in the list compare type and values.
        for (int i = 0; i < l1.size(); i++) {
            String t1 = l1.get(i).getType();
            String t2 = l2.get(i).getType();

            if(t1 == null || t2 == null || !t1.equalsIgnoreCase(t2)) {
                return false;
            }

            String tv1 = l1.get(i).getValue();
            String tv2 = l2.get(i).getValue();

            if (tv1 == null && tv2 == null) {
                return true;
            }
            else if (tv1 == null) {
                return false;
            }
            else if (tv2 == null) {
                return false;
            }

            if (!tv1.equalsIgnoreCase(tv2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if v1 contains the value(s) contained in v2.  Assumes values
     * are strings representing integer ranges.
     *
     * @param v1
     * @param v2
     * @return
     */
    public static boolean contains(String v1, String v2) {
        if ((v1 == null || v1.isEmpty()) && (v2 == null || v2.isEmpty())) {
            return true;
        }
        else if (v1 == null || v1.isEmpty()) {
            return false;
        }
        else if (v2 == null || v2.isEmpty()) {
            return false;
        }

        // Now we convert the supplied values into Sets Lists.
        Set<Integer> vs1 = stringToIntegerSet(v1);
        Set<Integer> vs2 = stringToIntegerSet(v2);
        return vs1.containsAll(vs2);
    }

    public static Set<Integer> stringToIntegerSet(String values) {
        Set<Integer> labels = new HashSet<>();

        if (values == null || values.isEmpty()) {
            return labels;
        }

        // Split the vlan first by comma, then by hyphen.
        Pattern pattern = Pattern.compile(",");
        String[] comma = pattern.split(values);
        for (int i = 0; i < comma.length; i++) {
            // Now by hyphen.
            pattern = Pattern.compile("-");
            String[] hyphen = pattern.split(comma[i]);

            // Just a single vlan.
            if (hyphen.length == 1) {
                labels.add(Integer.parseInt(hyphen[0].trim()));
            }
            // Two vlans in a range.
            else if (hyphen.length > 1 && hyphen.length < 3) {
                int min = Integer.parseInt(hyphen[0].trim());
                int max = Integer.parseInt(hyphen[1].trim());
                for (int j = min; j < max + 1; j++) {
                    labels.add(j);
                }
            }
            // This is unsupported.
            else {
                throw new IllegalArgumentException("Invalid string format: " + values);
            }
        }

        return labels;
    }

    private final static Comparator<SimpleLabel> compareSimpleLabel = new Comparator<SimpleLabel>() {
        @Override
        public int compare(SimpleLabel t1, SimpleLabel t2) {
            if (t1 == null || t1.getType() == null || t2 == null || t2.getType() == null) {
                throw new IllegalArgumentException();
            }

            String tt1 = t1.getType();
            String tt2 = t2.getType();

            int type = tt1.compareTo(tt2);
            if (type != 0) {
                return type;
            }

            int tv1;
            if (t1.getValue() == null) {
                tv1 = 0;
            }
            else {
                tv1 = Integer.parseInt(t1.getValue());
            }

            int tv2;
            if (t2.getValue() == null) {
                tv2 = 0;
            }
            else {
                tv2 = Integer.parseInt(t2.getValue());
            }

            return (tv1 > tv2 ? 1 : (tv1 == tv2 ? 0 : -1));
        }
    };
}
