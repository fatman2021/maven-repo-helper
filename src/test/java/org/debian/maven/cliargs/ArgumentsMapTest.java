package org.debian.maven.cliargs;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArgumentsMapTest {

    @Test
    public void testComplex() {
        ArgumentsMap argsMap = args("-a -b   -thallo --multiple=arg1 --multiple=arg2  --hallo=world --print and even more");
        assertTrue(argsMap.getBoolean(null, "a"));
        assertTrue(argsMap.getBoolean(null, "b"));
        assertTrue(argsMap.getBoolean("print", null));
        assertFalse(argsMap.getBoolean(null, "c"));
        assertFalse(argsMap.getBoolean("notthere", null));

        assertEquals("world", argsMap.getValueLong("hallo", null));
        assertEquals("even", argsMap.getArguments().get(1));
        assertEquals("hallo", argsMap.getValue(null, "t", null));
        assertEquals("default", argsMap.getValue("notthere", null, "default"));
        assertEquals("arg1", argsMap.getValueList("multiple", null).get(0));
        assertEquals("arg2", argsMap.getValueList("multiple", null).get(1));
    }

    @Test
    public void testEmpty() {
        ArgumentsMap argsMap = args(" ");
        assertTrue(argsMap.getArguments().isEmpty());
        assertFalse(argsMap.getBoolean(" ", " "));
    }

    public static ArgumentsMap args(String args) {
        String [] argsArray = args.split(" ");
        return ArgumentsMap.fromArgs(argsArray);
    }
}
