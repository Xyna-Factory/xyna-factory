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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Printdependencies;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



public class PrintdependenciesImpl extends XynaCommandImplementation<Printdependencies> {

  public void execute(OutputStream statusOutputStream, Printdependencies payload) throws XynaException {

    DependencySourceType type;
    try {
      type = DependencySourceType.getByName(payload.getObjectType(), true);
    } catch (IllegalArgumentException e) {
      throw new XDEV_UNSUPPORTED_FEATURE("Dependencies for object type '" + payload.getObjectType() + "'. Supported values are: "
          + DependencySourceType.getAllValidNamesAsSingleString() + ".");
    }

    if (payload.getApplicationName() != null && payload.getVersionName() == null) {
      writeToCommandLine(statusOutputStream, "Please specify version of the given application.");
      return;
    }
    Long revision =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(payload.getApplicationName(), payload.getVersionName(), payload.getWorkspaceName());


    DependencyNode rootNode =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
            .getDependencyNode(payload.getObject(), type, revision);

    if (rootNode == null) {
      writeToCommandLine(statusOutputStream, "The specified object is unknown.\n");
    } else if (rootNode.getDependentNodes() == null || rootNode.getDependentNodes().size() == 0) {
      writeToCommandLine(statusOutputStream, "The specified object is known but not used anywhere.\n");
    } else {
      if (payload.getRecurse()) {
        writeToCommandLine(statusOutputStream, "Found the following dependency tree:\n");
        printNodeDependenciesRecursively(statusOutputStream, rootNode, 0, new HashSet<DependencyNode>());
      } else {
        writeToCommandLine(statusOutputStream, "Found the following direct dependencies:\n");
        printNodeDependenciesToplevel(statusOutputStream, rootNode, 0);
      }
    }

  }


  private void printNodeDependenciesToplevel(OutputStream statusOutputStream, DependencyNode node, int depth) {
    printNode(statusOutputStream, node, depth, false);
    for (DependencyNode child : sort(node.getDependentNodes())) {
      printNode(statusOutputStream, child, depth + 1, false);
    }
  }


  private void printNode(OutputStream statusOutputStream, DependencyNode node, int depth, boolean mark) {
    printNode(this, statusOutputStream, node, depth, mark);
  }


  private void printNodeDependenciesRecursively(OutputStream statusOutputStream, DependencyNode node, int depth,
                                                HashSet<DependencyNode> workedInThisHierarchy) {
    printNode(statusOutputStream, node, depth, false);
    for (DependencyNode child : sort(node.getDependentNodes())) {
      if (workedInThisHierarchy.add(child)) {
        printNodeDependenciesRecursively(statusOutputStream, child, depth + 1, workedInThisHierarchy);
      } else {
        printNode(statusOutputStream, child, depth + 1, true);
      }
    }
  }

  public static void printNode(XynaCommandImplementation<?> xci, OutputStream statusOutputStream, DependencyNode node, int depth, boolean mark) {
    String indent = "";
    for (int i = 0; i < depth; i++) {
      indent += " . ";
    }
    indent += "* ";
    
    //RuntimeContext aus der Revision des DependencyNodes ermitteln
    String rcString = "";
    RuntimeContext rc;
    try {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(node.getRevision());
      rcString = " (" + rc.toString() + ")";
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // dann halt ohne RuntimeContext ausgeben
    }

    xci.writeLineToCommandLine(statusOutputStream, indent + node.getType() + ": " + node.getUniqueName() + rcString + (mark ? " ++" : ""));
  }

  
  private static final Comparator<DependencyNode> comparator = new Comparator<DependencyNode>() {

    public int compare(DependencyNode d1, DependencyNode d2) {
      int i = d1.getType().compareTo(d2.getType());
      if (i == 0) {
        i = d1.getUniqueName().compareTo(d2.getUniqueName());
      }
      return i;
    }
    
  };

  public static List<DependencyNode> sort(Set<DependencyNode> nodes) {
    List<DependencyNode> l = new ArrayList<DependencyNode>(nodes);
    Collections.sort(l, comparator);
    return l;
  }
}
