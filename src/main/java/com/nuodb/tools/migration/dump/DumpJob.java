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
package com.nuodb.tools.migration.dump;

import com.google.common.collect.Lists;
import com.nuodb.tools.migration.output.catalog.Entry;
import com.nuodb.tools.migration.output.catalog.EntryCatalog;
import com.nuodb.tools.migration.output.catalog.EntryImpl;
import com.nuodb.tools.migration.output.catalog.EntryWriter;
import com.nuodb.tools.migration.output.format.DataFormatFactory;
import com.nuodb.tools.migration.output.format.DataOutputFormat;
import com.nuodb.tools.migration.jdbc.JdbcServices;
import com.nuodb.tools.migration.jdbc.connection.ConnectionCallback;
import com.nuodb.tools.migration.jdbc.connection.ConnectionProvider;
import com.nuodb.tools.migration.jdbc.metamodel.Database;
import com.nuodb.tools.migration.jdbc.metamodel.DatabaseInspector;
import com.nuodb.tools.migration.jdbc.metamodel.Table;
import com.nuodb.tools.migration.jdbc.query.*;
import com.nuodb.tools.migration.job.JobBase;
import com.nuodb.tools.migration.job.JobExecution;
import com.nuodb.tools.migration.spec.NativeQuerySpec;
import com.nuodb.tools.migration.spec.SelectQuerySpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import static com.nuodb.tools.migration.jdbc.metamodel.ObjectType.*;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * @author Sergey Bushik
 */
public class DumpJob extends JobBase {

    private static final String QUERY_ENTRY_NAME = "query-%1$tH-%1$tM-%1$tS";

    protected final Log log = LogFactory.getLog(getClass());

    private JdbcServices jdbcServices;
    private EntryCatalog entryCatalog;
    private Collection<SelectQuerySpec> selectQuerySpecs;
    private Collection<NativeQuerySpec> nativeQuerySpecs;
    private String outputType;
    private Map<String, String> outputAttributes;
    private DataFormatFactory<DataOutputFormat> outputDataFormatFactory;

    @Override
    public void execute(final JobExecution execution) throws Exception {
        ConnectionProvider connectionProvider = jdbcServices.getConnectionProvider();
        connectionProvider.execute(new ConnectionCallback() {
            @Override
            public void execute(Connection connection) throws SQLException {
                DatabaseInspector databaseInspector = jdbcServices.getDatabaseIntrospector();
                databaseInspector.withObjectTypes(CATALOG, SCHEMA, TABLE, COLUMN);
                databaseInspector.withConnection(connection);
                Database database = databaseInspector.inspect();

                EntryWriter entryWriter = entryCatalog.openWriter();
                try {
                    for (SelectQuery selectQuery : createSelectQueries(database, selectQuerySpecs)) {
                        dump(execution, connection, selectQuery, entryWriter, createEntry(selectQuery, outputType));
                    }
                    for (NativeQuery nativeQuery : createNativeQueries(database, nativeQuerySpecs)) {
                        dump(execution, connection, nativeQuery, entryWriter, createEntry(nativeQuery, outputType));
                    }
                } finally {
                    entryWriter.close();
                }
            }
        });
    }

    protected Entry createEntry(SelectQuery selectQuery, String type) {
        Table table = selectQuery.getTables().get(0);
        return new EntryImpl(table.getName(), type);
    }

    protected Entry createEntry(NativeQuery nativeQuery, String type) {
        return new EntryImpl(String.format(QUERY_ENTRY_NAME, new Date()), type);
    }

    protected void dump(JobExecution execution, Connection connection, Query query, EntryWriter entryWriter,
                        Entry entry) throws SQLException {
        entryWriter.addEntry(entry);
        dump(execution, connection, query, entryWriter.getEntryOutput(entry));
    }

    protected void dump(JobExecution execution, Connection connection, Query query,
                        OutputStream output) throws SQLException {
        try {
            DataOutputFormat outputFormat = outputDataFormatFactory.createDataFormat(outputType);
            outputFormat.setAttributes(outputAttributes);
            outputFormat.setJdbcTypeAccessor(jdbcServices.getJdbcTypeAccessor());
            outputFormat.setOutputStream(output);
            dump(execution, connection, query, outputFormat);
        } finally {
            closeQuietly(output);
        }
    }

