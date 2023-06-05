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
package com.gip.xyna.xmcp.xfcli.undisclosed;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xods.filter.ClassMapFilters;
import com.gip.xyna.xmcp.xfcli.AllArgs;
import com.gip.xyna.xmcp.xfcli.CommandLineWriter;
import com.gip.xyna.xmcp.xfcli.XynaFactoryCLIConnection.CommandExecution;


public class ExplainClassMapFilter implements CommandExecution {
  
  static Logger logger = CentralFactoryLogging.getLogger(ExplainClassMapFilter.class);
  byte[] cachedHelpMessage;
  
  public void execute(AllArgs allArgs, CommandLineWriter clw ) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
    clw.writeLineToCommandLine("ClassMapFilters are a string based format that uses internally registered mappers and");
    clw.writeLineToCommandLine("a set of basic filters to allow a component to offer customizable filtering properties in it's own context.");
    clw.writeLineToCommandLine("There a four main building blocks of a classMapFilter that appear in three phases.");
    clw.writeLineToCommandLine("Initialization: class({CLASS_IDENTIFIER})");
    clw.writeLineToCommandLine("All filters start with the declaration of the class whose instances will be filtered, all mappers have to belong to that class.");
    List<Pair<String, Set<String>>> classesAndFilters = ClassMapFilters.getInstance().listClassesAndFilters();
    Set<String> classes = new HashSet<String>();
    for (Pair<String, Set<String>> pair : classesAndFilters) {
      classes.add(pair.getFirst());
    }
    clw.writeLineToCommandLine("  registered classes: " + classes);
    clw.writeLineToCommandLine("Filtering: map({MAPPER_IDENTIFIER}) and filter({FILTER_IDENTIFIER}({FILTER_PARAMETER}))");
    clw.writeLineToCommandLine("The first step in filtering has to be the mapping of an instance property to a string,");
    clw.writeLineToCommandLine("all following filters will operate on that value. After atleast one filter other mappings can be performend,");
    clw.writeLineToCommandLine("each followed by it's own filters.");
    clw.writeLineToCommandLine("  example: map(?).filter(?).filter(?).map(?).filter(?).map(?).filter(?).filter(?)");
    clw.writeLineToCommandLine("  registered mappers by class: ");
    for (Pair<String, Set<String>> pair : classesAndFilters) {
      clw.writeLineToCommandLine("    " + pair.getFirst() + ": " + pair.getSecond());
    }
    clw.writeLineToCommandLine("  registered filters: " + ClassMapFilters.getInstance().listFilterIdentifier());
    clw.writeLineToCommandLine("Termination: " + ClassMapFilters.TerminalStreamOperation.allMatch.toString() + "() or " +
                                                 ClassMapFilters.TerminalStreamOperation.anyMatch.toString() + "() or " +
                                                 ClassMapFilters.TerminalStreamOperation.noneMatch.toString() + "()");
    clw.writeLineToCommandLine("The termination operation controls how the filters are evaluated, if for example a filter chain is terminated ");
    clw.writeLineToCommandLine("with an allMatch() all filters in the expression have to accept their mapped value in order for an instance to be accepted.");
    clw.writeLineToCommandLine("Braces have to be escaped, if they are to be used in filter parameter");
    clw.writeLineToCommandLine("they can be escaped by enclosing them in \", if \" are needed in filter parameters they are escaped as \"\".");
    clw.writeLineToCommandLine("Example for a complete classMapFilter definition:");
    clw.writeLineToCommandLine("class(GenerationBase).map(isFactoryComponent).filter(WhiteList(\"true\")).map(fqXmlName).filter(RegExp(\"test\\.exclusion\\..*\")).noneMatch()");
    return;
  }

 }
