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

package com.gip.xyna.xfmg.xods.configuration;

import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.sets.WeakHashSet;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xnwh.exceptions.XNWH_EncryptionException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.securestorage.SecureStorage;

public class XynaPropertyUtils {
  
  /**
   * Interface, über das XynaPropertyUtils unabhängig wird von der Existenz einer laufenden XynaFactory.
   * Die Datenquelle der XynaProperties kann damit unabhängig implementiert werden. 
   * Über XynaPropertyUtils.exchangeXynaPropertySource(new XynaPropertySource(){...} ); kann dann die Datenquelle 
   * der Properties konfiguriert werden. 
   * Besser AbstractXynaPropertySource verwenden!
   */
  public interface XynaPropertySource {
    void registerDependency(XynaPropertyBase<?,?> property, String user);
    
    void registerDependency(XynaPropertyBase<?,?> property, UserType userType, String name);

    void register(XynaPropertyBase<?,?> property);

    void unregister(XynaPropertyBase<?,?> property);

    Set<String> getRegisteredPropertyNames();

    Set<XynaPropertyBase<?,?>> getRegisteredProperties(String name);

    String getProperty(String name);
    
    void setProperty(XynaPropertyBase<?,?> property, String newValue) throws PersistenceLayerException;
    
    void definitionChanged(XynaPropertyBase<?,?> property);
  }
  
  public static enum UserType {
    XynaFactory,
    Service,
    Workflow,
    Filter,
    Trigger,
    Plugin,
    Extern,
    Other,
    ;
  }
  
  
  /**
   * Weitgehende Implementierung des Interface XynaPropertySource, so dass alle Methoden außer 
   * {@link XynaPropertySource#getProperty(String)} für die meisten Zwecke ausreichend implementiert sind.  
   *
   */
  public static abstract class AbstractXynaPropertySource implements XynaPropertySource {
    protected ConcurrentHashMap<String, Set<XynaPropertyBase<?,?>>> properties = 
        new ConcurrentHashMap<String, Set<XynaPropertyBase<?,?>>>();
    
    public void register(XynaPropertyBase<?,?> property) {
      String propName = property.getPropertyName();
      do {
        Set<XynaPropertyBase<?,?>> existingSet = properties.get(propName);
        
        if( existingSet == null ) {
          Set<XynaPropertyBase<?,?>> newSet = new WeakHashSet<XynaPropertyBase<?,?>>(1);
          newSet.add(property);
          existingSet = properties.putIfAbsent(propName, Collections.synchronizedSet(newSet) );
          if( existingSet == null ) {
            break;
          }
        }
        
        synchronized (existingSet) {
          existingSet.add(property);
          if( properties.replace(propName, existingSet, existingSet) ) {
            break;
          }
        }
      } while(true);  
    }
 
    public void unregister(XynaPropertyBase<?,?> property) {
      String propName = property.getPropertyName();
      do {
        Set<XynaPropertyBase<?,?>> existingSet = properties.get(propName);
        if( existingSet == null ) {
          break; //sollte nicht passieren, aber ok
        }
        
        synchronized (existingSet) {
          existingSet.remove(property);
          if( existingSet.isEmpty() ) {
            if( properties.remove(propName, existingSet) ) {
              break;
            }
          } else {
            if( properties.replace(propName, existingSet, existingSet) ) {
              break;
            }
          }
        }
      } while(true);
    }
    
    public Set<String> getRegisteredPropertyNames() {
      return Collections.unmodifiableSet(properties.keySet());
    }
   
    public Set<XynaPropertyBase<?,?>> getRegisteredProperties(String name) {
      Set<XynaPropertyBase<?,?>> set = properties.get(name);
      synchronized (set) {
        return new HashSet<>(set);
      }
    }
    
    public void registerDependency(XynaPropertyBase<?,?> property, String user) {
    }
    public void registerDependency(XynaPropertyBase<?,?> property, UserType userType, String name) {
    }
    
    public void setProperty(XynaPropertyBase<?,?> property, String newValue) throws PersistenceLayerException {
      throw new UnsupportedOperationException("unsupported set of xyna property");
    }
  
    public void definitionChanged(XynaPropertyBase<?, ?> property) {
      //ignorieren
    }
  
  }
  
  private static XynaPropertySource xynaPropertySource = new XynaPropertySourceDefault();

