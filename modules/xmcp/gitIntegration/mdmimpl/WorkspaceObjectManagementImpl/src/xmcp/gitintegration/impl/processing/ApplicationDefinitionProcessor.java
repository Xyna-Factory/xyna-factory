/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EmptyRepositoryEvent;
import com.gip.xyna.xfmg.exceptions.XFMG_FailedToAddObjectToApplication;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationDefinitionInformation;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationEntryStorable.ApplicationEntryType;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RemoveApplicationParameters;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xopctrl.usermanagement.TemporarySessionAuthentication;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.ApplicationDefinition;
import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.ContentEntry;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.RuntimeContextDependency;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.impl.OutputCreator;
import xmcp.gitintegration.impl.TmpSessionAuthWrapper;



public class ApplicationDefinitionProcessor implements WorkspaceContentProcessor<ApplicationDefinition> {

  private static final String TAG_APPLICATIONDEFINITION = "applicationdefinition";
  private static final String TAG_NAME = "name";
  private static final String TAG_DOCUMENTATION = "documentation";
  private static final String TAG_RUNTIMECONTECTDEPENDENCIES = "runtimecontextdependencies";
  private static final String TAG_CONTENTENTRIES = "contententries";
  private static final String TAG_CONTENTENTRY = "contententry";
  private static final String TAG_CONTENTENTRY_TYPE = "type";
  private static final String TAG_CONTENTENTRY_FQNAME = "fqName";

  private static final String TEMPORARY_SESSION_AUTHENTICATION_USERNAME_CREATE = "ApplicationDefinitionProcessor.create";

  private static final RevisionManagement revisionManagement =
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private static final ApplicationManagementImpl applicationManagement =
      (ApplicationManagementImpl) XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getApplicationManagement();

  private static final Set<ApplicationEntryType> xmomType =
      Set.of(ApplicationEntryType.WORKFLOW, ApplicationEntryType.DATATYPE, ApplicationEntryType.EXCEPTION);

  private static final Set<ApplicationEntryType> triggerFilterType =
      Set.of(ApplicationEntryType.TRIGGER, ApplicationEntryType.TRIGGERINSTANCE, ApplicationEntryType.FILTER,
             ApplicationEntryType.FILTERINSTANCE);

  private static final RuntimeContextDependencyProcessor rcdp = new RuntimeContextDependencyProcessor();

  private static final EmptyRepositoryEvent emptyEvent = new EmptyRepositoryEvent();


