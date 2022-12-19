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
import xmcp.gitintegration.cli.impl.CreateworkspacexmlImpl;


/*
* THIS FILE IS GENERATED AUTOMATICALLY
* DO NOT MODIFY OR ADD TO SVN
*/
public class Createworkspacexml extends AXynaCommand {

  public static final String COMMAND_Createworkspacexml = "createworkspacexml";

  private static volatile Options allOptions = null;

  private static final String[] groups = new String[] {};
  private static final String[] dependencies = new String[] {};
  private static volatile XynaCommandImplementation<Createworkspacexml> executor = null;
  private String workspaceName;
  private boolean p;

  public void setWorkspaceName(String value) {
    this.workspaceName = value;
  }

  public String getWorkspaceName() {
    return this.workspaceName;
  }

  public boolean getPrintResult() {
    return this.p;
  }

  public String getCommandName() {
    return COMMAND_Createworkspacexml;
  }

  protected String getDescriptionString() {
    return "create workspace.xml for a given workspace";
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
      synchronized(Createworkspacexml.class) {
        if (allOptions != null) {
          return allOptions;
        }
        synchronized(AXynaCommand.class) {
          Options allOptionsTmp = new Options();
          allOptionsTmp.addOption(OptionBuilder.isRequired().hasArg().withDescription("Workspace to create workspace.xml for").withArgName("arg").create("workspaceName"));
          allOptionsTmp.addOption(new Option("p", "printResult", false, "Instead of writing to file, only display content"));
          allOptions = allOptionsTmp;
        }
      }
    }
    return allOptions;
  }

  protected void setFieldsByParsedOptions(Option[] options) {
    for (Option o: options) {
      if ("workspaceName".equals(o.getOpt())) {
        this.workspaceName = o.getValue();
        continue;
      }
      if ("p".equals(o.getOpt())) {
        this.p = true;
        continue;
      }
    }
  }

  private XMCP_INVALID_PARAMETERNUMBER createInvalidParaEx() throws XMCP_INVALID_PARAMETERNUMBER{
    return new XMCP_INVALID_PARAMETERNUMBER("<workspaceName>");
  }

  protected void parseUnrecognizedDataArguments(String[] args) throws XMCP_INVALID_PARAMETERNUMBER {
    // compare with the number of non-optional arguments
    if (args.length < 1) {
      throw createInvalidParaEx();
    }
    if (0 < args.length) {
      this.workspaceName = args[0];
    }
    for (int k=1; k<args.length; k++) {
      if ("-p".equals(args[k])) {
        this.p = true;
      }
    }
  }

  public void executeInternally(OutputStream statusOutputStream) throws XynaException {
    if (executor == null) {
      synchronized(getClass()) {
        if (executor == null) {
          executor = new CreateworkspacexmlImpl();
        }
      }
    }
    executor.execute(statusOutputStream, this);
  }

  public String getCommandAsString() throws XMCP_INVALID_PARAMETERNUMBER {
    StringBuilder sb = new StringBuilder(getCommandName());
    for (Option o: (Collection<Option>)getAllOptions().getOptions()) {
      if ("workspaceName".equals(o.getOpt())) {
        if (workspaceName == null) {
          if (o.isRequired()) {
            throw createInvalidParaEx();
          }
        } else {
          sb.append(" -").append(o.getOpt()).append(" ").append(workspaceName);
        }
        continue;
      }
    }
    return sb.toString();
  }

}