  /**
   * Austausch der XynaPropertySource. Dabei wird darauf geachtet, dass die Speicherung 
   * der Constructed-Properties erhalten bleibt.
   * @param newSource
   */
  public static void exchangeXynaPropertySource(XynaPropertySource newSource) {
    XynaPropertySource oldSource = xynaPropertySource;
    xynaPropertySource = newSource;
    //Achtung: unsicher gegen zeitgleiche Registrierungen in der alten Source
    for( String name : oldSource.getRegisteredPropertyNames() ) {
      for( XynaPropertyBase<?,?> property : oldSource.getRegisteredProperties(name) ) {
        newSource.register(property);
        property.propertyChanged(); //in property gecached-ter wert kann veraltet oder nicht initialisiert sein
      }
    }
  }
  
  public static XynaPropertySource getXynaPropertySource() {
    return xynaPropertySource;
  }
  
  /**
   * Default ohne Factory: 
   */
  public static class XynaPropertySourceDefault extends AbstractXynaPropertySource {
    
    private Map<String,Pair<UserType,String>> dependencies = null;
    private Map<String,String> setValues = new HashMap<String,String>();
    
    public String getProperty(String name) {
      return setValues.get(name);
    }
    
    public void registerDependency(XynaPropertyBase<?,?> property, String name) {
      registerDependency(property, UserType.XynaFactory, name);
    }
    
    public void registerDependency(XynaPropertyBase<?,?> property, UserType userType, String name) {
      if( dependencies == null ) {
        dependencies = new HashMap<String,Pair<UserType,String>>();
      }
      dependencies.put( property.getPropertyName(), Pair.of(userType,name) );
    }
    
    public void setProperty(XynaPropertyBase<?,?> property, String newValue ) throws PersistenceLayerException {
      setValues.put(property.getPropertyName(), newValue);
      for (XynaPropertyBase<?,?> prop : getRegisteredProperties(property.getPropertyName())) {
        prop.propertyChanged(newValue);
      }
    }
    
    public Map<String, Pair<UserType,String>> getDependencies() {
      return Collections.unmodifiableMap(dependencies);
    }
    
    public Map<String, String> getSetValues() {
      return Collections.unmodifiableMap(setValues);
    }
    
  }
  
  
 
  
  /**
   * 
   * FIXME hat nichts mit dem Rest der XynaPropertyUtils zu tun, daher ist der Platz hier völlig ungeeignet!
   * Refactoring leider aufwändig...
   */
  public static class XynaPropertyWithDefaultValue implements Serializable {
    
    private static final long serialVersionUID = -5016929158586338122L;
    
    private String name;
    private String value;
    private String defValue;
    private Map<DocumentationLanguage, String> documentation;
    private Map<DocumentationLanguage, String> defDocumentation;
    
    public XynaPropertyWithDefaultValue(String name, String value, String defValue) {
      this.name = name;
      this.value = value;
      this.defValue = defValue;
      this.documentation = fillDocumentation(null,null);
      this.defDocumentation = fillDocumentation(null,null);
    }
    
    public XynaPropertyWithDefaultValue(String name, String value, String defValue, 
                                        Map<DocumentationLanguage, String> doc, Map<DocumentationLanguage, String> defDoc) {
      this.name = name;
      this.value = value;
      this.defValue = defValue;
      this.documentation = fillDocumentation(doc,null);
      this.defDocumentation = fillDocumentation(defDoc,null);
    }
    
    public XynaPropertyWithDefaultValue(XynaPropertyWithDefaultValue xp) {
      this.name = xp.name;
      this.value = xp.value;
      this.defValue = xp.defValue;
      this.documentation = xp.documentation;
      this.defDocumentation = xp.defDocumentation;
    }

    
    @Override
    public String toString() {
      return "XynaPropertyWithDefaultValue("+name+","+value+","+defValue+")";
    }
    
    private static Map<DocumentationLanguage, String> fillDocumentation(Map<DocumentationLanguage, String> doc1, Map<DocumentationLanguage, String> doc2) {
      EnumMap<DocumentationLanguage, String> map = new EnumMap<DocumentationLanguage, String>(DocumentationLanguage.class);
      if( doc1 != null ) {
        map.putAll(doc1);
      }
      if( doc2 != null ) {
        map.putAll(doc2);
      }
      return Collections.unmodifiableMap( map );
    }
   
    
    public XynaPropertyWithDefaultValue(XynaPropertyBase<?, ?> property) {
      this( property.getPropertyName(), null, property.getDefaultValueAsString(), null, property.getDefaultDocumentation()  );
    }

