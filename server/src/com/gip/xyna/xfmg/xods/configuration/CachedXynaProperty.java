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
package com.gip.xyna.xfmg.xods.configuration;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.CounterMap;
import com.gip.xyna.utils.collections.sets.WeakHashSet;
import com.gip.xyna.xfmg.exceptions.XFMG_IllegalPropertyValueException;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBase;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;


public class CachedXynaProperty {

  /**
   * Konstante mit besonderem equals, so dass die Methode {@link ConcurrentHashMap#remove(Object, Object)} sicher 
   * verwendet werden kann, um nicht mehr verwendete CachedXynaProperty zu l�schen.
   */
  public static final CachedXynaProperty REMOVEABLE = new CachedXynaProperty(0,null,false,null);
  
  private static Logger logger = CentralFactoryLogging.getLogger(CachedXynaProperty.class); 
  
  private XynaPropertyWithDefaultValue cachedData;
  private Set<XynaPropertyBase<?,?>> instances;
  private boolean hidden = false; //versteckt die Property in allen Ausgaben
  private boolean hasDBEntry; //Ist Property in DB abgelegt?
  private PropertyCacher propertyCacher;
  private boolean hasDifferentDefaults = false;
  
  public interface PropertyCacher {

    /**
     * Property wird nicht mehr verwendet und kann daher entfernt werden
     * @param name
     */
    void canRemove(String name);
    
  }
  
  
  private CachedXynaProperty(int size, PropertyCacher propertyCacher, boolean hasDBEntry, XynaPropertyWithDefaultValue data) {
    this.instances = new WeakHashSet<XynaPropertyBase<?,?>>(size);
    this.propertyCacher = propertyCacher;
    this.hasDBEntry = hasDBEntry;
    this.cachedData = data;
  }
  
  public CachedXynaProperty(XynaPropertyBase<?, ?> property, PropertyCacher propertyCacher) {
    this(1, propertyCacher, false, new XynaPropertyWithDefaultValue(property) );
    instances.add(property);
    this.hidden = property.isHidden();
  }

  public CachedXynaProperty(XynaPropertyStorable storable, PropertyCacher propertyCacher) {
    this(0, propertyCacher, true, new XynaPropertyWithDefaultValue(storable) );
  }

  
  public synchronized void addInstance(XynaPropertyBase<?, ?> property) {
    if( property.isHidden() ) {
      hidden = true;
    }
    
    if( ! instances.add(property) ) {
      //XynaProperty ist doppelt registriert worden, daher hier nichts mehr zu tun
      return;
    }
    
    String oldDefVal = cachedData.getDefValue();
   
    XynaPropertyWithDefaultValue newData = new XynaPropertyWithDefaultValue(property);
    cachedData = XynaPropertyWithDefaultValue.combine(cachedData, newData);
    
    String newDefVal = cachedData.getDefValue();
    if( oldDefVal != null && ! oldDefVal.equals(newDefVal) ) {
      if( instances.size() == 1 ) {
        //Ok, alter Default wird nicht mehr verwendet
        hasDifferentDefaults =  false;
      } else {
        logger.info( "XynaProperty "+ cachedData.getName() +" has different default values ");
        hasDifferentDefaults = true;
      }
    } else {
      if( hasDifferentDefaults ) {
        tryResolveDifferentDefaults(true);
      }
    }
    
    if( logger.isDebugEnabled() ) {
      if( hasDifferentDefaults ) {
        logger.debug("add XynaPropertyInstance "+property+" -> "+instances.size()+" instances with different defaults");
      } else {
        logger.debug("add XynaPropertyInstance "+property+" -> "+instances.size()+" instances");
      }
    }
  }

  public synchronized void changeInstance(XynaPropertyBase<?, ?> property) {
    if( property.isHidden() ) {
      hidden = true;
    }
    XynaPropertyWithDefaultValue newData = new XynaPropertyWithDefaultValue(property);
    cachedData = XynaPropertyWithDefaultValue.combine(cachedData, newData);
  }

  
  public synchronized void removeInstance(XynaPropertyBase<?, ?> property) {
    instances.remove(property);
    if( logger.isDebugEnabled() ) {
      logger.debug( "remove XynaPropertyInstance "+property+" -> "+ instances.size()+" instances remaining" );
    }
    if( instances.isEmpty() ) {
      if( canBeRemoved() ) {
        propertyCacher.canRemove(cachedData.getName());
      } else {
        cachedData = XynaPropertyWithDefaultValue.changeDefaultValue(cachedData, null);
      }
    } else {
      if( hasDifferentDefaults ) {
        //bislang gab es mehrere Defaults, dies k�nnte nun anders sein
        tryResolveDifferentDefaults(false);
      }
    }
  }

  private void tryResolveDifferentDefaults(boolean changeDefaultToMostUsed ) {
    if( hasDifferentDefaults ) {
      logger.debug( "trying to resolve different defaults" );
    }
    CounterMap<String> defVals = new CounterMap<String>(); 
    for( XynaPropertyBase<?, ?> xp : instances ) {
      defVals.increment(xp.getDefaultValueAsString());
    }
    if( hasDifferentDefaults ) {
      logger.debug( "default values distribution: "+ defVals );
    }
    
    if( defVals.size() >= 1 && changeDefaultToMostUsed ) {
      //h�ufigster DefaultWert:
      String newDefValue = defVals.entryListSortedByCount(true).get(0).getKey(); 
      cachedData = XynaPropertyWithDefaultValue.changeDefaultValue(cachedData,newDefValue);
    }
    if( defVals.size() <= 1) {
      hasDifferentDefaults = false;
      if( logger.isDebugEnabled() ) {
        logger.debug( "different defaults resolved: default is now "+cachedData.getDefValue() );
      }
    } else {
      if( logger.isDebugEnabled() ) {
        logger.debug( "different defaults not resolved: default is now "+cachedData.getDefValue() );
      }
    }
  }

