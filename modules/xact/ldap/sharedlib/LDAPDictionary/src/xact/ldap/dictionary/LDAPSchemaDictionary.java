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
package xact.ldap.dictionary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.map.TypeMappingCache;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderDispatcher;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderType;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.novell.ldap.LDAPAttributeSchema;
import com.novell.ldap.LDAPObjectClassSchema;
import com.novell.ldap.LDAPSchema;


public class LDAPSchemaDictionary {

  private static Logger _logger = CentralFactoryLogging.getLogger(LDAPSchemaDictionary.class);


  private final static String TYPEMAPPING_COMPONENT_IDENTIFIER = "ldapobjectclassmap";

  private Map<String, LDAPObjectClassDictionaryEntry> objectclassDictionary = new HashMap<String, LDAPObjectClassDictionaryEntry>();
  private final TypeMappingCache typeMapping;

  // internal objectClasses that cannot be generated because they contain faked attributes
  private String[] generationExceptions = new String[] {"subentry", "subschema"};


  public LDAPSchemaDictionary(LDAPSchema schema) {
    try {
      typeMapping = new TypeMappingCache();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to initialize TypeMapping", e);
    }
    Map<String, LDAPAttributeTypeDictionaryEntry> attributeDictionary = populateAttributeTypes(schema);
    populateObjectClasses(schema, attributeDictionary);
  }

  
  public LDAPSchemaDictionary(ODSConnection con) throws PersistenceLayerException {
    try {
      typeMapping = new TypeMappingCache();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to initialize TypeMapping", e);
    }
    Collection<LDAPObjectClassDictionaryEntry> objectClasses = con.loadCollection(LDAPObjectClassDictionaryEntry.class);
    for (LDAPObjectClassDictionaryEntry ldapObjectClassDictionaryEntry : objectClasses) {
      insertDictionaryEntryInDictionaryPart(objectclassDictionary, ldapObjectClassDictionaryEntry);
    }
  }
  
  
  public void rebuildFromSchema(LDAPSchema schema) {
    Map<String, LDAPAttributeTypeDictionaryEntry> attributeDictionary = populateAttributeTypes(schema);
    populateObjectClasses(schema, attributeDictionary);
  }
  
  
  public void clear() {
    objectclassDictionary.clear();
  }

  
  public Set<LDAPObjectClassDictionaryEntry> getAllObjectClasses() {
    // returning as a set and having implemented equals and hashCode based on oid should prevent us from returning the same object multiple times
    return new HashSet<LDAPObjectClassDictionaryEntry>(objectclassDictionary.values());
  }


  private Map<String, LDAPAttributeTypeDictionaryEntry> populateAttributeTypes(LDAPSchema schema) {
    Map<String, LDAPAttributeTypeDictionaryEntry> attributeTypeDictionary = new HashMap<String, LDAPAttributeTypeDictionaryEntry>();
    @SuppressWarnings("unchecked")
    Enumeration<LDAPAttributeSchema> ldapAttributeEnumeration = schema.getAttributeSchemas();
    while (ldapAttributeEnumeration.hasMoreElements()) {
      LDAPAttributeSchema attribute = ldapAttributeEnumeration.nextElement();
      LDAPAttributeTypeDictionaryEntry generation = parseIntoGeneration(attribute, attributeTypeDictionary, schema);
      insertDictionaryEntryInDictionaryPart(attributeTypeDictionary, generation);
    }
    return attributeTypeDictionary;
  }


  private void populateObjectClasses(LDAPSchema schema, Map<String, LDAPAttributeTypeDictionaryEntry> attributeDictionary) {
    @SuppressWarnings("unchecked")
    Enumeration<LDAPObjectClassSchema> objectClasses = schema.getObjectClassSchemas();
    while (objectClasses.hasMoreElements()) {
      LDAPObjectClassSchema objectClass = objectClasses.nextElement();
      if (isValidObjectClassForGeneration(objectClass)) {
        LDAPObjectClassDictionaryEntry generation = parseIntoGeneration(objectClass, attributeDictionary, schema);
        insertDictionaryEntryInDictionaryPart(objectclassDictionary, generation);
      }
    }
  }


  private <E extends DictionaryEntry> void insertDictionaryEntryInDictionaryPart(Map<String, E> dictinaryPart, E entry) {
    for (String key : entry.getDictionaryKeys()) {
      _logger.debug("Inserting key in ldap dictionary: " + key);
      dictinaryPart.put(key, entry);
    }
  }


  private boolean isValidObjectClassForGeneration(LDAPObjectClassSchema objectClass) {
    for (String name : objectClass.getNames()) {
      if (Arrays.binarySearch(generationExceptions, name) >= 0) {
        return false;
      }
    }
    return true;
  }



  private LDAPAttributeTypeDictionaryEntry parseIntoGeneration(LDAPAttributeSchema currentSchema,
                                                               Map<String, LDAPAttributeTypeDictionaryEntry> attributeTypeDictionary,
                                                               LDAPSchema schema) {
    LDAPAttributeTypeDictionaryEntry generation = attributeTypeDictionary.get(currentSchema.getNames()[0].toLowerCase());
    if (generation == null) {
      generation = new LDAPAttributeTypeDictionaryEntry(currentSchema);
      String superclassName = currentSchema.getSuperior();
      if (superclassName != null && superclassName.length() > 0) {
        if (attributeTypeDictionary.containsKey(superclassName.toLowerCase())) {
          generation.setSuperattribute(attributeTypeDictionary.get(superclassName.toLowerCase()));
        } else {
          generation.setSuperattribute(parseIntoGeneration(schema.getAttributeSchema(superclassName), attributeTypeDictionary, schema));
        }
      }
    }
    return generation;
  }


