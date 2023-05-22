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

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;

public abstract class VCP_Abstract extends VetoCacheProcessor {

  private VetoCachePersistence persistence;
  private PersistVetos persistVetos;
  
  public VCP_Abstract(VetoCache vetoCache, VetoCachePersistence persistence) {
    super(vetoCache);
    this.persistence = persistence;
    this.persistVetos = new PersistVetos();
  }

  public VetoCachePersistence getPersistence() {
    return persistence;
  }
  
  @Override
  protected void startBatch() {
    //nichts zu tun
  }

  @Override
  protected void endBatch() {
    if( persistVetos.hasVetosToPersist() ) {
      PersistVetos pv = persistVetos;
      persistVetos = new PersistVetos();
      pv.persist(persistence);
      
      //State-Umsetzen der Scheduled
      for( VetoCacheEntry veto : pv.getScheduled() ) {
        if( veto.compareAndSetState(State.Scheduled, State.Used) ) {
          //in DB eingetragen
        } else {
          //Status Free erwartet bei schneller OrderExecution
          //andere Status unerwartet
          vetoCache.process(veto); //nochmal bearbeiten
        }
      }
      
      //State-Umsetzen der Free
      for( VetoCacheEntry veto : pv.getFreed() ) {
        processFreeAfterPersist(veto);
      }

      //Reprocess
      for( VetoCacheEntry veto : pv.getReprocess() ) {
        vetoCache.process(veto);
      }

    }
  }

  protected abstract void processFreeAfterPersist(VetoCacheEntry veto);

  /**
   * Merkt das Veto zur Persistierung vor
   * @param veto
   */
  protected void persist(VetoCacheEntry veto, boolean schedule) {
    persistVetos.add(veto, schedule);
  }

  @Override
  public String showBatch() {
    if( ! persistVetos.hasVetosToPersist() ) {
      return "P[]";
    } else {
      return "P[f="+persistVetos.getFreed()+",s="+persistVetos.getScheduled()+"]";
    }
  }

  @Override
  public String showInformation() {
    StringBuilder sb = new StringBuilder();
    appendImplInformation(sb);
    sb.append(", ");
    if( persistence == null ) {
      sb.append("not persistent");
    } else {
      persistence.appendInformation(sb);
    }
    return sb.toString();
  }
  
  protected abstract void appendImplInformation(StringBuilder sb);

  private static class PersistVetos {

    private List<VetoCacheEntry> reprocess;
    private List<VetoCacheEntry> scheduled;
    private List<VetoCacheEntry> freed;

    public PersistVetos() {
      scheduled = new ArrayList<>();
      freed = new ArrayList<>();
    }
    
    public boolean hasVetosToPersist() {
      return ! scheduled.isEmpty() || ! freed.isEmpty();
    }

    public void add(VetoCacheEntry veto, boolean schedule) {
      if( schedule ) {
        scheduled.add(veto);
      } else {
        freed.add(veto);
      }
      //Hier muss folgendes beachtet werden: Beim F�llen der Listen hier geht die urspr�ngliche Reihenfolge verloren.
      //Dies ist in folgenden 3 F�llen interessant: (A: Veto wird von einem weiteren Auftrag ben�tigt; B: schneller Auftrag gibt Veto zur�ck, 
      //C: schneller Auftrag gibt Veto zur�ck, ist aber zeitlich nicht mehr im Batch).
      //Da die VetoCacheEntry wiederverwendet werden, enthalten die Listen dann aber auch die aktuellsten Daten.
      //Aus einer zeitlichen Reihenfolge schedule(A,1,s), free(A,1,f), schedule(B,3,s), schedule(A,2,s), free(B,3,f), schedule(C,4,s) 
      //und versp�tet free(C,4,f)
      //wird ein Datensatz scheduled[(A,2,s), (A,2,s), (B,3,f), (C,4,f)], freed[(A,2,s), (B,3,f)]
      //Das Tripel (A,2,s) stellt dabei die Daten (<vetoName>,<orderId>,<status(scheduled,free)>) dar.
      //
      //Ausgelagert in VetoCachePersistence muss dann aus dem scheduled/freed-Datensatz als n�chstes bestimmt werden, 
      //welche Daten in der DB zu speichern bzw. zu l�schen sind. 
      //Dazu werden die scheduled-Daten genauer angeschaut, nur die mit Status "s" werden f�rs Speichern vorgesehen, 
      //die free-Daten werden immer gel�scht.
      //Damit werde obige Daten dann zu
      //persist[(A,2,s), (A,2,s)], delete[(A,2,s), (B,3,f)], reprocess[(B,3,f), (C,4,f)]
      //Bei der Persistierung muss dann beachtet werden, dass zuerst die Deletes und danach erst die Persists abgearbeitet werden, 
      //da sonst A fehlen w�rde. 
      //Die Reprocess-Eintr�ge f�hren dann dazu, dass Vetos B und C mit dem n�chsten Batch erneut gel�scht werden. 
    }

    public void persist(VetoCachePersistence persistence) {
      //Eine schnelle OrderExecution kann Eintr�ge in scheduled auf Status free setzen. Die noch erkannten
      //Eintr�ge werden hier noch als reprocess zur�ckgegeben.
      if( persistence != null ) {
        reprocess = persistence.persist( scheduled, freed );
      }
    }
    
    
    public List<VetoCacheEntry> getReprocess() {
      return reprocess;
    }
    
    public List<VetoCacheEntry> getFreed() {
      return freed;
    }
    
    public List<VetoCacheEntry> getScheduled() {
      return scheduled;
    }
    
  }

}
