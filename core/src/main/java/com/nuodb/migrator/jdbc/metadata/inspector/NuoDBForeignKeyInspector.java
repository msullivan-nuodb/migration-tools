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

import com.nuodb.migrator.jdbc.metadata.Column;
import com.nuodb.migrator.jdbc.metadata.ForeignKey;
import com.nuodb.migrator.jdbc.metadata.Identifier;
import com.nuodb.migrator.jdbc.metadata.Table;
import com.nuodb.migrator.jdbc.query.StatementCallback;
import com.nuodb.migrator.jdbc.query.StatementFactory;
import com.nuodb.migrator.jdbc.query.StatementTemplate;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import static com.nuodb.migrator.jdbc.metadata.inspector.InspectionResultsUtils.addTable;
import static com.nuodb.migrator.jdbc.metadata.inspector.NuoDBInspectorUtils.validateInspectionScope;
import static java.sql.ResultSet.CONCUR_READ_ONLY;
import static java.sql.ResultSet.TYPE_FORWARD_ONLY;

/**
 * @author Sergey Bushik
 */
public class NuoDBForeignKeyInspector extends ForeignKeyInspectorBase {

    private static final String QUERY =
            "SELECT PRIMARYTABLE.SCHEMA AS PKTABLE_SCHEM, PRIMARYTABLE.TABLENAME AS PKTABLE_NAME,\n" +
            "       PRIMARYFIELD.FIELD AS PKCOLUMN_NAME, FOREIGNTABLE.SCHEMA AS FKTABLE_SCHEM,\n" +
            "       FOREIGNTABLE.TABLENAME AS FKTABLE_NAME, FOREIGNFIELD.FIELD AS FKCOLUMN_NAME,\n" +
            "       FOREIGNKEYS.POSITION+1 AS KEY_SEQ, FOREIGNKEYS.UPDATERULE AS UPDATE_RULE,\n" +
            "       FOREIGNKEYS.DELETERULE AS DELETE_RULE, FOREIGNKEYS.DEFERRABILITY AS DEFERRABILITY\n" +
            "FROM SYSTEM.FOREIGNKEYS\n" +
            "INNER JOIN SYSTEM.TABLES PRIMARYTABLE ON PRIMARYTABLEID=PRIMARYTABLE.TABLEID\n" +
            "INNER JOIN SYSTEM.FIELDS PRIMARYFIELD ON PRIMARYTABLE.SCHEMA=PRIMARYFIELD.SCHEMA\n" +
            "AND PRIMARYTABLE.TABLENAME=PRIMARYFIELD.TABLENAME\n" +
            "AND FOREIGNKEYS.PRIMARYFIELDID=PRIMARYFIELD.FIELDID\n" +
            "INNER JOIN SYSTEM.TABLES FOREIGNTABLE ON FOREIGNTABLEID=FOREIGNTABLE.TABLEID\n" +
            "INNER JOIN SYSTEM.FIELDS FOREIGNFIELD ON FOREIGNTABLE.SCHEMA=FOREIGNFIELD.SCHEMA\n" +
            "AND FOREIGNTABLE.TABLENAME=FOREIGNFIELD.TABLENAME\n" +
            "AND FOREIGNKEYS.FOREIGNFIELDID=FOREIGNFIELD.FIELDID\n" +
            "WHERE SCHEMA=? AND TABLENAME=? ORDER BY PKTABLE_SCHEM, PKTABLE_NAME, KEY_SEQ ASC";

    public static void main(String[] args) {
        System.out.println(QUERY);
    }
    @Override
    public void inspectScope(InspectionContext inspectionContext,
                             TableInspectionScope inspectionScope) throws SQLException {
        validateInspectionScope(inspectionScope);
        super.inspectScope(inspectionContext, inspectionScope);
    }

    @Override
    protected Collection<? extends TableInspectionScope> createInspectionScopes(Collection<? extends Table> tables) {
        return createTableInspectionScopes(tables);
    }

    @Override
    protected void inspectScopes(final InspectionContext inspectionContext,
                                 final Collection<? extends TableInspectionScope> inspectionScopes) throws SQLException {
        StatementTemplate template = new StatementTemplate(inspectionContext.getConnection());
        template.execute(
                new StatementFactory<PreparedStatement>() {
                    @Override
                    public PreparedStatement create(Connection connection) throws SQLException {
                        return connection.prepareStatement(QUERY, TYPE_FORWARD_ONLY, CONCUR_READ_ONLY);
                    }
                },
                new StatementCallback<PreparedStatement>() {
                    @Override
                    public void execute(PreparedStatement statement) throws SQLException {
                        for (TableInspectionScope inspectionScope : inspectionScopes) {
                            statement.setString(1, inspectionScope.getSchema());
                            statement.setString(2, inspectionScope.getTable());
                            inspect(inspectionContext, statement.executeQuery());
                        }
                    }
                }
        );
    }

    private void inspect(InspectionContext inspectionContext, ResultSet foreignKeys) throws SQLException {
        InspectionResults inspectionResults = inspectionContext.getInspectionResults();
        ForeignKey foreignKey = null;
        while (foreignKeys.next()) {
            Table primaryTable = addTable(inspectionResults, null, foreignKeys.getString("FKTABLE_SCHEM"),
                    foreignKeys.getString("FKTABLE_NAME"));

            final Column primaryColumn = primaryTable.addColumn(foreignKeys.getString("FKCOLUMN_NAME"));

            Table foreignTable = addTable(inspectionResults, null, foreignKeys.getString("PKTABLE_SCHEM"),
                    foreignKeys.getString("PKTABLE_NAME"));

            final Column foreignColumn = foreignTable.addColumn(foreignKeys.getString("PKCOLUMN_NAME"));
            int position = foreignKeys.getInt("KEY_SEQ");

            if (position == 1) {
                primaryTable.addForeignKey(foreignKey = new ForeignKey(Identifier.EMPTY_IDENTIFIER));
                foreignKey.setPrimaryTable(primaryTable);
                foreignKey.setForeignTable(foreignTable);
                foreignKey.setUpdateAction(getReferentialAction(foreignKeys.getInt("UPDATE_RULE")));
                foreignKey.setDeleteAction(getReferentialAction(foreignKeys.getInt("DELETE_RULE")));
                foreignKey.setDeferrability(getDeferrability(foreignKeys.getInt("DEFERRABILITY")));
                inspectionResults.addObject(foreignKey);
            }
            if (foreignKey != null) {
                foreignKey.addReference(primaryColumn, foreignColumn, position);
            }
        }
    }

    @Override
    protected boolean supports(TableInspectionScope inspectionScope) {
        return inspectionScope.getSchema() != null && inspectionScope.getTable() != null;
    }
}