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
package com.nuodb.migration.jdbc.metamodel;

import java.util.Arrays;

/**
 * @author Sergey Bushik
 */
public class ColumnSetModelImpl implements ColumnSetModel {

    private String[] columns;
    private int[] columnTypes;

    public ColumnSetModelImpl(ColumnSetModel model) {
        this(model.getColumns(), model.getColumnTypes());
    }

    public ColumnSetModelImpl(String[] columns, int[] columnTypes) {
        this.columns = columns;
        this.columnTypes = columnTypes;
    }

    public boolean hasColumn(String column) {
        return Arrays.binarySearch(columns, column) >= 0;
    }

    public int getColumnType(int index) {
        return columnTypes[index];
    }

    @Override
    public void setColumnType(int index, int columnType) {
        columnTypes[index] = columnType;
    }

    public int[] getColumnTypes() {
        return columnTypes;
    }

    public String getColumn(int index) {
        return columns[index];
    }

    @Override
    public void setColumn(int index, String column) {
        columns[index] = column;
    }

    public String[] getColumns() {
        return columns;
    }

    public int getColumnCount() {
        return columns.length;
    }
}