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

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.novell.ldap.LDAPObjectClassSchema;


@Persistable(primaryKey = LDAPObjectClassDictionaryEntry.COL_OID, tableName = LDAPObjectClassDictionaryEntry.TABLENAME)
public class LDAPObjectClassDictionaryEntry extends Storable<LDAPObjectClassDictionaryEntry> implements DictionaryEntry {

  private static final long serialVersionUID = -7614373130241429140L;
  
  private static final Logger logger = CentralFactoryLogging.getLogger(LDAPObjectClassDictionaryEntry.class);

  final static String TABLENAME = "ldapobjectclasses";
  final static String COL_OID = "oid";
  final static String COL_ATTRIBUTES = "attributes";
  final static String COL_NAMES = "names";
  final static String COL_SUPERCLASSES = "superclasses";
  final static String COL_OBJECTCLASSTYPE = "objectclasstype";

  @Column(name=COL_OID, type=ColumnType.INHERIT_FROM_JAVA)
  private String oid;

  @Column(name=COL_NAMES, type=ColumnType.BLOBBED_JAVAOBJECT)
  private List<String> names;

  // the 'unique' name used for factory intern references
  private transient String prominentName;

  @Column(name=COL_ATTRIBUTES, type=ColumnType.BLOBBED_JAVAOBJECT)
  private List<SerializableClassloadedObject> attributes;
  
  private transient List<LDAPAttributeTypeDictionaryEntry> _attributes;

  @Column(name=COL_SUPERCLASSES, type=ColumnType.BLOBBED_JAVAOBJECT)
  private List<SerializableClassloadedObject> superclasses;
  
  private transient List<LDAPObjectClassDictionaryEntry> _superclasses;

