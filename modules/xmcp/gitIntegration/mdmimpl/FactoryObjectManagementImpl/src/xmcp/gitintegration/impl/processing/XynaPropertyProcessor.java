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
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FactoryXynaProperty;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryDocumentation;
import xmcp.gitintegration.MODIFY;



public class XynaPropertyProcessor implements FactoryContentProcessor<FactoryXynaProperty> {

  private static final String TAG_XYNAPROPERTY = "xynaproperty";
  private static final String TAG_KEY = "key";
  private static final String TAG_VALUE = "value";
  private static final String TAG_DEFAULTVALUE = "defaultvalue";
  private static final String TAG_DOCUMENTATIONS = "documentations";
  private static final String TAG_DOCUMENTATION = "documentation";
  private static final String TAG_LANG = "lang";
  private static final String TAG_TEXT = "text";

  private static final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal();


  @Override
  public List<FactoryXynaProperty> createItems() {
    Collection<XynaPropertyWithDefaultValue> data = multiChannelPortal.getPropertiesWithDefaultValuesReadOnly();
    List<FactoryXynaProperty> result = data.stream()
      .map(property -> {
        FactoryXynaProperty fp = new FactoryXynaProperty();
        fp.setKey(property.getName());
        fp.setValue(property.getValueOrDefValue());
        fp.setDefaultValue(property.getDefValue());
        fp.setDocumentations(
          property.getDocumentation().entrySet().stream()
            .sorted((entryA, entryB) -> entryA.getKey().name().compareTo(entryB.getKey().name()))
            .map(entry -> {
              FactoryDocumentation fd = new FactoryDocumentation();
              fd.setLang(entry.getKey().name());
              fd.setText(entry.getValue());
              return fd;
            })
            .collect(Collectors.toList())
        );
        return fp;
      })
      .collect(Collectors.toList());
    return result;
  }


  public void writeDocumentationItem(XmlBuilder builder, FactoryDocumentation item) {
    builder.startElement(TAG_DOCUMENTATION);
    builder.element(TAG_LANG, item.getLang());
    builder.element(TAG_TEXT, XmlBuilder.encode(item.getText()));
    builder.endElement(TAG_DOCUMENTATION);
  }


  @Override
  public void writeItem(XmlBuilder builder, FactoryXynaProperty item) {
    builder.startElement(TAG_XYNAPROPERTY);
    builder.element(TAG_KEY, XmlBuilder.encode(item.getKey()));
    if (item.getValue() != null) {
      builder.element(TAG_VALUE, XmlBuilder.encode(item.getValue()));
    }
    if (item.getDefaultValue() != null) {
      builder.element(TAG_DEFAULTVALUE, XmlBuilder.encode(item.getDefaultValue()));
    }
    builder.startElement(TAG_DOCUMENTATIONS);
    for (FactoryDocumentation fd : item.getDocumentations()) {
      writeDocumentationItem(builder, fd);
    }
    builder.endElement(TAG_DOCUMENTATIONS);
    builder.endElement(TAG_XYNAPROPERTY);
  }


  @Override
  public String getTagName() {
    return TAG_XYNAPROPERTY;
  }


  public FactoryDocumentation parseDocumentationItem(Node node) {
    FactoryDocumentation fd = new FactoryDocumentation();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_LANG)) {
        fd.setLang(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_TEXT)) {
        fd.setText(childNode.getTextContent());
      }
    }
    return fd;
  }


  public List<FactoryDocumentation> parseDocumentationsItem(Node node) {
    List<FactoryDocumentation> fds = new ArrayList<>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_DOCUMENTATION)) {
        fds.add(parseDocumentationItem(childNode));
      }
    }
    return fds;
  }


  @Override
  public FactoryXynaProperty parseItem(Node node) {
    FactoryXynaProperty fp = new FactoryXynaProperty();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_KEY)) {
        fp.setKey(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_VALUE)) {
        fp.setValue(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DEFAULTVALUE)) {
        fp.setDefaultValue(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_DOCUMENTATIONS)) {
        fp.setDocumentations(parseDocumentationsItem(childNode));
      }
    }
    return fp;
  }


  private boolean stringIsDifferent(String from, String to) {
    if (from == null) {
      from = "";
    }
    if (to == null) {
      to = "";
    }
    return !from.equals(to);
  }


  private String documentationsToString(List<? extends FactoryDocumentation> documentations) {
    if (documentations == null) {
      return "";
    }
    List<String> ds = new ArrayList<>();
    for (FactoryDocumentation documentation : documentations) {
      ds.add(documentation.getLang() + ":" + documentation.getText());
    }
    return String.join(";", ds);
  }


  @Override
  public List<FactoryContentDifference> compare(Collection<? extends FactoryXynaProperty> from, Collection<? extends FactoryXynaProperty> to) {
    List<FactoryContentDifference> fcdList = new ArrayList<FactoryContentDifference>();
    List<FactoryXynaProperty> toWorkingList = new ArrayList<FactoryXynaProperty>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, FactoryXynaProperty> toMap = new HashMap<String, FactoryXynaProperty>();
    for (FactoryXynaProperty toEntry : toWorkingList) {
      toMap.put(toEntry.getKey(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (FactoryXynaProperty fromEntry : from) {
        FactoryXynaProperty toEntry = toMap.get(fromEntry.getKey());

        FactoryContentDifference fcd = new FactoryContentDifference();
        fcd.setContentType(TAG_XYNAPROPERTY);
        fcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (
            stringIsDifferent(fromEntry.getValue(), toEntry.getValue()) ||
            stringIsDifferent(fromEntry.getDefaultValue(), toEntry.getDefaultValue()) ||
            stringIsDifferent(documentationsToString(fromEntry.getDocumentations()), documentationsToString(toEntry.getDocumentations()))
          ) {
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
    for (FactoryXynaProperty toEntry : toWorkingList) {
      FactoryContentDifference fcd = new FactoryContentDifference();
      fcd.setContentType(TAG_XYNAPROPERTY);
      fcd.setNewItem(toEntry);
      fcd.setDifferenceType(new CREATE());
      fcdList.add(fcd);
    }
    return fcdList;
  }


  @Override
  public String createItemKeyString(FactoryXynaProperty item) {
    return item.getKey();
  }


  @Override
  public String createDifferencesString(FactoryXynaProperty from, FactoryXynaProperty to) {
    StringBuffer ds = new StringBuffer();
    if (stringIsDifferent(from.getValue(), to.getValue())) {
      ds.append("\n");
      ds.append("    " + TAG_VALUE + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getValue() + "\"=>\"" + to.getValue() + "\"");
    }
    if (stringIsDifferent(from.getDefaultValue(), to.getDefaultValue())) {
      ds.append("\n");
      ds.append("    " + TAG_DEFAULTVALUE + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getDefaultValue() + "\"=>\"" + to.getDefaultValue() + "\"");
    }
    if (stringIsDifferent(documentationsToString(from.getDocumentations()), documentationsToString(to.getDocumentations()))) {
      ds.append("\n");
      ds.append("    " + TAG_DOCUMENTATIONS + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + documentationsToString(from.getDocumentations()) + "\"=>\"" + documentationsToString(to.getDocumentations()) + "\"");
    }
    return ds.toString();
  }


  @Override
  public void create(FactoryXynaProperty item) {
  }


  @Override
  public void modify(FactoryXynaProperty from, FactoryXynaProperty to) {
  }


  @Override
  public void delete(FactoryXynaProperty item) {
  }

}
