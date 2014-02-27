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

import com.nuodb.migrator.jdbc.metadata.Catalog;
import com.nuodb.migrator.jdbc.query.ParameterizedQuery;
import com.nuodb.migrator.jdbc.query.Query;
import com.nuodb.migrator.jdbc.query.SelectQuery;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static com.nuodb.migrator.jdbc.metadata.MetaDataType.SCHEMA;
import static com.nuodb.migrator.jdbc.metadata.inspector.InspectionResultsUtils.addSchema;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * @author Sergey Bushik
 */
public class MSSQLServerSchemaInspector extends ManagedInspectorBase<Catalog, SchemaInspectionScope> {

    public MSSQLServerSchemaInspector() {
        super(SCHEMA, SchemaInspectionScope.class);
    }

    @Override
    protected SchemaInspectionScope createInspectionScope(Catalog catalog) {
        return new SchemaInspectionScope(catalog.getName(), null);
    }

    @Override
    protected Query createQuery(InspectionContext inspectionContext, SchemaInspectionScope schemaInspectionScope) {
        Collection<Object> parameters = newArrayList();
        SelectQuery query = new SelectQuery();
        if (isEmpty(schemaInspectionScope.getCatalog())) {
            query.column("DB_NAME() AS TABLE_CATALOG");
        } else {
            query.column("? AS TABLE_CATALOG");
            parameters.add(schemaInspectionScope.getCatalog());
        }
        query.column("s.name AS TABLE_SCHEMA");
        String catalog = isEmpty(schemaInspectionScope.getCatalog()) ? EMPTY : schemaInspectionScope.getCatalog() + ".";
        query.from(catalog + "sys.schemas s");
        query.leftJoin(catalog + "sys.sysusers u", "u.name=s.name");

        query.where("(issqlrole=0 OR issqlrole IS NULL)");
        if (!isEmpty(schemaInspectionScope.getSchema())) {
            query.where("s.name LIKE ?");
            parameters.add(schemaInspectionScope.getSchema());
        }
        query.orderBy(newArrayList("TABLE_CATALOG", "TABLE_SCHEMA"));
        return new ParameterizedQuery(query, parameters);
    }

    @Override
    protected void processResultSet(InspectionContext inspectionContext, ResultSet schemas) throws SQLException {
        InspectionResults inspectionResults = inspectionContext.getInspectionResults();
        while (schemas.next()) {
            addSchema(inspectionResults, schemas.getString("TABLE_CATALOG"), schemas.getString("TABLE_SCHEMA"));
        }
    }
}
