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
package com.gip.xyna.update.utils;

import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;


public class StorableUpdater {

  protected static final Logger logger = CentralFactoryLogging.getLogger(StorableUpdater.class);
  
  /**
   * Liest alle alten Storables (oldClass) aus, transformiert sie und speichert sie als newClass ab.
   * @return old storables
   */
  public static <F extends Storable<?>, T extends Storable<?>> Collection<F> update(Class<F> oldClass, Class<T> newClass, Transformation<F,T> transformation, ODSConnectionType connectionType) throws PersistenceLayerException {
    ODS ods = ODSImpl.getInstance();
    ods.registerStorable(oldClass);
    ODSConnection con = ods.openConnection(connectionType);
    try{
      Collection<F> oldStorables = con.loadCollection(oldClass);
      List<T> newStorables = CollectionUtils.transformAndSkipNull(oldStorables, transformation);
      
      con.deleteAll(oldClass);

      ods.registerStorable(newClass);
      try {
        con.persistCollection(newStorables);
        con.commit();
      } finally {
        ods.unregisterStorable(newClass);
      }
      return oldStorables;
    } finally {      
      try {
        con.closeConnection();
      } catch (PersistenceLayerException e) {
        logger.warn("Can't close connection.", e);
      }
      
      Persistable persistableOldClass = Storable.getPersistable(oldClass);
      String tableNameLCOldClass = persistableOldClass.tableName().toLowerCase();
      
      Persistable persistableNewClass = Storable.getPersistable(newClass);
      String tableNameLCNewClass = persistableNewClass.tableName().toLowerCase();
      
      //if both classes point to the same table, do not unregister table twice
      if(!tableNameLCOldClass.equals(tableNameLCNewClass)) {
        ods.unregisterStorable(oldClass);    
      }
    }
  }
}
