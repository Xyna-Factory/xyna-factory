/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xmcp.gitintegration.impl.processing;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.RuntimeContextDependency;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.impl.TmpSessionAuthWrapper;



public class RuntimeContextDependencyProcessor implements WorkspaceContentProcessor<RuntimeContextDependency> {

  private static final String DEP_TYPE_WORSPACE = "Workspace";
  private static final String DEP_TYPE_APPLICATION = "Application";

  private static final String TAG_RUNTIMECONTEXTDEPENDENCY = "runtimecontextdependency";
  private static final String TAG_DEPTYPE = "depType";
  private static final String TAG_DEPNAME = "depName";
  private static final String TAG_DEPADDITION = "depAddition";

  private static final String TEMPORARY_SESSION_AUTHENTICATION_USERNAME_CREATE = "RuntimeContextDependencyProcessor.create";
  private static final String TEMPORARY_SESSION_AUTHENTICATION_USERNAME_MODIFY = "RuntimeContextDependencyProcessor.modify";
  private static final String TEMPORARY_SESSION_AUTHENTICATION_USERNAME_DELETE = "RuntimeContextDependencyProcessor.delete";

	private static final RevisionManagement revisionManagement = XynaFactory.isFactoryServer()
			? XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
			: null;
	private static final RuntimeContextDependencyManagement rtcDependencyManagement = XynaFactory.isFactoryServer()
			? XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
					.getRuntimeContextDependencyManagement()
			: null;

  @Override
  public String getTagName() {
    return TAG_RUNTIMECONTEXTDEPENDENCY;
  }


  @Override
  public String createItemKeyString(RuntimeContextDependency item) {
    return format(item);
  }


  @Override
  public String createDifferencesString(RuntimeContextDependency from, RuntimeContextDependency to) {
    String fromStr = format(from);
    String toStr = format(to);

    String differencesString = "";
    if (!fromStr.equals(toStr)) {
      differencesString = "=>" + toStr;
    }
    return differencesString;
  }


  @Override
  public void writeItem(XmlBuilder builder, RuntimeContextDependency item) {
    builder.startElement(TAG_RUNTIMECONTEXTDEPENDENCY);
    builder.element(TAG_DEPTYPE, item.getDepType());
    builder.element(TAG_DEPNAME, item.getDepName());
    if (item.getDepAddition() != null) {
      builder.element(TAG_DEPADDITION, item.getDepAddition());
    }
    builder.endElement(TAG_RUNTIMECONTEXTDEPENDENCY);
  }


