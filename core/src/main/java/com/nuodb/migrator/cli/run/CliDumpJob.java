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
package com.nuodb.migrator.cli.run;

import com.nuodb.migrator.cli.parse.Group;
import com.nuodb.migrator.cli.parse.Option;
import com.nuodb.migrator.cli.parse.OptionSet;
import com.nuodb.migrator.cli.parse.option.GroupBuilder;
import com.nuodb.migrator.cli.parse.option.OptionFormat;
import com.nuodb.migrator.jdbc.metadata.Table;
import com.nuodb.migrator.jdbc.query.QueryLimit;
import com.nuodb.migrator.spec.DumpJobSpec;
import com.nuodb.migrator.spec.QuerySpec;
import com.nuodb.migrator.spec.TableSpec;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newLinkedHashSet;
import static com.nuodb.migrator.context.ContextUtils.getMessage;
import static com.nuodb.migrator.utils.Priority.LOW;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.parseInt;
import static java.lang.Long.parseLong;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * An implementation of {@link CliRunAdapter} which assembles dump spec from provided command line after the validation
 * is passed.
 *
 * @author Sergey Bushik
 */
public class CliDumpJob extends CliJob<DumpJobSpec> {

    public CliDumpJob() {
        super(DUMP_COMMAND);
    }

    @Override
    protected Option createOption() {
        GroupBuilder group = newGroupBuilder().
                withName(getMessage(DUMP_GROUP_NAME)).withRequired(true);
        group.withOption(createSourceGroup());
        group.withOption(createOutputGroup());
        group.withOption(createMigrationModeGroup());
        group.withOption(createDataMigrationGroup());
        group.withOption(createSchemaMigrationGroup());
        return group.build();
    }

    @Override
    protected void bind(OptionSet optionSet) {
        DumpJobSpec dumpJobSpec = new DumpJobSpec();
        dumpJobSpec.setSourceSpec(parseSourceGroup(optionSet, this));
        dumpJobSpec.setOutputSpec(parseOutputGroup(optionSet, this));
        dumpJobSpec.setMigrationModes(parseMigrationModeGroup(optionSet, this));
        parseDataMigrationGroup(optionSet, dumpJobSpec);
        parseSchemaMigrationGroup(optionSet, dumpJobSpec);
        setJobSpec(dumpJobSpec);
    }

    @Override
    public void execute(Map<Object, Object> context) {
        getMigrator().execute(getJobSpec(), context);
    }

    protected Option createDataMigrationGroup() {
        GroupBuilder group = newGroupBuilder().withName(getMessage(DATA_MIGRATION_GROUP_NAME));
        group.withOption(createTableGroup());
        group.withOption(createQueryGroup());
        group.withOption(createTimeZoneOption());
        group.withOption(createThreadsOption());
        group.withOption(createQueryLimitOption());
        return group.build();
    }

    /**
     * Table option handles -table=users, -table=roles and stores it items the option in the  command line.
     */
    protected Group createTableGroup() {
        GroupBuilder group = newGroupBuilder().withName(getMessage(TABLE_GROUP_NAME)).withMaximum(MAX_VALUE);

        Option table = newBasicOptionBuilder().
                withName(TABLE_OPTION).
                withDescription(getMessage(TABLE_OPTION_DESCRIPTION)).
                withArgument(
                        newArgumentBuilder().
                                withName(getMessage(TABLE_ARGUMENT_NAME)).
                                withMinimum(1).
                                withMaximum(Integer.MAX_VALUE).
                                withRequired(true).build()
                ).build();
        group.withOption(table);

        OptionFormat optionFormat = new OptionFormat(getOptionFormat());
        optionFormat.setValuesSeparator(null);

        Option tableFilter = newRegexOptionBuilder().
                withName(TABLE_FILTER_OPTION).
                withDescription(getMessage(TABLE_FILTER_OPTION_DESCRIPTION)).
                withRegex(TABLE_FILTER_OPTION, 1, LOW).
                withArgument(
                        newArgumentBuilder().
                                withName(getMessage(TABLE_FILTER_ARGUMENT_NAME)).
                                withOptionFormat(optionFormat).
                                withMinimum(1).
                                withRequired(true).build()
                ).build();

        group.withOption(tableFilter);
        return group.build();
    }

    protected Option createThreadsOption() {
        return newBasicOptionBuilder().
                withName(THREADS_OPTION).
                withAlias(THREADS_SHORT_OPTION, OptionFormat.SHORT).
                withDescription(getMessage(THREADS_OPTION_DESCRIPTION)).
                withArgument(
                        newArgumentBuilder().
                                withName(getMessage(THREADS_ARGUMENT_NAME)).build()
                ).build();
    }

