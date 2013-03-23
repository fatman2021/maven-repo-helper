package org.debian.maven.util;

public class Strings {

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

    public static String repeat(String item, int times) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i < times; ++i) {
            sb.append(item);
        }
        return sb.toString();
    }
}