  @Override
  public List<WorkspaceContentDifference> compare(Collection<? extends ApplicationDefinition> from,
                                                  Collection<? extends ApplicationDefinition> to) {
    List<WorkspaceContentDifference> wcdList = new ArrayList<>();
    List<ApplicationDefinition> toWorkingList = to == null ? new ArrayList<>() : new ArrayList<>(to);

    HashMap<String, ApplicationDefinition> toMap = new HashMap<String, ApplicationDefinition>();
    for (ApplicationDefinition toEntry : toWorkingList) {
      toMap.put(toEntry.getName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (ApplicationDefinition fromEntry : from) {
        ApplicationDefinition toEntry = toMap.get(fromEntry.getName());
        WorkspaceContentDifference wcd = new WorkspaceContentDifference();

        wcd.setContentType(TAG_APPLICATIONDEFINITION);
        wcd.setExistingItem(fromEntry);
        wcd.setDifferenceType(toEntry == null ? new DELETE() : new MODIFY());
        toWorkingList.remove(toEntry); // remove entry from to-list, because it is not a new entry
        if (toEntry == null) {
          continue;
        }
        boolean hasChanged = !Objects.equals(fromEntry.getDocumentation(), toEntry.getDocumentation())
            || (!compareRuntimeContextDependencies(fromEntry, toEntry).isEmpty())
            || (!getNewContentEntries(fromEntry, toEntry).isEmpty()) 
            || (!getNewContentEntries(toEntry, fromEntry).isEmpty());
        if (hasChanged) {
          wcd.setNewItem(toEntry);
          wcdList.add(wcd);
        } // else: EQUAL -> ignore entry
      }
    }

    // iterate over toWorking-list (only CREATE-Entries remain)
    for (ApplicationDefinition toEntry : toWorkingList) {
      WorkspaceContentDifference wcd = new WorkspaceContentDifference();
      wcd.setContentType(TAG_APPLICATIONDEFINITION);
      wcd.setNewItem(toEntry);
      wcd.setDifferenceType(new CREATE());
      wcdList.add(wcd);
    }
    return wcdList;
  }


  private List<WorkspaceContentDifference> compareRuntimeContextDependencies(ApplicationDefinition from, ApplicationDefinition to) {
    return rcdp.compare(from.getRuntimeContextDependencies(), to.getRuntimeContextDependencies());
  }


  private String createContentEntryKey(ContentEntry ce) {
    return ce.getType() + ":" + ce.getFQName();
  }


  private List<ContentEntry> getNewContentEntries(ApplicationDefinition from, ApplicationDefinition to) {
    List<ContentEntry> resultList = new ArrayList<ContentEntry>();

    Set<String> fromSet = new HashSet<String>();
    if (from.getContentEntries() != null) {
      for (ContentEntry ce : from.getContentEntries()) {
        fromSet.add(createContentEntryKey(ce));
      }
    }

    if (to.getContentEntries() != null) {
      for (ContentEntry ce : to.getContentEntries()) {
        if (!fromSet.contains(createContentEntryKey(ce))) {
          resultList.add(ce);
        }
      }
    }
    return resultList;
  }


  @Override
  public ApplicationDefinition parseItem(Node node) {
    ApplicationDefinition.Builder adBuilder = new ApplicationDefinition.Builder();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_NAME)) {
        adBuilder.name(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DOCUMENTATION)) {
        adBuilder.documentation(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_RUNTIMECONTECTDEPENDENCIES)) {
        adBuilder.runtimeContextDependencies(parseRuntimeContextDependencies(childNode));
      } else if (childNode.getNodeName().equals(TAG_CONTENTENTRIES)) {
        adBuilder.contentEntries(parseContentEntries(childNode));
      }
    }
    return adBuilder.instance();
  }


  private static List<RuntimeContextDependency> parseRuntimeContextDependencies(Node node) {
    List<RuntimeContextDependency> rtcList = new ArrayList<RuntimeContextDependency>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(rcdp.getTagName())) {
        rtcList.add(rcdp.parseItem(childNode));
      }
    }
    return rtcList;
  }


  private static List<ContentEntry> parseContentEntries(Node node) {
    List<ContentEntry> ceList = new ArrayList<ContentEntry>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_CONTENTENTRY)) {
        ceList.add(parseContentEntry(childNode));
      }
    }
    return ceList;
  }


  private static ContentEntry parseContentEntry(Node node) {
    ContentEntry ce = new ContentEntry();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_CONTENTENTRY_TYPE)) {
        ce.setType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_CONTENTENTRY_FQNAME)) {
        ce.setFQName(childNode.getTextContent());
      }
    }
    return ce;
  }


  @Override
  public void writeItem(XmlBuilder builder, ApplicationDefinition item) {
    builder.startElement(TAG_APPLICATIONDEFINITION);
    builder.element(TAG_NAME, item.getName());
    builder.element(TAG_DOCUMENTATION, item.getDocumentation());
    if ((item.getRuntimeContextDependencies() != null) && (!item.getRuntimeContextDependencies().isEmpty())) {
      builder.startElement(TAG_RUNTIMECONTECTDEPENDENCIES);
      for (RuntimeContextDependency rtc : item.getRuntimeContextDependencies()) {
        rcdp.writeItem(builder, rtc);
      }
      builder.endElement(TAG_RUNTIMECONTECTDEPENDENCIES);
    }
    if ((item.getContentEntries() != null) && (!item.getContentEntries().isEmpty())) {
      builder.startElement(TAG_CONTENTENTRIES);
      for (ContentEntry ce : item.getContentEntries()) {
        builder.startElement(TAG_CONTENTENTRY);
        builder.element(TAG_CONTENTENTRY_TYPE, ce.getType());
        builder.element(TAG_CONTENTENTRY_FQNAME, ce.getFQName());
        builder.endElement(TAG_CONTENTENTRY);
      }
      builder.endElement(TAG_CONTENTENTRIES);
    }
    builder.endElement(TAG_APPLICATIONDEFINITION);
  }


  @Override
  public String getTagName() {
    return TAG_APPLICATIONDEFINITION;
  }


  @Override
  public String createItemKeyString(ApplicationDefinition item) {
    return item.getName();
  }


  @Override
  public String createDifferencesString(ApplicationDefinition from, ApplicationDefinition to) {
    StringBuffer ds = new StringBuffer();
    ds.append(from.getName()).append("\n");
    if (!Objects.equals(from.getDocumentation(), to.getDocumentation())) {
      ds.append("  " + TAG_DOCUMENTATION + ": ");
      ds.append(CREATE.class.getSimpleName() + ":  \"" + from.getDocumentation() + "\"=>\"" + to.getDocumentation() + "\"");
      ds.append("\n");
    }

    // Block TAG_RUNTIMECONTECTDEPENDENCIES
    List<WorkspaceContentDifference> wcdList = compareRuntimeContextDependencies(from, to);
    int createCount = 0;
    int modifyCount = 0;
    int deleteCount = 0;
    for (WorkspaceContentDifference wcd : wcdList) {
      String simpleName = wcd.getDifferenceType().getClass().getSimpleName();
      if (simpleName.equals(CREATE.class.getSimpleName())) {
        createCount++;
      } else if (simpleName.equals(MODIFY.class.getSimpleName())) {
        modifyCount++;
      } else if (simpleName.equals(DELETE.class.getSimpleName())) {
        deleteCount++;
      }
    }
    ds.append("  " + TAG_RUNTIMECONTECTDEPENDENCIES + ": ");
    ds.append(CREATE.class.getSimpleName() + ": " + createCount).append(", ");
    ds.append(MODIFY.class.getSimpleName() + ": " + modifyCount).append(", ");
    ds.append(DELETE.class.getSimpleName() + ": " + deleteCount);
    ds.append("\n");

    int tableCount = 0;
    for (WorkspaceContentDifference wcd : wcdList) {
      tableCount++;
      if (tableCount > OutputCreator.TABLE_LIMIT) {
        appendTruncatedList(ds);
        break;
      }
      String simpleName = wcd.getDifferenceType().getClass().getSimpleName();
      ds.append("    " + simpleName + ": ");
      if (simpleName.equals(CREATE.class.getSimpleName())) {
        ds.append(rcdp.format((RuntimeContextDependency) (wcd.getNewItem())));
      } else if (simpleName.equals(MODIFY.class.getSimpleName())) {
        ds.append(rcdp.createDifferencesString((RuntimeContextDependency) (wcd.getExistingItem()),
                                               (RuntimeContextDependency) (wcd.getNewItem())));
      } else if (simpleName.equals(DELETE.class.getSimpleName())) {
        ds.append(rcdp.format((RuntimeContextDependency) (wcd.getExistingItem())));
      }
      ds.append("\n");
    }

    // Block TAG_CONTENTENTRIES
    List<ContentEntry> createEntries = getNewContentEntries(from, to);
    List<ContentEntry> deleteEntries = getNewContentEntries(to, from);
    ds.append("  " + TAG_CONTENTENTRIES + ": ");
    ds.append(CREATE.class.getSimpleName() + ": " + createEntries.size()).append(", ");
    ds.append(DELETE.class.getSimpleName() + ": " + deleteEntries.size());
    ds.append("\n");

    tableCount = 0;
    for (ContentEntry ce : createEntries) {
      tableCount++;
      if (tableCount > OutputCreator.TABLE_LIMIT) {
        appendTruncatedList(ds);
        break;
      }
      ds.append("    " + CREATE.class.getSimpleName() + ": ");
      ds.append(createContentEntryKey(ce));
      ds.append("\n");
    }
    for (ContentEntry ce : deleteEntries) {
      tableCount++;
      if (tableCount > OutputCreator.TABLE_LIMIT) {
        appendTruncatedList(ds);
        break;
      }
      ds.append("    " + DELETE.class.getSimpleName() + ": ");
      ds.append(createContentEntryKey(ce));
      ds.append("\n");
    }
    return ds.toString();
  }


  private void appendTruncatedList(StringBuffer ds) {
    ds.append("    " + "...");
    ds.append("\n");
  }


  @Override
  public List<ApplicationDefinition> createItems(Long revision) {
    List<ApplicationDefinition> adList = new ArrayList<ApplicationDefinition>();
    List<ApplicationDefinitionInformation> adiList = applicationManagement.listApplicationDefinitions(revision);
    for (ApplicationDefinitionInformation adi : adiList) {
      ApplicationDefinition.Builder adBuilder = new ApplicationDefinition.Builder();
      adBuilder.name(adi.getName());
      adBuilder.documentation(adi.getComment());
      Collection<RuntimeDependencyContext> dependencies = adi.getRequirements();
      List<RuntimeContextDependency> rcdList = new ArrayList<RuntimeContextDependency>();
      adBuilder.runtimeContextDependencies(rcdList);
      for (RuntimeDependencyContext rdc : dependencies) {
        RuntimeContextDependency.Builder rcdBuilder = new RuntimeContextDependency.Builder();
        rcdBuilder.depType(rdc.getRuntimeDependencyContextType().toString());
        rcdBuilder.depName(rdc.getName());
        if (rdc.getAdditionalIdentifier() != null) {
          rcdBuilder.depAddition(rdc.getAdditionalIdentifier());
        }
        rcdList.add(rcdBuilder.instance());
      }
      List<ContentEntry> ceList = new ArrayList<ContentEntry>();
      adBuilder.contentEntries(ceList);
      List<ApplicationEntryStorable> aesList = applicationManagement.listApplicationDetails(adi.getName(), null, false, null, revision);
      for (ApplicationEntryStorable aes : aesList) {
        ContentEntry.Builder ceBuilder = new ContentEntry.Builder();
        ceBuilder.type(aes.getType());
        ceBuilder.fQName(aes.getName());
        ceList.add(ceBuilder.instance());
      }
      adList.add(adBuilder.instance());
      
      Collections.sort(rcdList, (x, y) -> x.getDepName().compareTo(y.getDepName()));
      Collections.sort(ceList, (x, y) -> x.getFQName().compareTo(y.getFQName()));
    }
    Collections.sort(adList, (x, y) -> x.getName().compareTo(y.getName()));
    return adList;
  }


  @Override
  public void create(ApplicationDefinition item, long revision) {
    try {
      applicationManagement.defineApplication(item.getName(), item.getDocumentation(), revision);

      Workspace ws = revisionManagement.getWorkspace(revision);
      RuntimeDependencyContext owner = new com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition(item.getName(), ws);
      for (RuntimeContextDependency rcd : item.getRuntimeContextDependencies()) {
        rcdp.create(owner, rcd);
      }
      for (ContentEntry ce : item.getContentEntries()) {
        addEntryToAppDef(ce, item.getName(), revision);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void modify(ApplicationDefinition from, ApplicationDefinition to, long revision) {
    try {
      Workspace ws = revisionManagement.getWorkspace(revision);
      RuntimeDependencyContext owner = new com.gip.xyna.xfmg.xfctrl.revisionmgmt.ApplicationDefinition(to.getName(), ws);

      applicationManagement.changeApplicationDefinitionComment(to.getName(), revision, to.getDocumentation());
      for (WorkspaceContentDifference wcd : compareRuntimeContextDependencies(from, to)) {
        String actualResolution = wcd.getDifferenceType().getClass().getSimpleName();
        if (actualResolution.equals(CREATE.class.getSimpleName())) {
          rcdp.create(owner, (RuntimeContextDependency) (wcd.getNewItem()));
        } else if (actualResolution.equals(MODIFY.class.getSimpleName())) {
          rcdp.modify(owner, (RuntimeContextDependency) (wcd.getExistingItem()), (RuntimeContextDependency) (wcd.getNewItem()));
        } else if (actualResolution.equals(DELETE.class.getSimpleName())) {
          rcdp.delete(owner, (RuntimeContextDependency) (wcd.getExistingItem()));
        }
      }

      for (ContentEntry ce : getNewContentEntries(to, from)) {
        ApplicationEntryType aet = ApplicationEntryType.valueOf(ce.getType());
        applicationManagement.removeObjectFromApplication(to.getName(), ce.getFQName(), aet, revision, emptyEvent, false, null);
      }

      for (ContentEntry ce : getNewContentEntries(from, to)) {
        addEntryToAppDef(ce, to.getName(), revision);
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private void addEntryToAppDef(ContentEntry ce, String appDefName, long revision) throws XFMG_FailedToAddObjectToApplication {
    ApplicationEntryType aet = ApplicationEntryType.valueOf(ce.getType());
    if (xmomType.contains(aet)) {
      applicationManagement.addXMOMObjectToApplication(ce.getFQName(), appDefName, revision, emptyEvent, false, null);
    } else if (triggerFilterType.contains(aet)) {
      applicationManagement.addObjectToApplicationDefinition(ce.getFQName(), aet, appDefName, revision, false, null, emptyEvent);
    } else {
      applicationManagement.addNonModelledObjectToApplication(ce.getFQName(), appDefName, null, aet, revision, false, null);
    }
  }


  @Override
  public void delete(ApplicationDefinition item, long revision) {
    try (TmpSessionAuthWrapper wrapper = new TmpSessionAuthWrapper(TEMPORARY_SESSION_AUTHENTICATION_USERNAME_CREATE,
                                                                   TemporarySessionAuthentication.TEMPORARY_CLI_USER_ROLE)) {
      RemoveApplicationParameters params = new RemoveApplicationParameters();
      params.setParentWorkspace(revisionManagement.getWorkspace(revision));
      params.setUser(wrapper.getTSA().getUsername());

      applicationManagement.removeApplicationVersion(item.getName(), null, params, emptyEvent);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
