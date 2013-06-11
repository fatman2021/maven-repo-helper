package org.debian.maven.cliargs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgumentsMap {
    private static final List<String> EMPTY_LIST = Collections.emptyList();

    private final Map<String, List<String>> longMap = new HashMap<String, List<String>>();
    private final Map<String, List<String>> shortMap = new HashMap<String, List<String>>();
    private final List<String> args = new ArrayList<String>();

    public ArgumentsMap(final ArgumentsIterable it) {
        for (Argument argument : it) {
            switch (argument.type) {
                case LONG:
                    addToMap(longMap, argument.name, argument.value);
                    break;
                case SHORT:
                    addToMap(shortMap, argument.name, argument.value);
                    break;
                case ARG:
                    args.add(argument.value);
                    break;
            }
        }
    }

    private void addToMap(Map<String, List<String>> map, String name, String value) {
        if (!map.containsKey(name)) {
            map.put(name, new ArrayList<String>());
        }
        map.get(name).add(value);
    }

    public List<String> getArguments() {
        return args;
    }

    /**
     * Returns the first argument or the given default value.
     */
    public String getFirstArgument(String defaultArgument) {
        if (args.isEmpty()) {
            return defaultArgument;
        }
        return getFirstArgument();
    }

    public String getFirstArgument() {
        return args.get(0);
    }

    public boolean getBoolean(String longName, String shortName) {
        return longMap.containsKey(longName) || shortMap.containsKey(shortName);
    }

    public boolean getBooleanLong(String longName) {
        return getBoolean(longName, null);
    }

    public String getValue(String longName, String shortName, String defaultValue) {
        if (longMap.containsKey(longName)) {
            return getLastOfList(longMap, longName);
        }
        if (shortMap.containsKey(shortName)) {
            return getLastOfList(shortMap, shortName);
        }
        return defaultValue;
    }

    public File getFile(String longName, String shortName, File defaultFile) {
        String fileName = getValue(longName, shortName, null);
        if (fileName == null) {
            return defaultFile;
        }
        return new File(fileName);
    }

    public List<String> getValueList(String longName, String shortName) {
        List<String> result = new ArrayList<String>();
        result.addAll(getAllValues(longMap, longName));
        result.addAll(getAllValues(shortMap, shortName));
        return result;
    }

    private Collection<String> getAllValues(Map<String, List<String>> map, String name) {
        return map.containsKey(name) ? map.get(name) : EMPTY_LIST;
    }

    public String getValueLong(String longName, String defaultValue) {
        return getValue(longName, null, defaultValue);
    }

    private String getLastOfList(Map<String, List<String>> map, String name) {
        List<String> list = map.get(name);
        return list.get(list.size() - 1);
    }

    public static ArgumentsMap fromArgs(String[] args) {
        return new ArgumentsMap(new ArgumentsIterable(args));
    }
}
