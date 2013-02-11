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

import com.nuodb.migration.jdbc.metadata.*;

/**
 * @author Sergey Bushik
 */
public class InspectionResultsUtils {

    public static Database addDatabase(InspectionResults inspectionResults) {
        Database database = inspectionResults.getObject(MetaDataType.DATABASE);
        if (database == null) {
            inspectionResults.addObject(database = new Database());
        }
        return database;
    }

    public static Catalog addCatalog(InspectionResults inspectionResults, String catalogName) {
        Database database = addDatabase(inspectionResults);
        Catalog catalog;
        Identifier catalogId = Identifier.valueOf(catalogName);
        if (database.hasCatalog(catalogId)) {
            catalog = database.getCatalog(catalogId);
        } else {
            catalog = database.addCatalog(catalogId);
            inspectionResults.addObject(catalog);
        }
        return catalog;
    }

    public static Schema addSchema(InspectionResults inspectionResults, String catalogName, String schemaName) {
        Catalog catalog = addCatalog(inspectionResults, catalogName);
        Schema schema;
        Identifier schemaId = Identifier.valueOf(schemaName);
        if (catalog.hasSchema(schemaId)) {
            schema = catalog.getSchema(schemaId);
        } else {
            schema = catalog.addSchema(schemaId);
            inspectionResults.addObject(schema);
        }
        return schema;
    }

    public static Table addTable(InspectionResults inspectionResults, String catalogName, String schemaName,
                                 String tableName) {
        Schema schema = addSchema(inspectionResults, catalogName, schemaName);
        Table table;
        Identifier tableId = Identifier.valueOf(tableName);
        if (schema.hasTable(tableId)) {
            table = schema.getTable(tableId);
        } else {
            table = schema.addTable(tableId);
            inspectionResults.addObject(table);
        }
        return table;
    }
}
