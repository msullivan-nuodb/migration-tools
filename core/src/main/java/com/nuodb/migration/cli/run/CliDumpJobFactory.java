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
package com.nuodb.migration.cli.run;

import com.nuodb.migration.cli.CliResources;
import com.nuodb.migration.cli.parse.CommandLine;
import com.nuodb.migration.cli.parse.Option;
import com.nuodb.migration.cli.parse.option.OptionToolkit;
import com.nuodb.migration.dump.DumpJobFactory;
import com.nuodb.migration.spec.DumpSpec;

/**
 * The factory instantiates a {@link CliDumpJobFactory.CliDumpJob}.
 *
 * @author Sergey Bushik
 */
public class CliDumpJobFactory extends CliRunSupport implements CliRunFactory, CliResources {

    /**
     * The "dump" literal command which is matched against the value on the command line. If matched the CliDump object
     * is constructed with {@link #createCliRun()} method.
     */
    private static final String COMMAND = "dump";

    public CliDumpJobFactory(OptionToolkit optionToolkit) {
        super(optionToolkit);
    }

    @Override
    public String getCommand() {
        return COMMAND;
    }

    @Override
    public CliRun createCliRun() {
        return new CliDumpJob();
    }

    /**
     * An implementation of {@link CliRunAdapter} which assembles withConnection spec from provided command line after
     * the validation is passed.
     */
    class CliDumpJob extends CliRunJob {

        public CliDumpJob() {
            super(COMMAND, new DumpJobFactory());
        }

        @Override
        protected Option createOption() {
            return newGroup()
                    .withName(getResources().getMessage(DUMP_GROUP_NAME))
                    .withOption(createSourceGroup())
                    .withOption(createOutputGroup())
                    .withOption(createSelectQueryGroup())
                    .withOption(createNativeQueryGroup())
                    .withOption(createTimeZoneOption())
                    .withRequired(true).build();
        }

        @Override
        protected void bind(CommandLine commandLine) {
            DumpSpec spec = new DumpSpec();
            spec.setSourceConnectionSpec(parseSourceGroup(commandLine, this));
            spec.setOutputSpec(parseOutputGroup(commandLine, this));
            spec.setSelectQuerySpecs(parseSelectQueryGroup(commandLine, this));
            spec.setNativeQuerySpecs(parseNativeQueryGroup(commandLine, this));
            spec.setTimeZone(parseTimeZone(commandLine, this));

            ((DumpJobFactory) getJobFactory()).setDumpSpec(spec);
        }
    }
}
