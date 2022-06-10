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

package com.gip.xyna.xnwh.persistence;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.update.UpdateObjectInputStream;
import com.gip.xyna.utils.db.UnsupportingResultSet;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;



/**
 * von abgeleitete klassen wird erwartet, korrekt die annotationen @Persistable und @Column zu verwenden zu jeder column
 * muss es einen getter geben, der so heisst, dass er auf den spaltennamen passt. er muss nicht notwendigerweise auf den
 * feldnamen passen. beispiel: <code>
 * {@literal @Column(name="colName")}<br>
 * private int myField;<br>
 * public int getColName() {<br>
 *   return myField;<br>
 * }<br>
 * </code>
 * 
 */
public abstract class Storable<T extends Storable<?>> implements Serializable {

  // TODO implement setAllFieldsFromData and getReader in this class instead of leaving this to the developer

  private static final long serialVersionUID = 1294652313288358794L;
  protected static final Logger logger = CentralFactoryLogging.getLogger(Storable.class);

  //FIXME alle caches müssen geleert werden, wenn das storable undeployed wird (oder redeployed)
  private static Map<Class<? extends Storable>, Persistable> cachedPersistables = new HashMap<Class<? extends Storable>, Persistable>();
  private static Map<Class<? extends Storable>, CompositeIndex[]> cachedCompositeIndices = new HashMap<Class<? extends Storable>, CompositeIndex[]>();
  private static Map<Class<? extends Storable>, Column[]> cachedColumns = new HashMap<Class<? extends Storable>, Column[]>();
  private static Map<Class<? extends Storable>, Map<String, FieldGetter>> getters =
      new HashMap<Class<? extends Storable>, Map<String, FieldGetter>>();
  private static Map<Class<? extends Storable>, ResultSetReader<?>> cachedResultSetReaders = new HashMap<Class<? extends Storable>, ResultSetReader<?>>();
  private static Map<Class<? extends Storable>, Map<String, GetterMethod>> getterMethods =
      new HashMap<Class<? extends Storable>, Map<String, GetterMethod>>();
  private static Object lock = new Object();

  public Storable() { //leerer konstruktor muss vorhanden sein, damit man per reflection objekt erstellen kann

  }


  public abstract ResultSetReader<? extends T> getReader();


