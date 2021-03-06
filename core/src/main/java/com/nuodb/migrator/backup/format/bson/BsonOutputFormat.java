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
package com.nuodb.migrator.backup.format.bson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.nuodb.migrator.backup.format.OutputFormatBase;
import com.nuodb.migrator.backup.format.OutputFormatException;
import com.nuodb.migrator.backup.format.value.Value;
import de.undercouch.bson4jackson.BsonFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.BitSet;

import static com.nuodb.migrator.backup.format.utils.BitSetUtils.toByteArray;
import static de.undercouch.bson4jackson.BsonGenerator.Feature.ENABLE_STREAMING;

/**
 * @author Sergey Bushik
 */
public class BsonOutputFormat extends OutputFormatBase implements BsonAttributes {

    private JsonGenerator bsonWriter;

    public BsonOutputFormat() {
        super(MAX_SIZE);
    }

    @Override
    public String getFormat() {
        return FORMAT;
    }

    @Override
    protected void init(OutputStream outputStream) {
        try {
            bsonWriter = createBsonFactory().createJsonGenerator(outputStream);
        } catch (IOException exception) {
            throw new OutputFormatException(exception);
        }
    }

    @Override
    protected void init(Writer writer) {
        try {
            bsonWriter = createBsonFactory().createJsonGenerator(writer);
        } catch (IOException exception) {
            throw new OutputFormatException(exception);
        }
    }

    protected BsonFactory createBsonFactory() {
        BsonFactory factory = new BsonFactory();
        factory.enable(ENABLE_STREAMING);
        return factory;
    }

    @Override
    public void writeStart() {
        try {
            bsonWriter.writeStartObject();
            bsonWriter.writeArrayFieldStart(ROWS_FIELD);
        } catch (IOException exception) {
            throw new OutputFormatException(exception);
        }
    }

    @Override
    public void writeValues(Value[] values) {
        try {
            bsonWriter.writeStartArray();
            BitSet nulls = new BitSet();
            for (int i = 0; i < values.length; i++) {
                nulls.set(i, values[i].isNull());
            }
            if (nulls.isEmpty()) {
                bsonWriter.writeNull();
            } else {
                bsonWriter.writeBinary(toByteArray(nulls));
            }
            for (int i = 0; i < values.length; i++) {
                Value value = values[i];
                if (!value.isNull()) {
                    switch (getValueTypes().get(i)) {
                        case BINARY:
                            bsonWriter.writeBinary(value.asBytes());
                            break;
                        case STRING:
                            bsonWriter.writeString(value.asString());
                            break;
                    }
                }
            }
            bsonWriter.writeEndArray();
        } catch (IOException exception) {
            throw new OutputFormatException(exception);
        }
    }

    @Override
    public void writeEnd() {
        try {
            bsonWriter.writeEndArray();
            bsonWriter.writeEndObject();
            bsonWriter.flush();
        } catch (IOException exception) {
            throw new OutputFormatException(exception);
        }
    }

    @Override
    public void close() {
        try {
            if (bsonWriter != null) {
                bsonWriter.close();
            }
        } catch (IOException exception) {
            throw new OutputFormatException(exception);
        }
    }
}
