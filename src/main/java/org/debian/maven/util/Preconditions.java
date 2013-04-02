package org.debian.maven.util;

public class Preconditions {

    public static <T> T checkNotNull(T reference) {
        if (reference == null) throw new NullPointerException();
        return reference;
    }

    public static String checkNotEmpty(String s) {
        if ("" == checkNotNull(s)) throw new IllegalArgumentException("Empty String");
        return s;
    }
}
