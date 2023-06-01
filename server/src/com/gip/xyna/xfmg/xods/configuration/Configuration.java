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



import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import com.gip.xyna.FunctionGroup;
import com.gip.xyna.FutureExecution;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xfmg.exceptions.XFMG_XynaPropertyModificationCounterCouldNotBeLoadedException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertySource;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertySourceDefault;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerInstanceIdUnknownException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Connection;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.ODSImpl.PersistenceLayerInstances;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;



/**
 * Management of Factory Configuration (aka Xyna Properties).
 * 
 * TODO Daten werden derzeit an 3 Stellen gespeichert: PL-History, PL-Default und xynaPropertyCache.
 * PL-Default ist nun unnötig; xynaPropertyCache speichert mehr als PL-Default.
 */
public class Configuration extends FunctionGroup {
  
  public static final String DEFAULT_NAME = "Configuration";
 
  private ReentrantReadWriteLock modCountReadWriteLock;
  private ReadLock modCountReadLock;
  private WriteLock modCountWriteLock;
  private Integer modCount;

  private final boolean isInPreInitMode;
  private volatile boolean isInitialized;
  protected ODS ods;

  //FIXME ConcurrentHashMap könnte hier listenerLock sparen?
  private ReentrantLock listenerLock;
  private Map<String, Set<IPropertyChangeListener>> listenerTargets; //für normale Listener
  //Speicherung der Properties und ihrer Instanzen
  private XynaPropertySourceFactory xynaPropertyCache;
  private Map<String,Pair<UserType,String>> dependencies; //nur ein temporärer speicher während der initialisierung, bevor dependencyregister da ist

  private DependencyRegister dependencyRegister;
  
  public Configuration() throws XynaException {
    super();
    isInPreInitMode = false;
  }

  //für tests/updates
  private Configuration(String cause) throws XynaException {

    super(cause);
    modCount = 0;
    isInPreInitMode = true;
    modCountReadWriteLock = new ReentrantReadWriteLock();
    dependencies = new ConcurrentHashMap<String,Pair<UserType,String>>();
    modCountReadLock = modCountReadWriteLock.readLock();
    ods = ODSImpl.getInstance();
    ods.registerStorable(XynaPropertyStorable.class);
    ods.registerStorable(ModCountStorable.class);
    
    listenerTargets = new HashMap<String, Set<IPropertyChangeListener>>();
    listenerLock = new ReentrantLock();
    xynaPropertyCache = new XynaPropertySourceFactory(this);
    XynaPropertyUtils.exchangeXynaPropertySource(xynaPropertyCache);
    loadXynaProperties();
  }


  public static Configuration getConfigurationPreInit() throws XynaException {
    return new Configuration("preInit");
  }


  public void init() throws XynaException {

    if (isInitialized) {
      return;
    }

    ods = ODSImpl.getInstance();
    ods.registerStorable(XynaPropertyStorable.class);
    ods.registerStorable(ModCountStorable.class);

    logger.debug("Initializing other components");
    modCountReadWriteLock = new ReentrantReadWriteLock();
    modCountReadLock = modCountReadWriteLock.readLock();
    modCountWriteLock = modCountReadWriteLock.writeLock();

    listenerTargets = new HashMap<String, Set<IPropertyChangeListener>>();
    listenerLock = new ReentrantLock();
         
    isInitialized = true;

    FutureExecution fExec = XynaFactory.getInstance().getFutureExecution();
    fExec.addTask(Configuration.class, "XynaProperty-preregister").
      after(PersistenceLayerInstances.class).
      execAsync(new Runnable() { public void run() { initConfiguration(); } });
    fExec.addMetaTask(XynaProperty.class, "XynaProperty").
      after(DependencyRegister.class,Configuration.class).
      execAsync(new Runnable() { public void run() { initXynaProperties(); } });
 
  }
  
  
  /**
   * XynaProperties können nun von DB gelesen werden und sind daher bereits funktionsfähig.
   * Dependency wird noch nicht gesetzt
   */
  private void initConfiguration() {
    XynaPropertySource xps = XynaPropertyUtils.getXynaPropertySource();
    xynaPropertyCache = new XynaPropertySourceFactory(this);
    dependencies = new ConcurrentHashMap<String,Pair<UserType,String>>(); 
    
    XynaPropertyUtils.exchangeXynaPropertySource(xynaPropertyCache);
    
    if( xps instanceof XynaPropertySourceDefault ) {
      XynaPropertySourceDefault xpsd = (XynaPropertySourceDefault)xps;
      if( xpsd.getDependencies() != null ) {
        dependencies.putAll( xpsd.getDependencies() );
      }
      if( ! xpsd.getSetValues().isEmpty() ) {
        logger.warn("XynaPropertySourceDefault has changed properties "+ xpsd.getSetValues() );
        //FIXME Änderungen in xps...
      }
    }
    
    loadXynaProperties();
  }
  
