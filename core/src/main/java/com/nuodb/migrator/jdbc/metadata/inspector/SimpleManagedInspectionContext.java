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
package com.nuodb.migrator.jdbc.metadata.inspector;

import com.nuodb.migrator.jdbc.JdbcUtils;
import com.nuodb.migrator.jdbc.dialect.Dialect;
import com.nuodb.migrator.jdbc.metadata.MetaData;
import com.nuodb.migrator.jdbc.metadata.MetaDataType;
import com.nuodb.migrator.jdbc.query.ParameterizedQuery;
import com.nuodb.migrator.jdbc.query.Query;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Map;

import static com.google.common.collect.Maps.newHashMap;

/**
 * @author Sergey Bushik
 */
public class SimpleManagedInspectionContext<M extends MetaData, I extends InspectionScope> implements
        ManagedInspectionContext<I> {

    private Map<Query, Statement> statements = newHashMap();
    private ManagedInspector<M, I> managedInspector;
    private InspectionContext inspectionContext;

    public SimpleManagedInspectionContext(ManagedInspector<M, I> managedInspector,
                                          InspectionContext inspectionContext) {
        this.managedInspector = managedInspector;
        this.inspectionContext = inspectionContext;
    }

    @Override
    public Statement createStatement(I inspectionScope, Query query) throws SQLException {
        Statement statement;
        if (query instanceof ParameterizedQuery) {
            Query innerQuery = ((ParameterizedQuery)query).getQuery();
            statement = statements.get(innerQuery);
            if (statement == null) {
                statements.put(innerQuery,
                        statement = managedInspector.createStatement(this, inspectionScope, query));
            }
        } else {
            statement = managedInspector.createStatement(this, inspectionScope, query);
        }
        return statement;
    }

    @Override
    public void closeStatement(I inspectionScope, Query query, Statement statement) throws SQLException {
    }

    @Override
    public void closeStatements() throws SQLException {
        for (Statement statement : statements.values()) {
            JdbcUtils.close(statement);
        }
    }

    @Override
    public Dialect getDialect() throws SQLException {
        return inspectionContext.getDialect();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return inspectionContext.getConnection();
    }

    @Override
    public InspectionResults getInspectionResults() {
        return inspectionContext.getInspectionResults();
    }

    @Override
    public void inspect(InspectionScope scope) throws SQLException {
        inspectionContext.inspect(scope);
    }

    @Override
    public void inspect(InspectionScope scope, MetaDataType... objectTypes) throws SQLException {
        inspectionContext.inspect(scope, objectTypes);
    }

    @Override
    public void inspect(MetaData object) throws SQLException {
        inspectionContext.inspect(object);
    }

    @Override
    public void inspect(MetaData object, MetaDataType... objectTypes) throws SQLException {
        inspectionContext.inspect(object, objectTypes);
    }

    @Override
    public void inspect(Collection<MetaData> objects, MetaDataType... objectTypes) throws SQLException {
        inspectionContext.inspect(objects, objectTypes);
    }

    @Override
    public void close() throws SQLException {
        inspectionContext.close();
    }
}