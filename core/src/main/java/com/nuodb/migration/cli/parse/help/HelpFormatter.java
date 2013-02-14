/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nuodb.migration.cli.parse.help;

import com.google.common.collect.Sets;
import com.nuodb.migration.cli.parse.Help;
import com.nuodb.migration.cli.parse.HelpHint;
import com.nuodb.migration.cli.parse.Option;
import com.nuodb.migration.cli.parse.OptionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.*;

@SuppressWarnings("unchecked")
public class HelpFormatter extends HelpHint {

    public final static String GUTTER = "    ";

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private static Set<HelpHint> USAGE_OUTPUT_HINTS;

    private static Set<HelpHint> HELP_OUTPUT_HINTS;

    private static Set<HelpHint> OPTION_OUTPUT_HINTS;

    static {
        USAGE_OUTPUT_HINTS = Collections.unmodifiableSet(
                Sets.newHashSet(
                        ALIASES,
                        OPTIONAL,
                        PROPERTY,
                        SWITCH,
                        GROUP_OPTIONS,
                        GROUP_ARGUMENTS,
                        GROUP_OUTER,
                        ARGUMENT_BRACKETED,
                        CONTAINER_ARGUMENT,
                        CONTAINER_GROUP
                ));
        HELP_OUTPUT_HINTS = Collections.unmodifiableSet(
                Sets.newHashSet(
                        ALIASES,
                        OPTIONAL,
                        GROUP,
                        CONTAINER_ARGUMENT
                ));
        OPTION_OUTPUT_HINTS = Collections.unmodifiableSet(
                Sets.newHashSet(
                        ALIASES,
                        OPTIONAL,
                        OPTIONAL_CHILD_GROUP,
                        PROPERTY,
                        SWITCH,
                        GROUP,
                        GROUP_OPTIONS,
                        GROUP_ARGUMENTS,
                        GROUP_OUTER,
                        ARGUMENT_BRACKETED,
                        CONTAINER_GROUP
                ));
    }

    protected transient final Logger logger = LoggerFactory.getLogger(getClass());
    protected Set<HelpHint> usageOutputHints = Sets.newHashSet(USAGE_OUTPUT_HINTS);
    protected Set<HelpHint> helpOutputHints = Sets.newHashSet(HELP_OUTPUT_HINTS);
    protected Set<HelpHint> optionOutputHints = Sets.newHashSet(OPTION_OUTPUT_HINTS);

    protected String executable;
    protected String header;
    protected String divider;
    protected String gutter = GUTTER;
    protected String footer;
    protected Option option;
    protected Comparator comparator;
    protected OptionException exception;

    public void format(OutputStream output) {
        format(new PrintWriter(output));
    }

    public void format(Writer writer) {
        try {
            header(writer);
            exception(writer);
            usage(writer);
            help(writer);
            footer(writer);
            writer.flush();
        } catch (IOException e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Failed formatting help", e);
            }
        }
    }

    protected void header(Writer writer) throws IOException {
        if (header != null) {
            divider(writer);
            line(writer, header);
        }
    }

    protected void exception(Writer writer) throws IOException {
        if (exception != null) {
            divider(writer);
            line(writer, exception.getMessage());
        }
    }

    protected void divider(Writer writer) throws IOException {
        if (divider != null) {
            line(writer, divider);
        }
    }

    protected void usage(Writer writer) throws IOException {
        divider(writer);
        StringBuilder usage = new StringBuilder("Usage:\n");
        usage.append(executable).append(" ");
        Option option;
        if ((exception != null) && (exception.getOption() != null)) {
            option = exception.getOption();
        } else {
            option = this.option;
        }
        option.help(usage, usageOutputHints, comparator);
        line(writer, usage.toString());
    }

    protected void help(Writer writer) throws IOException {
        line(writer, divider);
        Option option;
        if ((exception != null) && (exception.getOption() != null)) {
            option = exception.getOption();
        } else {
            option = this.option;
        }
        assert option != null;
        List<Help> helps = option.help(0, optionOutputHints, comparator);
        int usageWidth = 0;
        for (Help help : helps) {
            String content = help.help(helpOutputHints, comparator);
            usageWidth = Math.max(usageWidth, content.length());
        }
        for (Help help : helps) {
            line(writer,
                    pad(help.help(helpOutputHints, comparator), usageWidth),
                    gutter, help.getOption().getDescription());
        }
        line(writer, divider);
    }

    protected void footer(Writer writer) throws IOException {
        if (footer != null) {
            line(writer, footer);
            line(writer, divider);
        }
    }

    protected void line(Writer writer, String... chunks) throws IOException {
        for (String chunk : chunks) {
            if (chunk != null) {
                writer.write(chunk);
            }
        }
        writer.write(LINE_SEPARATOR);
    }

    protected static String pad(String text, int width) {
        StringBuilder padded = new StringBuilder();
        if (text != null) {
            padded.append(text);
        }
        int left = padded.length();
        for (int i = left; i < width; ++i) {
            padded.append(' ');
        }
        return padded.toString();
    }

    public void setUsageOutputHints(Set<HelpHint> usageOutputHints) {
        this.usageOutputHints = usageOutputHints;
    }

    public void setHelpOutputHints(Set<HelpHint> helpOutputHints) {
        this.helpOutputHints = helpOutputHints;
    }

    public void setOptionOutputHints(Set<HelpHint> optionOutputHints) {
        this.optionOutputHints = optionOutputHints;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setException(OptionException exception) {
        this.exception = exception;
    }

    public void setComparator(Comparator comparator) {
        this.comparator = comparator;
    }

    public void setDivider(String divider) {
        this.divider = divider;
    }

    public void setOption(Option option) {
        this.option = option;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public void setExecutable(String executable) {
        this.executable = executable;
    }

    public void setGutter(String gutter) {
        this.gutter = gutter;
    }
}