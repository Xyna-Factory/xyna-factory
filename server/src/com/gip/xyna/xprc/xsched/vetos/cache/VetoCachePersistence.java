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
package com.gip.xyna.xprc.xsched.vetos.cache;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

public interface VetoCachePersistence {
  
  public void init() throws XynaException;
  
  public int getOwnBinding();

  /**
   * Vetos mit �bergebenem Binding werden gelesen und in den VetoCache eingetragen
   * Binding darf -1 sein: alle Vetos werden gelesen
   * Macht eine Pr�fung, ob Veto korrekt in DB eingetragen war.
   * Falls nicht, werden diese aus der DB entfernt.
   * @param binding
   */
  public void initVetoCache(int binding);
  
  /**
   * Speichert die Vetos aus toPersist und l�scht die Vetos aus toDelete.
   * Zur�ckgegeben wird die Liste der Vetos, die doch nicht gespeichert wurden, beispielsweise weil 
   * der Status doch nicht Scheduled ist. Dies kann sich laufend �ndern, daher kann das schlecht 
   * zuvor �berpr�ft werden.
   * @param toPersist
   * @param toDelete
   * @return
   */
  public List<VetoCacheEntry> persist(List<VetoCacheEntry> toPersist, List<VetoCacheEntry> toDelete);
  
  /**
   * Macht eine Pr�fung, ob Vetos mit anderem als ownBinding korrekt in DB eingetragen waren. 
   * Falls nicht, werden diese aus dem VetoCache entfernt
   */
  public void cleanupVetoCache();
  
  /**
   * Anzeige der Vetos in der GUI: beliebige Suchparameter werden durch Suche in der DB unterst�tzt
   */
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException;

  /**
   * Anzeige von Informationen, mindestens etwas wie "persistent" oder "persistent (failed, cached 4 statements)"
   */
  public void appendInformation(StringBuilder sb);

}
