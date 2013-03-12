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

import com.google.common.io.Files;
import com.google.common.io.NullOutputStream;
import com.nuodb.migrator.jdbc.connection.ConnectionServices;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static com.nuodb.migrator.jdbc.metadata.generator.FileScriptExporter.OUTPUT_ENCODING;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Sergey Bushik
 */
public class ScriptExporterTest {

    private Collection<String> scripts;

    @BeforeMethod
    public void setUp() {
        scripts = newArrayList(
                "CREATE TABLE \"users\" (\"used_id\" INTEGER);",
                "CREATE TABLE \"links\" (\"link_id\" INTEGER);"
        );
    }

    @DataProvider(name = "exportScripts")
    public Object[][] createExportScriptsData() throws Exception {
        File dir = Files.createTempDir();
        dir.deleteOnExit();
        File file = new File(dir, "schema.sql");
        file.deleteOnExit();

        ConnectionServices connectionServices = mock(ConnectionServices.class);
        Connection connection = mock(Connection.class);
        when(connectionServices.getConnection()).thenReturn(connection);

        Statement statement = mock(Statement.class);
        when(connection.createStatement()).thenReturn(statement);

        return new Object[][] {
                {new FileScriptExporter(file, OUTPUT_ENCODING)},
                {new ConnectionScriptExporter(connectionServices)},
                {new WriterScriptExporter(new NullOutputStream())}
        };
    }

    @Test(dataProvider = "exportScripts")
    public void testExportScripts(ScriptExporter scriptExporter) throws Exception {
        scriptExporter.open();
        scriptExporter.exportScripts(scripts);
        scriptExporter.close();
    }
}
