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
package com.nuodb.migrator.jdbc.type;

import com.nuodb.migrator.jdbc.model.Field;
import com.nuodb.migrator.jdbc.model.FieldFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.nuodb.migrator.jdbc.model.FieldFactory.newField;
import static com.nuodb.migrator.utils.ValidationUtils.isNotNull;

/**
 * @author Sergey Bushik
 */
@SuppressWarnings("unchecked")
public class SimpleJdbcValueAccessProvider implements JdbcValueAccessProvider {

    private final JdbcTypeRegistry jdbcTypeRegistry;

    public SimpleJdbcValueAccessProvider(JdbcTypeRegistry jdbcTypeRegistry) {
        isNotNull(jdbcTypeRegistry, "Type registry is required");
        this.jdbcTypeRegistry = jdbcTypeRegistry;
    }

    @Override
    public <T> JdbcValueGetter<T> getJdbcValueGetter(int typeCode) {
        return getJdbcValueGetter(new JdbcTypeDesc(typeCode));
    }

    @Override
    public <T> JdbcValueGetter<T> getJdbcValueGetter(int typeCode, String typeName) {
        return getJdbcValueGetter(new JdbcTypeDesc(typeCode, typeName));
    }

    @Override
    public <T> JdbcValueGetter<T> getJdbcValueGetter(JdbcTypeDesc jdbcTypeDesc) {
        return getJdbcValueGetter(jdbcTypeRegistry.getJdbcType(jdbcTypeDesc, true));
    }

    @Override
    public <T> JdbcValueGetter<T> getJdbcValueGetter(JdbcTypeValue<T> jdbcTypeValue) {
        return new SimpleJdbcValueGetter<T>(jdbcTypeRegistry, jdbcTypeValue);
    }

    @Override
    public JdbcValueSetter getJdbcValueSetter(int typeCode) {
        return getJdbcValueSetter(new JdbcTypeDesc(typeCode));
    }

    @Override
    public JdbcValueSetter getJdbcValueSetter(int typeCode, String typeName) {
        return getJdbcValueSetter(new JdbcTypeDesc(typeCode, typeName));
    }

    @Override
    public JdbcValueSetter getJdbcValueSetter(JdbcTypeDesc jdbcTypeDesc) {
        return getJdbcValueSetter(jdbcTypeRegistry.getJdbcType(jdbcTypeDesc, true));
    }

    @Override
    public JdbcValueSetter getJdbcValueSetter(JdbcTypeValue jdbcTypeValue) {
        return new SimpleJdbcValueSetter(jdbcTypeRegistry, jdbcTypeValue);
    }

    @Override
    public <T> JdbcValueAccess<T> getJdbcValueGetter(Connection connection, ResultSet resultSet,
                                                     int columnIndex, Field field) {
        try {
            return new SimpleJdbcValueAccess<T>(
                    (JdbcValueGetter<T>) getJdbcValueGetter(field.getTypeCode(), field.getTypeName()),
                    connection, resultSet, columnIndex, field);
        } catch (SQLException exception) {
            throw new JdbcValueAccessException(exception);
        }
    }

    @Override
    public <T> JdbcValueAccess<T> getJdbcValueGetter(Connection connection, PreparedStatement statement,
                                                     int columnIndex, Field field) {
        try {
            return new SimpleJdbcValueAccess<T>(
                    getJdbcValueSetter(field.getTypeCode(), field.getTypeName()),
                    connection, statement, columnIndex, field);
        } catch (SQLException exception) {
            throw new JdbcValueAccessException(exception);
        }
    }

    @Override
    public <T> JdbcValueAccess<T> getJdbcValueGetter(Connection connection, ResultSet resultSet, int columnIndex) {
        try {
            return getJdbcValueGetter(connection, resultSet, columnIndex, FieldFactory.newField(resultSet, columnIndex));
        } catch (SQLException exception) {
            throw new JdbcValueAccessException(exception);
        }
    }

    @Override
    public <T> JdbcValueAccess<T> getJdbcValueGetter(Connection connection, PreparedStatement statement,
                                                     int columnIndex) {
        try {
            return getJdbcValueGetter(connection, statement, columnIndex,
                    newField(statement.getMetaData(), columnIndex));
        } catch (SQLException exception) {
            throw new JdbcValueAccessException(exception);
        }
    }
}