  /**
   * DependencyRegister ist nun initialisiert, daher dort anmelden. 
   * Nun sind XynaProperties voll funktionsfähig
   */
  private void initXynaProperties() {
    dependencyRegister = XynaFactory.getInstance().getFactoryManagementPortal().getXynaFactoryControl().getDependencyRegister();

    logger.info("Configuration has "+dependencies.size()+" preregistered dependencies, registering now" );
    for( Map.Entry<String,Pair<UserType,String>> entry : dependencies.entrySet() ) {
      addDependency(entry.getKey(),entry.getValue().getFirst(), entry.getValue().getSecond());
    }
    dependencies = null;
    
    XynaProperty.CONFIGURATION_DIRECTPERSISTENCE.registerDependency(UserType.XynaFactory,DEFAULT_NAME);
  }
  
  private void loadXynaProperties() {
    try {
      loadModCountStorable();
      copyFromHistoryToDefault();
    } catch (XynaException e) {
      throw new RuntimeException(e); //TODO besser
    }

    readPropertiesFromDB();
  }

  private void readPropertiesFromDB() {
    Connection con = ods.openConnection();
    try {
      for (XynaPropertyStorable storable : con.loadCollection(XynaPropertyStorable.class) ) {
        xynaPropertyCache.addProperty( storable );
      }
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("Failed to obtain properties.", e);
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error("Failed to close connection", e);
      }
    }
  }

  public void addDependency(String key, UserType userType, String name) {
    if( dependencyRegister != null ) {
      dependencyRegister.addDependency(DependencySourceType.XYNAPROPERTY, key, dependencySourceType(userType), name);
    } else {
      dependencies.put(key,Pair.of(userType, name));
    }
  }
  
  private DependencySourceType dependencySourceType(UserType userType) {
    if( userType == null ) {
      return DependencySourceType.XYNAFACTORY;
    }
    switch( userType ) {
      case Extern :
        return DependencySourceType.XYNAFACTORY; //TODO
      case Filter :
        return DependencySourceType.FILTER;
      case Other :
        return DependencySourceType.XYNAFACTORY; //TODO
      case Plugin :
        return DependencySourceType.XYNAFACTORY; //TODO
      case Service :
        return DependencySourceType.DATATYPE;
      case Trigger :
        return DependencySourceType.TRIGGER;
      case Workflow :
        return DependencySourceType.WORKFLOW;
      case XynaFactory :
        return DependencySourceType.XYNAFACTORY;
      default:
        return DependencySourceType.XYNAFACTORY; //TODO
    }
  }

  public void shutdown() throws XynaException {

    if (!isInitialized) {
      return;
    }

    modCountWriteLock.lock();
    try {
      storeModCountStorable();
    } finally {
      modCountWriteLock.unlock();
    }
    copyFromDefaultToHistory();

  }

  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Null not allowed as property key");
    }
    if( xynaPropertyCache == null ) {
      return null; //FIXME
    } else {
      return xynaPropertyCache.getPropertyWithDefaultValue(key);
    }
  }

  
  public String getProperty(String key) {
    if (key == null) {
      throw new IllegalArgumentException("Null not allowed as property key");
    }
    if( isInPreInitMode ) {
      return readProperty(key);
    } else {
      if( xynaPropertyCache == null ) {
        return readProperty(key); //FIXME
      }
      XynaPropertyWithDefaultValue prop = xynaPropertyCache.getPropertyWithDefaultValue(key);
      if( prop != null ) {
        return prop.getValue();
      } else {
        return null;
      }
    }
  }

  public String readProperty(String key) {

    if (key == null) {
      throw new IllegalArgumentException("Null not allowed as property key");
    }

    Connection con = ods.openConnection();
    try {
      XynaPropertyStorable propStorable = new XynaPropertyStorable(key);
      try {
        con.queryOneRow(propStorable);
        
        
        return propStorable.getPropertyValue();
      } catch (PersistenceLayerException e) {
        logger.error("Failed to obtain property " + key, e);
        return null;
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        if (logger.isTraceEnabled()) {
          logger.trace("tried to access unkown property (" + key + ").");
        }
        return null;
      }
    } finally {
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.error("Failed to close connection after getting a property value for key '" + key + "'", e);
      }
    }
    
  }


  public boolean isPropertyFactoryComponent(String key) throws PersistenceLayerException {
    if (key == null) {
      return false;
    }
    Connection con = ods.openConnection();
    try {
      XynaPropertyStorable propStorable = new XynaPropertyStorable(key);
      con.queryOneRow(propStorable);
      return propStorable.isFactoryComponent();
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return false;
    } finally {
      con.closeConnection();
    }
  }


  /**
   * Sets a property, overwriting previously stored values
   * @throws XFMG_IllegalPropertyValueException 
   */
  public void setProperty(String key, String value) throws PersistenceLayerException, XFMG_IllegalPropertyValueException {
    setProperty(key, value, false, false);
  }


  public void setProperty(String key, String value, boolean clusterwide) throws PersistenceLayerException, XFMG_IllegalPropertyValueException {
    setProperty(key, value, false, clusterwide);
  }

  public void setProperty(XynaPropertyWithDefaultValue property) throws PersistenceLayerException, XFMG_IllegalPropertyValueException {
    setProperty(property.getName(), property.getValue(), property.getDocumentation(), false, false);
  }


  /**
   * Sets a property, overwriting previously stored values allowing a flag that identifies a property as belonging to a
   * factory component
   * @throws XFMG_IllegalPropertyValueException 
   */
  public void setFactoryComponentProperty(String key, String value) throws PersistenceLayerException, XFMG_IllegalPropertyValueException {
    setProperty(key, value, true, true);
  }

  protected void setProperty(String key, String value, boolean isFactoryComponent, boolean clusterwide)
                  throws PersistenceLayerException, XFMG_IllegalPropertyValueException {
    setProperty(key, value, null, isFactoryComponent, clusterwide);
  }

  protected void setProperty(String key, String value, Map<DocumentationLanguage,String> documentation, boolean isFactoryComponent, boolean clusterwide)
      throws PersistenceLayerException, XFMG_IllegalPropertyValueException {

    // FIXME SPS prio5: implement local access even when clustered, i.e. take into account the 'clusterWide' flag:
    //                  * read access always returns the local value
    //                  * write access can modify either only the local value or the value on all nodes

    if (logger.isDebugEnabled()) {
      logger.debug("call setProperty (key='" + key + "', value='" + value + "').");
    }

    if (key == null || key.length() == 0) {
      throw new IllegalArgumentException("Property key may not be null or empty");
    }
    XynaPropertySourceFactory xpc = xynaPropertyCache;
    if (xpc == null) {
      throw new RuntimeException("Configuration is not initialized.");
    }
    if (value == null) {
      removeProperty(key);
      return;
    }
    boolean validated = xpc.validateProperty( key, value );
    if( ! validated ) {
      logger.info("Setting unvalidated value \""+value+"\" for property \""+key+"\"");
    }
    
    XynaPropertyStorable propStorable = xpc.getStorable(key); 
    fillPropertyStorable( propStorable, value, documentation, isFactoryComponent );
    
    persistProperty(propStorable);

    xpc.setProperty(propStorable); //Zuerst die Properties ändern
    notifyListeners(key); //danach die PropertyChangeListener informieren
  }


  private void fillPropertyStorable(XynaPropertyStorable propStorable, String value,
                                    Map<DocumentationLanguage, String> documentation, boolean isFactoryComponent) {
    if (logger.isDebugEnabled()) {
      if( propStorable.getPropertyValue() != null ) {
        logger.debug("Previous value of property was '" + propStorable.getPropertyValue() + "'");
      }
    }
    propStorable.setPropertyValue(value);
    
    if(documentation != null) {
      if (propStorable.getPropertyDocumentation() != null) {
        propStorable.getPropertyDocumentation().putAll(documentation);
      } else {
        propStorable.setPropertyDocumentation(documentation);
      }
    }   
    propStorable.setIsFactoryComponent(isFactoryComponent);
  }

  public boolean addPropertyDocumentation(String key, DocumentationLanguage lang, String documentation) throws PersistenceLayerException {
    if (logger.isDebugEnabled()) {
      logger.debug("call setPropertyDocumentation (key='" + key + "', language='" + lang + "', documentation='" + documentation + "').");
    }

    if (key == null || key.length() == 0) {
      throw new IllegalArgumentException("Property key may not be null or empty");
    }
    
    XynaPropertyStorable propStorable = xynaPropertyCache.getStorable(key);
    //neue Documentation hinzufuegen
    propStorable.addPropertyDocumentation(lang, documentation);
    xynaPropertyCache.addProperty(propStorable);
    
    persistProperty( propStorable );
    return true;
  }


  private void persistProperty(XynaPropertyStorable propStorable) throws PersistenceLayerException {
    Connection con = ods.openConnection();
    try {
      con.persistObject(propStorable);
      con.commit();
    } finally {
      con.closeConnection();
    }

    if (XynaProperty.CONFIGURATION_DIRECTPERSISTENCE.get()) {
      con = ods.openConnection(ODSConnectionType.HISTORY);
      try {
        con.persistObject(propStorable);
        con.commit();
      } finally {
        con.closeConnection();
      }
    }
    
    incrementModCount();
  }

  public void removeProperty(String key) throws PersistenceLayerException {
    removeProperty(key, false);
  }


  public void removeProperty(String key, boolean clusterwide) throws PersistenceLayerException {

    if (key == null) {
      throw new IllegalArgumentException("Cannot remove property <null>");
    }

    if (logger.isDebugEnabled()) {
      logger.debug("removing property (key='" + key + "').");
    }

    Connection con = ods.openConnection();
    try {
      con.deleteOneRow(new XynaPropertyStorable(key));
      con.commit();
    } finally {
      con.closeConnection();
    }

    con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      con.deleteOneRow(new XynaPropertyStorable(key));
      con.commit();
    } finally {
      con.closeConnection();
    }

    xynaPropertyCache.removeProperty(key);
    notifyListeners(key);
    incrementModCount();
  }
  

  protected void incrementModCount() {

    if (isInPreInitMode) {
      return;
    }

    modCountWriteLock.lock();
    try {
      modCount++;
    } finally {
      modCountWriteLock.unlock();
    }
    try {
      writeModCountToDisk();
    } catch (PersistenceLayerException e) {
      logger.error("could not save modcount", e);
    }
  }


  private void writeModCountToDisk() throws PersistenceLayerException {
    if ( XynaProperty.CONFIGURATION_DIRECTPERSISTENCE.get() ) {
      modCountReadLock.lock();
      try {
        storeModCountStorable();
      } finally {
        modCountReadLock.unlock();
      }
    }
  }


  private int getModCount() {
    modCountReadLock.lock();
    try {
      return modCount;
    } finally {
      modCountReadLock.unlock();
    }
  }


  public PropertyMap<String, String> getPropertiesReadOnly() {
    return new PropertyMap<String, String>( xynaPropertyCache.getNameValueMap(true), getModCount());
  }
  
  public Collection<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly() {
    return xynaPropertyCache.getPropertiesWithDefaultValuesReadOnly();
  }
  

  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  /**
   * priorität wirkt sich auf die reihenfolge der ausführung beim propertychange aus.
   * FIXME priorität derzeit nicht verwendet!
   * 
   * @param prio 0 = am höchsten, > 0 niedriger. 
   */
  void addPropertyChangeListener(int prio, IPropertyChangeListener listener) {
    //TODO siehe bugz 15959: neue methode, die propertychangelistener weakreferenced hält
    
    //prio benötigt, damit man innerhalb eines changehandlers XynapropertyUtils verwenden kann. falls
    //die changehandler in der falschen reihenfolge ausgeführt werden, liest man immer den alten wert.
    
    if (listener.getWatchedProperties() == null) {
      logger.warn("tried to add property change listener that did not specify any properties: " + listener);
      return;
    }
    if( ! isInitialized ) {
      logger.warn("tried to add property change listener for unitialized Configuration: "+ listener, new RuntimeException() );
      return;
    }
    
    listenerLock.lock();
    try {
      
      for (String s : listener.getWatchedProperties()) {
        if (!listenerTargets.containsKey(s)) {
          Set<IPropertyChangeListener> newSet = new HashSet<IPropertyChangeListener>(1);
          newSet.add(listener);
          listenerTargets.put(s, newSet);
        } else {
          Set<IPropertyChangeListener> oldList = listenerTargets.get(s);
          if (oldList.contains(listener)) {
            logger.warn("tried to add property change listener for property where listener already existed: " + listener);
            if (logger.isTraceEnabled()) {
              logger.trace(null, new Exception());
            }
          } else {
            //hier eine neue liste erstellen, damit es keine concurrentmodificationexceptions gibt für den fall, dass
            //jemand in einem propertychangehandler einen weiteren propertychangehandler hinzufügt.
            //das hört sich komisch an, ist aber der fall, wenn man im propertychangehandler das erstemal
            //ein xynapropertyutils.xynapropertybase.get() aufruft.
            Set<IPropertyChangeListener> newSet = new HashSet<IPropertyChangeListener>(oldList.size() + 1);
            newSet.add(listener);
            newSet.addAll(oldList);
            listenerTargets.put(s, newSet);
          }
        }
      }

    } finally {
      listenerLock.unlock();
    }
  }
  
  
  

  public void addPropertyChangeListener(IPropertyChangeListener listener) {
    addPropertyChangeListener(1, listener);    
  }


  public void removePropertyChangeListener(IPropertyChangeListener listener) {

    listenerLock.lock();
    try {
      for (String s : listener.getWatchedProperties()) {
        Set<IPropertyChangeListener> oldSet = listenerTargets.get(s);
        if (oldSet == null) {
          throw new RuntimeException("tried to remove unknown property change listener");
        }
        if (oldSet.contains(listener)) {
          oldSet.remove(listener);
          if( oldSet.isEmpty() ) {
            listenerTargets.remove(s);
          }
        } else {
          throw new RuntimeException("tried to remove unknown property change listener");
        }
      }
    } finally {
      listenerLock.unlock();
    }

  }

  public void notifyListeners(String changedPropertyKey) {

    if (isInPreInitMode) {
      return;
    }

    // FIXME potentiell sollte man hier die Listener erst in eine separate Map kopieren und dann aufrufen, um
    //       das Lock nicht so lange zu haben und potentiell Deadlocks zu provozieren
    listenerLock.lock();
    try {
      Set<IPropertyChangeListener> oldSet = listenerTargets.get(changedPropertyKey);
      if (oldSet != null) {
        for (IPropertyChangeListener listener : oldSet) {
          listener.propertyChanged();
        }
      }
    } finally {
      listenerLock.unlock();
    }

  }


  private void copyFromHistoryToDefault() throws PersistenceLayerException {
    if (!ods.isSamePhysicalTable(XynaPropertyStorable.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
      try {
        ods.copy(XynaPropertyStorable.class, ODSConnectionType.HISTORY, ODSConnectionType.DEFAULT);
      } catch (XNWH_PersistenceLayerInstanceIdUnknownException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private void copyFromDefaultToHistory() throws PersistenceLayerException {
    if (!ods.isSamePhysicalTable(XynaPropertyStorable.TABLE_NAME, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY)) {
      try {
        ods.copy(XynaPropertyStorable.class, ODSConnectionType.DEFAULT, ODSConnectionType.HISTORY);
      } catch (XNWH_PersistenceLayerInstanceIdUnknownException e) {
        throw new RuntimeException(e);
      }
    }
  }  
  
  private void storeModCountStorable() throws PersistenceLayerException {
    Connection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      ModCountStorable storable = new ModCountStorable("1", modCount);
      con.persistObject(storable);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }
  
  private void loadModCountStorable() throws XFMG_XynaPropertyModificationCounterCouldNotBeLoadedException {
    Connection con = ods.openConnection(ODSConnectionType.HISTORY);
    try {
      try {
        ModCountStorable storable = new ModCountStorable("1", null);
        con.queryOneRow(storable);
        modCount = storable.getModcount();
      } catch(XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        modCount = 0;
      } finally {
        con.closeConnection();
      }
    } catch(PersistenceLayerException e) {
      throw new XFMG_XynaPropertyModificationCounterCouldNotBeLoadedException(e);
    }
  }

  @Persistable(primaryKey = ModCountStorable.COL_ID, tableName = ModCountStorable.TABLE_NAME)
  public static class ModCountStorable extends Storable<ModCountStorable> {

    private static final long serialVersionUID = -1486347216567878740L;
    
    public final static String TABLE_NAME = "configurationmodcount";
    public final static String COL_ID = "id";
    public final static String COL_MODCOUNT = "modcount";
    
    @Column(name = COL_ID)
    private String id;
    @Column(name = COL_MODCOUNT)
    private Integer modcount;
    
    public ModCountStorable() {
    }
    
    public ModCountStorable(String id, Integer modcount) {
      this.id = id;
      this.modcount = modcount;
    }
    
    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public Integer getModcount() {
      return modcount;
    }

    public void setModcount(Integer modcount) {
      this.modcount = modcount;
    }

    @Override
    public ResultSetReader<? extends ModCountStorable> getReader() {
      return new ResultSetReader<ModCountStorable>() {

        public ModCountStorable read(ResultSet rs) throws SQLException {
          return new ModCountStorable(rs.getString(COL_ID), rs.getInt(COL_MODCOUNT));
        }
      };
    }

    @Override
    public Object getPrimaryKey() {
      if(id == null) {
        return ""; // Workaround für Bug, wenn Einträge ohne Id persistiert wurden ...
      } else {
        return id;
      }
    }

    @Override
    public <U extends ModCountStorable> void setAllFieldsFromData(U data) {
      ModCountStorable cast = data;
      id = cast.id;
      modcount = cast.modcount;      
    }
    
  }
  
}