    public XynaPropertyWithDefaultValue(XynaPropertyStorable storable) {
      this( storable.getPropertyKey(), storable.getPropertyValue(), null, storable.getPropertyDocumentation(), null );
    }

    public String getName() {
      return name;
    }

    public String getValue() {
      return value;
    }

    /**
     * Returns the value if set and the default value otherwise.
     */
    public String getValueOrDefValue() {
      return value != null ? value : defValue;
    }

    public String getDefValue() {
      return defValue;
    }

    public Map<DocumentationLanguage, String> getDocumentation(){
      return documentation;
    }

    public Map<DocumentationLanguage, String> getDefDocumentation(){
      return defDocumentation;
    }
    
    public int hashCode() {
      return name.hashCode();
    }
    
    public boolean equals(Object obj) {
      if(obj instanceof XynaPropertyWithDefaultValue) {
        return ((XynaPropertyWithDefaultValue)obj).name.equals(name);
      }
      return false;
    }
    
    public XynaPropertyWithDefaultValue modifyValue(String newValue) {
      XynaPropertyWithDefaultValue xp = new XynaPropertyWithDefaultValue(name,newValue,defValue);
      xp.documentation = documentation;
      xp.defDocumentation = defDocumentation;
      return xp;
    }
    
    public static XynaPropertyWithDefaultValue combine(XynaPropertyWithDefaultValue first,
                                                       XynaPropertyWithDefaultValue second) {
      
      if( ! first.name.equals(second.name) ) {
        throw new IllegalArgumentException("properties with different names");
      }
      
      String name = first.name;
      String value = second.value == null ? first.value : second.value;
      String defValue = second.defValue == null ? first.defValue : second.defValue;
      
      XynaPropertyWithDefaultValue xp = new XynaPropertyWithDefaultValue(name,value,defValue);
      
      xp.documentation = fillDocumentation(first.getDocumentation(), second.getDocumentation() );
      xp.defDocumentation = fillDocumentation(first.getDefDocumentation(), second.getDefDocumentation() );
      return xp;
    }

    public static XynaPropertyWithDefaultValue changeDefaultValue(XynaPropertyWithDefaultValue old,
                                                                  String newDefValue) {
      XynaPropertyWithDefaultValue xp = new XynaPropertyWithDefaultValue(old);
      xp.defValue = newDefValue;
      return xp;
    }  
    
  }
  
  
  public static abstract class XynaPropertyBase<T,P extends XynaPropertyBase<T,P>>  {
    protected static Logger logger = CentralFactoryLogging.getLogger(XynaProperty.class);
    
    protected String name;
    protected T defValue;
    protected String typename;
    protected Map<DocumentationLanguage, String> defDocumentation;
    protected volatile T lastRead;
    private volatile boolean initialized = false;
    private boolean hidden;
    
    
    
    protected XynaPropertyBase(String name, T defValue, String typename, boolean register ) {
      this.name = name;
      this.defValue = defValue;
      this.typename = typename;
      this.defDocumentation = new EnumMap<DocumentationLanguage, String>(DocumentationLanguage.class);
      if( register ) {
        xynaPropertySource.register(this);
      }
    }
    
    protected XynaPropertyBase(String name, T defValue, boolean register ) {
      this(name,defValue,(defValue==null? "unknown type":defValue.getClass().getSimpleName()), register);
    }
     
    protected XynaPropertyBase(String name, T defValue, String typename ) {
      this.name = name;
      this.defValue = defValue;
      this.typename = typename;
      this.defDocumentation = new EnumMap<DocumentationLanguage, String>(DocumentationLanguage.class);
      xynaPropertySource.register(this);
    }

    @SuppressWarnings("unchecked")
    public P setDefaultDocumentation(DocumentationLanguage lang, String doc) {
      this.defDocumentation.put(lang, doc);
      xynaPropertySource.definitionChanged(this);
      return (P)this;
    }

    @SuppressWarnings("unchecked")
    public P setHidden(boolean hidden) {
      this.hidden = hidden;
      xynaPropertySource.definitionChanged(this);
      return (P)this;
    }
    
    public boolean isHidden() {
      return hidden;
    }
    
    public String getPropertyName() {
      return name;
    }

    public void propertyChanged() {
      lastRead = read();
    }
    
    public T getDefaultValue() {
      return defValue;
    }
    
    public Map<DocumentationLanguage, String> getDefaultDocumentation(){
      return defDocumentation;
    }
    