  public synchronized Set<XynaPropertyBase<?, ?>> getInstances() {
    //Kopie ist zwar teuer, Methode sollte aber auch praktisch nicht aufgerufen werden
    return new HashSet<XynaPropertyBase<?, ?>> ( instances);
  }

  public synchronized void addDBData(XynaPropertyStorable storable) {
    cachedData = XynaPropertyWithDefaultValue.combine(cachedData, new XynaPropertyWithDefaultValue(storable));
    hasDBEntry = true;
    for ( XynaPropertyBase<?,?> prop : instances ) {
      prop.propertyChanged(cachedData.getValue());
    }
  }

  public static Transformation<CachedXynaProperty, XynaPropertyWithDefaultValue> transformationToXynaPropertyWithDefaultValue() {
    return new TransformationToXynaPropertyWithDefaultValue(); 
  }
  
  public static Transformation<CachedXynaProperty, String> transformationToValue(boolean showDefaultValue) {
    return new TransformationToStringValue(showDefaultValue);
  }


  private static class TransformationToXynaPropertyWithDefaultValue implements Transformation<CachedXynaProperty,XynaPropertyWithDefaultValue> {
    public XynaPropertyWithDefaultValue transform(CachedXynaProperty from) {
      if( from.canBeListed() ) {
        return from.cachedData;
      } else {
        return null;
      }
    }
  }
  
  private static class TransformationToStringValue implements Transformation<CachedXynaProperty,String> {
    private boolean showDefaultValue;
    public TransformationToStringValue(boolean showDefaultValue) {
      this.showDefaultValue = showDefaultValue;
    }
    public String transform(CachedXynaProperty from) {
      if( from.canBeListed() ) {
        String value = from.cachedData.getValue();
        if( value == null && showDefaultValue ) {
          value = from.cachedData.getDefValue();
        }
        return value;
      } else {
        return null;
      }
    }
  }

  public synchronized boolean validate(String value) throws XFMG_IllegalPropertyValueException {
    if ( instances.isEmpty() ) {
      return false; //keine Instanz zum Validieren vorhanden
    }
    boolean validated = false;
    for ( XynaPropertyBase<?,?> prop : instances ) {
      prop.validate(value);
      validated = true;
    }
    return validated;
  }

  /**
   * Darf die Property in den Listen-Ausgaben angezeigt werden?
   * @return
   */
  public synchronized boolean canBeListed() {
    if( hidden ) {
      return false; //ist versteckt und darf nicht angezeigt werden
    }
    if( canBeRemoved() ) {
      propertyCacher.canRemove(cachedData.getName());
      return false; //wird nicht mehr verwendet und sollte daher nicht angezeigt werden
    }
    return true; //kann angezeigt werden
  }

  public synchronized void removeProperty() {
    cachedData = cachedData.modifyValue( null );
    hasDBEntry = false;
    if( instances.isEmpty() ) {
      propertyCacher.canRemove(cachedData.getName());
    } else {
      for ( XynaPropertyBase<?,?> prop : instances ) {
        prop.propertyChanged(null);
      }
    }
  }

  /**
   * Kann Property gel�scht werden, weil sie niemand mehr verwendet?
   */
  public synchronized boolean canBeRemoved() {
    return instances.isEmpty() && !hasDBEntry;
  }

  @Override
  public synchronized int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((cachedData == null) ? 0 : cachedData.hashCode());
    result = prime * result + (hasDBEntry ? 1231 : 1237);
    result = prime * result + (hidden ? 1231 : 1237);
    result = prime * result + ((instances == null) ? 0 : instances.hashCode());
    return result;
  }

  @Override
  public synchronized boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    CachedXynaProperty other = (CachedXynaProperty) obj;
    
    if( this == REMOVEABLE ) {
      return other.canBeRemoved(); 
    } else if( other == REMOVEABLE ) {
      return canBeRemoved();
    }
    
    if (cachedData == null) {
      if (other.cachedData != null)
        return false;
    } else if (!cachedData.equals(other.cachedData))
      return false;
    if (hasDBEntry != other.hasDBEntry)
      return false;
    if (hidden != other.hidden)
      return false;
    if (instances == null) {
      if (other.instances != null)
        return false;
    } else if (!instances.equals(other.instances))
      return false;
    return true;
  }

  public synchronized XynaPropertyStorable getStorable() {
    XynaPropertyStorable xps = new XynaPropertyStorable(cachedData.getName());
    xps.setPropertyDocumentation(cachedData.getDocumentation());
    xps.setPropertyValue(cachedData.getValue());
    return xps;
  }

  public synchronized String getPropertyValueOrDefault() {
    if( hasDBEntry ) {
      return cachedData.getValue();
    }
    if( hasDifferentDefaults ) {
      //FIXME mehrere Defaults...
      return cachedData.getDefValue();
    } else {
      return cachedData.getDefValue();
    }
  }

  public synchronized XynaPropertyWithDefaultValue getCachedData() {
    return cachedData;
  }


}
