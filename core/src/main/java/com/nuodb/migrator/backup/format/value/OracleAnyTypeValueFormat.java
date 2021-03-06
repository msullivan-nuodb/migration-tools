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
package com.nuodb.migrator.backup.format.value;

import com.nuodb.migrator.jdbc.model.Field;
import com.nuodb.migrator.jdbc.type.JdbcValueAccess;

import java.lang.reflect.Method;
import java.util.Map;

import static com.nuodb.migrator.backup.format.value.ValueUtils.string;
import static com.nuodb.migrator.utils.ReflectionUtils.getClassLoader;
import static com.nuodb.migrator.utils.ReflectionUtils.invokeMethod;
import static org.apache.commons.lang3.ArrayUtils.EMPTY_CLASS_ARRAY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Sergey Bushik
 */
public class OracleAnyTypeValueFormat extends LazyInitValueFormatBase<Object> {

    private static final String TYPE_DESCRIPTOR_CLASS_NAME = "oracle.sql.TypeDescriptor";

    private Method getTypeName;

    @Override
    protected Value doGetValue(JdbcValueAccess<Object> access, Map<String, Object> options) throws Exception {
        Object value = access.getValue(options);
        return string(value != null ? (String) invokeMethod(value, getTypeName) : null);
    }

    @Override
    protected void doSetValue(Value variant, JdbcValueAccess<Object> access, Map<String, Object> options)
            throws Exception {
        String value = variant.asString();
        access.setValue(!isEmpty(value) ? value : null, options);
    }

    @Override
    protected void doLazyInit() {
        ClassLoader classLoader = getClassLoader();
        try {
            getTypeName = classLoader.loadClass(TYPE_DESCRIPTOR_CLASS_NAME).getMethod("getTypeName", EMPTY_CLASS_ARRAY);
        } catch (Exception exception) {
            throw new ValueFormatException(exception);
        }
    }

    @Override
    public ValueType getValueType(Field field) {
        return ValueType.STRING;
    }
}
