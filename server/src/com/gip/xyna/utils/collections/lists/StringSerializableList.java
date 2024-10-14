/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.utils.collections.lists;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.WrappedList;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.misc.CsvUtils;
import com.gip.xyna.utils.misc.StringTransformation;


/**
 * StringSerializableList ist eine Implementation von {@link java.util.List List} und 
 * {@link com.gip.xyna.utils.db.types.StringSerializable StringSerializable}, die in der 
 * Datenbank als String abgespeichert wird und beim Auslesen aus der DB wieder in eine Liste
 * verwandelt wird.<br>
 * <ul>
 * <li>Intern werden die Daten in einer ArrayList oder der im Konstruktor übergebenen List 
 *     gespeichert.</li>
 * <li>Eintragen von null ist nicht erlaubt, da dies Probleme beim Deserialisieren macht. Beim
 *     Versuch, null im Konstruktor oder über <code>add</code> einzutragen, wird eine 
 *     IllegalArgumentException geworfen.</li>
 * <li>Um die Listenwerte in einzelne Strings und umgekehrt zu verwandeln, werden die 
 *     Transformationen aus {@link com.gip.xyna.utils.misc.StringTransformation StringTransformation}
 *     verwendet.</li>
 * <li>Um die einzelnen Strings zu einem String zusammenzufassen bzw. einen String wieder zu 
 *     EinzelStrings aufzuspalten, wird eine Implementierung des Interfaces 
 *     {@link com.gip.xyna.utils.collections.lists.StringSerializableList.SerializeAlgorithm SerializeAlgorithm}
 *     verwendet. Drei Implementierungen sind bereits vorhanden, weitere könnten im Konstruktor 
 *     übergeben werden:
 *     <ul>
 *     <li>SeparatorSerializeAlgorithm ist eine zur bisherigen StringSerializableList aus 
 *         com.gip.xyna.utils.db.types kompatible Implementierung mit einem festen Trennstring (default ", ").
 *         Die einzelnen Listenwerte dürfen daher beim Umwandeln in einen String diesen 
 *         Trennstring nicht produzieren!</li>
 *     <li>CSVSerializeAlgorithm erzeugt einen CSV-String, der mit beliebigen 
 *         Listenwerten (außer null) umgehen kann.</li>
 *     <li>AutoSeparatorSerializeAlgorithm sucht sich selbständig ein Trennzeichen, welches nicht 
 *         in den umgewandelten Strings der Listenelemente vorkommt.</li>
 *     </ul></li>
 * <li>Um den komplizierten Konstruktor nicht verwenden zu müssen gibt es statische Methoden:
 *    <ul>
 *    <li>separator(Class&lt;E&gt; entryClass)</li>
 *    <li>separator(Class&lt;E&gt; entryClass, String separator)</li>
 *    <li>csv(Class&lt;E&gt; entryClass)</li>
 *    <li>autoSeparator(Class&lt;E&gt; entryClass)</li>
 *    <li>autoSeparator(Class&lt;E&gt; entryClass, String allAllowedSeparators)</li>
 *    <li>autoSeparator(Class&lt;E&gt; entryClass, String allAllowedSeparators, char defaultSeparator)</li>
 *    </ul></li>
 * <li>Zur Umgehung der hier genannten Einschränkungen wäre die direkte Implementierung von 
 *     {@link com.gip.xyna.utils.db.types.StringSerializable StringSerializable} die Alternative.</li>
 * </ul> 
 * <br>
 * <pre>
 *   //Beispielhafte Verwendung in den Storables:
 *   Column(name = COLUMN_CONFIGURATION, size=500)
 *   private final StringSerializableList&lt;String&gt; configuration = StringSerializableList.autoSeparator(String.class);
 * 
 *   public &lt;U extends ExampleStorable&gt; void setAllFieldsFromData(U data) {
 *     ExampleStorable cast = data;
 *     configuration.setValues(cast.getConfiguration()); //oder auch addAll, da noch leer
 *   }
 *   private static void setAllFieldsFromResultSet(ExampleStorable target, ResultSet rs) throws SQLException {
 *     target.configuration.deserializeFromString(rs.getString(COLUMN_CONFIGURATION));
 *   }
 *   public void setConfiguration(List&lt;String&gt; configuration) {
 *     this.configuration.setValues(configuration);
 *   }
 * </pre>
 * 
 */
public class StringSerializableList<E> extends WrappedList<E> implements StringSerializable<StringSerializableList<E>> {
  