    /**
     * Liest die XynaProperty einmalig und gibt diese als Typ <code>&lt;T&gt;</code> zurück.
     * @return
     */
    public T readOnlyOnce() {
      lastRead = readInternal();
      return lastRead;
    }
    
    //package private, setzt den neuen Wert
    void propertyChanged(String newPropVal) {
      lastRead = readInternal(newPropVal);
      initialized = true;
    }
    
    /**
     * Liest die XynaProperty und gibt diese als Typ <code>&lt;T&gt;</code> zurück
     * @return
     */
    public T read() {
      lastRead = readInternal();
      initialized = true;
      return lastRead;
    }
    
    protected T readInternal() {
      return readInternal(readPropVal());
    }
  
    protected T readInternal(String propVal) {
      if (propVal == null) {
        if (logger.isDebugEnabled()) {
          logger.debug("Property <"+name+"> not set; using default <"+defValue+">");
        }
        return defValue;
      } else {
        try {
          return fromString(propVal);
        } catch (Exception e) {
          if (logger.isInfoEnabled()) {
            logger.info("Property <"+name+"> not correctly set: expecting type <"+typename+">, got string <"+propVal
                + ">; using default <" + defValue+">", e);
          }
          return defValue;
        }
      }
    }
    
    protected abstract T fromString(String string) throws Exception;
    
    /**
     * Gibt den zuletzt gelesenen, gecachten Wert der XynaProperty als Typ <code>&lt;T&gt;</code> zurück. 
     * Der Wert wird aktualisiert, wenn sich die XynaProperty ändert.
     * @return
     */
    public T get() {
      if (!initialized) {
        return read();
      }
      return lastRead;
    }
    
    @Override
    public String toString() {
      return name;
    }
    
    protected String readPropVal() {
      return xynaPropertySource.getProperty(name);
    }
    
    protected String readPropValNotNull() {
      String propVal = xynaPropertySource.getProperty(name);
      if (propVal != null) {
        return propVal;
      } else {
        if (logger.isDebugEnabled()) {
          logger.debug("property "+name+" not set. using default value = "+defValue);
        }
        return getDefaultValueAsString();
      }
    }

    protected String getDefaultValueAsString() {
      if( defValue == null ) {
        return null;
      } else {
        return String.valueOf(defValue);
      }
    }

    /**
     * @deprecated use unregister() instead
     */
    @Deprecated
    public void clearPropertyChangeListener() {
      initialized = false;
      xynaPropertySource.unregister(this);
    }
    
    public void unregister() {
      initialized = false;
      xynaPropertySource.unregister(this);
    }

    /**
     * Registrierung, dass XynaProperty von user verwendet wird
     * @param user
     */
    @Deprecated
    public void registerDependency(String user) {
      xynaPropertySource.registerDependency(this, user);
    }
    
    /**
     * Registrierung, dass XynaProperty von userType/name verwendet wird
     * @param userType
     * @param name
     */
    public void registerDependency(UserType userType, String name) {
      xynaPropertySource.registerDependency(this, userType, name);
    }

    public void set( T value ) throws PersistenceLayerException {
      setString(String.valueOf(value));
    }
    
    protected void setString(String value) throws PersistenceLayerException {
      xynaPropertySource.setProperty(this, value);
      lastRead = readInternal(); //zur Sicherheit nochmal lesen
    }
    
    public void validate(String value) throws XFMG_IllegalPropertyValueException {
      try {
        fromString(value);
      } catch( Exception e ) {
        throw new XFMG_IllegalPropertyValueException(name,value,e); 
      }
    }

  }

  public static class XynaPropertyDouble extends XynaPropertyBase<Double, XynaPropertyDouble> {
    
    public XynaPropertyDouble(String name, Double defValue) {
      super(name, defValue, "double");
    }
    
    protected Double fromString(String string) throws Exception {
      return Double.valueOf(string);
    }
  }

  public static class XynaPropertyInt extends XynaPropertyBase<Integer,XynaPropertyInt> {

    public XynaPropertyInt(String name, Integer defValue) {
      super(name, defValue, "int");
    }
    
    protected Integer fromString(String string) throws Exception {
      return Integer.valueOf(string);
    }
  }

  public static class XynaPropertyLong extends XynaPropertyBase<Long,XynaPropertyLong> {

    public XynaPropertyLong(String name, Long defValue) {
      super(name, defValue, "long");
    }

    protected Long fromString(String string) throws Exception {
      return Long.valueOf(string);
    }

  }
  
