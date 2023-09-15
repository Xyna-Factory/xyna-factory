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

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;

import xmcp.gitintegration.DELETE;
import xmcp.gitintegration.FactoryContentDifference;
import xmcp.gitintegration.FactoryRole;
import xmcp.gitintegration.MODIFY;



public class RoleProcessor implements FactoryContentProcessor<FactoryRole> {

  private static final String TAG_ROLE = "role";
  private static final String TAG_NAME = "name";
  private static final String TAG_RIGHTS = "rights";
  private static final String TAG_RIGHT = "right";
  private static final String TAG_DOMAIN = "domain";
  private static final String TAG_DESCRIPTION = "description";
  private static final List<IgnorePatternInterface<FactoryRole>> ignorePatterns = createIgnorePatterns();


  private static List<IgnorePatternInterface<FactoryRole>> createIgnorePatterns() {
    List<IgnorePatternInterface<FactoryRole>> resultList = new ArrayList<>();
    return Collections.unmodifiableList(resultList);
  }


  @Override
  public List<FactoryRole> createItems() {
    List<FactoryRole> result = new ArrayList<FactoryRole>();
    Collection<Role> roles = null;
    try {
      roles = XynaFactory.getInstance().getFactoryManagement().getRoles();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Could not query factory roles.");
    }

    for (Role role : roles) {
      FactoryRole.Builder builder = new FactoryRole.Builder();
      List<String> allRights = new ArrayList<>(role.getRightsAsList());
      allRights.addAll(role.getScopedRights());
      builder.name(role.getName()).domain(role.getDomain()).description(role.getDescription()).rights(allRights);
      result.add(builder.instance());
    }

    return result;
  }


  @Override
  public void writeItem(XmlBuilder builder, FactoryRole item) {
    builder.startElement(TAG_ROLE);
    builder.element(TAG_NAME, item.getName());
    builder.element(TAG_DOMAIN, item.getDomain());
    builder.element(TAG_DESCRIPTION, XmlBuilder.encode(item.getDescription()));
    builder.startElement(TAG_RIGHTS);
    for (String right : item.getRights()) {
      builder.element(TAG_RIGHT, XmlBuilder.encode(right));
    }
    builder.endElement(TAG_RIGHTS);
    builder.endElement(TAG_ROLE);
  }


  @Override
  public String getTagName() {
    return TAG_ROLE;
  }


