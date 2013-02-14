/**
 * Copyright (c) 2012, NuoDB, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of NuoDB, Inc. nor the names of its contributors may
 *       be used to endorse or promote products derived from this software
 *       without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL NUODB, INC. BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.nuodb.migration.resultset.format.xml;

import com.nuodb.migration.jdbc.model.ValueModelList;
import com.nuodb.migration.resultset.format.ResultSetInputBase;
import com.nuodb.migration.resultset.format.ResultSetInputException;
import com.nuodb.migration.resultset.format.utils.BinaryEncoder;
import com.nuodb.migration.resultset.format.value.SimpleValueFormatModel;
import com.nuodb.migration.resultset.format.value.ValueFormatModel;
import com.nuodb.migration.resultset.format.value.ValueVariant;
import com.nuodb.migration.resultset.format.value.ValueVariantType;
import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import javax.xml.stream.*;
import java.util.Iterator;

import static com.nuodb.migration.jdbc.model.ValueModelFactory.createValueModelList;
import static com.nuodb.migration.resultset.format.value.ValueVariantType.STRING;
import static com.nuodb.migration.resultset.format.value.ValueVariantType.fromAlias;
import static com.nuodb.migration.resultset.format.value.ValueVariants.binary;
import static com.nuodb.migration.resultset.format.value.ValueVariants.string;
import static java.lang.String.format;
import static javax.xml.XMLConstants.NULL_NS_URI;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;

/**
 * @author Sergey Bushik
 */
public class XmlResultSetInput extends ResultSetInputBase implements XmlAttributes {

    private XMLStreamReader reader;
    private Iterator<ValueVariant[]> iterator;

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    public void initInput() {
        String encoding = (String) getAttribute(ATTRIBUTE_ENCODING, ENCODING);

        XMLInputFactory factory = XMLInputFactory.newInstance();
        try {
            if (getReader() != null) {
                reader = factory.createXMLStreamReader(getReader());
            } else if (getInputStream() != null) {
                reader = factory.createXMLStreamReader(getInputStream(), encoding);
            }
        } catch (XMLStreamException e) {
            throw new ResultSetInputException(e);
        }
        iterator = createInputIterator();

    }

    protected Iterator<ValueVariant[]> createInputIterator() {
        return new XmlInputIterator();
    }

    @Override
    protected void doReadBegin() {
        ValueModelList<ValueFormatModel> valueFormatModelList = createValueModelList();
        if (isNextElement(RESULT_SET_ELEMENT) && isNextElement(COLUMNS_ELEMENT)) {
            while (isNextElement(COLUMN_ELEMENT)) {
                String column = getAttributeValue(NULL_NS_URI, ATTRIBUTE_NAME);
                ValueFormatModel valueFormatModel = new SimpleValueFormatModel();
                valueFormatModel.setName(column);
                valueFormatModel.setValueVariantType(fromAlias(getAttributeValue(NULL_NS_URI, ATTRIBUTE_VARIANT)));
                if (column == null) {
                    Location location = reader.getLocation();
                    throw new ResultSetInputException(
                            format("Element %s doesn't have %s attribute [location at %d:%d]",
                                    COLUMN_ELEMENT, ATTRIBUTE_NAME,
                                    location.getLineNumber(), location.getColumnNumber()));
                }
                valueFormatModelList.add(valueFormatModel);
            }
        }
        setValueFormatModelList(valueFormatModelList);
    }

    @Override
    public boolean hasNextRow() {
        return iterator != null && iterator.hasNext();
    }

    @Override
    public void readRow() {
        setValues(iterator.next());
    }

    protected ValueVariant[] doReadRow() {
        ValueVariant[] values = null;
        if (isCurrentElement(ROW_ELEMENT) || isNextElement(ROW_ELEMENT)) {
            int i = 0;
            ValueModelList<ValueFormatModel> valueFormatModelList = getValueFormatModelList();
            values = new ValueVariant[valueFormatModelList.size()];
            while (isNextElement(COLUMN_ELEMENT)) {
                String nil = getAttributeValue(W3C_XML_SCHEMA_INSTANCE_NS_URI, SCHEMA_NIL_ATTRIBUTE);
                if (!StringUtils.equals(nil, "true")) {
                    String value;
                    try {
                         value = reader.getElementText();
                    } catch (XMLStreamException exception) {
                        throw new ResultSetInputException(exception);
                    }
                    ValueVariantType valueVariantType = valueFormatModelList.get(i).getValueVariantType();
                    valueVariantType = valueVariantType != null ? valueVariantType : STRING;
                    switch (valueVariantType) {
                        case BINARY:
                            values[i] = binary(BinaryEncoder.HEX.decode(value));
                        break;
                        case STRING:
                            values[i] = string(XmlEscape.INSTANCE.unescape(value));
                        break;
                    }
                }
                i++;
            }
        }
        return values;
    }

    protected boolean isNextElement(String name) {
        while (reader.getEventType() != XMLStreamConstants.END_DOCUMENT) {
            try {
                switch (reader.next()) {
                    case XMLStreamConstants.START_ELEMENT:
                        return reader.getLocalName().equals(name);
                }
            } catch (XMLStreamException exception) {
                throw new ResultSetInputException(exception);
            }
        }
        return false;
    }

    protected boolean isCurrentElement(String element) {
        return reader.getEventType() == XMLStreamConstants.START_ELEMENT &&
                reader.getLocalName().equals(element);
    }

    protected String getAttributeValue(String namespace, String attribute) {
        for (int index = 0; index < reader.getAttributeCount(); index++) {
            QName name = reader.getAttributeName(index);
            if (name.getNamespaceURI().equals(namespace) && name.getLocalPart().equals(attribute)) {
                return reader.getAttributeValue(index);
            }
        }
        return null;
    }

    @Override
    protected void doReadEnd() {
        try {
            reader.close();
        } catch (XMLStreamException exception) {
            throw new ResultSetInputException(exception);
        }
    }

    class XmlInputIterator implements Iterator<ValueVariant[]> {

        private ValueVariant[] current;

        @Override
        public boolean hasNext() {
            if (current == null) {
                current = doReadRow();
            }
            return current != null;
        }

        @Override
        public ValueVariant[] next() {
            ValueVariant[] next = current;
            current = null;
            if (next == null) {
                next = doReadRow();
                if (next == null) {
                    throw new ResultSetInputException("No more rows available");
                }
            }
            return next;
        }

        @Override
        public void remove() {
            throw new ResultSetInputException("Removal is unsupported operation");
        }
    }
}