    protected Option createQueryGroup() {
        GroupBuilder group = newGroupBuilder().withName(getMessage(QUERY_GROUP_NAME)).withMaximum(MAX_VALUE);

        OptionFormat optionFormat = new OptionFormat(getOptionFormat());
        optionFormat.setValuesSeparator(null);

        Option query = newBasicOptionBuilder().
                withName(QUERY_OPTION).
                withDescription(getMessage(QUERY_OPTION_DESCRIPTION)).
                withArgument(
                        newArgumentBuilder().
                                withName(getMessage(QUERY_ARGUMENT_NAME)).
                                withMinimum(1).
                                withMaximum(MAX_VALUE).
                                withOptionFormat(optionFormat).
                                withRequired(true).build()
                ).build();
        group.withOption(query);

        return group.build();
    }

    protected Option createQueryLimitOption() {
        return newBasicOptionBuilder().
                withName(QUERY_LIMIT_OPTION).
                withDescription(getMessage(QUERY_LIMIT_OPTION_DESCRIPTION)).
                withArgument(
                        newArgumentBuilder().
                                withName(getMessage(QUERY_LIMIT_ARGUMENT_NAME)).build()
                ).build();
    }

    protected void parseDataMigrationGroup(OptionSet optionSet, DumpJobSpec jobSpec) {
        parseTableGroup(optionSet, jobSpec);
        jobSpec.setQuerySpecs(parseQueryGroup(optionSet));
        jobSpec.setTimeZone(parseTimeZoneOption(optionSet, this));
        jobSpec.setThreads(parseThreadsOption(optionSet, this));
        jobSpec.setQueryLimit(parseQueryLimitOption(optionSet, this));
    }

    protected void parseTableGroup(OptionSet optionSet, DumpJobSpec jobSpec) {
        Map<String, TableSpec> tableQueryMapping = newHashMap();
        for (String table : optionSet.<String>getValues(TABLE_OPTION)) {
            tableQueryMapping.put(table, new TableSpec(table));
        }
        for (Iterator<String> iterator = optionSet.<String>getValues(
                TABLE_FILTER_OPTION).iterator(); iterator.hasNext(); ) {
            String name = iterator.next();
            TableSpec tableSpec = tableQueryMapping.get(name);
            if (tableSpec == null) {
                tableQueryMapping.put(name, tableSpec = new TableSpec(name));
            }
            tableSpec.setFilter(iterator.next());
        }
        jobSpec.setTableSpecs(newArrayList(tableQueryMapping.values()));
    }

    protected Collection<QuerySpec> parseQueryGroup(OptionSet optionSet) {
        List<QuerySpec> querySpecs = newArrayList();
        for (String query : optionSet.<String>getValues(QUERY_OPTION)) {
            querySpecs.add(new QuerySpec(query));
        }
        return querySpecs;
    }

    protected Integer parseThreadsOption(OptionSet optionSet, Option option) {
        String threadsValue = (String) optionSet.getValue(THREADS_OPTION);
        return !isEmpty(threadsValue) ? parseInt(threadsValue) : null;
    }

    protected QueryLimit parseQueryLimitOption(OptionSet optionSet, Option option) {
        String queryLimitValue = (String) optionSet.getValue(QUERY_LIMIT_OPTION);
        return !isEmpty(queryLimitValue) ? new QueryLimit(parseLong(queryLimitValue)) : null;
    }

    @Override
    protected Group createSchemaMigrationGroup() {
        GroupBuilder group = newGroupBuilder().withName(getMessage(SCHEMA_MIGRATION_GROUP_NAME));

        OptionFormat optionFormat = new OptionFormat(getOptionFormat());
        optionFormat.setValuesSeparator(null);

        Option tableType = newBasicOptionBuilder().
                withName(TABLE_TYPE_OPTION).
                withDescription(getMessage(TABLE_TYPE_OPTION_DESCRIPTION)).
                withArgument(
                        newArgumentBuilder().
                                withName(getMessage(TABLE_TYPE_ARGUMENT_NAME)).
                                withMaximum(MAX_VALUE).build()
                ).build();
        group.withOption(tableType);

        Option metaData = newRegexOptionBuilder().
                withName(META_DATA_OPTION).
                withDescription(getMessage(META_DATA_OPTION_DESCRIPTION)).
                withRegex(META_DATA_OPTION, 1, LOW).
                withArgument(
                        newArgumentBuilder().
                                withName(getMessage(META_DATA_ARGUMENT_NAME)).
                                withOptionFormat(optionFormat).
                                withMinimum(1).withMaximum(MAX_VALUE).build()
                )
                .build();
        group.withOption(metaData);
        return group.build();
    }

    protected void parseSchemaMigrationGroup(OptionSet optionSet, DumpJobSpec jobSpec) {
        if (optionSet.hasOption(META_DATA_OPTION)) {
            jobSpec.setObjectTypes(parseObjectTypes(optionSet));
        }
        jobSpec.setTableTypes(parseTableTypes(optionSet));
    }
}