  @Override
  public FactoryRole parseItem(Node node) {
    FactoryRole.Builder builder = new FactoryRole.Builder();
    NodeList childNodes = node.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      switch (childNode.getNodeName()) {
        case TAG_NAME :
          builder.name(childNode.getTextContent());
          break;
        case TAG_RIGHTS :
          parseRights(childNode, builder);
          break;
        case TAG_DOMAIN :
          builder.domain(childNode.getTextContent());
          break;
        case TAG_DESCRIPTION :
          builder.description(childNode.getTextContent());
          break;
      }
    }
    return builder.instance();
  }


  private void parseRights(Node node, FactoryRole.Builder builder) {
    NodeList childNodes = node.getChildNodes();
    List<String> rights = new ArrayList<String>();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node childNode = childNodes.item(i);
      if (childNode.getNodeName().equals(TAG_RIGHT))
        rights.add(childNode.getTextContent());
    }
    builder.rights(rights);
  }


  @Override
  public List<FactoryContentDifference> compare(Collection<? extends FactoryRole> from, Collection<? extends FactoryRole> to) {
    List<FactoryContentDifference> fcdList = new ArrayList<>();
    List<FactoryRole> toWorkingList = to == null ? new ArrayList<>() : new ArrayList<>(to);

    HashMap<String, FactoryRole> toMap = new HashMap<String, FactoryRole>();
    for (FactoryRole toEntry : toWorkingList) {
      toMap.put(createItemKeyString(toEntry), toEntry);
    }

    // iterate over from-list
    // create MODIFY and DELETE entries
    if (from != null) {
      for (FactoryRole fromEntry : from) {
        FactoryRole toEntry = toMap.get(createItemKeyString(fromEntry));
        FactoryContentDifference wcd = new FactoryContentDifference();

        wcd.setContentType(TAG_ROLE);
        wcd.setExistingItem(fromEntry);
        wcd.setDifferenceType(toEntry == null ? new DELETE() : new MODIFY());
        toWorkingList.remove(toEntry); // remove entry from to-list
        boolean hasChanged = toEntry == null || !Objects.equals(fromEntry.getDescription(), toEntry.getDescription())
            || rightsChanged(fromEntry.getRights(), toEntry.getRights());
        if (hasChanged) {
          wcd.setNewItem(toEntry);
          fcdList.add(wcd);
        } // else: EQUAL -> ignore entry
      }
    }
    return fcdList;
  }


  private boolean rightsChanged(List<String> rights1, List<String> rights2) {
    if (rights1.size() != rights2.size()) {
      return true;
    }
    List<String> r1 = new ArrayList<String>(rights1);
    List<String> r2 = new ArrayList<String>(rights2);
    Collections.sort(r1);
    Collections.sort(r2);
    for (int i = 0; i < r1.size(); i++) {
      if (!Objects.equals(r1.get(i), r2.get(i))) {
        return true;
      }
    }

    return false;
  }


  @Override
  public String createItemKeyString(FactoryRole item) {
    return String.format("%s/%s", item.getName(), item.getDomain());
  }


  @Override
  public String createDifferencesString(FactoryRole from, FactoryRole to) {
    int maxLines = 5;
    List<String> added = new ArrayList<String>(from.getRights());
    added.removeAll(to.getRights());
    List<String> removed = new ArrayList<String>(to.getRights());
    removed.removeAll(from.getRights());
    StringBuilder sb = new StringBuilder();

    if (!Objects.equals(from.getDescription(), to.getDescription())) {
      sb.append("\n").append("  Description: ").append(from.getDescription()).append(" -> ").append(to.getDescription());
    }

    if (!Objects.equals(from.getDomain(), to.getDomain())) {
      sb.append("\n").append("  Domain: ").append(from.getDomain()).append(" -> ").append(to.getDomain());
    }

    if (!added.isEmpty() || !removed.isEmpty()) {
      sb.append(String.format("\nRights:  %d added, %d removed\n", added.size(), removed.size()));
      int printedLines = 0;
      for (int i = 0; i < Math.min(added.size(), maxLines); i++) {
        printedLines++;
        sb.append("    ADD ").append(added.get(i)).append("\n");
      }
      for (int i = 0; i < Math.min(removed.size(), maxLines - printedLines); i++) {
        printedLines++;
        sb.append("    REMOVE ").append(removed.get(i)).append("\n");
      }
    }
    return sb.toString();
  }


  @Override
  public void create(FactoryRole item) {
    try {
      XynaFactory.getInstance().getFactoryManagement().createRole(item.getName(), item.getDomain());
      XynaFactory.getInstance().getFactoryManagement().setDescriptionOfRole(item.getName(), item.getDomain(), item.getDescription());
      if (item.getRights() != null) {
        for (String right : item.getRights()) {
          XynaFactory.getInstance().getFactoryManagement().grantRightToRole(item.getName(), right);
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void modify(FactoryRole from, FactoryRole to) {
    try {
      if (!Objects.equals(from.getDescription(), to.getDescription())) {
        XynaFactory.getInstance().getFactoryManagement().setDescriptionOfRole(from.getName(), from.getDomain(), to.getDescription());
      }
      if (rightsChanged(from.getRights(), to.getRights())) {
        for (String right : from.getRights()) {
          XynaFactory.getInstance().getFactoryManagement().revokeRightFromRole(from.getName(), right);
        }
        for (String right : to.getRights()) {
          XynaFactory.getInstance().getFactoryManagement().grantRightToRole(to.getName(), right);
        }
      }

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void delete(FactoryRole item) {
    try {
      XynaFactory.getInstance().getFactoryManagement().deleteRole(item.getName(), item.getDomain());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public List<IgnorePatternInterface<FactoryRole>> getIgnorePatterns() {
    return ignorePatterns;
  }

}