  @Override
  public RuntimeContextDependency parseItem(Node node) {
    RuntimeContextDependency rcd = new RuntimeContextDependency();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_DEPTYPE)) {
        rcd.setDepType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DEPNAME)) {
        rcd.setDepName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DEPADDITION)) {
        rcd.setDepAddition(childNode.getTextContent());
      }
    }
    return rcd;
  }


  @Override
  public List<RuntimeContextDependency> createItems(Long revision) {
    List<RuntimeContextDependency> rcdList = new ArrayList<RuntimeContextDependency>();
    try {
      Workspace ws = revisionManagement.getWorkspace(revision);
      Collection<RuntimeDependencyContext> dependencies = rtcDependencyManagement.getDependencies(ws);
      for (RuntimeDependencyContext rdc : dependencies) {
        RuntimeContextDependency rcd = new RuntimeContextDependency();
        rcd.setDepType(rdc.getRuntimeDependencyContextType().toString());
        rcd.setDepName(rdc.getName());
        if (rdc.getAdditionalIdentifier() != null) {
          rcd.setDepAddition(rdc.getAdditionalIdentifier());
        }
        rcdList.add(rcd);
      }
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
    Collections.sort(rcdList, (x, y) -> format(x).compareTo(format(y)));
    return rcdList;
  }


  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends RuntimeContextDependency> from,
                                                  Collection<? extends RuntimeContextDependency> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<WorkspaceContentDifference>();
    List<RuntimeContextDependency> toWorkingList = new ArrayList<RuntimeContextDependency>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, RuntimeContextDependency> toMap = new HashMap<String, RuntimeContextDependency>();
    for (RuntimeContextDependency toEntry : toWorkingList) {
      String key = toEntry.getDepType() + ":" + toEntry.getDepName();
      toMap.put(key, toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (RuntimeContextDependency fromEntry : from) {
        String key = fromEntry.getDepType() + ":" + fromEntry.getDepName();
        RuntimeContextDependency toEntry = toMap.get(key);

        WorkspaceContentDifference wcd = new WorkspaceContentDifference();
        wcd.setContentType(TAG_RUNTIMECONTEXTDEPENDENCY);
        wcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if ((fromEntry.getDepAddition() != null) && (toEntry.getDepAddition() != null)
              && !fromEntry.getDepAddition().equals(toEntry.getDepAddition())) {
            wcd.setDifferenceType(new MODIFY());
            wcd.setNewItem(toEntry);
            toWorkingList.remove(toEntry); // remove entry from to-list
          } else {
            toWorkingList.remove(toEntry); // remove entry from to-list
            continue; // EQUAL -> ignore entry
          }
        } else {
          wcd.setDifferenceType(new DELETE());
        }
        wcdList.add(wcd);
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for (RuntimeContextDependency toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_RUNTIMECONTEXTDEPENDENCY);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  @Override
  public void create(RuntimeContextDependency item, long revision) {
    try {
      create(revisionManagement.getWorkspace(revision), item);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public void create(RuntimeDependencyContext owner, RuntimeContextDependency item) {
    try (TmpSessionAuthWrapper wrapper = new TmpSessionAuthWrapper(TEMPORARY_SESSION_AUTHENTICATION_USERNAME_CREATE,
                                                                   TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE)) {
      RuntimeDependencyContext dependency = createRuntimeDependencyContext(item);
      rtcDependencyManagement.addDependency(owner, dependency, wrapper.getTSA().getUsername(), true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void modify(RuntimeContextDependency from, RuntimeContextDependency to, long revision) {
    try {
      modify(revisionManagement.getWorkspace(revision), from, to);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public void modify(RuntimeDependencyContext owner, RuntimeContextDependency from, RuntimeContextDependency to) {
    try (TmpSessionAuthWrapper wrapper = new TmpSessionAuthWrapper(TEMPORARY_SESSION_AUTHENTICATION_USERNAME_MODIFY,
                                                                   TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE)) {
      // get all current workspace dependencies
      Collection<RuntimeDependencyContext> dependencies = rtcDependencyManagement.getRequirements(owner);

      // remove from-runtimeContextDepencency from workspace dependency list 
      String fromStr = format(from);
      List<RuntimeDependencyContext> newDependenyList = new ArrayList<RuntimeDependencyContext>();
      boolean hastFound = false;
      for (RuntimeDependencyContext rdc : dependencies) {
        if (fromStr.equals(format(rdc))) {
          hastFound = true; //found from-entry is not added to newDependencyList
          continue;
        }
        newDependenyList.add(rdc);
      }

      if (!hastFound) {
        throw new RuntimeException("from entry " + fromStr + " not found in dependencies of the "
            + owner.getRuntimeDependencyContextType().getClass().getSimpleName() + ":" + owner.getName());
      }

      // add to-Element to the updated newDepencencyList
      newDependenyList.add(createRuntimeDependencyContext(to));

      rtcDependencyManagement.modifyDependencies(owner, newDependenyList, wrapper.getTSA().getUsername());

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void delete(RuntimeContextDependency item, long revision) {
    try {
      delete(revisionManagement.getWorkspace(revision), item);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public void delete(RuntimeDependencyContext owner, RuntimeContextDependency item) {
    try (TmpSessionAuthWrapper wrapper = new TmpSessionAuthWrapper(TEMPORARY_SESSION_AUTHENTICATION_USERNAME_DELETE,
                                                                   TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE)) {
      RuntimeDependencyContext dependency = createRuntimeDependencyContext(item);
      rtcDependencyManagement.removeDependency(owner, dependency, wrapper.getTSA().getUsername());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Convert RuntimeContextDependency to RuntimeDependencyContext
   * @param item
   *   RuntimeContextDependency (Item defined in GitIntegration) to convert
   * @return
   *  RuntimeDependencyContext (Item defined in XynaFactory)
   */
  private static RuntimeDependencyContext createRuntimeDependencyContext(RuntimeContextDependency item) {
    RuntimeDependencyContext dependency = null;
    if (item.getDepType().equals(DEP_TYPE_WORSPACE)) {
      dependency = new Workspace(item.getDepName());
    } else if (item.getDepType().equals(DEP_TYPE_APPLICATION)) {
      dependency = new Application(item.getDepName(), item.getDepAddition());
    } else {
      throw new RuntimeException("Invalid RuntimeContextDependenc.depType: " + item.getDepType());
    }
    return dependency;
  }


  /**
   * Create String Format: Workspace:&lt;WorkspaceName&gt; or Application:&lt;ApplicationName&gt;/&lt;Version&gt;
   * @param item
   *  RuntimeContextDependency (Item defined in GitIntegration) to format
   * @return
   *  formatted String of item
   */
  public String format(RuntimeContextDependency item) {
    StringBuffer sb = new StringBuffer();
    sb.append(item.getDepType()).append(":").append(item.getDepName());
    if (item.getDepAddition() != null) {
      sb.append("/");
      sb.append(item.getDepAddition());
    }
    return sb.toString();
  }


  /**
   * Create String Format: Workspace:&lt;WorkspaceName&gt; or Application:&lt;ApplicationName&gt;/&lt;Version&gt;
   * @param item
   *  RuntimeDependencyContext (Item defined in XynaFactory) to format
   * @return
   *  formatted String of item
   */
  private static String format(RuntimeDependencyContext item) {
    StringBuffer sb = new StringBuffer();
    sb.append(item.getRuntimeDependencyContextType().toString()).append(":").append(item.getName());
    if (item.getAdditionalIdentifier() != null) {
      sb.append("/");
      sb.append(item.getAdditionalIdentifier());
    }
    return sb.toString();
  }
}
