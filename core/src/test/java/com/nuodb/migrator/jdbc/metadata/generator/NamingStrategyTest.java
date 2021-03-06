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
package com.nuodb.migrator.jdbc.metadata.generator;

import com.nuodb.migrator.jdbc.dialect.NuoDBDialect;
import com.nuodb.migrator.jdbc.metadata.Database;
import com.nuodb.migrator.jdbc.metadata.MetaData;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.nuodb.migrator.jdbc.metadata.MetaDataUtils.*;
import static org.testng.Assert.assertEquals;

/**
 * @author Sergey Bushik
 */
public class NamingStrategyTest {

    private ScriptGeneratorManager scriptGeneratorManager;

    @BeforeMethod
    public void setUp() {
        NuoDBDialect dialect = new NuoDBDialect();

        Database database = new Database();
        database.setDialect(dialect);

        scriptGeneratorManager = new ScriptGeneratorManager();
        scriptGeneratorManager.setTargetDialect(dialect);
        scriptGeneratorManager.setTargetCatalog(null);
        scriptGeneratorManager.setTargetSchema("target");
    }

    @DataProvider(name = "getName")
    public Object[][] createGetNameData() {
        return new Object[][]{
                {createSchema(null, "schema"), false, "schema"},
                {createSchema(null, "schema"), true, "\"schema\""},
                {createTable(null, "schema", "table"), false, "table"},
                {createTable(null, "schema", "table"), true, "\"table\""},
                {createColumn(null, "schema", "table", "column"), false, "column"},
                {createColumn(null, "schema", "table", "column"), true, "\"column\""}
        };
    }

    @DataProvider(name = "getQualifiedName")
    public Object[][] createGetQualifiedNameData() {
        return new Object[][]{
                {createSchema(null, "schema"), false, "schema"},
                {createSchema(null, "schema"), true, "\"schema\""},
                {createTable(null, "schema", "table"), false, "target.table"},
                {createTable(null, "schema", "table"), true, "\"target\".\"table\""},
                {createColumn(null, "schema", "table", "column"), false, "column"},
                {createColumn(null, "schema", "table", "column"), true, "\"column\""}
        };
    }

    @Test(dataProvider = "getName")
    public void testGetName(MetaData object, boolean normalize, String name) {
        assertEquals(scriptGeneratorManager.getName(object, normalize), name);
    }

    @Test(dataProvider = "getQualifiedName")
    public void testGetQualifiedName(MetaData object, boolean normalize, String qualifiedName) {
        assertEquals(scriptGeneratorManager.getQualifiedName(object, normalize), qualifiedName);
    }
}
