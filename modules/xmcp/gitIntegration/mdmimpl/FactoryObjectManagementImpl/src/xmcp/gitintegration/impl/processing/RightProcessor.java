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
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.CREATE;
import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryRight;
import xmcp.gitintegration.Localization;
import xmcp.gitintegration.MODIFY;



public class RightProcessor implements FactoryContentProcessor<FactoryRight> {

  private static final String TAG_RIGHT = "right";
  private static final String TAG_NAME = "name";
  private static final String TAG_PARAMETER = "parameter";
  private static final String TAG_TYPE = "type";
  private static final String TAG_LOCALIZATIONS = "localizations";
  private static final String TAG_LOCALIZATION = "localization";
  private static final String TAG_IDENTIFIER = "identifier";
  private static final String TAG_LANGUAGE = "language";
  private static final String TAG_TEXT = "text";

  private static final List<IgnorePatternInterface<FactoryRight>> ignorePatterns = createIgnorePatterns();


  private static List<IgnorePatternInterface<FactoryRight>> createIgnorePatterns() {
    List<IgnorePatternInterface<FactoryRight>> resultList = new ArrayList<>();
    resultList.add(new NameIgnorePattern());
    return Collections.unmodifiableList(resultList);
  }


  @Override
  public List<FactoryRight> createItems() {
    List<FactoryRight> rightList = new ArrayList<FactoryRight>();
    try {
      Collection<RightScope> rightScopeList =
          XynaFactory.getInstance().getFactoryManagement().getRightScopes(DocumentationLanguage.EN.toString());
      if (rightScopeList != null) {
        for (RightScope rightScope : rightScopeList) {
          FactoryRight right = new FactoryRight();
          right.setName(rightScope.getName());
          try {
            // Extract parameter after the first :
            String parameter = rightScope.getDefinition().substring(rightScope.getDefinition().indexOf(":") + 1);
            if (parameter != null) {
              right.setParameter(parameter);
            }
          } catch (Exception e) {
          }
          right.setLocalizations(createLocalizationList(rightScope.getName()));
          rightList.add(right);
        }
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return rightList;
  }


  private RightScope getRightScope(String language, String rightName) {
    RightScope rightScope = null;
    try {
      Collection<RightScope> rightScopeList = XynaFactory.getInstance().getFactoryManagement().getRightScopes(language);
      if (rightScopeList != null) {
        for (RightScope entry : rightScopeList) {
          if (rightName.equals(entry.getName())) {
            rightScope = entry;
          }
        }
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    }
    return rightScope;
  }


  private List<Localization> createLocalizationList(String rightName) {
    List<Localization> locationList = new ArrayList<Localization>();
    for (DocumentationLanguage docLang : DocumentationLanguage.values()) {
      RightScope rigthScope = getRightScope(docLang.toString(), rightName);
      if (rigthScope != null) {
        Localization loc = new Localization();
        loc.setType("RIGHT");
        loc.setIdentifier(rightName);
        loc.setLanguage(docLang.toString());
        loc.setText(rigthScope.getDocumentation());
        locationList.add(loc);
      }
    }
    return locationList;
  }


  @Override
  public void writeItem(XmlBuilder builder, FactoryRight item) {
    builder.startElement(TAG_RIGHT);
    builder.element(TAG_NAME, item.getName());
    if (item.getParameter() != null) {
      builder.element(TAG_PARAMETER, XmlBuilder.encode(item.getParameter()));
    }
    if ((item.getLocalizations() != null) && (item.getLocalizations().size() > 0)) {
      builder.startElement(TAG_LOCALIZATIONS);
      for (Localization loc : item.getLocalizations()) {
        builder.startElement(TAG_LOCALIZATION);
        builder.element(TAG_TYPE, loc.getType());
        builder.element(TAG_IDENTIFIER, loc.getIdentifier());
        builder.element(TAG_LANGUAGE, loc.getLanguage());
        builder.element(TAG_TEXT, XmlBuilder.encode(loc.getText()));
        builder.endElement(TAG_LOCALIZATION);
      }
      builder.endElement(TAG_LOCALIZATIONS);
    }
    builder.endElement(TAG_RIGHT);
  }


  @Override
  public String getTagName() {
    return TAG_RIGHT;
  }


  @Override
  public FactoryRight parseItem(Node node) {
    FactoryRight right = new FactoryRight();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_NAME)) {
        right.setName(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_PARAMETER)) {
        right.setParameter(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_LOCALIZATIONS)) {
        right.setLocalizations(parseLocalizations(childNode));
      }
    }
    return right;
  }