  private static final long serialVersionUID = 1L;
  private final SerializeAlgorithm serializeAlgorithm;
  private transient Transformation<E,String> toStringTransformation;
  private transient Transformation<String,E> toValueTransformation;
  private final Class<E> entryClass;
  

  public StringSerializableList( Class<E> entryClass, List<E> entries, 
      SerializeAlgorithm serializeAlgorithm) {
    super(entries);
    this.entryClass = entryClass;
    this.serializeAlgorithm = serializeAlgorithm;
    this.toStringTransformation = StringTransformation.toString(entryClass);
    this.toValueTransformation = StringTransformation.toValue(entryClass);
  }
  
  /*
  Vor Aktivierung dieses Konstruktors muss geklärt werden, wie die Serialierung in readObject wieder die 
  Transformationen wiederherstellen kann!
  public StringSerializableList( List<E> entries, 
      SerializeAlgorithm serializeAlgorithm, Transformation<E,String> toStringTransformation, Transformation<String,E> toValueTransformation) {
    super(entries);
    this.entryClass = null;
    this.serializeAlgorithm = serializeAlgorithm;
    this.toStringTransformation = toStringTransformation;
    this.toValueTransformation = toValueTransformation;
  }
  */
  
  public static <T> StringSerializableList<T> csv(Class<T> entryClass) {
    return new StringSerializableList<>(entryClass, new ArrayList<T>(), new CSVSerializeAlgorithm() );
  }
  
  public static <T> StringSerializableList<T> separator(Class<T> entryClass) {
    return new StringSerializableList<>(entryClass, new ArrayList<T>(), new SeparatorSerializeAlgorithm() );
  }
  public static <T> StringSerializableList<T> separator(Class<T> entryClass, String separator) {
    return new StringSerializableList<>(entryClass, new ArrayList<T>(), new SeparatorSerializeAlgorithm(separator) );
  }
  
  public static <T> StringSerializableList<T> autoSeparator(Class<T> entryClass ) {
    return new StringSerializableList<>(entryClass, new ArrayList<T>(), new AutoSeparatorCharSerializeAlgorithm() );
  }
  public static <T> StringSerializableList<T> autoSeparator(Class<T> entryClass, String allAllowedSeparators) {
    return new StringSerializableList<>(entryClass, new ArrayList<T>(), new AutoSeparatorCharSerializeAlgorithm(allAllowedSeparators) );
  }
  public static <T> StringSerializableList<T> autoSeparator(Class<T> entryClass, String allAllowedSeparators, char defaultSeparator ) {
    return new StringSerializableList<>(entryClass, new ArrayList<T>(), new AutoSeparatorCharSerializeAlgorithm(allAllowedSeparators, defaultSeparator) );
  }
  
  @Override
  public String serializeToString() {
    List<String> strings = CollectionUtils.transform(wrapped, toStringTransformation );
    return serializeAlgorithm.serialize(strings);
  }

  @Override
  public StringSerializableList<E> deserializeFromString(String string) {
    List<String> strings = serializeAlgorithm.deserialize(string);
    wrapped.clear();
    CollectionUtils.transform(strings, toValueTransformation, wrapped);
    return this;
  }

  public StringSerializableList<E> setValues(Collection<E> values) {
    clear();
    addAll(values);
    return this;
  }
  
  @Override
  public boolean equals(Object obj) {
    return super.equals(obj); //nur Listenwerte gehen in equals und hashCode ein
  }
  
  @Override
  public int hashCode() {
    return super.hashCode();
  }
  
