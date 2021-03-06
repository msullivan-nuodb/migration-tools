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
package com.nuodb.migrator.spec;

import com.nuodb.migrator.jdbc.commit.CommitStrategy;
import com.nuodb.migrator.jdbc.query.InsertType;

import java.util.Collection;
import java.util.Map;
import java.util.TimeZone;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static com.nuodb.migrator.spec.MigrationMode.DATA;
import static com.nuodb.migrator.spec.MigrationMode.SCHEMA;

/**
 * @author Sergey Bushik
 */
public class LoadJobSpec extends ScriptGeneratorJobSpecBase {

    private ConnectionSpec targetSpec;
    private Collection<MigrationMode> migrationModes = newHashSet(DATA, SCHEMA);
    private TimeZone timeZone;
    private ResourceSpec inputSpec;
    private InsertType insertType;
    private CommitStrategy commitStrategy;
    private Map<String, InsertType> tableInsertTypes = newHashMap();

    public Collection<MigrationMode> getMigrationModes() {
        return migrationModes;
    }

    public void setMigrationModes(Collection<MigrationMode> migrationModes) {
        this.migrationModes = migrationModes;
    }

    public ConnectionSpec getTargetSpec() {
        return targetSpec;
    }

    public void setTargetSpec(ConnectionSpec targetSpec) {
        this.targetSpec = targetSpec;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(TimeZone timeZone) {
        this.timeZone = timeZone;
    }

    public InsertType getInsertType() {
        return insertType;
    }

    public void setInsertType(InsertType insertType) {
        this.insertType = insertType;
    }

    public CommitStrategy getCommitStrategy() {
        return commitStrategy;
    }

    public void setCommitStrategy(CommitStrategy commitStrategy) {
        this.commitStrategy = commitStrategy;
    }

    public ResourceSpec getInputSpec() {
        return inputSpec;
    }

    public void setInputSpec(ResourceSpec inputSpec) {
        this.inputSpec = inputSpec;
    }

    public Map<String, InsertType> getTableInsertTypes() {
        return tableInsertTypes;
    }

    public void setTableInsertTypes(Map<String, InsertType> tableInsertTypes) {
        this.tableInsertTypes = newHashMap(tableInsertTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        LoadJobSpec that = (LoadJobSpec) o;

        if (commitStrategy != null ? !commitStrategy.equals(that.commitStrategy) : that.commitStrategy != null)
            return false;
        if (inputSpec != null ? !inputSpec.equals(that.inputSpec) : that.inputSpec != null) return false;
        if (insertType != that.insertType) return false;
        if (migrationModes != null ? !migrationModes.equals(that.migrationModes) : that.migrationModes != null)
            return false;
        if (tableInsertTypes != null ? !tableInsertTypes.equals(that.tableInsertTypes) : that.tableInsertTypes != null)
            return false;
        if (targetSpec != null ? !targetSpec.equals(that.targetSpec) : that.targetSpec != null) return false;
        if (timeZone != null ? !timeZone.equals(that.timeZone) : that.timeZone != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (targetSpec != null ? targetSpec.hashCode() : 0);
        result = 31 * result + (migrationModes != null ? migrationModes.hashCode() : 0);
        result = 31 * result + (timeZone != null ? timeZone.hashCode() : 0);
        result = 31 * result + (inputSpec != null ? inputSpec.hashCode() : 0);
        result = 31 * result + (insertType != null ? insertType.hashCode() : 0);
        result = 31 * result + (commitStrategy != null ? commitStrategy.hashCode() : 0);
        result = 31 * result + (tableInsertTypes != null ? tableInsertTypes.hashCode() : 0);
        return result;
    }
}
