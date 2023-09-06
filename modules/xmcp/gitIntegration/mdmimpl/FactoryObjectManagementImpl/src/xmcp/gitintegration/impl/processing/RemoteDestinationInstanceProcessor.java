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
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.RemoteDestinationInstanceInformation;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryRemoteDestinationInstance;
import xmcp.gitintegration.MODIFY;
import xmcp.gitintegration.Parameter;



public class RemoteDestinationInstanceProcessor implements FactoryContentProcessor<FactoryRemoteDestinationInstance> {

  private static final String TAG_REMOTEDESTINATIONINSTANCE = "remotedestinationinstance";
  private static final String TAG_NAME = "name";
  private static final String TAG_DESCRIPTION = "description";
  private static final String TAG_TYPENAME = "typename";
  private static final String TAG_EXECUTIONTIMEOUT = "executiontimeout";
  private static final String TAG_PARAMETERS = "parameters";
  private static final String TAG_PARAMETER = "parameter";
  private static final String TAG_KEY = "key";
  private static final String TAG_VALUE = "value";

  private static final List<IgnorePatternInterface<FactoryRemoteDestinationInstance>> ignorePatterns = createIgnorePatterns();


  private static List<IgnorePatternInterface<FactoryRemoteDestinationInstance>> createIgnorePatterns() {
    List<IgnorePatternInterface<FactoryRemoteDestinationInstance>> resultList = new ArrayList<>();
    resultList.add(new NameIgnorePattern());
    return Collections.unmodifiableList(resultList);
  }


  @Override
  public List<FactoryRemoteDestinationInstance> createItems() {
    List<FactoryRemoteDestinationInstance> instanceList = new ArrayList<FactoryRemoteDestinationInstance>();
    Collection<RemoteDestinationInstanceInformation> infoList = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
        .getRemoteDestinationManagement().listRemoteDestinationInstances();
    if (infoList != null) {
      for (RemoteDestinationInstanceInformation infoEntry : infoList) {
        FactoryRemoteDestinationInstance instance = new FactoryRemoteDestinationInstance();
        instance.setName(infoEntry.getName());
        instance.setDescription(infoEntry.getDescription());
        instance.setDestinationTypeName(infoEntry.getTypename());
        if (infoEntry.getExecutionTimeout() != null) {
          instance.setExecutionTimeout(infoEntry.getExecutionTimeout().serializeToString());
        }
        Map<String, String> startParameterMap = infoEntry.getStartparameter();
        List<Parameter> parameters = new ArrayList<Parameter>();
        if ((startParameterMap != null) && (startParameterMap.keySet() != null)) {
          // Sortieren nach Key
          SortedMap<String, String> sortedMap = new TreeMap<String, String>();
          for (String key : startParameterMap.keySet()) {
            sortedMap.put(key, startParameterMap.get(key));
          }
          for (String key : sortedMap.keySet()) {
            Parameter param = new Parameter();
            param.setKey(key);
            param.setValue(sortedMap.get(key));
            parameters.add(param);
          }
        }
        instance.setParameters(parameters);
        instanceList.add(instance);
      }
    }
    return instanceList;
  }


  @Override
  public void writeItem(XmlBuilder builder, FactoryRemoteDestinationInstance item) {
    builder.startElement(TAG_REMOTEDESTINATIONINSTANCE);
    builder.element(TAG_NAME, item.getName());
    if (item.getDescription() != null) {
      builder.element(TAG_DESCRIPTION, XmlBuilder.encode(item.getDescription()));
    }
    if (item.getDestinationTypeName() != null) {
      builder.element(TAG_TYPENAME, item.getDestinationTypeName());
    }
    if (item.getExecutionTimeout() != null) {
      builder.element(TAG_EXECUTIONTIMEOUT, item.getExecutionTimeout());
    }
    if ((item.getParameters() != null) && (item.getParameters().size() > 0)) {
      builder.startElement(TAG_PARAMETERS);
      for (Parameter param : item.getParameters()) {
        builder.startElement(TAG_PARAMETER);
        builder.element(TAG_KEY, param.getKey());
        builder.element(TAG_VALUE, XmlBuilder.encode(param.getValue()));
        builder.endElement(TAG_PARAMETER);
      }
      builder.endElement(TAG_PARAMETERS);
    }
    builder.endElement(TAG_REMOTEDESTINATIONINSTANCE);
  }


  @Override
  public String getTagName() {
    return TAG_REMOTEDESTINATIONINSTANCE;
  }