    protected void dump(final JobExecution execution, final Connection connection, final Query query,
                        final DataOutputFormat output) throws SQLException {
        QueryTemplate queryTemplate = new QueryTemplate(connection);
        queryTemplate.execute(
                new StatementBuilder<PreparedStatement>() {
                    @Override
                    public PreparedStatement build(Connection connection) throws SQLException {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Preparing SQL query %1$s", query.toQuery()));
                        }
                        PreparedStatement statement = connection.prepareStatement(query.toQuery(),
                                TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
                        // forces driver to stream result setValue http://goo.gl/kl1Nr
                        statement.setFetchSize(Integer.MIN_VALUE);
                        return statement;
                    }
                },
                new StatementCallback<PreparedStatement>() {
                    @Override
                    public void execute(PreparedStatement statement) throws SQLException {
                        if (log.isDebugEnabled()) {
                            log.debug(String.format("Writing dump with %1$s", output.getClass().getName()));
                        }
                        ResultSet resultSet = statement.executeQuery();
                        output.outputBegin(resultSet);
                        while (execution.isRunning() && resultSet.next()) {
                            output.outputRow(resultSet);
                        }
                        output.outputEnd(resultSet);
                    }
                }
        );
    }

    protected Collection<SelectQuery> createSelectQueries(Database database,
                                                          Collection<SelectQuerySpec> selectQuerySpecs) {
        Collection<SelectQuery> selectQueries = Lists.newArrayList();
        if (selectQuerySpecs.isEmpty()) {
            selectQueries.addAll(createSelectQueries(database));
        } else {
            for (SelectQuerySpec selectQuerySpec : selectQuerySpecs) {
                selectQueries.add(createSelectQuery(database, selectQuerySpec));
            }
        }
        return selectQueries;
    }

    protected Collection<SelectQuery> createSelectQueries(Database database) {
        Collection<SelectQuery> selectQueries = Lists.newArrayList();
        for (Table table : database.listTables()) {
            SelectQueryBuilder builder = new SelectQueryBuilder();
            builder.setTable(table);
            builder.setQualifyNames(true);
            selectQueries.add(builder.build());
        }
        return selectQueries;
    }

    protected SelectQuery createSelectQuery(Database database, SelectQuerySpec selectQuerySpec) {
        String tableName = selectQuerySpec.getTable();
        SelectQueryBuilder builder = new SelectQueryBuilder();
        builder.setQualifyNames(true);
        builder.setTable(database.findTable(tableName));
        builder.setColumns(selectQuerySpec.getColumns());
        builder.addFilter(selectQuerySpec.getFilter());
        return builder.build();
    }

    protected Collection<NativeQuery> createNativeQueries(Database database,
                                                          Collection<NativeQuerySpec> nativeQuerySpecs) {
        Collection<NativeQuery> queries = Lists.newArrayList();
        for (NativeQuerySpec nativeQuerySpec : nativeQuerySpecs) {
            NativeQueryBuilder builder = new NativeQueryBuilder();
            builder.setQuery(nativeQuerySpec.getQuery());
            queries.add(builder.build());
        }
        return queries;
    }

    public JdbcServices getJdbcServices() {
        return jdbcServices;
    }

    public void setJdbcServices(JdbcServices jdbcServices) {
        this.jdbcServices = jdbcServices;
    }

    public EntryCatalog getEntryCatalog() {
        return entryCatalog;
    }

    public void setEntryCatalog(EntryCatalog entryCatalog) {
        this.entryCatalog = entryCatalog;
    }

    public Collection<SelectQuerySpec> getSelectQuerySpecs() {
        return selectQuerySpecs;
    }

    public void setSelectQuerySpecs(Collection<SelectQuerySpec> selectQuerySpecs) {
        this.selectQuerySpecs = selectQuerySpecs;
    }

    public Collection<NativeQuerySpec> getNativeQuerySpecs() {
        return nativeQuerySpecs;
    }

    public void setNativeQuerySpecs(Collection<NativeQuerySpec> nativeQuerySpecs) {
        this.nativeQuerySpecs = nativeQuerySpecs;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public Map<String, String> getOutputAttributes() {
        return outputAttributes;
    }

    public void setOutputAttributes(Map<String, String> outputAttributes) {
        this.outputAttributes = outputAttributes;
    }

    public DataFormatFactory<DataOutputFormat> getOutputDataFormatFactory() {
        return outputDataFormatFactory;
    }

    public void setOutputDataFormatFactory(DataFormatFactory<DataOutputFormat> outputDataFormatFactory) {
        this.outputDataFormatFactory = outputDataFormatFactory;
    }
}