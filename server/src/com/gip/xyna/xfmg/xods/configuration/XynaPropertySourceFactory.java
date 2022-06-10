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
package com.gip.xyna.xfmg.xods.configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xfmg.xods.configuration.CachedXynaProperty.PropertyCacher;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.UserType;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBase;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertySource;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


/**
 * XynaPropertySource-Implementierung in der Factory: 
 * alle Methoden werden an die richtigen Stellen der Factory weitergereicht:
 * <ul>
 * <li>FactoryManagement zum Lesen und Schreiben der Property</li>
 * <li>DependencyRegister für Verwender-Anzeige</li>
 * </ul>
 */
public class XynaPropertySourceFactory implements XynaPropertySource, PropertyCacher {
  
  private Configuration configuration;
  private ConcurrentHashMap<String, CachedXynaProperty> properties = 
      new ConcurrentHashMap<String, CachedXynaProperty>();
  
  public XynaPropertySourceFactory(Configuration configuration ) {
    this.configuration = configuration;
  }
  
  public void register(XynaPropertyBase<?,?> property) {
    String propName = property.getPropertyName();
    CachedXynaProperty cxp = properties.get(propName);
    if( cxp == null ) {
      cxp = new CachedXynaProperty(property,this);
      CachedXynaProperty existing = properties.putIfAbsent(propName, cxp);
      if( existing != null ) {
        cxp = existing; //jemand anderes war schneller, dessen CachedXynaProperty übernehmen
        cxp.addInstance(property);
      }
    } else {
      cxp.addInstance(property);
    }
  }

  public void unregister(XynaPropertyBase<?,?> property) {
    String name = property.getPropertyName();
    CachedXynaProperty cxp = properties.get(name);
    if( cxp == null ) {
      //unerwartet, nichts zu tun
      return;
    }
    cxp.removeInstance(property);
  }
  
  public Set<String> getRegisteredPropertyNames() {
    return Collections.unmodifiableSet(properties.keySet());
  }
 
  public Set<XynaPropertyBase<?,?>> getRegisteredProperties(String name) {
    CachedXynaProperty cxp = properties.get(name);
    if( cxp == null ) {
      return Collections.emptySet();
    }
    return cxp.getInstances();
  }
  

  public String getProperty(String name) {
    //hiermit lesen die XynaPropertyBase-Instanzen in der DB
    return configuration.readProperty(name);
  }
  
  public void registerDependency(XynaPropertyBase<?,?> property, String name) {
    registerDependency(property, UserType.XynaFactory, name);
  }
  public void registerDependency(XynaPropertyBase<?,?> property, UserType userType, String name) {
    configuration.addDependency(property.getPropertyName(), userType, name);
    register(property);
  }

  public void setProperty(XynaPropertyBase<?,?> property, String newValue ) throws PersistenceLayerException {
    try {
      configuration.setProperty(property.getPropertyName(), newValue);
    } catch (XFMG_IllegalPropertyValueException e) {
      //unerwarter Fehler, da set-Anweisung von der Property selbst ausging
      throw new RuntimeException(e);
    }
  }

  public List<XynaPropertyWithDefaultValue> getPropertiesWithDefaultValuesReadOnly() {
    return CollectionUtils.transformAndSkipNull(properties.values(), CachedXynaProperty.transformationToXynaPropertyWithDefaultValue() );
  }

  public Map<String, String> getNameValueMap(boolean showDefaultValue ) {
    return CollectionUtils.transformValuesAndSkipNull( properties, CachedXynaProperty.transformationToValue(showDefaultValue) );
  }

  public void addProperty(XynaPropertyStorable storable) {
    String name = storable.getPropertyKey();
    CachedXynaProperty cxp = properties.get(name); 
    if( cxp != null ) {
      cxp.addDBData(storable);
      return;
    } else {
      cxp = new CachedXynaProperty(storable,this);
      CachedXynaProperty existing = properties.putIfAbsent(name, cxp);
      if( existing != null ) {
        existing.addDBData(storable);
        //jemand anderes war schneller, dessen CachedXynaProperty übernehmen
      }
    }
  }

  public void setProperty(XynaPropertyStorable storable) {
    String name = storable.getPropertyKey();
    CachedXynaProperty cxp = properties.get(name);
    if( cxp == null ) {
      cxp = new CachedXynaProperty(storable,this);
      CachedXynaProperty existing = properties.putIfAbsent(name, cxp);
      if( existing != null ) {
        cxp = existing;
        //jemand anderes war schneller, dessen CachedXynaProperty übernehmen
      }
    }
    cxp.addDBData(storable);
  }

  public boolean validateProperty(String name, String value) throws XFMG_IllegalPropertyValueException {
    CachedXynaProperty cxp = properties.get(name);
    if( cxp == null ) {
      return false; //unbekannte Property, kann nicht validiert werden
    }
    return cxp.validate(value);
  }

  public boolean contains(String name) {
    return properties.containsKey(name);
  }

  public void removeProperty(String name) {
    CachedXynaProperty cxp = properties.get(name);
    if( cxp != null ) {
      cxp.removeProperty();
    } else {
      //nichts zu tun;
    }
  }

  public XynaPropertyStorable getStorable(String name) {
    CachedXynaProperty cxp = properties.get(name);
    if( cxp != null ) {
      return cxp.getStorable();
    } else {
      return new XynaPropertyStorable(name);
    }
  }

  public void canRemove(String name) {
    CachedXynaProperty cxp = properties.get(name);
    if( cxp != null && cxp.canBeRemoved() ) {
      properties.remove(name, CachedXynaProperty.REMOVEABLE);
    }
  }

  public XynaPropertyWithDefaultValue getPropertyWithDefaultValue(String name) {
    CachedXynaProperty cxp = properties.get(name);
    if( cxp != null ) {
      return cxp.getCachedData();
    } else {
      return null;
    }
  }

  public void definitionChanged(XynaPropertyBase<?, ?> property) {
    String propName = property.getPropertyName();
    CachedXynaProperty cxp = properties.get(propName);
    if( cxp == null ) {
      //sollte nicht vorkommen
      register(property);
    } else {
      cxp.changeInstance(property);
    }
  }
 

}