  @Override
  public FactoryRemoteDestinationInstance parseItem(Node node) {
    FactoryRemoteDestinationInstance instance = new FactoryRemoteDestinationInstance();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_NAME)) {
        instance.setName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DESCRIPTION)) {
        instance.setDescription(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_TYPENAME)) {
        instance.setDestinationTypeName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_EXECUTIONTIMEOUT)) {
        instance.setExecutionTimeout(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_PARAMETERS)) {
        instance.setParameters(parseParameters(childNode));
      }
    }
    return instance;
  }


  public Parameter parseParameter(Node node) {
    Parameter param = new Parameter();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_KEY)) {
        param.setKey(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_VALUE)) {
        param.setValue(childNode.getTextContent());
      }
    }
    return param;
  }


  public List<Parameter> parseParameters(Node node) {
    List<Parameter> locList = new ArrayList<Parameter>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_PARAMETER)) {
        locList.add(parseParameter(childNode));
      }
    }
    return locList;
  }


  @Override
  public List<FactoryContentDifference> compare(Collection<? extends FactoryRemoteDestinationInstance> from,
                                                Collection<? extends FactoryRemoteDestinationInstance> to) {
    List<FactoryContentDifference> fcdList = new ArrayList<FactoryContentDifference>();
    List<FactoryRemoteDestinationInstance> toWorkingList = new ArrayList<FactoryRemoteDestinationInstance>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, FactoryRemoteDestinationInstance> toMap = new HashMap<String, FactoryRemoteDestinationInstance>();
    for (FactoryRemoteDestinationInstance toEntry : toWorkingList) {
      toMap.put(toEntry.getName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (FactoryRemoteDestinationInstance fromEntry : from) {
        FactoryRemoteDestinationInstance toEntry = toMap.get(fromEntry.getName());

        FactoryContentDifference fcd = new FactoryContentDifference();
        fcd.setContentType(TAG_REMOTEDESTINATIONINSTANCE);
        fcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (!Objects.equals(fromEntry.getDescription(), toEntry.getDescription())
              || !Objects.equals(fromEntry.getDestinationTypeName(), toEntry.getDestinationTypeName())
              || !Objects.equals(fromEntry.getExecutionTimeout(), toEntry.getExecutionTimeout())
              || !Objects.equals(paramtersToString(fromEntry.getParameters()), paramtersToString(toEntry.getParameters()))) {
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
    for (FactoryRemoteDestinationInstance toEntry : toWorkingList) {
      FactoryContentDifference fcd = new FactoryContentDifference();
      fcd.setContentType(TAG_REMOTEDESTINATIONINSTANCE);
      fcd.setNewItem(toEntry);
      fcd.setDifferenceType(new CREATE());
      fcdList.add(fcd);
    }
    return fcdList;
  }


  @Override
  public String createItemKeyString(FactoryRemoteDestinationInstance item) {
    return item.getName();
  }


  @Override
  public String createDifferencesString(FactoryRemoteDestinationInstance from, FactoryRemoteDestinationInstance to) {
    StringBuffer ds = new StringBuffer();
    if (!Objects.equals(from.getDescription(), to.getDescription())) {
      ds.append("\n");
      ds.append("    " + TAG_DESCRIPTION + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getDescription() + "\"=>\"" + to.getDescription() + "\"");
    }
    if (!Objects.equals(from.getDestinationTypeName(), to.getDestinationTypeName())) {
      ds.append("\n");
      ds.append("    " + TAG_TYPENAME + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getDestinationTypeName() + "\"=>\"" + to.getDestinationTypeName() + "\"");
    }
    if (!Objects.equals(from.getExecutionTimeout(), to.getExecutionTimeout())) {
      ds.append("\n");
      ds.append("    " + TAG_EXECUTIONTIMEOUT + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getExecutionTimeout() + "\"=>\"" + to.getExecutionTimeout() + "\"");
    }
    if (!Objects.equals(paramtersToString(from.getParameters()), paramtersToString(to.getParameters()))) {
      ds.append("\n");
      ds.append("    " + TAG_PARAMETERS + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + paramtersToString(from.getParameters()) + "\"=>\""
          + paramtersToString(to.getParameters()) + "\"");
    }
    return ds.toString();
  }


  private String paramtersToString(List<? extends Parameter> parameters) {
    if (parameters == null) {
      return "";
    }
    SortedMap<String, String> sortedMap = new TreeMap<String, String>();
    for (Parameter parameter : parameters) {
      sortedMap.put(parameter.getKey(), parameter.getValue());
    }
    List<String> ds = new ArrayList<>();
    for (String key : sortedMap.keySet()) {
      ds.add(key + ":" + sortedMap.get(key));
    }
    return String.join(";", ds);
  }


  @Override
  public void create(FactoryRemoteDestinationInstance item) {
    // TODO
  }


  @Override
  public void modify(FactoryRemoteDestinationInstance from, FactoryRemoteDestinationInstance to) {
    // TODO
  }


  @Override
  public void delete(FactoryRemoteDestinationInstance item) {
    // TODO
  }


  @Override
  public List<IgnorePatternInterface<FactoryRemoteDestinationInstance>> getIgnorePatterns() {
    return ignorePatterns;
  }


  public static final class NameIgnorePattern extends RegexIgnorePattern<FactoryRemoteDestinationInstance> {

    public NameIgnorePattern() {
      super("name");
    }


    @Override
    public boolean ignore(FactoryRemoteDestinationInstance item, String value) {
      return item.getName().matches(getRegexPart(value));
    }
  }


}