  public void readFromPersistence(Connection con) throws PersistenceLayerException,
      XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    con.queryOneRow(this);
  }


  public static <S extends Storable> S clone( S toBeCloned ) {
    try {
      S clone = (S)toBeCloned.getClass().getConstructor().newInstance();
      clone.setAllFieldsFromData(toBeCloned);
      return clone;
    } catch (Exception e) { //InstantiationException, IllegalAccessException
      logger.warn("Could not clone "+toBeCloned.getClass(), e);
      return null;
    }
  }


  public static <S extends Storable> ResultSetReader<S> getResultSetReader(Class<S> klass2) {
    ResultSetReader<?> r = cachedResultSetReaders.get(klass2);
    if( r == null ) {
      synchronized (lock) {
        r = cachedResultSetReaders.get(klass2);
        if (r == null) {
          try {
            Storable<?> s = klass2.getConstructor().newInstance();
            r = s.getReader();
            cachedResultSetReaders.put(klass2, r);
          } catch (Exception e) { //InstantiationException, IllegalAccessException
            logger.warn("Could not get ResultSetReader for "+klass2, e);
          }
        }
      }
    }
    return (ResultSetReader<S>)r;
  }


  public String getTableName() {
    return getPersistable(getClass()).tableName();
  }

  public static String getTableNameLowerCase(Class<? extends Storable> klass2) {
    return getPersistable(klass2).tableName().toLowerCase();
  }

  public static Persistable getPersistable(Class<? extends Storable> klass2) {
    Persistable persi = cachedPersistables.get(klass2);
    if (persi == null) {
      synchronized (lock) {
        persi = cachedPersistables.get(klass2);
        if (persi == null) {
          persi = klass2.getAnnotation(Persistable.class);
          Class<?> klass = klass2;
          while (persi == null && klass != Object.class) {
            klass = klass.getSuperclass();
            persi = klass.getAnnotation(Persistable.class);
          }
          if (persi == null) {
            throw new RuntimeException("class " + klass2.getName() + " has to define @" + Persistable.class.getName()
                + " annotation (or extend a class which does)");
          }
          cachedPersistables.put(klass2, persi);
        }
      }
    }
    return persi;
  }
  
  private static final CompositeIndex[] EMPTY_COMPOSITE_INDEX_ARRAY = new CompositeIndex[0];
  
  public static CompositeIndex[] getCompositeIndices(Class<? extends Storable> klass2) {
    CompositeIndex[] indices = cachedCompositeIndices.get(klass2);
    if (indices == null) {
      synchronized (lock) {
        indices = cachedCompositeIndices.get(klass2);
        if (indices == null) {
          Class<?> klass = klass2;
          while (klass != Object.class) {           
            CompositeIndices compositeIndices = klass.getAnnotation(CompositeIndices.class);
            indices = mergeCompositeIndices(indices, compositeIndices);
            klass = klass.getSuperclass();
          }
          if (indices == null) {
            indices = EMPTY_COMPOSITE_INDEX_ARRAY;
          }            
          cachedCompositeIndices.put(klass2, indices);
        }
      }
    }
    return indices;
  }
  
  
  private static CompositeIndex[] mergeCompositeIndices(CompositeIndex[] indices, CompositeIndices additionalCompositeIndex) {
    if (indices == null) {
      if (additionalCompositeIndex == null) {
       return null;
      } else {
       return additionalCompositeIndex.indices(); 
      }
    } else if (additionalCompositeIndex == null) {
      return indices;
    } else {
      CompositeIndex[] merge = new CompositeIndex[indices.length + additionalCompositeIndex.indices().length];
      System.arraycopy(indices, 0, merge, 0, indices.length);
      System.arraycopy(additionalCompositeIndex.indices(), 0, merge, indices.length, additionalCompositeIndex.indices().length);
      return merge;
    }
  }


  /**
   * lazy geladen
   * @return spalten
   */
  public Column[] getColumns() {
    return getColumns(getClass());
  }
  
  
  public static Column[] getColumns(Class<? extends Storable> clazz) {
    Column[] columns = cachedColumns.get(clazz);
    if (columns == null) {
      synchronized (lock) {
        columns = cachedColumns.get(clazz);
        if (columns == null) {
          ArrayList<Column> cols = new ArrayList<Column>();
          ArrayList<Field> fields = new ArrayList<Field>();
          Class<?> klass = clazz;
          while (klass != Storable.class) {
            fields.addAll(Arrays.asList(klass.getDeclaredFields()));
            klass = klass.getSuperclass();
          }
          for (Field f : fields) {
            Column c = f.getAnnotation(Column.class);
            if (c != null) {
              cols.add(c);
            }
          }
          columns = cols.toArray(new Column[0]);
          cachedColumns.put(clazz, columns);
        }
      }
    }
    return columns;
  }


  public static Field getColumn(Column col, Class<? extends Storable> klass2) {
    Class<?> klass = klass2;
    while (klass != Storable.class) {
      Field[] fields = klass.getDeclaredFields();
      for (Field f : fields) {
        Column c = f.getAnnotation(Column.class);
        if (c != null && c.name().equals(col.name())) {
          return f;
        }
      }
      klass = klass.getSuperclass();
    }
    return null;
  }


  private static class FieldGetter {

    private Field f;


    public FieldGetter(Field f) {
      this.f = f;
    }


    public Serializable get(Storable s) {
      try {
        return (Serializable) f.get(s);
      } catch (IllegalAccessException e) {
        throw new RuntimeException("could not access field " + f.getName() + " in " + s);
      } catch (ClassCastException e) {
        throw new RuntimeException("Storable may only contain SerializableS (violating: " + f.getName()
            + " is not serializable)", e);
      }
    }


    public void set(Storable storable, Serializable value) {
      try {
        f.set(storable, value);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException("could not write field " + f.getName() + " in " + storable + ". value was "
            + (value == null ? "null" : "(" + value.getClass().getCanonicalName() + ") " + value));
      } catch (IllegalAccessException e) {
        throw new RuntimeException("could not access field " + f.getName() + " in " + storable, e);
      }
    }

  }
  
  
  private static class GetterMethod {

    private Method m;


    public GetterMethod(Method m) {
      this.m = m;
    }


    public Serializable get(Storable s) {
      try {
          return (Serializable) m.invoke(s);
      } catch (IllegalArgumentException e) {
        throw new RuntimeException("illegal argument while calling getter without arguments");
      } catch (InvocationTargetException e) {
        throw new RuntimeException("exception while calling getter: " + m.getName());
      } catch (IllegalAccessException e) {
        throw new RuntimeException("could not access method " + m.getName() + " in " + s);
      } catch (ClassCastException e) {
        throw new RuntimeException("Storable may only contain SerializableS (violating: " + m.getName()
            + " is not serializable)", e);
      }
    }

  }

  public Serializable getValueByColName(Column col) {
    return getValueByColString(col.name());
  }


  /**
   * default = false.
   * wenn true zurückgegeben wird, verwendet {@link #getValueByColString(String)} die getter
   * (via {@link #getValueByColStringViaGetter(String)}, ansonsten wird direkt per 
   * reflection auf die felder zugegriffen.
   */
  protected boolean useGettersToAccessColValues() {
    return false;
  }
  
  public void setValueByColumnName(String columnName, Serializable value) {
    Map<String, FieldGetter> m = getters.get(getClass());
    if (m == null) {
      synchronized (lock) {
        m = getters.get(getClass());
        if (m == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("initializing getters for fields in " + this);
          }
          Map<String, FieldGetter> temp = new HashMap<String, FieldGetter>();

          ArrayList<Field> fields = new ArrayList<Field>();
          Class<?> klass = getClass();
          while (klass != Storable.class) {
            fields.addAll(Arrays.asList(klass.getDeclaredFields()));
            klass = klass.getSuperclass();
          }
          for (final Field f : fields) {
            Column c = f.getAnnotation(Column.class);
            if (c != null) {
              if (!f.isAccessible()) {
                f.setAccessible(true);
              }
              temp.put(c.name(), new FieldGetter(f));
            }
          }
          getters.put(getClass(), temp);
          m = temp;
        }
      }
    }
    m.get(columnName).set(this, value);
  }

  public Serializable getValueByColString(String columnName) {
    if (useGettersToAccessColValues()) {
      return getValueByColStringViaGetter(columnName);
    }
    Map<String, FieldGetter> m = getters.get(getClass());
    if (m == null) {
      synchronized (lock) {
        m = getters.get(getClass());
        if (m == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("initializing getters for fields in " + this);
          }
          Map<String, FieldGetter> temp = new HashMap<String, FieldGetter>();

          ArrayList<Field> fields = new ArrayList<Field>();
          Class<?> klass = getClass();
          while (klass != Storable.class) {
            fields.addAll(Arrays.asList(klass.getDeclaredFields()));
            klass = klass.getSuperclass();
          }
          for (final Field f : fields) {
            Column c = f.getAnnotation(Column.class);
            if (c != null) {
              if (!f.isAccessible()) {
                f.setAccessible(true);
              }
              temp.put(c.name(), new FieldGetter(f));
            }
          }
          getters.put(getClass(), temp);
          m = temp;
        }
      }
    }
    return m.get(columnName).get(this);
  }
  
  
  public Serializable getValueByColStringViaGetter(String columnName) {
    Map<String, GetterMethod> m = getterMethods.get(getClass());
    if (m == null) {
      synchronized (lock) {
        m = getterMethods.get(getClass());
        if (m == null) {
          if (logger.isDebugEnabled()) {
            logger.debug("initializing getters for fields in " + this);
          }
          Map<String, GetterMethod> temp = new HashMap<String, GetterMethod>();
          
          Class<?> klass = getClass();
          while (klass != Storable.class) {
            List<Field> fields = Arrays.asList(klass.getDeclaredFields());
            for (Field field : fields) {
              Column c = field.getAnnotation(Column.class);
              if (c != null) {
                String colName = c.name();
                List<Method> methods = Arrays.asList(klass.getDeclaredMethods());
                for (Method method : methods) {
                  if (method.getParameterTypes().length == 0 &&
                      method.getName().equalsIgnoreCase("get"+colName)) {
                    if (!method.isAccessible()) {
                      method.setAccessible(true);
                    }
                    temp.put(colName, new GetterMethod(method));
                  }
                }
              }
            }
            klass = klass.getSuperclass();
          }
          getterMethods.put(getClass(), temp);
          m = temp;
        }
      }
    }
    return m.get(columnName).get(this);
  }


  protected static byte[] readByteArrayDirectlyFromResultSet(ResultSet rs, String colName) throws SQLException {
    try {
      Blob blob = rs.getBlob(colName);
      if (rs.wasNull()) {
        return null;
      }
      InputStream is = blob.getBinaryStream();
      try {
        int available = is.available();
        if (available == 0)
          return new byte[0];
        byte[] ret = new byte[available];
        if (is.read(ret) < available) {
          throw new SQLException("could not read all bytes from blob");
        }
        return ret;
      } catch (IOException e) {
        throw (SQLException) new SQLException("problem reading blob from resultset in column " + colName).initCause(e);
      } finally {
        try {
          blob.free();
        } catch (Exception e) {
          try {
            is.close();
          } catch (IOException e1) {
            logger.info(null, e);
          }
        }
      }
    } catch (SQLException e) {
      //FIXME Workaround für memory implementierung
      if (e.getMessage().equals(UnsupportingResultSet.UNSUPPORTED_MESSAGE)) {
        if (logger.isTraceEnabled()) {
          logger.trace("readByteArrayDirectlyFromResultSet failed. this is normal for objects in memory persistence layer.", e);
        }
        return (byte[]) rs.getObject(colName);
      } else {
        throw e;
      }
    }
  }


  protected Object readBlobbedJavaObjectFromResultSet(ResultSet rs, String colName) throws SQLException {
    return readBlobbedJavaObjectFromResultSet(rs, colName, null, false);
  }
  
  protected Object readBlobbedJavaObjectFromResultSet(ResultSet rs, String colName, String id) throws SQLException {
    return readBlobbedJavaObjectFromResultSet(rs, colName, id, false);
  }
  
  protected Object readBlobbedJavaObjectFromResultSet(ResultSet rs, String colName, String id, boolean preferNoDeserialization) throws SQLException {
    try {
      Blob blob = rs.getBlob(colName);
      if (rs.wasNull()) {
        return null;
      }
      InputStream is = blob.getBinaryStream();
      if (preferNoDeserialization) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        try {
          while (-1 != (n = is.read(buf))) {
            baos.write(buf, 0, n);
          }
        } catch (IOException e) {
          String msg = "could not read " + colName + (id != null ? " (" + id + ")" : "") + " from blob";
          throw (SQLException) new SQLException(msg).initCause(e);
        }
        return baos.toByteArray();
      } else {
        try {
          return deserializeByColName(colName, is);
        } catch (RuntimeException e) {
          String msg = "could not read " + colName + (id != null ? " (" + id + ")" : "") + " from blob";
          throw (SQLException) new SQLException(msg).initCause(e);
        } catch (IOException e) {
          String msg = "could not read " + colName + (id != null ? " (" + id + ")" : "") + " from blob";
          throw (SQLException) new SQLException(msg).initCause(e);
        } finally {
          try {
            blob.free();
          } catch (Exception e) {
            try {
              is.close();
            } catch (IOException e1) {
              logger.info(null, e);
            }
          }
        }
      }
    } catch (SQLException e) {
      //FIXME Workaround für memory implementierung
      if (UnsupportingResultSet.UNSUPPORTED_MESSAGE.equals(e.getMessage())) {
        if (logger.isTraceEnabled()) {
          logger.trace("readBlobbedJavaObjectFromResultSet failed. this is normal "
              + "for objects in some persistence layer implementations. going to try rs.getObject().", e);
        }
        return rs.getObject(colName);
      } else {
        throw e;
      }
    }
  }
   
  /**
   * Object val soll in OutputStream os geschrieben werden.
   * Die Serialisierung kann in abgeleiteten Klassen pro Spalte colName individuell angepasst werden.
   */
  public void serializeByColName( String colName, Object val, OutputStream os) throws IOException {
    ObjectOutputStream oos = getObjectOutputStreamForStorable(os);
    if (!(val instanceof SerializableClassloadedObject)) {
      val = new SerializableClassloadedObject((Serializable) val);
    }
    oos.writeObject(val);
    oos.flush();
  }
  
  /**
   * Object soll aus InputStream is gelesen und zurückgegeben werden.
   * Die Serialisierung kann in abgeleiteten Klassen pro Spalte colName individuell angepasst werden.
   */
  public Object deserializeByColName( String colName, InputStream is) throws IOException {
    try {
      ObjectInputStream ois = getObjectInputStreamForStorable(is); //der objectinputstream kann damit überschrieben werden
      Object o = ois.readObject();
      if (o instanceof SerializableClassloadedObject) {
        o = ((SerializableClassloadedObject) o).getObject();
      }
      return o;
    } catch (ClassNotFoundException e) {
      throw (IOException) new IOException("could not resolve classname from stored object").initCause(e);
    }
  }
 
  //folgende methoden könnten auch hier definiert sein und per reflection herausfinden, wie das command auszusehen hat (cachen)
  /**
   * @return wert des pks
   */
  public abstract Object getPrimaryKey();


  /**
   * kopiert werte aus data in aktuelles objekt. dazu muss data mindestens die gleichen felder besitzen, kann also eine
   * abgeleitete klasse sein.
   */
  public abstract <U extends T> void setAllFieldsFromData(U data);


  /**
   * von storables kann das überschrieben werden, zb für updates durch {@link UpdateObjectInputStream} wird von
   * {@link #readBlobbedJavaObjectFromResultSet(ResultSet, String)} benutzt, um einzelne spalten aus dem resultset als
   * blob zu lesen.
   * @return überschriebener stream
   */
  public ObjectInputStream getObjectInputStreamForStorable(InputStream in) throws IOException {
    if (in instanceof ObjectInputStream) {
      return (ObjectInputStream)in;
    }
    return new ObjectInputStream(in);
  }
  
  
  public ObjectOutputStream getObjectOutputStreamForStorable(OutputStream out) throws IOException {
    if (out instanceof ObjectOutputStream) {
      return (ObjectOutputStream)out;
    }
    return new ObjectOutputStream(out);
  }


  public static void clearCache(Class<? extends Storable> tableClass) {
    synchronized (lock) {
      cachedColumns.remove(tableClass);
      cachedCompositeIndices.remove(tableClass);
      cachedPersistables.remove(tableClass);
      getters.remove(tableClass);
      getterMethods.remove(tableClass);
      cachedResultSetReaders.remove(tableClass);
    }
  }
  
    
}
