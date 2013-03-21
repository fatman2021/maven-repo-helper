package org.debian.maven.util;

/*
 * Copyright 2013 Thomas Koch <thomas@koch.ro>
 * Copyright 2009 Ludovic Claude.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class XMLWriterWrapper {
    private final XMLStreamWriter writer;

    public XMLWriterWrapper(XMLStreamWriter writer) {
        super();
        this.writer = writer;
    }

    public XMLStreamWriter getWriter() {
        return writer;
    }

    public void indent(int inLevel) throws XMLStreamException {
        writer.writeCharacters("\n");
        for (int i = 0; i < inLevel; i++) {
            writer.writeCharacters("\t");
        }
    }

    public XMLWriterWrapper writeFilledElement(String element, String content) throws XMLStreamException {
        writer.writeStartElement(element);
        writer.writeCharacters(content);
        writer.writeEndElement();
        return this;
    }

    public XMLWriterWrapper writeFilledElement(String element, String content, int inLevel) throws XMLStreamException {
        indent(inLevel);
        return writeFilledElement(element, content);
    }
}
