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
package com.nuodb.migration.jdbc.metadata.inspector;

import com.nuodb.migration.jdbc.connection.ConnectionProvider;
import com.nuodb.migration.jdbc.connection.ConnectionServices;
import com.nuodb.migration.jdbc.connection.JdbcPoolingConnectionProvider;
import com.nuodb.migration.jdbc.dialect.DialectResolver;
import com.nuodb.migration.jdbc.dialect.SimpleDialectResolver;
import com.nuodb.migration.jdbc.metadata.Database;
import com.nuodb.migration.jdbc.metadata.MetaDataType;
import com.nuodb.migration.spec.JdbcConnectionSpec;
import com.nuodb.migration.utils.SimplePriorityList;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;

import static com.nuodb.migration.jdbc.metadata.MetaDataType.*;

/**
 * Reads database meta data and creates meta model from it. Root meta model object is {@link Database} containing set of
 * catalogs, each catalog has a collection of schemas and schema is a wrapper of collection of a tables.
 *
 * @author Sergey Bushik
 */
public class InspectionManager {

    private Connection connection;
    private InspectionScope inspectionScope;
    private DialectResolver dialectResolver = new SimpleDialectResolver();
    private Collection<Inspector> inspectors = new SimplePriorityList<Inspector>();
    private MetaDataType[] objectTypes = MetaDataType.TYPES;

    public InspectionManager() {
        addInspector(new SimpleDatabaseInspector());
        addInspector(new SimpleCatalogInspector());

        InspectorResolver schemaInspector = new InspectorResolver(SCHEMA, new SimpleSchemaInspector());
        schemaInspector.register("NuoDB", new NuoDBSchemaInspector());
        addInspector(schemaInspector);

        InspectorResolver tableInspector = new InspectorResolver(TABLE, new SimpleTableInspector());
        tableInspector.register("NuoDB", new NuoDBTableInspector());
        addInspector(tableInspector);

        InspectorResolver columnInspector = new InspectorResolver(COLUMN, new SimpleColumnInspector());
        columnInspector.register("NuoDB", new NuoDBColumnInspector());
        addInspector(columnInspector);

        InspectorResolver indexInspector = new InspectorResolver(INDEX, new SimpleIndexInspector());
        indexInspector.register("NuoDB", new NuoDBIndexInspector());
        addInspector(indexInspector);

        InspectorResolver primaryKeyInspector = new InspectorResolver(PRIMARY_KEY, new SimplePrimaryKeyInspector());
        primaryKeyInspector.register("NuoDB", new NuoDBPrimaryKeyInspector());
        addInspector(primaryKeyInspector);

        InspectorResolver foreignKeyInspector = new InspectorResolver(FOREIGN_KEY, new SimpleForeignKeyInspector());
        foreignKeyInspector.register("NuoDB", new NuoDBForeignKeyInspector());
        addInspector(foreignKeyInspector);

        InspectorResolver checkInspector = new InspectorResolver(CHECK);
        checkInspector.register("NuoDB", new NuoDBCheckInspector());
        checkInspector.register("PostgreSQL", new PostgreSQLCheckInspector());
        checkInspector.register("Microsoft SQL Server", new MSSQLServerCheckInspector());
        checkInspector.register("Oracle", new OracleCheckInspector());
        addInspector(checkInspector);

        InspectorResolver autoIncrementInspector = new InspectorResolver(AUTO_INCREMENT);
        autoIncrementInspector.register("MySQL", new MySQLAutoIncrementInspector());
        autoIncrementInspector.register("PostgreSQL", new PostgreSQLAutoIncrementInspector());
        autoIncrementInspector.register("Microsoft SQL Server", new MSSQLServerAutoIncrementInspector());
        addInspector(autoIncrementInspector);
    }

    public InspectionResults inspect() throws SQLException {
        return inspect(MetaDataType.TYPES);
    }

    public InspectionResults inspect(MetaDataType... objectTypes) throws SQLException {
        return inspect(getInspectionScope(), objectTypes);
    }

    public InspectionResults inspect(InspectionScope inspectionScope, MetaDataType... objectTypes) throws SQLException {
        InspectionContext inspectionContext = createInspectionContext(objectTypes);
        try {
            inspectionContext.inspect(inspectionScope, objectTypes);
        } finally {
            releaseInspectionContext(inspectionContext);
        }
        return inspectionContext.getInspectionResults();
    }

    protected InspectionContext createInspectionContext(MetaDataType[] objectTypes) {
        return new SimpleInspectionContext(this, objectTypes);
    }

    protected void releaseInspectionContext(InspectionContext inspectionContext) throws SQLException {
        inspectionContext.getConnection().commit();
    }

    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public DialectResolver getDialectResolver() {
        return dialectResolver;
    }

    public void setDialectResolver(DialectResolver dialectResolver) {
        this.dialectResolver = dialectResolver;
    }

    public InspectionScope getInspectionScope() {
        return inspectionScope;
    }

    public void setInspectionScope(InspectionScope inspectionScope) {
        this.inspectionScope = inspectionScope;
    }

    public MetaDataType[] getObjectTypes() {
        return objectTypes;
    }

    public void setObjectTypes(MetaDataType[] objectTypes) {
        this.objectTypes = objectTypes;
    }

    public void addInspector(Inspector inspector) {
        inspectors.add(inspector);
    }

    public Collection<Inspector> getInspectors() {
        return inspectors;
    }

    public void setInspectors(Collection<Inspector> inspectors) {
        this.inspectors = inspectors;
    }

    public static void main(String[] args) throws Exception {
        JdbcConnectionSpec mysql = new JdbcConnectionSpec();
        mysql.setDriverClassName("com.mysql.jdbc.Driver");
        mysql.setUrl("jdbc:mysql://localhost:3306/generate-schema");
        mysql.setUsername("root");

        JdbcConnectionSpec nuodb = new JdbcConnectionSpec();
        nuodb.setDriverClassName("com.nuodb.jdbc.Driver");
        nuodb.setUrl("jdbc:com.nuodb://localhost/test");
        nuodb.setSchema("hockey");
        nuodb.setUsername("dba");
        nuodb.setPassword("goalie");

        ConnectionProvider connectionProvider = new JdbcPoolingConnectionProvider(nuodb);
        ConnectionServices connectionServices = connectionProvider.getConnectionServices();
        try {
            InspectionManager inspectionManager = new InspectionManager();
            inspectionManager.setInspectionScope(
                    new TableInspectionScope(connectionServices.getCatalog(), connectionServices.getSchema()));
            inspectionManager.setConnection(connectionServices.getConnection());
            Database database = inspectionManager.inspect().getObject(MetaDataType.DATABASE);
            System.out.println(database);
        } finally {
            connectionServices.close();
        }
    }
}
