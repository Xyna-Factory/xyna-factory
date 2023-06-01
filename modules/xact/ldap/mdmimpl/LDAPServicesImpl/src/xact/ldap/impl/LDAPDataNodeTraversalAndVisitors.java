/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xact.ldap.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPEntry;

import xact.ldap.LDAPAbstractObjectclass;
import xact.ldap.LDAPAuxiliaryObjectclass;
import xact.ldap.LDAPBaseObjectclass;
import xact.ldap.LDAPDataNode;
import xact.ldap.LDAPRelativeDistinguishedName;
import xact.ldap.dictionary.LDAPAttributeTypeDictionaryEntry;
import xact.ldap.dictionary.LDAPObjectClassDictionaryEntry;
import xact.ldap.dictionary.LDAPSchemaDictionary;


public class LDAPDataNodeTraversalAndVisitors {
  
  private final static Logger logger = LDAPServicesServiceOperationImpl.logger;

  public static void traverseLDAPNodeHierarchy(LDAPDataNode root, LDAPNodeHierarchyAttributeVisitor visitor) {
    LDAPObjectClassDictionaryEntry rootObjectClass = LDAPServicesServiceOperationImpl.resolveXynaObjectFromDictionary(root);
    logger.debug("traverseLDAPNodeHierarchy: Found root obj class in dictionary: " + String.valueOf(rootObjectClass));
    traverseLDAPNodeHierarchy(root, rootObjectClass, visitor);
    List<? extends LDAPBaseObjectclass> localClasses = root.getLocalObjectclasses();
    if (localClasses != null) {
      for (LDAPBaseObjectclass ldapBaseObjectclass : localClasses) {
        LDAPObjectClassDictionaryEntry entry = LDAPServicesServiceOperationImpl.resolveXynaObjectFromDictionary(ldapBaseObjectclass);

        logger.debug("traverseLDAPNodeHierarchy: Found obj class in dictionary: " + String.valueOf(entry.toString()));
        traverseLDAPNodeHierarchy(ldapBaseObjectclass, entry, visitor);
      }
    }
  }

  static void traverseLDAPNodeHierarchy(LDAPDataNode root, LDAPObjectClassDictionaryEntry rootObjectClass, LDAPNodeHierarchyAttributeVisitor visitor) {
    if (root != null) {
      visitor.visit(root, rootObjectClass);
      if (rootObjectClass.getLDAPAttributes() != null) {
        for (LDAPAttributeTypeDictionaryEntry attribute : rootObjectClass.getLDAPAttributes()) {
          visitor.visit(root, attribute);
        }
      }
      if (rootObjectClass.getLDAPSuperclasses() != null) {
        for (LDAPObjectClassDictionaryEntry superObjectClass : rootObjectClass.getLDAPSuperclasses()) {
          traverseLDAPNodeHierarchy(root, superObjectClass, visitor);
        }
      }
    }
  }

  static void traverseLDAPNodeHierarchy(LDAPBaseObjectclass root, LDAPObjectClassDictionaryEntry rootObjectClass, LDAPNodeHierarchyAttributeVisitor visitor) {
    if (root instanceof LDAPAuxiliaryObjectclass) {
      visitor.visit((LDAPAuxiliaryObjectclass)root, rootObjectClass);
      for (LDAPAttributeTypeDictionaryEntry attribute : rootObjectClass.getLDAPAttributes()) {
        visitor.visit((LDAPAuxiliaryObjectclass)root, attribute);
      }
    } else {
      visitor.visit((LDAPAbstractObjectclass)root, rootObjectClass);
    }
    List<LDAPObjectClassDictionaryEntry> superclasses = rootObjectClass.getLDAPSuperclasses();
    if (rootObjectClass.getSuperclasses() != null) {
      for (LDAPObjectClassDictionaryEntry superclass : superclasses) {
        traverseLDAPNodeHierarchy(root, superclass, visitor);
      }
    }
  }



