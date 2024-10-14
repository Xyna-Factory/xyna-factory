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
package com.gip.xyna.xprc.xbatchmgmt.storables;

import java.util.List;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessCustomizationStorable.Operation;


/**
 * CustomizationUpdater führt ein Update auf einem BatchProcessCustomizationStorable aus.
 *
 */
public class CustomizationUpdater implements WarehouseRetryExecutableNoResult {
  
  private static Logger logger = CentralFactoryLogging.getLogger(CustomizationUpdater.class);
  private BatchProcessCustomizationStorable customization;
  private UpdateTask updateTask;
  private List<Double> counter;
  private BatchProcessArchiveStorable archive;
  private List<String> custom;
  
  public CustomizationUpdater(BatchProcessCustomizationStorable customization,
                              BatchProcessArchiveStorable archive) {
    this.customization = customization;
    this.archive = archive;
  }


  /**
   * Inkrementieren der Counter um die übergebenen Werte
   * @param counter
   */
  public void addCounter(List<Double> counter) {
    updateTask = UpdateTask.Add;
    this.counter = counter;
  }

  /**
   * Setzen der Counter auf die übergebenen Werte
   * @param counter
   */
  public void setCounter(List<Double> counter) {
    updateTask = UpdateTask.Set;
    this.counter = counter;
  }

  /**
   * Setzen der Custom-Felder
   * @param custom
   */
  public void setCustom(List<String> custom) {
    updateTask = UpdateTask.Custom;
    this.custom = custom;
  }

    
  public void update() {
    Lock lock = getLock();
    lock.lock();
    try {
      updateTask.update(customization,archive,this);
      WarehouseRetryExecutor.buildCriticalExecutor().
        storables( getStorableClassList()).
        execute(this);
    } catch (PersistenceLayerException e) {
      logger.warn("Could not update Customization for batchProcess "+ customization.getBatchProcessId() );
      //Die Exception wird hier unterdrückt mit folgendem Grund:
      //1) die Slaves können in ihrer Execution-Phase diesen Fehler nicht behandeln
      //2) nach kurzer Zeit wird der nächste Slave ebenfalls ein Update machen und dann die aktuelle 
      //   Änderung mit persistieren
      //3) falls dies ein dauerhaftes Problem ist, wird entweder BatchProcessArchive noch geschrieben werden
      //   oder auch hier ein ernstes Problem vorliegen -> TODO auf Alternative statt Default schreiben
      //4) hat die Factory bei so massiven DB-Problemen ernstere Schwierigkeiten als nur fehlende 
      //   Counter-Aktualisierungen
    } finally {
      lock.unlock();
    }
  }

  private Lock getLock() {
    if( updateTask.updateCustomization() ) {
      return customization.getLock();
    } else {
      return archive.getLock();
    }
  }


  protected List<Double> getCounter() {
    return counter;
  }
  
  protected List<String> getCustom() {
    return custom;
  }
  
  private StorableClassList getStorableClassList() {
    return new StorableClassList(BatchProcessCustomizationStorable.class, BatchProcessArchiveStorable.class);
  }

  public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
    if( updateTask.updateCustomization() ) {
      con.persistObject(customization);
    } else {
      con.persistObject(archive);
    }
    con.commit();
  }
  
  
  private enum UpdateTask {
    Set(true) {
      public void update(BatchProcessCustomizationStorable customization,
                         BatchProcessArchiveStorable archive,
                         CustomizationUpdater customizationUpdater) {
        List<Double> counter = customizationUpdater.getCounter();
        int max = Math.min(BatchProcessCustomizationStorable.NUM_COUNTER, counter.size() );
        for( int i=0; i<max; ++i ) {
          customization.modifyCounter(i, Operation.Set, counter.get(i) );
        }
      }
    },
    Add(true) {
      public void update(BatchProcessCustomizationStorable customization,
                         BatchProcessArchiveStorable archive,
                         CustomizationUpdater customizationUpdater) {
        List<Double> counter = customizationUpdater.getCounter();
        int max = Math.min(BatchProcessCustomizationStorable.NUM_COUNTER, counter.size() );
        for( int i=0; i<max; ++i ) {
          customization.modifyCounter(i, Operation.Add, counter.get(i) );
        }
      }
    },
    Custom(false) {
      public void update(BatchProcessCustomizationStorable customization,
                         BatchProcessArchiveStorable archive,
                         CustomizationUpdater customizationUpdater) {
        List<String> custom = customizationUpdater.getCustom();
        int max = Math.min(BatchProcessArchiveStorable.NUM_CUSTOM, custom.size() );
        for( int i=0; i<max; ++i ) {
          archive.setCustom(i, custom.get(i)); 
        }
      };
    };
    
    private boolean updateCustomization;
    
    private UpdateTask(boolean updateCustomization) {
      this.updateCustomization = updateCustomization;
    }
    public boolean updateCustomization() {
      return updateCustomization;
    }
    public abstract void update(BatchProcessCustomizationStorable customization,
                                BatchProcessArchiveStorable archive,
                                CustomizationUpdater customizationUpdater);
 
  }

}
