/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */

package com.gip.xyna.xmcp.xfcli;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.exceptions.XMCP_INVALID_PARAMETERNUMBER;



public abstract class AXynaCommand implements Comparable<AXynaCommand> {

  private static final Logger logger = CentralFactoryLogging.getLogger(AXynaCommand.class);
  public static final String CLI_APPLICATION_OPTION_STRING = "cliapplication";
  public static final String CLI_VERSION_OPTION_STRING = "cliversion";
  public static final String CLI_WORKSPACE_OPTION_STRING = "cliworkspace";
  public static final String CLI_CLASSLOADER_ID_OPTION_STRING = "cliclid";
  public static final String CLI_CLASSLOADER_NAME_OPTION_STRING = "cliclname";
  public static final String DEPENDENCIES_OPTION_NONE = "NONE";
  

  private boolean parsed = false;
  private boolean quiet = false;
  private AllArgs allArgs;

  public final void execute(OutputStream statusOutputStream) throws XynaException {
    // is thread safety a concern here?
    if (!parsed) {
      throw new IllegalStateException("Command has to be parsed before it can be executed.");
    }
    executeInternally(statusOutputStream);
  }
  public abstract void executeInternally(OutputStream statusOutputStream) throws XynaException;


  protected abstract Options getAllOptions();


  protected abstract String[] getGroups();


  public abstract String getCommandName();


  public boolean isDeprecated() {
    // implementing classes overwrite this method if they are deprecated
    return false;
  }
  
  /**
   * options, die wegen abwärtskompatibilität noch bekannt sein sollen, aber nicht mehr in verwendung sind 
   */
  public String[] getOldOptionNames() {
    return null;
  }


  public final String getDescription() {
    HelpFormatter formatter = new HelpFormatter();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(baos);
    formatter.printUsage(pw, 100, getCommandName(), getAllOptions());
    pw.close();
    try {
      return baos.toString(Constants.DEFAULT_ENCODING).trim().replace("usage: ", "") + ": " + getDescriptionString();
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unexpected exception: " + e.getMessage(), e);
    }
  }
  
  protected abstract String getDescriptionString();

  protected abstract String getExtendedDescriptionString();

  public final String getExtendedDescription() {
    HelpFormatter formatter = new HelpFormatter();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PrintWriter pw = new PrintWriter(baos);
    String extendedDescr = getExtendedDescriptionString();
    String descr = getDescriptionString();
    if (extendedDescr != null) {
      descr += "\n" + extendedDescr;
    }
    formatter.printHelp(pw, 100, getCommandName(), descr, getAllOptions(), 3, 10, null, true);
    pw.close();
    try {
      return baos.toString(Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unexpected exception: " + e.getMessage(), e);
    }
  }


  public final boolean isQuietSet() {
    return this.quiet;
  }


  public final void parse(AllArgs allArgs) throws ParseException, XMCP_INVALID_PARAMETERNUMBER {
    this.allArgs = allArgs;
    CommandLineParser parser = new GnuParser() {

      @SuppressWarnings("rawtypes")
      @Override
      protected void processOption(String optionName, ListIterator iter) throws ParseException {
        String[] old = getOldOptionNames();
        if (old != null) {
          for (String s : old) {
            if (s.equals(optionName) || (("-" + s).equals(optionName))) {
              logger.warn("Option '" + optionName + "' is not supported any more (command='" + getCommandName() + "').");
              return;
            }
          }
        }
        super.processOption(optionName, iter);
      }

    };
    String[] args = allArgs.getArgsAsArray(0);
    try {
      CommandLine line = parser.parse(getAllOptionsIncludingCommandIndentification(), args);
      setFieldsByParsedOptions(line.getOptions());
    } catch (MissingOptionException e) {
      // this is the fallback solution if no option definition is written and all argument values are just
      // passed in a row.      
      if (getAllOptions().getRequiredOptions().size() == e.getMissingOptions().size() && args != null
          && args.length > 0 && dontAllowParametersButAllowFlags(args)) {
        parseUnrecognizedDataArguments(args);
      } else {
        throw e;
      }
    }

    parsed = true;
  }
  
  
  private boolean dontAllowParametersButAllowFlags(String[] args) {
    for (String s : args) {
      if (s.startsWith("-")) {
        Option o = getAllOptions().getOption(s);
        if (o == null || o.hasArg()) {
          //o == null => unbekannter parameter
          //o.hasArg => parameter mit arg(s)
          return false;
        }       
        //o ist bekannter parameter ohne args - also ein flag -> erlaubt
      }
    }
    return true;
  }
  
  
  public Options getAllOptionsIncludingCommandIndentification() {
    Options regularOptions = getAllOptions();
    Options options = new Options();
    for (Object option : regularOptions.getOptions()) {
      options.addOption((Option) option);
    }
    options.addOption(CLI_APPLICATION_OPTION_STRING, true, "");
    options.addOption(CLI_CLASSLOADER_ID_OPTION_STRING, true, "");
    options.addOption(CLI_CLASSLOADER_NAME_OPTION_STRING, true, "");
    options.addOption(CLI_VERSION_OPTION_STRING, true, "");
    options.addOption(CLI_WORKSPACE_OPTION_STRING, true, "");
    return options;
  }
  
  public List<String> getAdditionalArguments() {
    return allArgs.getAdditionals();
  }
  
  public AllArgs getAllArgs() {
    return allArgs;
  }

  protected abstract void parseUnrecognizedDataArguments(String[] args) throws XMCP_INVALID_PARAMETERNUMBER;


  protected abstract void setFieldsByParsedOptions(Option[] options);


  public int compareTo(AXynaCommand command) {
    if (command == null) {
      return -1;
    }
    return getCommandName().compareTo(command.getCommandName());
  }
  
  public abstract String getCommandAsString() throws XMCP_INVALID_PARAMETERNUMBER;

}
