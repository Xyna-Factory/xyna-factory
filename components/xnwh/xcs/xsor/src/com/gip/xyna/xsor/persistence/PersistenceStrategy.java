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
package com.gip.xyna.xsor.persistence;



import java.util.Iterator;

import com.gip.xyna.xsor.protocol.XSORPayload;



/**
 * Schnittstelle zur Persistierung der in der Koh�renz gehaltenen Daten
 * Die Methoden werden alle synchron aufgerufen. falls asynchrone Persistierung gew�nscht ist, muss 
 * die Implementierung der {@link PersistenceStrategy} sich selbst darum k�mmern.
 */
public interface PersistenceStrategy {

  public static class XSORPayloadPersistenceBean {

    private final XSORPayload xsorPayload;
    private final long releaseTime;
    private final long modificationTime;


    public XSORPayloadPersistenceBean(XSORPayload xsorPayload, long releaseTime, long modificationTime) {
      this.modificationTime = modificationTime;
      this.releaseTime = releaseTime;
      this.xsorPayload = xsorPayload;
    }


    public XSORPayload getXSORPayload() {
      return xsorPayload;
    }


    public long getReleaseTime() {
      return releaseTime;
    }


    public long getModificationTime() {
      return modificationTime;
    }
  }


  /**
   * das �bergebene objekt wird vom aufrufer nicht weiterverwendet, kann also 
   * beliebig lange gehalten werden, ohne davon ausgehen zu m�ssen, dass
   * es von jemand anderem ge�ndert wird.
   */
  public void createObject(XSORPayload xsorPayload, long releaseTime, long modificationTime) throws PersistenceException;


  /**
   * @see #createObject(XSORPayload) 
   */
  public void updateObject(XSORPayload xsorPayload, long releaseTime, long modificationTime) throws PersistenceException;


  /**
   * @see #createObject(XSORPayload) 
   */
  public void deleteObject(XSORPayload xsorPayload) throws PersistenceException;


  public Iterator<XSORPayloadPersistenceBean> loadObjects(String tableName, Class<?> clazz) throws PersistenceException;


  public void clearAllData(String tableName, Class<? extends XSORPayload> clazz) throws PersistenceException;

}
