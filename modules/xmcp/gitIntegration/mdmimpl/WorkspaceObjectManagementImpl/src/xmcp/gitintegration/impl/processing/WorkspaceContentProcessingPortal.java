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
package xmcp.gitintegration.impl.processing;



import java.util.ArrayList;
import org.w3c.dom.Node;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.WorkspaceContentDifference;
import xmcp.gitintegration.WorkspaceContentDifferenceType;
import xmcp.gitintegration.WorkspaceContentDifferences;
import xmcp.gitintegration.WorkspaceContentItem;
import xmcp.gitintegration.WorkspaceContentType;
import xmcp.gitintegration.impl.ResolveWorkspaceDifferencesParameter;
import xmcp.gitintegration.storage.WorkspaceDifferenceListStorage;



//'Interface' between the outside world (service group / CLI) and WorkpsaceContentProcessors
public class WorkspaceContentProcessingPortal implements XynaContentProcessingPortal<WorkspaceContentItem, WorkspaceContentDifference>{

  //String is the tagName
  protected static final Map<String, WorkspaceContentProcessor<? extends WorkspaceContentItem>> parserTypes = new HashMap<>();
  protected static final List<WorkspaceContentProcessor<? extends WorkspaceContentItem>> processorOrder = new LinkedList<>();
  protected static final Map<Class<? extends WorkspaceContentType>, WorkspaceContentProcessor<? extends WorkspaceContentItem>> registeredTypes =
      createRegisteredTypesMap();


  private static Map<Class<? extends WorkspaceContentType>, WorkspaceContentProcessor<? extends WorkspaceContentItem>> createRegisteredTypesMap() {
    Map<Class<? extends WorkspaceContentType>, WorkspaceContentProcessor<? extends WorkspaceContentItem>> result;
    result = new HashMap<>();
    //register WorkspaceContentProcessors here: addToMap(result, new <WorkspaceContentType>Processor());
    addToMap(result, new RuntimeContextDependencyProcessor());
    addToMap(result, new OrderTypeProcessor());
    addToMap(result, new DatatypeProcessor());
    addToMap(result, new OrderInputSourceProcessor());
    addToMap(result, new TriggerProcessor());
    addToMap(result, new TriggerInstanceProcessor());
    addToMap(result, new FilterProcessor());
    addToMap(result, new FilterInstanceProcessor());
    addToMap(result, new XMOMStorableProcessor());
    addToMap(result, new ApplicationDefinitionProcessor());
    return result;
  }


  public static final Map<String, WorkspaceContentDifferenceType> differenceTypes =
      Map.ofEntries(Map.entry(CREATE.class.getSimpleName(), new CREATE()), 
                    Map.entry(MODIFY.class.getSimpleName(), new MODIFY()),
                    Map.entry(DELETE.class.getSimpleName(), new DELETE()));


  @SuppressWarnings("unchecked")
  protected static void addToMap(Map<Class<? extends WorkspaceContentType>, WorkspaceContentProcessor<? extends WorkspaceContentItem>> map,
                                 WorkspaceContentProcessor<? extends WorkspaceContentItem> toAdd) {
    map.put((Class<? extends WorkspaceContentType>) getWorkspaceContentTypeFromProcessor(toAdd), toAdd);
    parserTypes.put(toAdd.getTagName(), toAdd);
    processorOrder.add(toAdd);
  }


