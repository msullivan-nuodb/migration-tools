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
package com.nuodb.migrator.jdbc.dialect;

import com.nuodb.migrator.jdbc.metadata.Column;
import com.nuodb.migrator.jdbc.query.StatementCallback;
import com.nuodb.migrator.jdbc.query.StatementFactory;
import com.nuodb.migrator.jdbc.query.StatementTemplate;
import com.nuodb.migrator.jdbc.resolve.DatabaseInfo;
import com.nuodb.migrator.jdbc.session.Session;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import static com.google.common.collect.Iterables.contains;
import static com.google.common.collect.Iterables.get;
import static com.google.common.collect.Lists.newArrayList;
import static com.nuodb.migrator.jdbc.metadata.inspector.MySQLColumn.getEnum;
import static com.nuodb.migrator.jdbc.resolve.DatabaseInfoUtils.MYSQL;
import static java.lang.String.valueOf;
import static java.sql.Types.*;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.split;

/**
 * Allow for MySQL implicit defaults to move over to the NuoDB schema explicitly
 * <a hre="http://dev.mysql.com/doc/refman/5.5/en/data-type-defaults.html">MySQL Data Type Defaults</a>
 *
 * @author Sergey Bushik
 */
@SuppressWarnings("unchecked")
public class MySQLImplicitDefaultsTranslator extends ColumnTranslatorBase implements ImplicitDefaultsTranslator {
    /**
     * Does not check if strict mode is set, as it's always set by MySQL JDBC driver for the
     */
    public static final boolean CHECK_STRICT_MODE = false;
    public static final String SQL_MODE = "SQL_MODE";
    public static final String STRICT_ALL_TABLES = "STRICT_ALL_TABLES";
    public static final String STRICT_TRANS_TABLES = "STRICT_TRANS_TABLES";
    public static final String SQL_MODE_QUERY = "SELECT @@GLOBAL.SQL_MODE";

    private boolean checkStrictMode = CHECK_STRICT_MODE;
    private boolean implicitDefaults = IMPLICIT_DEFAULTS;
    private String sqlModeQuery = SQL_MODE_QUERY;

    public MySQLImplicitDefaultsTranslator() {
        super(MYSQL);
    }

    @Override
    protected boolean canTranslate(Script script, Column column, DatabaseInfo databaseInfo) {
        return script.getScript() == null && !column.isNullable() && isImplicitDefaults(script);
    }

    /**
     * Checks if any of the strict modes [STRICT_ALL_TABLE, STRICT_TRANS_TABLES] is on for this session
     *
     * @param script and associated session
     * @return true if any of the strict modes is on
     */
    protected boolean isImplicitDefaults(Script script) {
        if (isCheckStrictMode()) {
            Collection<String> sqlMode = getSqlMode(script.getSession(), false);
            return contains(sqlMode, STRICT_ALL_TABLES) || contains(sqlMode, STRICT_TRANS_TABLES);
        } else {
            return isImplicitDefaults();
        }
    }

    protected Collection<String> getSqlMode(Session session, boolean read) {
        Collection<String> sqlModes = (Collection<String>) session.get(SQL_MODE);
        if (sqlModes == null || read) {
            try {
                session.put(SQL_MODE, sqlModes = getSqlMode(session.getConnection()));
            } catch (SQLException exception) {
                throw new TranslatorException("Can't read SQL mode", exception);
            }
        }
        return sqlModes;
    }

    protected Collection<String> getSqlMode(Connection connection) throws SQLException {
        final Collection<String> sqlMode = newArrayList();
        StatementTemplate template = new StatementTemplate(connection);
        template.execute(
                new StatementFactory<Statement>() {
                    @Override
                    public Statement create(Connection connection) throws SQLException {
                        return connection.createStatement();
                    }
                }, new StatementCallback<Statement>() {
                    @Override
                    public void process(Statement statement) throws SQLException {
                        ResultSet resultSet = statement.executeQuery(getSqlModeQuery());
                        while (resultSet.next()) {
                            sqlMode.addAll(asList(split(resultSet.getString(1), ',')));
                        }
                    }
                }
        );
        return sqlMode;
    }

    /**
     * If strict mode is not enabled, MySQL sets the column to the implicit default value for the column data type: <ul>
     * <li>For numeric types, the default is 0</li> <li>Integer or floating-point types declared with the AUTO_INCREMENT
     * attribute, the default is the next value in the sequence. This is handled by sequences</li> <li>For string types
     * other than ENUM, the default value is the empty string</li> <li>For ENUM, the default is the first enumeration
     * value</li> <li>For date and time types other than TIMESTAMP, the default is the appropriate “zero” value for the
     * type, we skip this</li> <li>For the first TIMESTAMP column in a table, the default value is the current date and
     * time. This is converted to an explicit thing by MySQL, so we are OK</li> </ul>
     *
     * @param script       default source script to translate
     * @param column       column for which the default value is translated
     * @param databaseInfo target database info
     * @return string translated according to implicit rules
     */
    @Override
    protected Script translate(Script script, Column column, DatabaseInfo databaseInfo) {
        String result;
        switch (column.getTypeCode()) {
            case TINYINT:
            case SMALLINT:
            case INTEGER:
            case BIGINT:
            case FLOAT:
            case REAL:
            case DOUBLE:
            case NUMERIC:
            case DECIMAL:
                result = valueOf(0);
                break;
            case CHAR:
            case VARCHAR:
            case BINARY:
            case VARBINARY:
            case BLOB:
            case CLOB:
                Collection<String> values = getEnum(column);
                if (!values.isEmpty()) {
                    result = get(values, 0);
                } else {
                    result = EMPTY;
                }
                break;
            default:
                result = script.getScript();
        }
        // case ENUM, and SET
        return result != null ? new SimpleScript(result, databaseInfo) : null;
    }

    @Override
    public boolean isImplicitDefaults() {
        return implicitDefaults;
    }

    @Override
    public void setImplicitDefaults(boolean implicitDefaults) {
        this.implicitDefaults = implicitDefaults;
    }

    public boolean isCheckStrictMode() {
        return checkStrictMode;
    }

    public void setCheckStrictMode(boolean checkStrictMode) {
        this.checkStrictMode = checkStrictMode;
    }

    public String getSqlModeQuery() {
        return sqlModeQuery;
    }

    public void setSqlModeQuery(String sqlModeQuery) {
        this.sqlModeQuery = sqlModeQuery;
    }
}