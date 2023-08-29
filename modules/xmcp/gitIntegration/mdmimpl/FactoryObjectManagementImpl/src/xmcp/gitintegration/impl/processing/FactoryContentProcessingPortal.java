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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Node;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryContentDifferences;
import xmcp.gitintegration.FactoryContentItem;
import xmcp.gitintegration.FactoryObjectManagement;
import xmcp.gitintegration.FactoryXmlEntryType;
import xmcp.gitintegration.FactoryXmlIgnoreEntry;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.WorkspaceContentDifferenceType;
import xmcp.gitintegration.impl.ResolveFactoryDifferencesParameter;
import xmcp.gitintegration.storage.FactoryDifferenceListStorage;



public class FactoryContentProcessingPortal {

  //String is the tagName
  protected static final HashMap<String, FactoryContentProcessor<? extends FactoryContentItem>> parserTypes = new HashMap<>();
  protected static final HashMap<Class<? extends FactoryContentItem>, FactoryContentProcessor<? extends FactoryContentItem>> registeredTypes =
      createRegisteredTypesMap();


  private static HashMap<Class<? extends FactoryContentItem>, FactoryContentProcessor<? extends FactoryContentItem>> createRegisteredTypesMap() {
    HashMap<Class<? extends FactoryContentItem>, FactoryContentProcessor<? extends FactoryContentItem>> result = new HashMap<>();

    //register FactoryContentProcessors here: addToMap(result, new <FactoryContentType>Processor());
    addToMap(result, new CapacityProcessor());
    addToMap(result, new XynaPropertyProcessor());

    return result;
  }


  public static final HashMap<String, WorkspaceContentDifferenceType> differenceTypes = setupDifferenceTypes();


  private static HashMap<String, WorkspaceContentDifferenceType> setupDifferenceTypes() {
    HashMap<String, WorkspaceContentDifferenceType> result = new HashMap<>();
    result.put(CREATE.class.getSimpleName(), new CREATE());
    result.put(MODIFY.class.getSimpleName(), new MODIFY());
    result.put(DELETE.class.getSimpleName(), new DELETE());
    return result;
  }


  @SuppressWarnings("unchecked")
  protected static void addToMap(HashMap<Class<? extends FactoryContentItem>, FactoryContentProcessor<? extends FactoryContentItem>> map,
                                 FactoryContentProcessor<? extends FactoryContentItem> toAdd) {
    map.put((Class<? extends FactoryContentItem>) getFactoryContentItemTypeFromProcessor(toAdd), toAdd);
    parserTypes.put(toAdd.getTagName(), toAdd);
  }


