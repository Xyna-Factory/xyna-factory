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

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xprc.XynaProcessingBase;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;
import com.gip.xyna.xprc.xsched.CapacityInformation;
import com.gip.xyna.xprc.xsched.CapacityManagement.State;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FactoryCapacity;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.MODIFY;



public class CapacityProcessor implements FactoryContentProcessor<FactoryCapacity> {

  private static final String TAG_CAPACITY = "capacity";
  private static final String TAG_NAME = "name";
  private static final String TAG_CARDINALITY = "cardinality";
  private static final String TAG_STATE = "state";

  private static XynaProcessingBase xynaProcessing;
  private static final List<IgnorePatternInterface<FactoryCapacity>> ignorePatterns = createIgnorePatterns();


  private static XynaProcessingBase getProcessing() {
    if (xynaProcessing == null) {
      xynaProcessing = XynaFactory.getInstance().getProcessing();
    }
    return xynaProcessing;
  }


  private static List<IgnorePatternInterface<FactoryCapacity>> createIgnorePatterns() {
    List<IgnorePatternInterface<FactoryCapacity>> resultList = new ArrayList<>();
    resultList.add(new NameIgnorePattern());
    return Collections.unmodifiableList(resultList);
  }


  @Override
  public List<FactoryCapacity> createItems() {
    List<FactoryCapacity> capList = new ArrayList<FactoryCapacity>();
    Collection<CapacityInformation> capInfoList = getProcessing().listCapacityInformation();
    for (CapacityInformation capInfo : capInfoList) {
      FactoryCapacity cap = new FactoryCapacity();
      cap.setCapacityName(capInfo.getName());
      cap.setCardinality(capInfo.getCardinality());
      cap.setState(capInfo.getState().toString());
      capList.add(cap);
    }
    return capList;
  }


  @Override
  public void writeItem(XmlBuilder builder, FactoryCapacity item) {
    builder.startElement(TAG_CAPACITY);
    builder.element(TAG_NAME, item.getCapacityName());
    builder.element(TAG_CARDINALITY, Integer.toString(item.getCardinality()));
    builder.element(TAG_STATE, item.getState());
    builder.endElement(TAG_CAPACITY);
  }


  @Override
  public String getTagName() {
    return TAG_CAPACITY;
  }


  @Override
  public FactoryCapacity parseItem(Node node) {
    FactoryCapacity capacity = new FactoryCapacity();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_NAME)) {
        capacity.setCapacityName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_CARDINALITY)) {
        capacity.setCardinality(Integer.parseInt(childNode.getTextContent()));
      } else if (childNode.getNodeName().equals(TAG_STATE)) {
        capacity.setState(childNode.getTextContent());
      }
    }
    return capacity;
  }


  @Override
  public List<FactoryContentDifference> compare(Collection<? extends FactoryCapacity> from, Collection<? extends FactoryCapacity> to) {
    List<FactoryContentDifference> fcdList = new ArrayList<FactoryContentDifference>();
    List<FactoryCapacity> toWorkingList = new ArrayList<FactoryCapacity>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, FactoryCapacity> toMap = new HashMap<String, FactoryCapacity>();
    for (FactoryCapacity toEntry : toWorkingList) {
      toMap.put(toEntry.getCapacityName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (FactoryCapacity fromEntry : from) {
        FactoryCapacity toEntry = toMap.get(fromEntry.getCapacityName());

        FactoryContentDifference fcd = new FactoryContentDifference();
        fcd.setContentType(TAG_CAPACITY);
        fcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          boolean sameCardinality = fromEntry.getCardinality() == toEntry.getCardinality();
          boolean sameState = fromEntry.getState().equals(toEntry.getState());
          if (!sameCardinality || !sameState) {
            fcd.setDifferenceType(new MODIFY());
            fcd.setNewItem(toEntry);
            toWorkingList.remove(toEntry); // remove entry from to-list
          } else {
            toWorkingList.remove(toEntry); // remove entry from to-list
            continue; // EQUAL -> ignore entry
          }
        } else {
          fcd.setDifferenceType(new DELETE());
        }
        fcdList.add(fcd);
      }
    }
    // iterate over toWorking-list (only CREATE-Entries remain)
    for (FactoryCapacity toEntry : toWorkingList) {
      FactoryContentDifference fcd = new FactoryContentDifference();
      fcd.setContentType(TAG_CAPACITY);
      fcd.setNewItem(toEntry);
      fcd.setDifferenceType(new CREATE());
      fcdList.add(fcd);
    }
    return fcdList;
  }


  @Override
  public String createItemKeyString(FactoryCapacity item) {
    return item.getCapacityName();
  }


  @Override
  public String createDifferencesString(FactoryCapacity from, FactoryCapacity to) {
    StringBuffer ds = new StringBuffer();
    if (from.getCardinality() != to.getCardinality()) {
      ds.append("\n");
      ds.append("    " + TAG_CARDINALITY + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getCardinality() + "\"=>\"" + to.getCardinality() + "\"");
    }
    if (from.getState() != to.getState()) {
      ds.append("\n");
      ds.append("    " + TAG_STATE + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getState() + "\"=>\"" + to.getState() + "\"");

    }
    return ds.toString();
  }


  @Override
  public void create(FactoryCapacity item) {
    try {
      getProcessing().addCapacity(item.getCapacityName(), item.getCardinality(), State.valueOf(item.getState()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void modify(FactoryCapacity from, FactoryCapacity to) {
    try {
      if (from.getCardinality() != to.getCardinality()) {
        getProcessing().changeCapacityCardinality(from.getCapacityName(), to.getCardinality());
      }
      if (!from.getState().equals(to.getState())) {
        getProcessing().changeCapacityState(from.getCapacityName(), State.valueOf(to.getState()));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void delete(FactoryCapacity item) {
    try {
      getProcessing().removeCapacity(item.getCapacityName());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<IgnorePatternInterface<FactoryCapacity>> getIgnorePatterns() {
    return ignorePatterns;
  }


  public static final class NameIgnorePattern extends RegexIgnorePattern<FactoryCapacity> {

    public NameIgnorePattern() {
      super("name");
    }

    @Override
    public boolean ignore(FactoryCapacity item, String value) {
      return item.getCapacityName().matches(getRegexPart(value));
    }
  }


}
