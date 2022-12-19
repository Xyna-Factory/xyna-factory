/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xmcp.gitintegration.cli.generated;

import java.io.OutputStream;
import com.gip.xyna.utils.exceptions.XynaException;
import java.util.List;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import org.apache.commons.cli.OptionBuilder;
import java.util.Collection;
import org.apache.commons.cli.Options;
import com.gip.xyna.xmcp.exceptions.XMCP_INVALID_PARAMETERNUMBER;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;
import org.apache.commons.cli.Option;
import java.util.ArrayList;
import xmcp.gitintegration.cli.impl.ResolveworkspacexmlImpl;


/*
* THIS FILE IS GENERATED AUTOMATICALLY
* DO NOT MODIFY OR ADD TO SVN
*/
public class Resolveworkspacexml extends AXynaCommand {

  public static final String COMMAND_Resolveworkspacexml = "resolveworkspacexml";

  private static volatile Options allOptions = null;

  private static final String[] groups = new String[] {};
  private static final String[] dependencies = new String[] {};
  private static volatile XynaCommandImplementation<Resolveworkspacexml> executor = null;
  private String id;
  private String entry;
  private String resolution;
  private boolean a;
  private boolean c;

  public void setId(String value) {
    this.id = value;
  }

  public void setEntry(String value) {
    this.entry = value;
  }

  public void setResolution(String value) {
    this.resolution = value;
  }

  public String getId() {
    return this.id;
  }

  public String getEntry() {
    return this.entry;
  }

  public String getResolution() {
    return this.resolution;
  }

  public boolean getAll() {
    return this.a;
  }

  public boolean getClose() {
    return this.c;
  }

  public String getCommandName() {
    return COMMAND_Resolveworkspacexml;
  }

  protected String getDescriptionString() {
    return "resolve part of, or an entire workspace difference list.";
  }

  protected String getExtendedDescriptionString() {
    return null;
  }

  protected String[] getGroups() {
    return groups;
  }

  public static String[] getDependencies() {
    return dependencies;
  }

  public Options getAllOptions() {
    if (allOptions == null) {
      synchronized(Resolveworkspacexml.class) {
        if (allOptions != null) {
          return allOptions;
        }
        synchronized(AXynaCommand.class) {
          Options allOptionsTmp = new Options();
          allOptionsTmp.addOption(OptionBuilder.isRequired().hasArg().withDescription("workspace difference list id. See listworkspacediffs").withArgName("arg").create("id"));
          allOptionsTmp.addOption(OptionBuilder.hasArg().withDescription("Entry in the workspace difference list to resolve").withArgName("arg").create("entry"));
          allOptionsTmp.addOption(OptionBuilder.hasArg().withDescription("How to resolve the difference: CREATE/MODIFY/DELETE. If not specified, the suggested resolution is chosen.").withArgName("arg").create("resolution"));
          allOptionsTmp.addOption(new Option("a", "all", false, "apply to an entire workspace difference list, instead of a specific entry"));
          allOptionsTmp.addOption(new Option("c", "close", false, "Do not apply any changes to the workspace, even if entry and/or resolution are specified. Closes the workspace difference list."));
          allOptions = allOptionsTmp;
        }
      }
    }
    return allOptions;
  }

  protected void setFieldsByParsedOptions(Option[] options) {
    for (Option o: options) {
      if ("id".equals(o.getOpt())) {
        this.id = o.getValue();
        continue;
      }
      if ("entry".equals(o.getOpt())) {
        this.entry = o.getValue();
        continue;
      }
      if ("resolution".equals(o.getOpt())) {
        this.resolution = o.getValue();
        continue;
      }
      if ("a".equals(o.getOpt())) {
        this.a = true;
        continue;
      }
      if ("c".equals(o.getOpt())) {
        this.c = true;
        continue;
      }
    }
  }

  private XMCP_INVALID_PARAMETERNUMBER createInvalidParaEx() throws XMCP_INVALID_PARAMETERNUMBER{
    return new XMCP_INVALID_PARAMETERNUMBER("<id> <entry> <resolution>");
  }

  protected void parseUnrecognizedDataArguments(String[] args) throws XMCP_INVALID_PARAMETERNUMBER {
    // compare with the number of non-optional arguments
    if (args.length < 1) {
      throw createInvalidParaEx();
    }
    if (0 < args.length) {
      this.id = args[0];
    }
    if (1 < args.length) {
      this.entry = args[1];
    }
    if (2 < args.length) {
      this.resolution = args[2];
    }
    for (int k=3; k<args.length; k++) {
      if ("-a".equals(args[k])) {
        this.a = true;
      }
      if ("-c".equals(args[k])) {
        this.c = true;
      }
    }
  }

  public void executeInternally(OutputStream statusOutputStream) throws XynaException {
    if (executor == null) {
      synchronized(getClass()) {
        if (executor == null) {
          executor = new ResolveworkspacexmlImpl();
        }
      }
    }
    executor.execute(statusOutputStream, this);
  }

  public String getCommandAsString() throws XMCP_INVALID_PARAMETERNUMBER {
    StringBuilder sb = new StringBuilder(getCommandName());
    for (Option o: (Collection<Option>)getAllOptions().getOptions()) {
      if ("id".equals(o.getOpt())) {
        if (id == null) {
          if (o.isRequired()) {
            throw createInvalidParaEx();
          }
        } else {
          sb.append(" -").append(o.getOpt()).append(" ").append(id);
        }
        continue;
      }
      if ("entry".equals(o.getOpt())) {
        if (entry == null) {
          if (o.isRequired()) {
            throw createInvalidParaEx();
          }
        } else {
          sb.append(" -").append(o.getOpt()).append(" ").append(entry);
        }
        continue;
      }
      if ("resolution".equals(o.getOpt())) {
        if (resolution == null) {
          if (o.isRequired()) {
            throw createInvalidParaEx();
          }
        } else {
          sb.append(" -").append(o.getOpt()).append(" ").append(resolution);
        }
        continue;
      }
    }
    return sb.toString();
  }

}