  public static class XynaPropertyBoolean extends XynaPropertyBase<Boolean,XynaPropertyBoolean> {

    public XynaPropertyBoolean(String name, Boolean defValue) {
      super(name, defValue, "boolean");
    }

    protected Boolean fromString(String string) throws Exception {
      if( string.equalsIgnoreCase("true") ) {
        return Boolean.TRUE;
      } else if( string.equalsIgnoreCase("false") ) {
        return Boolean.FALSE;
      } else {
        throw new IllegalArgumentException("\""+string+"\" is neither \"true\" nor \"false\"" );
      }
    }
    
  }

  public static class XynaPropertyString extends XynaPropertyBase<String,XynaPropertyString> {

    private boolean nullable = false;
    
    public XynaPropertyString(String name, String defValue, boolean nullable) {
      this(name, defValue);
      this.nullable = nullable;
    }
    
    public XynaPropertyString(String name, String defValue) {
      super(name, defValue, "String");
    }

    protected String readInternal() {
      if (nullable) {
        return super.readInternal();
      } else {
        return readPropValNotNull();
      }
    }
    
    protected String fromString(String string) throws Exception {
      return string;
    }
    
  }
  

  public static class XynaPropertyEnum<E extends Enum<E>> extends XynaPropertyBase<E,XynaPropertyEnum<E>> {
    
    private Class<E> enumType;
    
    public XynaPropertyEnum(String name, Class<E> enumType, E defValue) {
      super(name, defValue, "enum "+enumType.getSimpleName());
      this.enumType = enumType;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> XynaPropertyEnum<E> construct(String name, E defValue) {
      if( defValue == null ) {
        throw new IllegalArgumentException("construct need non null defValue");
      }
      return new XynaPropertyEnum<E>(name, (Class<E>)defValue.getClass(), defValue);
    }

    protected E fromString(String string) throws Exception {
      return Enum.valueOf(enumType, string);
    }
    
  }
  
  public static class XynaPropertyUnreadableString extends XynaPropertyBase<String,XynaPropertyUnreadableString> {

    public XynaPropertyUnreadableString(String name, String defValue) {
      super(name, defValue, "unreadableString");
    }

    protected String fromString(String string) throws Exception {
      return SecureStorage.staticDecrypt( getPropertyName(), string);
    }

    @Override
    public void set(String value) throws PersistenceLayerException {
      try {
        String encrypted = SecureStorage.staticEncrypt( getPropertyName(), value);
        super.set(encrypted);
      } catch (XNWH_EncryptionException e) {
        throw new RuntimeException(e);
      }
    }
    
  }

  public static class XynaPropertyBuilds<T> extends XynaPropertyBase<T,XynaPropertyBuilds<T>> {

    public interface Builder<T> {
      T fromString(String string) throws ParsingException;
      String toString(T value);
      
      public static class ParsingException extends Exception {
        private static final long serialVersionUID = 1L;
        public ParsingException() { super(); }
        public ParsingException(String message) { super(message); }
        public ParsingException(Throwable t) { super(t); }
        public ParsingException(String message, Throwable t) { super(message, t); }
      }
    }

    private Builder<T> builder;
    
    public XynaPropertyBuilds(String name, Builder<T> builder, String defValue) throws Builder.ParsingException {
      super(name,builder.fromString(defValue),false);
      this.builder = builder;
      xynaPropertySource.register(this);
    }
    
    public XynaPropertyBuilds(String name, Builder<T> builder, T defValue) {
      super(name,defValue,false);
      this.builder = builder;
      xynaPropertySource.register(this);      
    }
    
    /**
     * Konstruktor für StringSerializable-Objekte, keine Angabe des Builders nötig
     * @param name
     * @param defValue
     * @throws IllegalArgumentException wenn defValue null oder nicht stringSerializable ist
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public XynaPropertyBuilds(String name, T defValue) {
      super(name,defValue,false);
      if( !( defValue instanceof StringSerializable) ) {
        throw new IllegalArgumentException("This constructor must be called with stringSerializable non-null object");
      }
      this.builder = new StringSerializableBuilder((StringSerializable<T>)defValue);
      xynaPropertySource.register(this);
    }
    
    /**
     * Builder, der StringSerializable Objekte erzeugen kann 
     *
     * @param <S>
     */
    private static class StringSerializableBuilder<S extends StringSerializable<S>> implements Builder<S> {

      private S example;

      public StringSerializableBuilder(S example) {
        this.example = example;
      }