  private LDAPObjectClassDictionaryEntry parseIntoGeneration(LDAPObjectClassSchema currentSchema,
                                                             Map<String, LDAPAttributeTypeDictionaryEntry> attributeDictionary,
                                                             LDAPSchema schema) {
    LDAPObjectClassDictionaryEntry generation = objectclassDictionary.get(currentSchema.getNames()[0].toLowerCase());
    if (generation == null) {
      generation = new LDAPObjectClassDictionaryEntry(currentSchema);
      List<LDAPObjectClassDictionaryEntry> superclasses = new ArrayList<LDAPObjectClassDictionaryEntry>();
      String[] superclassNames = currentSchema.getSuperiors();
      if (superclassNames != null && superclassNames.length > 0) {
        for (String superclassName : currentSchema.getSuperiors()) {
          if (objectclassDictionary.containsKey(superclassName.toLowerCase())) {
            superclasses.add(objectclassDictionary.get(superclassName.toLowerCase()));
          } else {
            superclasses.add(parseIntoGeneration(schema.getObjectClassSchema(superclassName), attributeDictionary, schema));
          }
        }
        generation.setLDAPSuperclasses(superclasses);
      }

      List<String> attributeNames = new ArrayList<String>();
      if (currentSchema.getRequiredAttributes() != null) {
        attributeNames.addAll(Arrays.asList(currentSchema.getRequiredAttributes()));
      }
      if (currentSchema.getOptionalAttributes() != null) {
        attributeNames.addAll(Arrays.asList(currentSchema.getOptionalAttributes()));
      }

      Set<LDAPAttributeTypeDictionaryEntry> allSuperClasseAttributes = new HashSet<LDAPAttributeTypeDictionaryEntry>();
      collectSuperClassAttributes(generation, allSuperClasseAttributes);

      Set<LDAPAttributeTypeDictionaryEntry> attributes = new HashSet<LDAPAttributeTypeDictionaryEntry>();
      for (String attributeName : attributeNames) {
        LDAPAttributeTypeDictionaryEntry attribute = attributeDictionary.get(attributeName.toLowerCase());
        if (!allSuperClasseAttributes.contains(attribute)) {
          attributes.add(attribute);
        }
      }
      generation.setLDAPAttributes(new ArrayList<LDAPAttributeTypeDictionaryEntry>(attributes));
    }
    return generation;
  }


  private void collectSuperClassAttributes(LDAPObjectClassDictionaryEntry objectClass, Set<LDAPAttributeTypeDictionaryEntry> attributes) {
    if (objectClass.getAttributes() != null) {
      attributes.addAll(objectClass.getLDAPAttributes());
    }

    if (objectClass.getSuperclasses() != null) {
      for (LDAPObjectClassDictionaryEntry superclass : objectClass.getLDAPSuperclasses()) {
        collectSuperClassAttributes(superclass, attributes);
      }
    }
  }


  public void persistDicitionary(ODSConnection con) throws PersistenceLayerException {
    con.persistCollection(getAllObjectClasses());
    con.commit();
  }


  public LDAPObjectClassDictionaryEntry lookupByNameOrOid(String uniqueIdentifier) {
   return objectclassDictionary.get(uniqueIdentifier);
  }


  public LDAPObjectClassDictionaryEntry lookupByXynaObjectName(String xynaObjectName) {
    String ldapName = typeMapping.lookupReverse(TYPEMAPPING_COMPONENT_IDENTIFIER, xynaObjectName);
    return lookupByNameOrOid(ldapName.toLowerCase());
  }

  public String resolveNameOrOidToXynaObjectName(String uniqueIdentifier) {
    return typeMapping.lookup(TYPEMAPPING_COMPONENT_IDENTIFIER, uniqueIdentifier);
  }

  public Class<? extends XynaObject> resolveNameOrOidToXynaObjectClass(String uniqueIdentifier, ClassLoader cl) {
    return typeMapping.lookupClass(cl, TYPEMAPPING_COMPONENT_IDENTIFIER, uniqueIdentifier);
  }
  
  public Class<? extends XynaObject> resolveNameOrOidToXynaObjectClass(String uniqueIdentifier, Long revision) {
    String fqXOName = resolveNameOrOidToXynaObjectName(uniqueIdentifier);
    String fqName;
    try {
      fqName = GenerationBase.transformNameForJava(fqXOName);
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    ClassLoaderDispatcher cld = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher();
    ClassLoaderBase clb = cld.findClassLoaderByType(fqName, revision, ClassLoaderType.MDM, true);
    if (clb == null) {
      throw new IllegalStateException("Class for " + uniqueIdentifier + " not found from rev"+revision);
    }
    Class<?> clazz;
    try {
      clazz = clb.loadClass(fqName);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    if (!XynaObject.class.isAssignableFrom(clazz)) {
      throw new RuntimeException("invalid class name: " + uniqueIdentifier + "("+fqName+") is not a xynaobject class.");
    }
    @SuppressWarnings("unchecked")
    Class<? extends XynaObject> clazzXO = (Class<? extends XynaObject>) clazz;
    return clazzXO;
  }

  
  private final static String namePrefixForClasses = "LDAP";
  private final static String namePrefixForVariables = "ldap";
  
  private final static Pattern invalidNameCharacters = Pattern.compile("[^\\w_]+");

  public static String generateValidNameForClass(String ldapname) {
    return invalidNameCharacters.matcher(namePrefixForClasses + ldapname.substring(0,1).toUpperCase() + ldapname.substring(1)).replaceAll("_");
  }
  
  public static String generateValidNameForAttribute(String ldapname) {
    return invalidNameCharacters.matcher(namePrefixForVariables + ldapname.substring(0,1).toUpperCase() + ldapname.substring(1)).replaceAll("_");
  }

}
