
package org.debian.maven.cliargs;

import static org.junit.Assert.*;

import org.debian.maven.cliargs.Argument.Type;
import org.junit.Test;

public class ArgumentTest {

    @Test
    public void test() {
        parse("-t", "t", null, Type.SHORT);
        parse("--template", "template", null, Type.LONG);
        parse("argum", null, "argum", Type.ARG);

        // test trimming left
        parse(" -t", "t", null, Type.SHORT);
        parse(" --template", "template", null, Type.LONG);
        parse(" argum", null, "argum", Type.ARG);

        // test trimming right
        parse("-t ", "t", null, Type.SHORT);
        parse("--template ", "template", null, Type.LONG);
        parse("argum ", null, "argum", Type.ARG);

        // test option parameters
        parse("-thallo", "t", "hallo", Type.SHORT);
        parse("--template=hi", "template", "hi", Type.LONG);

        // test option parameters and trimming
        parse(" -thallo ", "t", "hallo", Type.SHORT);
        parse(" --template=hi ", "template", "hi", Type.LONG);
    }

    private void parse(String input, String name, String value, Type type) {
        Argument argument = new Argument(input);
        assertEquals("name does not match "+input, name, argument.name);
        assertEquals("value does not match "+input, value, argument.value);
        assertEquals("type does not match "+input, type.toString(), argument.type.toString());
    }
}