      public S fromString(String string)
          throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
        return example.deserializeFromString(string);
      }

      public String toString(S value) {
        return value.serializeToString();
      }
      
    }
    
    @Override
    protected T fromString(String string) throws Exception {
      return builder.fromString(string);
    }
        
    @Override
    public void set( T value ) throws PersistenceLayerException {
      setString( builder.toString(value) );
    }
    
    @Override
    protected String getDefaultValueAsString() {
      return builder.toString(defValue);
    }
    
  }

  /**
   * XynaProperty für Zeitdauern, mit wählbarer Zeiteinheit (Sekunde, Millisekunde etc.)
   *
   */
  public static class XynaPropertyDuration extends XynaPropertyBase<Duration,XynaPropertyDuration> {
    
    private TimeUnit defaultTimeUnit;
    private Duration durationInMillis; //In den meisten Fällen wird die Zeit in ms benötigt, dies spart die Extra-Umrechnung bei jedem getMillis()-Aufruf 
    
    public XynaPropertyDuration(String name, Duration duration, TimeUnit defaultTimeUnit ) {
      super(name, duration, "duration");
      if (duration == null) {
        this.durationInMillis = new Duration(0);
      } else {
        this.durationInMillis = duration.convertTo(TimeUnit.MILLISECONDS);
      }
      this.defaultTimeUnit = defaultTimeUnit;
    }

    public XynaPropertyDuration(String name, String defValue, TimeUnit defaultTimeUnit) {
      this(name, Duration.valueOf(defValue,defaultTimeUnit), defaultTimeUnit);
    }
    
    public XynaPropertyDuration(String name, Duration duration ) {
      this(name, duration, TimeUnit.SECONDS);
    }

    public XynaPropertyDuration(String name, String defValue) {
      this(name, defValue, TimeUnit.SECONDS);
    }

    protected Duration fromString(String string) throws Exception {
      Duration dur = Duration.valueOf(string, defaultTimeUnit);
      durationInMillis = dur.convertTo(TimeUnit.MILLISECONDS);
      return dur;
    }

    @Override
    public void set(Duration value) throws PersistenceLayerException {
      setString( value.serializeToString() );
    }
        
    /**
     * Ausgabe der Zeit in Millisekunden, optimiert für häufige Verwendung
     * @return
     */
    public long getMillis() {
      get(); //Initialisierung von durationInMillis
      return durationInMillis.getNumber();
    }
  }

  /**
   * TODO abwärtskompatible Variante der XynaPropertyDuration
   * @param <T> nur Long und Integer erlaubt!
   */
  public static class XynaPropertyDurationCompatible<T extends Number> extends XynaPropertyBase<T,XynaPropertyDurationCompatible<T>> {
    private TimeUnit defaultTimeUnit;
    private Duration duration; 
    private Duration durationInMillis; //In den meisten Fällen wird die Zeit in ms benötigt, dies spart die Extra-Umrechnung bei jedem getMillis()-Aufruf 
    private Class<T> type; 
    
    @SuppressWarnings("unchecked")
    public XynaPropertyDurationCompatible(String name, T defVal, TimeUnit defaultTimeUnit ) {
      super(name, defVal, "durationCompatible");
      this.duration = new Duration(defVal.longValue(), defaultTimeUnit );
      this.durationInMillis = duration.convertTo(TimeUnit.MILLISECONDS);
      this.defaultTimeUnit = defaultTimeUnit;
      this.type = (Class<T>) defVal.getClass();
    }
    
    public XynaPropertyDurationCompatible(String name, T defVal) {
      this(name, defVal, TimeUnit.SECONDS);
    }

    @Override
    protected T fromString(String string) throws Exception {
      duration = Duration.valueOf(string, defaultTimeUnit);
      durationInMillis = duration.convertTo(TimeUnit.MILLISECONDS);
      return cast( type, duration.getDuration(defaultTimeUnit) );
    }
    
    private static <U extends Number> U cast(Class<U> type, long duration) {
      Object obj = null;
      if( type == Long.class ) {
        obj = Long.valueOf(duration);
      } else if( type == Integer.class ) {
        obj = Integer.valueOf((int)duration);
      }
      return type.cast(obj);
    }
     
    /**
     * Ausgabe der Zeit in Millisekunden, optimiert für häufige Verwendung
     * @return
     */
    public long getMillis() {
      get(); //Initialisierung von durationInMillis
      return durationInMillis.getNumber();
    }
  }

  
}
