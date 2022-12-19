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

import java.util.List;
import java.util.ArrayList;
import com.gip.xyna.xmcp.xfcli.AXynaCommand;


/*
* THIS FILE IS GENERATED AUTOMATICALLY
* DO NOT MODIFY OR ADD TO SVN
*/
public final class OverallInformationProvider {

  private OverallInformationProvider() {
  }

  public static List<Class<? extends AXynaCommand>> getCommands() throws ClassNotFoundException{
    List<Class<? extends AXynaCommand>> list = new ArrayList<Class<? extends AXynaCommand>>();
    Class<? extends AXynaCommand> nextClass;
    nextClass = (Class<? extends AXynaCommand>) Class.forName("xmcp.gitintegration.cli.generated.Compareworkspacexml");
    list.add(nextClass);
    nextClass = (Class<? extends AXynaCommand>) Class.forName("xmcp.gitintegration.cli.generated.Createworkspacexml");
    list.add(nextClass);
    nextClass = (Class<? extends AXynaCommand>) Class.forName("xmcp.gitintegration.cli.generated.Resolveworkspacexml");
    list.add(nextClass);
    return list;
  }

  public static void onDeployment() {
    try {
      for (Class<? extends AXynaCommand> command : getCommands() ) {
        com.gip.xyna.xmcp.xfcli.CLIRegistry.getInstance().registerCLICommand(command);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not register cli commands.", e);
    }
  }

  public static void onUndeployment() {
    try {
      for (Class<? extends AXynaCommand> command : getCommands() ) {
        com.gip.xyna.xmcp.xfcli.CLIRegistry.getInstance().unregisterCLICommand(command);
      }
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("could not register cli commands.", e);
    }
  }

}
