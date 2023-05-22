/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.HashSet;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Printusedobjects;



public class PrintusedobjectsImpl extends XynaCommandImplementation<Printusedobjects> {

  public void execute(OutputStream statusOutputStream, Printusedobjects payload) throws XynaException {

    String dependencyTypeName = payload.getObjectType();
    DependencySourceType type;
    try {
      type = DependencySourceType.getByName(dependencyTypeName, true);
    } catch (IllegalArgumentException e) {

      throw new XDEV_UNSUPPORTED_FEATURE("Dependencies for object type '" + payload.getObjectType() + "'. Supported values are: "
          + DependencySourceType.getAllValidNamesAsSingleString() + ".");
    }

    Long revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());

    DependencyNode rootNode =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
            .getDependencyNode(payload.getObject(), type, revision);

    if (rootNode == null) {
      writeToCommandLine(statusOutputStream, "The specified object is unknown.\n");
    } else if (rootNode.getUsedNodes().size() > 0) {
      if (payload.getRecurse()) {
        writeToCommandLine(statusOutputStream, "Found the following tree of used objects:\n");
        printUsedNodesRecursively(statusOutputStream, rootNode, 0, new HashSet<DependencyNode>());
      } else {
        writeToCommandLine(statusOutputStream, "Found the following directly used objects:\n");
        printUsedNodesToplevel(statusOutputStream, rootNode, 0);
      }
    } else {
      writeToCommandLine(statusOutputStream, "The specified object does not use any other objects.\n");
    }

  }


  private void printUsedNodesToplevel(OutputStream statusOutputStream, DependencyNode node, int depth) {
    printNode(statusOutputStream, node, depth, false);
    for (DependencyNode child : PrintdependenciesImpl.sort(node.getUsedNodes())) {
      printNode(statusOutputStream, child, depth + 1, false);
    }
  }


  private void printNode(OutputStream statusOutputStream, DependencyNode node, int depth, boolean mark) {
    PrintdependenciesImpl.printNode(this, statusOutputStream, node, depth, mark);
  }


  private void printUsedNodesRecursively(OutputStream statusOutputStream, DependencyNode node, int depth,
                                         HashSet<DependencyNode> workedInThisHierarchy) {
    printNode(statusOutputStream, node, depth, false);
    for (DependencyNode child : PrintdependenciesImpl.sort(node.getUsedNodes())) {
      if (workedInThisHierarchy.add(child)) {
        printUsedNodesRecursively(statusOutputStream, child, depth + 1, workedInThisHierarchy);
      } else {
        printNode(statusOutputStream, child, depth + 1, true);
      }
    }
  }
}
