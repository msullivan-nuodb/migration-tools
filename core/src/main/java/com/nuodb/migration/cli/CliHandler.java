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
package com.nuodb.migration.cli;

import com.nuodb.migration.bootstrap.Bootable;
import com.nuodb.migration.bootstrap.config.BootstrapConfig;
import com.nuodb.migration.cli.parse.Option;
import com.nuodb.migration.cli.parse.OptionException;
import com.nuodb.migration.cli.parse.OptionSet;
import com.nuodb.migration.cli.parse.option.OptionToolkit;
import com.nuodb.migration.cli.parse.parser.ParserImpl;

import java.util.Arrays;

/**
 * Main entry point for the command line interface, for the names of the available options see {@link CliOptions}.
 *
 * @author Sergey Bushik
 */
public class CliHandler extends CliHandlerSupport implements Bootable {

    public static final int CLI_ERROR = 2;

    public CliHandler() {
    }

    public CliHandler(OptionToolkit optionToolkit) {
        super(optionToolkit);
    }

    @Override
    public void boot(BootstrapConfig config, String[] arguments) {
        try {
            Option root = createOption();
            if (log.isTraceEnabled()) {
                log.trace(String.format("Parsing cli arguments: %1$s", Arrays.asList(arguments)));
            }
            OptionSet options = new ParserImpl().parse(arguments, root);
            handleOptionSet(options, root);
        } catch (OptionException exception) {
            handleOptionException(exception);
            System.exit(CLI_ERROR);
        }
    }
}