  @Column(name=COL_OBJECTCLASSTYPE, type=ColumnType.INHERIT_FROM_JAVA)
  private int objectclasstype;


  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("");
    s.append("LDAPObjectClassDictionaryEntry { \n ");
    s.append(" oid: ").append(oid);
    s.append(", objectclasstype: ").append(objectclasstype);
    s.append(", names: [ ");
    if (names != null) {
      for (String name : names) {
        s.append(name).append(" ");
      }
    }
    s.append("] \n ");
    s.append(", attributes: [ \n");
    if (_attributes != null) {
      for (LDAPAttributeTypeDictionaryEntry attr : _attributes) {
        s.append(attr.toString()).append(" ");
      }
    }
    s.append("] \n ");
    s.append(" \n } \n ");
    return s.toString();
  }

  private transient CommonOidRealm oidRealm;

  public LDAPObjectClassDictionaryEntry() { 
    attributes = new ArrayList<SerializableClassloadedObject>();
    _attributes = new ArrayList<LDAPAttributeTypeDictionaryEntry>();
    superclasses = new ArrayList<SerializableClassloadedObject>();
    _superclasses = new ArrayList<LDAPObjectClassDictionaryEntry>();
  }

  LDAPObjectClassDictionaryEntry(LDAPObjectClassSchema schema) {
    this();
    oid = schema.getID();
    oidRealm = CommonOidRealm.getCommenOidRealm(oid);
    names = Arrays.asList(schema.getNames());
    prominentName = seekMostProminentName(names);
    objectclasstype = schema.getType();
  }


  private LDAPObjectClassDictionaryEntry(String oid) {
    this();
    this.oid = oid;
    this.oidRealm = CommonOidRealm.getCommenOidRealm(oid);
  }


  public List<SerializableClassloadedObject> getAttributes() {
    return attributes;
  }


  public void setAttributes(List<SerializableClassloadedObject> attributes) {
    this.attributes = attributes;
  }
  
  
  public List<LDAPAttributeTypeDictionaryEntry> getLDAPAttributes() {
    if ((_attributes == null || _attributes.size() == 0) && attributes != null) {
      _attributes = new ArrayList<LDAPAttributeTypeDictionaryEntry>();
      for (SerializableClassloadedObject attribute : attributes) {
        _attributes.add((LDAPAttributeTypeDictionaryEntry) attribute.getObject());
      }
    }
    return _attributes;
  }


  public void setLDAPAttributes(List<LDAPAttributeTypeDictionaryEntry> attributes) {
    this._attributes = attributes;
    List<SerializableClassloadedObject> serializableAttributes = new ArrayList<SerializableClassloadedObject>();
    for (LDAPAttributeTypeDictionaryEntry ldapAttribute : attributes) {
      serializableAttributes.add(new SerializableClassloadedObject(ldapAttribute));
    }
    setAttributes(serializableAttributes);
  }


  public String getOid() {
    return oid;
  }


  public List<String> getNames() {
    return names;
  }


  public List<SerializableClassloadedObject> getSuperclasses() {
    return superclasses;
  }


  public void setSuperclasses(List<SerializableClassloadedObject> superclasses) {
    this.superclasses = superclasses;
  }
  
  
  public List<LDAPObjectClassDictionaryEntry> getLDAPSuperclasses() {
    if ((_superclasses == null || _superclasses.size() == 0) && superclasses != null) {
      _superclasses = new ArrayList<LDAPObjectClassDictionaryEntry>();
      for (SerializableClassloadedObject superclass : superclasses) {
        _superclasses.add((LDAPObjectClassDictionaryEntry) superclass.getObject());
      }
    }
    return _superclasses;
  }


  public void setLDAPSuperclasses(List<LDAPObjectClassDictionaryEntry> superclasses) {
    this._superclasses = superclasses;
    List<SerializableClassloadedObject> serializableSuperclass = new ArrayList<SerializableClassloadedObject>();
    for (LDAPObjectClassDictionaryEntry ldapSuperclass : superclasses) {
      serializableSuperclass.add(new SerializableClassloadedObject(ldapSuperclass));
    }
    setSuperclasses(serializableSuperclass);
  }


  public CommonOidRealm getOidRealm() {
    if (oidRealm == null) {
      oidRealm = CommonOidRealm.getCommenOidRealm(oid);
    }
    return oidRealm;
  }

  public int getObjectclasstype() {
    return objectclasstype;
  }

  public String getProminentName() {
    if (prominentName == null) { //happened...but how?
      prominentName = seekMostProminentName(names);
    }
    return prominentName;
  }


  @Override
  public boolean equals(Object obj) {
    if (obj instanceof LDAPObjectClassDictionaryEntry) {
      return this.oid.equals(((LDAPObjectClassDictionaryEntry) obj).oid);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    // TODO cache
    return oid.hashCode();
  }


  static String seekMostProminentName(List<String> names) {
    String mostProminent = null;
    for (String name : names) {
      if (mostProminent == null) {
        mostProminent = name;
      } else if (name.length() > mostProminent.length()) {
        mostProminent = name;
      }
    }
    // check null and throw?
    return mostProminent;
  }


  public List<String> getDictionaryKeys() {
    List<String> dictionaryKeys = new ArrayList<String>();
    dictionaryKeys.add(oid);
    for (String name : names) {
      dictionaryKeys.add(name.toLowerCase());
    }
    return dictionaryKeys;
  }


  private static class LDAPObjectClassDictionaryEntryReader implements ResultSetReader<LDAPObjectClassDictionaryEntry> {

    @SuppressWarnings("unchecked")
    public LDAPObjectClassDictionaryEntry read(ResultSet rs) throws SQLException {
      LDAPObjectClassDictionaryEntry entry = new LDAPObjectClassDictionaryEntry(rs.getString(COL_OID));
      entry.objectclasstype = rs.getInt(COL_OBJECTCLASSTYPE);
      entry.names = (List<String>) entry.readBlobbedJavaObjectFromResultSet(rs, COL_NAMES);
      entry.prominentName = seekMostProminentName(entry.names);
      entry.attributes = (List<SerializableClassloadedObject>) entry.readBlobbedJavaObjectFromResultSet(rs, COL_ATTRIBUTES);
      for (SerializableClassloadedObject attribute : entry.attributes) {
        entry._attributes.add((LDAPAttributeTypeDictionaryEntry) attribute.getObject());
      }
      entry.superclasses = (List<SerializableClassloadedObject>) entry.readBlobbedJavaObjectFromResultSet(rs, COL_SUPERCLASSES);
      for (SerializableClassloadedObject superclass : entry.superclasses) {
        entry._superclasses.add((LDAPObjectClassDictionaryEntry) superclass.getObject());
      }
      return entry;
    }

  }

  static ResultSetReader<LDAPObjectClassDictionaryEntry> reader = new LDAPObjectClassDictionaryEntryReader();

  @Override
  public ResultSetReader<? extends LDAPObjectClassDictionaryEntry> getReader() {
    return reader;
  }


  @Override
  public Object getPrimaryKey() {
    return oid;
  }


  @Override
  public <U extends LDAPObjectClassDictionaryEntry> void setAllFieldsFromData(U data) {
    LDAPObjectClassDictionaryEntry cast = data;
    this.oid = cast.oid;
    this.oidRealm = CommonOidRealm.getCommenOidRealm(oid);
    this.names = cast.names;
    this.prominentName = seekMostProminentName(names);
    this.attributes = cast.attributes;
    for (SerializableClassloadedObject attribute : cast.attributes) {
      this._attributes.add((LDAPAttributeTypeDictionaryEntry) attribute.getObject());
    }
    this.superclasses = cast.superclasses;
    for (SerializableClassloadedObject superclass : cast.superclasses) {
      this._superclasses.add((LDAPObjectClassDictionaryEntry) superclass.getObject());
    }
    this.objectclasstype = cast.objectclasstype;
  }
  
  
  @Override
  public ObjectInputStream getObjectInputStreamForStorable(InputStream in) throws IOException {
    ClassLoader cl = LDAPObjectClassDictionaryEntry.class.getClassLoader();
    ObjectInputStream ois = null;
    if (cl instanceof ClassLoaderBase) {
      final Long revision = ((ClassLoaderBase)cl).getRevision();
      ois = new ObjectInputStream(in) {
         
        @Override
        public void defaultReadObject() throws IOException, ClassNotFoundException {
          super.defaultReadObject();
          Object currObj = null;
          try {
            currObj = getCurrentObject();
          } catch (SecurityException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (IllegalArgumentException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (IllegalAccessException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (NoSuchFieldException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (NoSuchMethodException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (InvocationTargetException e) {
            logger.debug("Failed to adjust revision", e);
          }
          try {
            if (currObj != null && currObj instanceof SerializableClassloadedObject) {
              Field revisionField = SerializableClassloadedObject.class.getDeclaredField("revision");
              revisionField.setAccessible(true);
              revisionField.set(currObj, revision);
              logger.debug("Reset revision to: " + String.valueOf(revision));
            }
          } catch (SecurityException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (IllegalArgumentException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (NoSuchFieldException e) {
            logger.debug("Failed to adjust revision", e);
          } catch (IllegalAccessException e) {
            logger.debug("Failed to adjust revision", e);
          }
        }
        
        private Object getCurrentObject() throws SecurityException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
          Field currObjField;
          try {
            currObjField = ObjectInputStream.class.getDeclaredField("curObj");
            currObjField.setAccessible(true);
            return currObjField.get(this);
          } catch (NoSuchFieldException e) {
            Field curContextField = ObjectInputStream.class.getDeclaredField("curContext");
            curContextField.setAccessible(true);
            Object curContext = curContextField.get(this);
            if (curContext != null) {
              Field objField = curContext.getClass().getDeclaredField("obj");
              objField.setAccessible(true);
              return objField.get(curContext);
            } else {
              return null;
            }
          }
        }
        
      };
    } else {
      ois = super.getObjectInputStreamForStorable(in);
    }
    return ois;
    
  }


}