  public Localization parseLocalization(Node node) {
    Localization loc = new Localization();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_TYPE)) {
        loc.setType(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_IDENTIFIER)) {
        loc.setIdentifier(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_LANGUAGE)) {
        loc.setLanguage(childNode.getTextContent());
      } else if (childNode.getNodeName().equals(TAG_TEXT)) {
        loc.setText(childNode.getTextContent());
      }
    }
    return loc;
  }


  public List<Localization> parseLocalizations(Node node) {
    List<Localization> locList = new ArrayList<Localization>();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_LOCALIZATION)) {
        locList.add(parseLocalization(childNode));
      }
    }
    return locList;
  }


  @Override
  public List<FactoryContentDifference> compare(Collection<? extends FactoryRight> from, Collection<? extends FactoryRight> to) {
    List<FactoryContentDifference> fcdList = new ArrayList<FactoryContentDifference>();
    List<FactoryRight> toWorkingList = new ArrayList<FactoryRight>();
    if (to != null) {
      toWorkingList.addAll(to);
    }
    HashMap<String, FactoryRight> toMap = new HashMap<String, FactoryRight>();
    for (FactoryRight toEntry : toWorkingList) {
      toMap.put(toEntry.getName(), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (FactoryRight fromEntry : from) {
        FactoryRight toEntry = toMap.get(fromEntry.getName());

        FactoryContentDifference fcd = new FactoryContentDifference();
        fcd.setContentType(TAG_RIGHT);
        fcd.setExistingItem(fromEntry);
        if (toEntry != null) {
          if (!Objects.equals(fromEntry.getParameter(), toEntry.getParameter())
              || stringIsDifferent(localizationsToString(fromEntry.getLocalizations()),
                                   localizationsToString(toEntry.getLocalizations()))) {
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
    for (FactoryRight toEntry : toWorkingList) {
      FactoryContentDifference fcd = new FactoryContentDifference();
      fcd.setContentType(TAG_RIGHT);
      fcd.setNewItem(toEntry);
      fcd.setDifferenceType(new CREATE());
      fcdList.add(fcd);
    }
    return fcdList;
  }


  @Override
  public String createItemKeyString(FactoryRight item) {
    return item.getName();
  }


  @Override
  public String createDifferencesString(FactoryRight from, FactoryRight to) {
    StringBuffer ds = new StringBuffer();
    if (!Objects.equals(from.getParameter(), to.getParameter())) {
      ds.append("\n");
      ds.append("    " + TAG_PARAMETER + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + from.getParameter() + "\"=>\"" + to.getParameter() + "\"");
    }
    if (stringIsDifferent(localizationsToString(from.getLocalizations()), localizationsToString(to.getLocalizations()))) {
      ds.append("\n");
      ds.append("    " + TAG_LOCALIZATIONS + " ");
      ds.append(MODIFY.class.getSimpleName() + " \"" + localizationsToString(from.getLocalizations()) + "\"=>\""
          + localizationsToString(to.getLocalizations()) + "\"");
    }
    return ds.toString();
  }


  private String localizationsToString(List<? extends Localization> localizations) {
    if (localizations == null) {
      return "";
    }
    SortedMap<String, String> sortedMap = new TreeMap<String, String>();
    for (Localization localization : localizations) {
      sortedMap.put(localization.getLanguage() + ":" + localization.getIdentifier(), localization.getText());
    }
    List<String> ds = new ArrayList<>();
    for (String key : sortedMap.keySet()) {
      ds.add(key + ":" + sortedMap.get(key));
    }
    return String.join(";", ds);
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


  @Override
  public void create(FactoryRight item) {
    try {
      if (item.getParameter() != null && item.getParameter().length() > 0) {
        XynaFactory.getInstance().getXynaMultiChannelPortal().createRight(item.getName() + ":" + item.getParameter());
      } else {
        XynaFactory.getInstance().getXynaMultiChannelPortal().createRight(item.getName());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void modify(FactoryRight from, FactoryRight to) {
    // TODO
  }


  @Override
  public void delete(FactoryRight item) {
    try {
      if (item.getParameter() != null && item.getParameter().length() > 0) {
        XynaFactory.getInstance().getXynaMultiChannelPortal().deleteRight(item.getName() + ":" + item.getParameter());
      } else {
        XynaFactory.getInstance().getXynaMultiChannelPortal().deleteRight(item.getName());
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<IgnorePatternInterface<FactoryRight>> getIgnorePatterns() {
    return ignorePatterns;
  }


  public static final class NameIgnorePattern extends RegexIgnorePattern<FactoryRight> {

    public NameIgnorePattern() {
      super("name");
    }


    @Override
    public boolean ignore(FactoryRight item, String value) {
      return item.getName().matches(getRegexPart(value));
    }
  }


}
