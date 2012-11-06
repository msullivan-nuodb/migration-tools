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
package com.nuodb.migration.jdbc.type.access;

import com.nuodb.migration.jdbc.metamodel.ValueModel;
import com.nuodb.migration.jdbc.type.JdbcType;
import com.nuodb.migration.jdbc.type.JdbcTypeRegistry;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * @author Sergey Bushik
 */
public interface JdbcTypeAccessor extends JdbcTypeRegistry {

    <T> JdbcTypeValueGetter<T> createValueGetter(int typeCode);

    <T> JdbcTypeValueGetter<T> createValueGetter(JdbcType<T> jdbcType);

    <T> JdbcTypeValueSetter<T> createValueSetter(int typeCode);

    <T> JdbcTypeValueSetter<T> createValueSetter(JdbcType<T> jdbcType);

    <T> JdbcTypeValueAccessor<T> createResultSetAccessor(ResultSet resultSet, int column);

    <T> JdbcTypeValueAccessor<T> createResultSetAccessor(ResultSet resultSet, int column, ValueModel valueModel);

    <T> JdbcTypeValueAccessor<T> createStatementAccessor(PreparedStatement statement, int column);

    <T> JdbcTypeValueAccessor<T> createStatementAccessor(PreparedStatement statement, int column, ValueModel valueModel);
}