  private static Class<?> getFactoryContentItemTypeFromProcessor(FactoryContentProcessor<? extends FactoryContentItem> processor) {
    return (Class<?>) ((java.lang.reflect.ParameterizedType) processor.getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
  }


  public FactoryContentItem parseWorkspaceContentItem(Node node) {
    if (node == null) {
      return null;
    }
    FactoryContentProcessor<? extends FactoryContentItem> parser = parserTypes.get(node.getNodeName());
    if (parser == null) {
      return null;
    }
    FactoryContentItem result = parser.parseItem(node);
    return result;
  }


  public List<FactoryContentItem> createItems() {
     // TODO
    List<? extends FactoryXmlIgnoreEntry> ignoreEntryList = FactoryObjectManagement.listFactoryXmlIgnoreEntries();
    
    List<FactoryContentItem> result = new LinkedList<FactoryContentItem>();
    for (FactoryContentProcessor<? extends FactoryContentItem> supportedType : registeredTypes.values()) {
      List<? extends FactoryContentItem> subList = supportedType.createItems();
      
//      List<IgnorePatternInterface<?>> ignorePatternList  =  supportedType.getIgnorePatterns();
      
      // Liste durchlaufen mit dem Start am Ende
      for(int i=subList.size()-1; i>=0;  i--) {
        FactoryContentItem item = subList.get(i);
        
 //       for(IgnorePatternInterface<?> pattern : ignorePatternList) {
           // valide aufrufen
           // if valide == true
           //   ignore aufrufen 
           //   if ignore == true
           //     subList.remove(i);
//        }
        
      }
      
      result.addAll(subList);
    }

    return result;
  }


  public void writeItem(XmlBuilder builder, FactoryContentItem item) {
    writeItemInternal(builder, item);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void writeItemInternal(XmlBuilder builder, FactoryContentItem item) {
    if (item == null) {
      return;
    }

    FactoryContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    processor.writeItem(builder, item);
  }


  public void createItem(FactoryContentItem item) {
    createItemInternal(item);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void createItemInternal(FactoryContentItem item) {
    FactoryContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    processor.create(item);
  }


  public void deleteItem(FactoryContentItem item) {
    deleteItemInternal(item);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void deleteItemInternal(FactoryContentItem item) {
    FactoryContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    processor.delete(item);
  }


  public void modifyItem(FactoryContentItem from, FactoryContentItem to) {
    modifyItemInternal(from, to);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private void modifyItemInternal(FactoryContentItem from, FactoryContentItem to) {
    FactoryContentProcessor processor = registeredTypes.get(from.getClass());
    checkProcessor(processor, from.getClass());
    processor.modify(from, to);
  }


  public String getTagName(Class<? extends FactoryContentItem> workspaceContentItem) {
    return getTagNameInternal(workspaceContentItem);
  }


  @SuppressWarnings({"rawtypes"})
  public String getTagNameInternal(Class<? extends FactoryContentItem> workspaceContentItem) {
    FactoryContentProcessor processor = registeredTypes.get(workspaceContentItem);
    checkProcessor(processor, workspaceContentItem);
    return processor.getTagName();
  }


  public String createItemKeyString(FactoryContentItem item) {
    return createItemKeyStringInternal(item);
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private String createItemKeyStringInternal(FactoryContentItem item) {
    if (item == null) {
      return null;
    }
    FactoryContentProcessor processor = registeredTypes.get(item.getClass());
    checkProcessor(processor, item.getClass());
    return processor.createItemKeyString(item);
  }


  public String createDifferenceString(FactoryContentDifference diff) {
    return createDifferenceStringInternal(diff);
  }


  public List<FactoryXmlEntryType> listFactoryXmlEntrytypes() {
    List<FactoryXmlEntryType> resultList = new ArrayList<FactoryXmlEntryType>();
    for (FactoryContentProcessor<? extends FactoryContentItem> processor : parserTypes.values()) {
      FactoryXmlEntryType type = new FactoryXmlEntryType();
      type.setName(processor.getTagName());
      for (IgnorePatternInterface<? extends FactoryContentItem> ignorePattern : processor.getIgnorePatterns()) {
        List<String> ignoreEntryList = new ArrayList<String>();
        ignoreEntryList.add(ignorePattern.getPattern());
      }
      resultList.add(type);
    }
    return resultList;
  }


  public List<FactoryXmlIgnoreEntry> listInvalidateFactoryXmlIgnoreEntries() {
    List<FactoryXmlIgnoreEntry> resultList = new ArrayList<>();
    List<? extends FactoryXmlIgnoreEntry> entryList = FactoryObjectManagement.listFactoryXmlIgnoreEntries();
    for (FactoryXmlIgnoreEntry entry : entryList) {
      FactoryContentProcessor<? extends FactoryContentItem> processor = parserTypes.get(entry.getConfigType());

      boolean validPatternFound = false;
      for (IgnorePatternInterface<? extends FactoryContentItem> ignorePattern : processor.getIgnorePatterns()) {
        if (ignorePattern.validate(entry.getValue())) {
          validPatternFound = true;
          break;
        }
      }
      if (!validPatternFound) {
        FactoryXmlIgnoreEntry factoryXmlIgnoreEntry = new FactoryXmlIgnoreEntry();
        factoryXmlIgnoreEntry.setConfigType(entry.getConfigType());
        factoryXmlIgnoreEntry.setValue(entry.getValue());
        resultList.add(entry);
      }
    }
    return resultList;
  }


  @SuppressWarnings({"unchecked", "rawtypes"})
  private String createDifferenceStringInternal(FactoryContentDifference diff) {
    FactoryContentProcessor processor = registeredTypes.get(diff.getExistingItem().getClass());
    checkProcessor(processor, diff.getExistingItem().getClass());
    return processor.createDifferencesString(diff.getExistingItem(), diff.getNewItem());
  }


  public String resolve(ResolveFactoryDifferencesParameter param) {
    FactoryDifferenceListStorage storage = new FactoryDifferenceListStorage();
    if (param.getClose()) {
      storage.deleteWorkspaceDifferenceList(param.getFactoryDifferenceListId());
      return "Factory Difference List with id " + param.getFactoryDifferenceListId() + " closed.";
    }

    StringBuilder sb = new StringBuilder();
    FactoryContentDifferences differences = storage.loadDifferences(param.getFactoryDifferenceListId());
    if (param.getEntry().isPresent()) {
      resolveSingleDifference(param, differences, sb);
    } else {
      resolveAllDifferences(param, differences, sb);
    }

    if (differences.getDifferences().size() > 0) {
      storage.persist(differences);
      sb.append("There are " + differences.getDifferences().size() + " differences left in list " + differences.getListId());
    } else {
      //all done. Remove list
      storage.deleteWorkspaceDifferenceList(differences.getListId());
      sb.append("All differences in list " + differences.getListId() + " have been resolved. List closed");
    }

    return sb.toString();
  }


  private boolean tryResolveItem(FactoryContentDifference entry, Optional<String> resolution, StringBuilder sb) {
    try {
      resolveItem(entry, resolution);
      return true;
    } catch (Exception e) {
      //if there is an exception, do not remove the entry
      sb.append("Exception occurred while resolving item " + entry.getId() + ". It remains in the list. Details: " + e.getMessage());
      sb.append("\n");
    }
    return false;
  }


  private void resolveAllDifferences(ResolveFactoryDifferencesParameter param, FactoryContentDifferences differences, StringBuilder sb) {
    List<? extends FactoryContentDifference> differenceList = differences.getDifferences();
    for (int i = differenceList.size() - 1; i >= 0; i--) {
      FactoryContentDifference entry = differenceList.get(i);
      boolean success = tryResolveItem(entry, param.getResolution(), sb);
      if (success) {
        differenceList.remove(entry);
      }
    }
  }


  private void resolveSingleDifference(ResolveFactoryDifferencesParameter param, FactoryContentDifferences differences, StringBuilder sb) {
    long entryId = param.getEntry().get();
    Optional<? extends FactoryContentDifference> entryOptional = findEntry(differences, entryId);
    if (entryOptional.isEmpty()) {
      long id = differences.getListId();
      throw new RuntimeException("No entry " + entryId + " found in workspace difference list " + id);
    }
    FactoryContentDifference entry = entryOptional.get();
    boolean success = tryResolveItem(entry, param.getResolution(), sb);
    if (success) {
      differences.getDifferences().remove(entry);
    }
  }


  private Optional<? extends FactoryContentDifference> findEntry(FactoryContentDifferences diffs, long entryId) {
    return diffs.getDifferences().stream().filter(x -> x.getId() == entryId).findFirst();
  }


  private void resolveItem(FactoryContentDifference difference, Optional<String> resolution) {
    String actualResolution = difference.getDifferenceType().getClass().getSimpleName();
    if (!resolution.isEmpty()) {
      actualResolution = determineActualResolution(actualResolution, resolution.get());
    }

    if (actualResolution.equals("")) {
      return;
    } else if (actualResolution.equals(CREATE.class.getSimpleName())) {
      createItem(difference.getNewItem());
      return;
    } else if (actualResolution.equals(MODIFY.class.getSimpleName())) {
      modifyItem(difference.getExistingItem(), difference.getNewItem());
      return;
    } else if (actualResolution.equals(DELETE.class.getSimpleName())) {
      deleteItem(difference.getExistingItem());
      return;
    }

    throw new RuntimeException("Unexpected resolution: \"" + actualResolution + "\".");

  }


  public FactoryContentItem parseFactoryContentItem(Node node) {
    if (node == null) {
      return null;
    }
    FactoryContentProcessor<? extends FactoryContentItem> parser = parserTypes.get(node.getNodeName());
    if (parser == null) {
      return null;
    }
    FactoryContentItem result = parser.parseItem(node);
    return result;
  }


  private String determineActualResolution(String suggested, String desired) {
    if (suggested.equals(desired)) {
      return suggested;
    }

    if (suggested.equals(CREATE.class.getSimpleName()) && desired.equals(MODIFY.class.getSimpleName())) {
      return suggested;
    }

    return "";
  }


  private void checkProcessor(FactoryContentProcessor<?> processor, Class<?> clazz) {
    checkProcessor(processor, clazz.getSimpleName());
  }


  private void checkProcessor(FactoryContentProcessor<?> processor, String type) {
    if (processor == null) {
      throw new RuntimeException("Unknown Factory Content Type: " + type);
    }
  }


  public List<FactoryContentDifference> compare(FactoryContent c1, FactoryContent c2) {
    List<FactoryContentDifference> result = new LinkedList<>();
    Collection<FactoryContentProcessor<? extends FactoryContentItem>> types = registeredTypes.values();
    for (FactoryContentProcessor<? extends FactoryContentItem> type : types) {
      List<FactoryContentDifference> differencesThisType = compareSingleType(c1, c2, type);
      result.addAll(differencesThisType);
    }

    int id = 0;
    for (FactoryContentDifference difference : result) {
      difference.setId(id++);
    }

    return result;
  }


  @SuppressWarnings("unchecked")
  private <T extends FactoryContentItem> List<FactoryContentDifference> compareSingleType(FactoryContent c1, FactoryContent c2,
                                                                                          FactoryContentProcessor<T> processor) {
    Class<T> expectedClass = (Class<T>) getFactoryContentItemTypeFromProcessor(processor);
    List<FactoryContentDifference> result = new LinkedList<>();
    List<T> from = (List<T>) c1.getFactoryContentItems().stream().filter(x -> matchClass(x, expectedClass)).collect(Collectors.toList());
    List<T> to = (List<T>) c2.getFactoryContentItems().stream().filter(x -> matchClass(x, expectedClass)).collect(Collectors.toList());

    result.addAll(processor.compare(from, to));

    return result;
  }


  private boolean matchClass(Object obj, Class<?> clazz) {
    return obj.getClass().equals(clazz);
  }
}
