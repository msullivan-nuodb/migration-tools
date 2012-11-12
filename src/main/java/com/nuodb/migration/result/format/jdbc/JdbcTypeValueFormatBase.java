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
package com.nuodb.migration.result.format.jdbc;

import com.nuodb.migration.jdbc.type.access.JdbcTypeValueAccessor;

import static com.nuodb.migration.jdbc.type.JdbcTypeNameMap.INSTANCE;

/**
 * @author Sergey Bushik
 */
public abstract class JdbcTypeValueFormatBase<T> implements JdbcTypeValueFormat<T> {

    @Override
    public String getValue(JdbcTypeValueAccessor<T> accessor) {
        try {
            return doGetValue(accessor);
        } catch (JdbcTypeValueException exception) {
            throw exception;
        } catch (Exception exception) {
            throw newColumnValueFormatFailure(accessor, exception);
        }
    }

    protected abstract String doGetValue(JdbcTypeValueAccessor<T> accessor) throws Exception;

    @Override
    public void setValue(JdbcTypeValueAccessor<T> accessor, String value) {
        try {
            doSetValue(accessor, value);
        } catch (JdbcTypeValueException exception) {
            throw exception;
        } catch (Exception exception) {
            throw newColumnValueFormatFailure(accessor, exception);
        }
    }

    protected abstract void doSetValue(JdbcTypeValueAccessor<T> accessor, String value) throws Exception;

    protected JdbcTypeValueException newColumnValueFormatFailure(JdbcTypeValueAccessor accessor, Exception exception) {
        int typeCode = accessor.getColumnModel().getTypeCode();
        return new JdbcTypeValueException(
                String.format("Failed processing jdbc type %s", INSTANCE.getTypeName(typeCode)), exception);
    }
}
