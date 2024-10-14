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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryXmlIgnoreEntry;
import xmcp.gitintegration.storage.FactoryXmlIgnoreEntryStorage;



public class FactoryXmlIgnoreEntryProcessor implements FactoryContentProcessor<FactoryXmlIgnoreEntry> {

  private static final String TAG_FACTORYXMLIGNOREENTRY = "factoryxmlignoreentry";
  private static final String TAG_CONFIGTYPENAME = "configtype";
  private static final String TAG_VALUE = "value";

  private static final List<IgnorePatternInterface<FactoryXmlIgnoreEntry>> ignorePatterns = createIgnorePatterns();

  private static List<IgnorePatternInterface<FactoryXmlIgnoreEntry>> createIgnorePatterns() {
    List<IgnorePatternInterface<FactoryXmlIgnoreEntry>> resultList = new ArrayList<>();
    return Collections.unmodifiableList(resultList);
  }
  
  @Override
  public List<FactoryXmlIgnoreEntry> createItems() {
    FactoryXmlIgnoreEntryStorage storage = new FactoryXmlIgnoreEntryStorage();
    return storage.listAllFactoryXmlIgnoreEntries();
  }


  @Override
  public void writeItem(XmlBuilder builder, FactoryXmlIgnoreEntry item) {
    builder.startElement(TAG_FACTORYXMLIGNOREENTRY);
    builder.element(TAG_CONFIGTYPENAME, item.getConfigType());
    builder.element(TAG_VALUE, item.getValue());
    builder.endElement(TAG_FACTORYXMLIGNOREENTRY);
  }


  @Override
  public String getTagName() {
    return TAG_FACTORYXMLIGNOREENTRY;
  }


  @Override
  public FactoryXmlIgnoreEntry parseItem(Node node) {
    FactoryXmlIgnoreEntry entry = new FactoryXmlIgnoreEntry();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_CONFIGTYPENAME)) {
        entry.setConfigType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_VALUE)) {
        entry.setValue(childNode.getTextContent());
      }
    }
    return entry;
  }


  @Override
  public List<FactoryContentDifference> compare(Collection<? extends FactoryXmlIgnoreEntry> from,
                                                Collection<? extends FactoryXmlIgnoreEntry> to) {
    List<FactoryContentDifference> fcdList = new ArrayList<FactoryContentDifference>();
    List<FactoryXmlIgnoreEntry> toWorkingList = new ArrayList<FactoryXmlIgnoreEntry>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, FactoryXmlIgnoreEntry> toMap = new HashMap<String, FactoryXmlIgnoreEntry>();
    for (FactoryXmlIgnoreEntry toEntry : toWorkingList) {
      toMap.put(toEntry.getConfigType() + ":" + toEntry.getValue(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (FactoryXmlIgnoreEntry fromEntry : from) {
        FactoryXmlIgnoreEntry toEntry = toMap.get(fromEntry.getConfigType() + ":" + fromEntry.getValue());

        FactoryContentDifference fcd = new FactoryContentDifference();
        fcd.setContentType(TAG_FACTORYXMLIGNOREENTRY);
        fcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          toWorkingList.remove(toEntry); // remove entry from to-list
          continue; // EQUAL -> ignore entry
        } else {
          fcd.setDifferenceType(new DELETE());
        }
        fcdList.add(fcd);
      }
    }
    // iterate over toWorking-list (only CREATE-Entries remain)
    for (FactoryXmlIgnoreEntry toEntry : toWorkingList) {
      FactoryContentDifference fcd = new FactoryContentDifference();
      fcd.setContentType(TAG_FACTORYXMLIGNOREENTRY);
      fcd.setNewItem(toEntry);
      fcd.setDifferenceType(new CREATE());
      fcdList.add(fcd);
    }
    return fcdList;
  }


  @Override
  public String createItemKeyString(FactoryXmlIgnoreEntry item) {
    return item.getConfigType() + ":" + item.getValue();
  }


  @Override
  public String createDifferencesString(FactoryXmlIgnoreEntry from, FactoryXmlIgnoreEntry to) {
    // Modify is not possible
    return "";
  }


  @Override
  public void create(FactoryXmlIgnoreEntry item) {
    FactoryXmlIgnoreEntryStorage storage = new FactoryXmlIgnoreEntryStorage();
    storage.addFactoryXmlIgnoreEntry(item.getConfigType(), item.getValue());
  }


  @Override
  public void modify(FactoryXmlIgnoreEntry from, FactoryXmlIgnoreEntry to) {
    FactoryXmlIgnoreEntryStorage storage = new FactoryXmlIgnoreEntryStorage();
    storage.removeFactoryXmlIgnoreEntry(from.getConfigType(), from.getValue());
    storage.addFactoryXmlIgnoreEntry(to.getConfigType(), to.getValue());
  }


  @Override
  public void delete(FactoryXmlIgnoreEntry item) {
    FactoryXmlIgnoreEntryStorage storage = new FactoryXmlIgnoreEntryStorage();
    storage.removeFactoryXmlIgnoreEntry(item.getConfigType(), item.getValue());
  }


  @Override
  public List<IgnorePatternInterface<FactoryXmlIgnoreEntry>> getIgnorePatterns() {
    return ignorePatterns;
  }

}