  private void readObject(java.io.ObjectInputStream stream)
      throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    this.toStringTransformation = StringTransformation.toString(entryClass);
    this.toValueTransformation = StringTransformation.toValue(entryClass);
  }

  

  /**
   * Serialisieren einer String-Liste nach String und Deserialisieren von String zur String-Liste.
   *
   */
  public interface SerializeAlgorithm extends Serializable {
    /**
     * Serialisieren der strings in einen String
     * @param strings
     * @return
     */
    String serialize(List<String> strings);
    
    /**
     * Deseralisieren des Strings in eine String-Liste
     * @param string
     * @return
     */
    List<String> deserialize(String string);
  }
  
  /**
   * Serialisieren einer String-Liste nach String und Deserialisieren von String zur String-Liste
   * mit einem festen Trennstring.
   * Die einzelnen Listenwerte dürfen daher diesen Trennstring nicht enthalten!
   */
  public static class SeparatorSerializeAlgorithm implements SerializeAlgorithm {
    private static final long serialVersionUID = 1L;
    private final String separator;
    private final Pattern splitPattern;
    
    public SeparatorSerializeAlgorithm() {
      this(", ");
    }
    public SeparatorSerializeAlgorithm(String separator) {
      this.separator = separator;
      this.splitPattern = Pattern.compile("\\Q"+separator+"\\E");
    }
    @Override
    public String serialize(List<String> strings) {
      StringBuilder sb = new StringBuilder();
      String sep = "";
      for(String s : strings ) {
        sb.append(sep).append(s);
        sep = separator;
      }
      return sb.toString();
    }

    @Override
    public List<String> deserialize(String string) {
      if( string == null || string.isEmpty() ) {
        return Collections.emptyList();
      }
      return Arrays.asList(splitPattern.split(string));
    }
    
  }
  
  /**
   * Serialisieren einer String-Liste nach String und Deserialisieren von String zur String-Liste
   * mit einem automatisch ermittelten Trennzeichen, welches nicht in den Listenelemente vorkommt.
   */
  public static class AutoSeparatorCharSerializeAlgorithm implements SerializeAlgorithm {
    private static final long serialVersionUID = 1L;
    
    private final String allAllowedSeparators;
    private final char defaultSeparator;

    public AutoSeparatorCharSerializeAlgorithm() {
      this.allAllowedSeparators = ":|/;\\@-_.+#=[]?$%&!";
      this.defaultSeparator = 0;
    }
    public AutoSeparatorCharSerializeAlgorithm(String allAllowedSeparators) {
      this.allAllowedSeparators = allAllowedSeparators;
      this.defaultSeparator = 0;
    }
    public AutoSeparatorCharSerializeAlgorithm(String allAllowedSeparators, char defaultSeparator) {
      this.allAllowedSeparators = allAllowedSeparators;
      this.defaultSeparator = defaultSeparator;
    }
    
    @Override
    public String serialize(List<String> strings) {
      char separator = findSeparator(strings);
      StringBuilder sb = new StringBuilder();
      for(String s : strings ) {
        sb.append(s).append(separator);
      }
      return sb.toString();
    }
    
    @Override
    public List<String> deserialize(String string) {
      if( string == null || string.isEmpty() ) {
        return Collections.emptyList();
      }
      char separator = findSeparator(string);
      Pattern splitPattern = Pattern.compile("\\Q"+separator+"\\E");
      return Arrays.asList( splitPattern.split(string) );
    }

    private char findSeparator(List<String> strings) {
      for (int i =0; i<allAllowedSeparators.length(); ++i ) {
        char sepChar = allAllowedSeparators.charAt(i);
        boolean foundChar = false;
        if (strings != null) {
          for (String s : strings) {
            if (s.indexOf(sepChar) > -1) {
              foundChar = true;
              break;
            }
          }
        }
        if (!foundChar) {
          return sepChar;
        }
      }
      throw new IllegalArgumentException("didn't find any possible separator character.");
    }
    
    private char findSeparator(String string) {
      char lastChar = string.charAt(string.length()-1);
      if( allAllowedSeparators.indexOf(lastChar) > -1 ) {
        return lastChar;
      }
      if( defaultSeparator != 0 ) {
        return defaultSeparator;
      }
      throw new IllegalArgumentException("didn't find any possible separator character.");
    }
    
  }

  /**
   * Serialisieren einer String-Liste nach String und Deserialisieren von String zur String-Liste
   * als CSV.
   */
  public static class CSVSerializeAlgorithm implements SerializeAlgorithm {
    private static final long serialVersionUID = 1L;

    private final String separator;
    private String masker;
    
    public CSVSerializeAlgorithm() {
      this(CsvUtils.CSV_SEPARATOR, CsvUtils.CSV_MASKER);
    }
    
    public CSVSerializeAlgorithm(String separator, String masker) {
      this.separator = separator;
      this.masker = masker;
    }

    @Override
    public String serialize(List<String> strings) {
      return CsvUtils.toCSV(strings, separator, masker);
    }

    @Override
    public List<String> deserialize(String string) {
      if( string == null || string.isEmpty() ) {
        return Collections.emptyList();
      }
      List<String> list = new ArrayList<>();
      for (String s : new CsvUtils.CSVIterable(string, separator, masker) ) {
        list.add(s);
      }
      return list;
    }
    
  }
  
}