  private static LDAPAttribute generateLDAPAttributeFromXynaObject(XynaObject xynaObject, LDAPAttributeTypeDictionaryEntry attribute) {
    String[] values = getXynaObjectValueAsTransformedLDAPString(xynaObject, attribute);
    if (values == null || values.length <= 0) {
      return null;
    } else if (values.length == 1) {
      logger.debug("generateLDAPAttributeFromXynaObject: for " + attribute.getProminentName() + "  value: " + values[0]);
      return new LDAPAttribute(attribute.getProminentName(), values[0]);
    } else {
      logger.debug("generateLDAPAttributeFromXynaObject: for " + attribute.getProminentName() + "  value: " + Arrays.toString(values));
      return new LDAPAttribute(attribute.getProminentName(), values);
    }
  }


  private static String[] getXynaObjectValueAsTransformedLDAPString(XynaObject xynaObject, LDAPAttributeTypeDictionaryEntry attribute) {
    String attributeXynaName = LDAPSchemaDictionary.generateValidNameForAttribute(attribute.getProminentName());
    try {
      Object value = xynaObject.get(attributeXynaName);
      if (value == null) {
        return null;
      } else if (value instanceof List) {
        List list = (List)value;
        String[] stringValues = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
          stringValues[i] = convertXynaObjectValueToString(list.get(i), attribute);
        }
        return stringValues;
      } else {
        String singleValue = convertXynaObjectValueToString(value, attribute);
        if (singleValue == null || singleValue.length() == 0) {
          return null;
        } else {
          return new String[] {singleValue};
        }
      }
    } catch (InvalidObjectPathException e) {
      // could happen with ignored multiple inheritance
      logger.warn("attribute " + attributeXynaName + " could not be retrieved from " + xynaObject);
      return null;
    }
  }


  private static String convertXynaObjectValueToString(Object obj, LDAPAttributeTypeDictionaryEntry attribute) {
    return attribute.getSyntax().transformToString(obj);
  }




  static interface LDAPNodeHierarchyAttributeVisitor {

    public void visit(LDAPDataNode node, LDAPObjectClassDictionaryEntry entry);

    public void visit(LDAPDataNode node, LDAPAttributeTypeDictionaryEntry attribute);

    public void visit(LDAPAuxiliaryObjectclass node, LDAPObjectClassDictionaryEntry entry);

    public void visit(LDAPAuxiliaryObjectclass node, LDAPAttributeTypeDictionaryEntry attribute);

    public void visit(LDAPAbstractObjectclass node, LDAPObjectClassDictionaryEntry entry);
  }


  static class LDAPAttributeSetGenerationVisitor implements LDAPNodeHierarchyAttributeVisitor {

    private LDAPAttributeSet attributes;
    private Set<String> attributesFormingRdn;

    LDAPAttributeSetGenerationVisitor(String rdn) {
      attributes = new LDAPAttributeSet();
      attributesFormingRdn = new HashSet<String>();
      String[] singleColumnsFromRdn = rdn.split("(?<!\\\\)\\+");
      for (String singleRdn : singleColumnsFromRdn) {
        attributesFormingRdn.add(singleRdn.substring(0, singleRdn.indexOf("=")));
      }
    }

    LDAPAttributeSet getAttributeSet() {
      return attributes;
    }

    public void visit(LDAPDataNode node, LDAPObjectClassDictionaryEntry entry) {
      addObjectClassToAttributeSet(entry);
    }

    public void visit(LDAPDataNode node, LDAPAttributeTypeDictionaryEntry attribute) {
      if (!isAttributeContainedInRdn(attribute)) {
        LDAPAttribute attrib = generateLDAPAttributeFromXynaObject(node, attribute);
        if (attrib != null) {
          attributes.add(attrib);
        }
      }
    }

    public void visit(LDAPAuxiliaryObjectclass node, LDAPObjectClassDictionaryEntry entry) {
      addObjectClassToAttributeSet(entry);
    }

    public void visit(LDAPAuxiliaryObjectclass node, LDAPAttributeTypeDictionaryEntry attribute) {
      if (!isAttributeContainedInRdn(attribute)) {
        LDAPAttribute attrib = generateLDAPAttributeFromXynaObject(node, attribute);
        if (attrib != null) {
          attributes.add(attrib);
        }
      }
    }

    public void visit(LDAPAbstractObjectclass node, LDAPObjectClassDictionaryEntry entry) {
      addObjectClassToAttributeSet(entry);
    }


    private boolean isAttributeContainedInRdn(LDAPAttributeTypeDictionaryEntry attribute) {
      for (String attributeName : attribute.getNames()) {
        if (attributesFormingRdn.contains(attributeName)) {
          return true;
        }
      }
      return false;
    }

    private void addObjectClassToAttributeSet(LDAPObjectClassDictionaryEntry entry) {
      LDAPAttribute attribute = attributes.getAttribute(LDAPServicesServiceOperationImpl.OBJECTCLASS_ATTRIBUTE);
      if (attribute == null) {
        attributes.add(new LDAPAttribute(LDAPServicesServiceOperationImpl.OBJECTCLASS_ATTRIBUTE, entry.getProminentName()));
      } else {
        attribute.addValue(entry.getProminentName());
      }
    }

  }


  static class LDAPSelectAllAttributesVisitor implements LDAPNodeHierarchyAttributeVisitor {

    private Set<String> allAttributes;

    LDAPSelectAllAttributesVisitor() {
      allAttributes = new HashSet<String>();
      allAttributes.add(LDAPServicesServiceOperationImpl.OBJECTCLASS_ATTRIBUTE);
    }

    String[] getAllAttributeNames() {
      return allAttributes.toArray(new String[allAttributes.size()]);
    }

    public void visit(LDAPDataNode node, LDAPObjectClassDictionaryEntry entry) {}

    public void visit(LDAPDataNode node, LDAPAttributeTypeDictionaryEntry attribute) {
      allAttributes.add(attribute.getProminentName());
    }

    public void visit(LDAPAuxiliaryObjectclass node, LDAPObjectClassDictionaryEntry entry) {}

    public void visit(LDAPAuxiliaryObjectclass node, LDAPAttributeTypeDictionaryEntry attribute) {
      allAttributes.add(attribute.getProminentName());
    }

    public void visit(LDAPAbstractObjectclass node, LDAPObjectClassDictionaryEntry entry) {}

  }


  static class LDAPFilterBuilderVisitor implements LDAPNodeHierarchyAttributeVisitor {

    boolean onObjectclass;
    List<String> filter;

    LDAPFilterBuilderVisitor() {
      this(new ArrayList<String>());
    }

    LDAPFilterBuilderVisitor(List<String> presetFilters) {
      this(presetFilters, true);
    }
    
    LDAPFilterBuilderVisitor(List<String> presetFilters, boolean onObjectclass) {
      filter = presetFilters;
      this.onObjectclass = onObjectclass;
    }

    String getCompleteFilterString() {
      if (filter.size() <= 0) {
        return "("+LDAPServicesServiceOperationImpl.OBJECTCLASS_ATTRIBUTE+"=*)";
      } else if (filter.size() == 1) {
        return '(' + filter.get(0) + ')';
      } else {
        StringBuilder filterBuilder = new StringBuilder("(&");
        for (String singleFilter : filter) {
          filterBuilder.append('(')
                       .append(singleFilter)
                       .append(')');
        }
        filterBuilder.append(")");
        return filterBuilder.toString();
      }
    }

    public void visit(LDAPDataNode node, LDAPObjectClassDictionaryEntry entry) {
      if (onObjectclass) {
        filter.add(LDAPServicesServiceOperationImpl.OBJECTCLASS_ATTRIBUTE + "=" + entry.getProminentName());
      }
    }

    public void visit(LDAPDataNode node, LDAPAttributeTypeDictionaryEntry attribute) {
      String[] values = getXynaObjectValueAsTransformedLDAPString(node, attribute);
      if (values != null) {
        for (String value : values) {
          filter.add(attribute.getProminentName() + "=" + value);
        }
      }
    }

    public void visit(LDAPAuxiliaryObjectclass node, LDAPObjectClassDictionaryEntry entry) {
      if (onObjectclass) {
        filter.add(LDAPServicesServiceOperationImpl.OBJECTCLASS_ATTRIBUTE + "=" + entry.getProminentName());
      }
    }

    public void visit(LDAPAuxiliaryObjectclass node, LDAPAttributeTypeDictionaryEntry attribute) {
      String[] values = getXynaObjectValueAsTransformedLDAPString(node, attribute);
      if (values != null) {
        for (String value : values) {
          filter.add(attribute.getProminentName() + "=" + value);
        }
      }
    }

    public void visit(LDAPAbstractObjectclass node, LDAPObjectClassDictionaryEntry entry) {
      if (onObjectclass) {
        filter.add(LDAPServicesServiceOperationImpl.OBJECTCLASS_ATTRIBUTE + "=" + entry.getProminentName());
      }
    }

  }


  static class XynaObjectFillingVisitor implements LDAPNodeHierarchyAttributeVisitor {

    private final LDAPEntry ldapEntry;

    XynaObjectFillingVisitor(LDAPEntry ldapEntry) {
      this.ldapEntry = ldapEntry;
    }

    public void visit(LDAPDataNode node, LDAPObjectClassDictionaryEntry entry) {
      node.setLDAPRelativeDistinguishedName(new LDAPRelativeDistinguishedName(LDAPServicesServiceOperationImpl.unescapeRdn(dnToRdn(ldapEntry.getDN()))));
    }

    public void visit(LDAPDataNode node, LDAPAttributeTypeDictionaryEntry attribute) {
      // allthough we filtered with prominentNames...might the result contain other names?
      // they do -.-'
      for (String attributeName : attribute.getNames()) {
        LDAPAttribute attrib = ldapEntry.getAttribute(attributeName);
        if (attrib != null) {
          fillXynaObject(node, attrib, attribute);
          break;
        }
      }
    }

    public void visit(LDAPAuxiliaryObjectclass node, LDAPObjectClassDictionaryEntry entry) { /* instantiation happens outside, we won't be called*/ }

    public void visit(LDAPAuxiliaryObjectclass node, LDAPAttributeTypeDictionaryEntry attribute) {
      for (String attributeName : attribute.getNames()) {
        LDAPAttribute attrib = ldapEntry.getAttribute(attributeName);
        if (attrib != null) {
          fillXynaObject(node, attrib, attribute);
          break;
        }
      }
    }

    public void visit(LDAPAbstractObjectclass node, LDAPObjectClassDictionaryEntry entry) { /* instantiation happens outside, we won't be called*/ }

    private String dnToRdn(String dn) {
      return dn.substring(0, dn.indexOf(","));
    }

    private void fillXynaObject(XynaObject node, LDAPAttribute attribute, LDAPAttributeTypeDictionaryEntry attributeEntry) {
      if (attribute != null) {
        Object xynaObjectValue;
        if (attributeEntry.isList()) {
          xynaObjectValue = attributeEntry.getSyntax().transformLDAPAttributeToObjectList(attribute);
        } else {
          xynaObjectValue = attributeEntry.getSyntax().transformLDAPAttributeToObject(attribute);
        }
        String xynaOjectVariableName = LDAPSchemaDictionary.generateValidNameForAttribute(attributeEntry.getProminentName());
        try {
          node.set(xynaOjectVariableName, xynaObjectValue);
        } catch (XDEV_PARAMETER_NAME_NOT_FOUND e) {
          LDAPServicesServiceOperationImpl.logger.warn("The memberVariable " + xynaOjectVariableName + " could not be found in " + node.getClass().getCanonicalName());
        }
      }
    }

  }


}