  private static Class<?> getWorkspaceContentTypeFromProcessor(WorkspaceContentProcessor<? extends WorkspaceContentItem> processor) {
    return (Class<?>) ((java.lang.reflect.ParameterizedType) processor.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
  }


  public WorkspaceContentItem parseWorkspaceContentItem(Node node) {
    if (node == null) {
      return null;
    }
    WorkspaceContentProcessor<? extends WorkspaceContentItem> parser = parserTypes.get(node.getNodeName());
    if (parser == null) {
      return null;
    }
    WorkspaceContentItem result = parser.parseItem(node);
    return result;
  }


  public List<WorkspaceContentDifference> compare(WorkspaceContent c1, WorkspaceContent c2) {
    List<WorkspaceContentDifference> result = new LinkedList<>();
    for (WorkspaceContentProcessor<? extends WorkspaceContentItem> type : processorOrder) {
      List<WorkspaceContentDifference> differencesThisType = compareSingleType(c1, c2, type);
      result.addAll(differencesThisType);
    }

    int id = 0;
    for (WorkspaceContentDifference difference : result) {
      difference.setEntryId(id++);
    }

    return result;
  }


  @SuppressWarnings("unchecked")
  private <T extends WorkspaceContentItem> List<WorkspaceContentDifference> compareSingleType(WorkspaceContent c1, WorkspaceContent c2,
                                                                                              WorkspaceContentProcessor<T> processor) {
    Class<T> expectedClass = (Class<T>) getWorkspaceContentTypeFromProcessor(processor);
    List<WorkspaceContentDifference> result = new LinkedList<>();
    List<T> from = (List<T>) c1.getWorkspaceContentItems();
    List<T> to = (List<T>) c2.getWorkspaceContentItems();
    from = from == null ? new ArrayList<>() : from.stream().filter(x -> matchClass(x, expectedClass)).collect(Collectors.toList());
    to = to == null ? new ArrayList<>() : to.stream().filter(x -> matchClass(x, expectedClass)).collect(Collectors.toList());

    result.addAll(processor.compare(from, to));

    return result;
  }


  private boolean matchClass(Object obj, Class<?> clazz) {
    return obj.getClass().equals(clazz);
  }


  public List<WorkspaceContentItem> createItems(Long revision) {
    List<WorkspaceContentItem> result = new LinkedList<WorkspaceContentItem>();

    for (WorkspaceContentProcessor<? extends WorkspaceContentItem> supportedType : processorOrder) {
      List<? extends WorkspaceContentItem> subList = supportedType.createItems(revision);
      result.addAll(subList);
    }

    return result;
  }


  public void writeItem(XmlBuilder builder, WorkspaceContentItem item) {
    writeItemInternal(builder, item);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void writeItemInternal(XmlBuilder builder, WorkspaceContentItem item) {
    if (item == null) {
      return;
    }
    WorkspaceContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    processor.writeItem(builder, item);
  }


  public void createItem(WorkspaceContentItem item, String workspaceName) {
    long revision = convertWorkspaceNameToRevision(workspaceName);
    createItemInternal(item, revision);
  }


  private long convertWorkspaceNameToRevision(String workspaceName) {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(new Workspace(workspaceName));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void createItemInternal(WorkspaceContentItem item, long revision) {
    WorkspaceContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    processor.create(item, revision);
  }


  public void deleteItem(WorkspaceContentItem item, String workspaceName) {
    long revision = convertWorkspaceNameToRevision(workspaceName);
    deleteItemInternal(item, revision);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void deleteItemInternal(WorkspaceContentItem item, long revision) {
    WorkspaceContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    processor.delete(item, revision);
  }


  public void modifyItem(WorkspaceContentItem from, WorkspaceContentItem to, String workspaceName) {
    long revision = convertWorkspaceNameToRevision(workspaceName);
    modifyItemInternal(from, to, revision);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void modifyItemInternal(WorkspaceContentItem from, WorkspaceContentItem to, long revision) {
    WorkspaceContentProcessor processor = registeredTypes.get(from.getClass());
    checkProcessor(processor, from.getClass());
    processor.modify(from, to, revision);
  }


  public String createItemKeyString(WorkspaceContentItem item) {
    return createItemKeyStringInternal(item);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private String createItemKeyStringInternal(WorkspaceContentItem item) {
    if (item == null) {
      return null;
    }
    WorkspaceContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    return processor.createItemKeyString(item);
  }


  public String createDifferenceString(WorkspaceContentDifference diff) {
    return createDifferenceStringInternal(diff);
  }


  public String getTagName(Class<? extends WorkspaceContentItem> workspaceContentItem) {
    return getTagNameInternal(workspaceContentItem);
  }


  @SuppressWarnings({"rawtypes"})
  public String getTagNameInternal(Class<? extends WorkspaceContentItem> workspaceContentItem) {
    WorkspaceContentProcessor processor = registeredTypes.get(workspaceContentItem);
    checkProcessor(processor, workspaceContentItem);
    return processor.getTagName();
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private String createDifferenceStringInternal(WorkspaceContentDifference diff) {
    WorkspaceContentProcessor processor = registeredTypes.get(diff.getExistingItem().getClass());
    checkProcessor(processor, diff.getExistingItem().getClass());
    return processor.createDifferencesString(diff.getExistingItem(), diff.getNewItem());
  }


  public String closeDifferenceList(long listid) {
    WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
    storage.deleteWorkspaceDifferenceList(listid);
    return "Workspace Difference List with id " + listid + " closed.";
  }


  public String resolveList(long diffListId, List<ResolveWorkspaceDifferencesParameter> paramlist) {
    WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
    StringBuilder sb = new StringBuilder();
    WorkspaceContentDifferences differences = storage.loadDifferences(diffListId);
    for (ResolveWorkspaceDifferencesParameter param : paramlist) {
      resolveSingleDifference(param, differences, sb);
    }
    finishResolve(storage, differences, sb);
    return sb.toString();
  }


  public String resolveAll(long diffListId, Optional<String> resolution) {
    WorkspaceDifferenceListStorage storage = new WorkspaceDifferenceListStorage();
    StringBuilder sb = new StringBuilder();
    WorkspaceContentDifferences differences = storage.loadDifferences(diffListId);
    resolveAllDifferencesImpl(resolution, differences, sb);
    finishResolve(storage, differences, sb);
    return sb.toString();
  }


  private void finishResolve(WorkspaceDifferenceListStorage storage, WorkspaceContentDifferences differences,
                             StringBuilder sb) {
    if (differences.getDifferences().size() > 0) {
      storage.persist(differences);
      sb.append("There are " + differences.getDifferences().size() + " differences left in list " + differences.getListId());
    } else {
      //all done. Remove list
      storage.deleteWorkspaceDifferenceList(differences.getListId());
      sb.append("All differences in list " + differences.getListId() + " have been resolved. List closed");
    }
  }


  private boolean tryResolveItem(WorkspaceContentDifference entry, String workspaceName, Optional<String> resolution, StringBuilder sb) {
    try {
      resolveItem(entry, workspaceName, resolution);
      return true;
    } catch (Exception e) {
      //if there is an exception, do not remove the entry
      sb.append("Exception occurred while resolving item " + entry.getEntryId() + ". It remains in the list. Details: " + e.getMessage());
      sb.append("\n");
    }
    return false;
  }


  private void resolveAllDifferencesImpl(Optional<String> resolution, WorkspaceContentDifferences differences,
                                         StringBuilder sb) {
    List<? extends WorkspaceContentDifference> differenceList = differences.getDifferences();
    for (int i = differenceList.size() - 1; i >= 0; i--) {
      WorkspaceContentDifference entry = differenceList.get(i);
      boolean success = tryResolveItem(entry, differences.getWorkspaceName(), resolution, sb);
      if (success) {
        differenceList.remove(entry);
      }
    }
  }


  private void resolveSingleDifference(ResolveWorkspaceDifferencesParameter param, WorkspaceContentDifferences differences,
                                       StringBuilder sb) {
    if (!param.getEntry().isPresent()) { return; }
    long entryId = param.getEntry().get();
    Optional<? extends WorkspaceContentDifference> entryOptional = findEntry(differences, entryId);
    if (entryOptional.isEmpty()) {
      long id = differences.getListId();
      throw new RuntimeException("No entry " + entryId + " found in workspace difference list " + id);
    }
    WorkspaceContentDifference entry = entryOptional.get();
    boolean success = tryResolveItem(entry, differences.getWorkspaceName(), param.getResolution(), sb);
    if (success) {
      differences.getDifferences().remove(entry);
    }
  }


  private Optional<? extends WorkspaceContentDifference> findEntry(WorkspaceContentDifferences diffs, long entryId) {
    return diffs.getDifferences().stream().filter(x -> x.getEntryId() == entryId).findFirst();
  }


  private void resolveItem(WorkspaceContentDifference difference, String workspaceName, Optional<String> resolution) {
    String actualResolution = difference.getDifferenceType().getClass().getSimpleName();
    if (!resolution.isEmpty()) {
      validateResolution(resolution.get());
      actualResolution = determineActualResolution(actualResolution, resolution.get());
    }

    if (actualResolution.equals("")) {
      return;
    } else if (actualResolution.equals(CREATE.class.getSimpleName())) {
      createItem(difference.getNewItem(), workspaceName);
      return;
    } else if (actualResolution.equals(MODIFY.class.getSimpleName())) {
      modifyItem(difference.getExistingItem(), difference.getNewItem(), workspaceName);
      return;
    } else if (actualResolution.equals(DELETE.class.getSimpleName())) {
      deleteItem(difference.getExistingItem(), workspaceName);
      return;
    }

    throw new RuntimeException("Unexpected resolution: \"" + actualResolution + "\".");

  }


  private void validateResolution(String resolution) {
    resolution = resolution.toUpperCase();
    if (!differenceTypes.containsKey(resolution)) {
      throw new RuntimeException("Unknown resolution. Allowed Values are: " + String.join(", ", differenceTypes.keySet()));
    }
  }


  /***
   * Returns either the resolution to use, or the empty string, if nothing needs to be done to resolve this entry
   * @param suggested
   * 'natural' resolution to
   * @param desired
   * resolution provided by the user
   * @return
   * resolution to use. If no action should be taken to resolve the difference, this method returns the empty string
   */
  private String determineActualResolution(String suggested, String desired) {
    if (suggested.equals(desired)) {
      return suggested;
    }

    if (suggested.equals(CREATE.class.getSimpleName()) && desired.equals(MODIFY.class.getSimpleName())) {
      return suggested;
    }

    return "";
  }


  private void checkProcessor(WorkspaceContentProcessor<?> processor, Class<?> clazz) {
    checkProcessor(processor, clazz.getSimpleName());
  }


  private void checkProcessor(WorkspaceContentProcessor<?> processor, String type) {
    if (processor == null) {
      throw new RuntimeException("Unknown Workspace Content Type: " + type);
    }
  }

}
