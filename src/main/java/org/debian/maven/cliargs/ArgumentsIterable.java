package org.debian.maven.cliargs;

import java.util.Iterator;

public class ArgumentsIterable implements Iterable<Argument> {
    private final String[] args;

    public ArgumentsIterable(String[] args) {
        this.args = args;
    }

    @Override
    public Iterator<Argument> iterator() {
        return new ArgumentsIterator();
    }

    private class ArgumentsIterator implements Iterator<Argument> {
        private int pointer = -1;
        private int next = -1;

        @Override
        public boolean hasNext() {
            if (pointer == next) {
                do {
                    pointer++;
                } while (pointer < args.length && args[pointer].isEmpty());
            }
            return pointer < args.length;
        }

        @Override
        public Argument next() {
            hasNext();
            next = pointer;
            return new Argument(args[next].trim());
        }

        @Override
        public void remove() {
        }
    }
}
