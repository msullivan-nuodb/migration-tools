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
import com.nuodb.migrator.jdbc.metadata.Database;
import com.nuodb.migrator.jdbc.metadata.Schema;
import com.nuodb.migrator.jdbc.metadata.Table;
import org.testng.annotations.Test;

import static com.nuodb.migrator.jdbc.metadata.inspector.InspectionResultsUtils.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Sergey Bushik
 */
public class InspectionResultsUtilsTest {

    @Test
    public void testAddDatabase() {
        InspectionResults inspectionResults = mock(InspectionResults.class);
        Database database = addDatabase(inspectionResults);
        verify(inspectionResults).addObject(database);
    }

    @Test
    public void testAddCatalog() {
        InspectionResults inspectionResults = mock(InspectionResults.class);
        Catalog catalog = addCatalog(inspectionResults, "catalog");

        verify(inspectionResults).addObject(catalog);
        verify(inspectionResults).addObject(catalog.getDatabase());
    }

    @Test
    public void testAddSchema() {
        InspectionResults inspectionResults = mock(InspectionResults.class);
        Schema schema = addSchema(inspectionResults, "catalog", "schema");

        verify(inspectionResults).addObject(schema);
        verify(inspectionResults).addObject(schema.getCatalog());
        verify(inspectionResults).addObject(schema.getDatabase());
    }

    @Test
    public void testAddTable() {
        InspectionResults inspectionResults = mock(InspectionResults.class);
        Table table = addTable(inspectionResults, "catalog", "schema", "table");

        verify(inspectionResults).addObject(table);
        verify(inspectionResults).addObject(table.getSchema());
        verify(inspectionResults).addObject(table.getCatalog());
        verify(inspectionResults).addObject(table.getDatabase());
    }
}
