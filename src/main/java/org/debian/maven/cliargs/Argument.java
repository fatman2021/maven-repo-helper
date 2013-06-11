package org.debian.maven.cliargs;

public class Argument {
    public final String name;
    public final String value;
    public final Type type;

    Argument(final String arg) {
        final String trimmed = arg.trim();

        if (trimmed.startsWith("--")) {
            type = Type.LONG;
            int equalsPosition = trimmed.indexOf("=");
            if (-1 == equalsPosition) {
                name = trimmed.substring(2);
                value = null;
            } else {
                name = trimmed.substring(2, equalsPosition);
                value = trimmed.substring(equalsPosition + 1);
            }
        } else if (trimmed.startsWith("-")) {
            type = Type.SHORT;
            name = trimmed.substring(1, 2);
            value = trimmed.length() <= 2 ? null : trimmed.substring(2);
        } else {
            type = Type.ARG;
            name = null;
            value = trimmed;
        }
    }

    public static enum Type {
        LONG, SHORT, ARG
    }
}
