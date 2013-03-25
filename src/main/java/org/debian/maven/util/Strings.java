package org.debian.maven.util;

public class Strings {

    /**
     * Join all items with the glue string.
     *
     * The toString() method is used on the items.
     */
    public static String join(Iterable<? extends Object> items, String glue) {
        StringBuffer sb = new StringBuffer();
        boolean first = true;
        for (Object item : items) {
            if(!first) sb.append(glue);
            first = false;
            sb.append(item);
        }
        return sb.toString();
    }

    /**
     * Return a string repeating the item the given number of times.
     */
    public static String repeat(String item, int times) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i < times; ++i) {
            sb.append(item);
        }
        return sb.toString();
    }

    /**
     * Format a line feed terminated property line for a java properties file.
     *
     * e.g.: mypropertyname=mypropertyvalue\n
     */
    public static String propertyLine(String name, String value) {
        StringBuffer sb = new StringBuffer(name.length() + value.length() + 2);
        sb.append(name);
        sb.append("=");
        sb.append(value);
        sb.append("\n");
        return sb.toString();
    }
}
