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
package com.gip.xyna.xprc.xsched.vetos;

import java.util.Collection;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoAllocationDenied;
import com.gip.xyna.xprc.exceptions.XPRC_AdministrativeVetoDeallocationDenied;
import com.gip.xyna.xprc.xsched.scheduling.OrderInformation;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSearchResult;
import com.gip.xyna.xprc.xsched.selectvetos.VetoSelectImpl;

public interface VetoManagementInterface {

  /**
   * Versucht, die �bergebenen Vetos f�r die angegebene OrderInformation zu belegen.
   * Kann mehrfach gerufen werden, loggt dann aber eine Meldung.
   * @param orderInformation
   * @param vetos
   * @param urgency
   * @return
   */
  public VetoAllocationResult allocateVetos(OrderInformation orderInformation, List<String> vetos, long urgency);

  /**
   * Macht die Belegungen des letzten allocateVetos(...) r�ckg�ngig.
   * @param orderInformation
   * @param vetos
   */
  public void undoAllocation(OrderInformation orderInformation, List<String> vetos);

  /**
   * Macht die Belegungen des letzten allocateVetos(...) permanent.
   * @param orderInformation
   * @param vetos
   */
  public void finalizeAllocation(OrderInformation orderInformation, List<String> vetos);  

  /**
   * Versucht, die Vetos f�r die �bergebene XynaOrder freizugeben.
   * Kann mehrfach gerufen werden, loggt dann aber eine Meldung.
   * @param orderInformation
   * @return false, wenn Vetos nicht oder von einem anderen Auftrag belegt waren 
   */
  public boolean freeVetos(OrderInformation orderInformation);

  /**
   * Versucht, die Vetos f�r die �bergebene XynaOrder freizugeben.
   * Falls keine allokierten Vetos direkt gefunden werden, werden nochmal 
   * alle Vetos durchsucht.
   * Kann mehrfach gerufen werden, loggt dann aber eine Meldung.
   * @param orderId
   * @return false, wenn Vetos nicht oder von einem anderen Auftrag belegt waren 
   */
  public boolean freeVetosForced(long orderId);

 
  /**
   * Setzt ein administratives Veto
   * @param administrativeVeto
   * @throws XPRC_AdministrativeVetoAllocationDenied
   * @throws PersistenceLayerException
   */
  public void allocateAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XPRC_AdministrativeVetoAllocationDenied, PersistenceLayerException;

  /**
   * �ndert Dokumentation eines administrativen Vetos, gibt alte Dokumentation zur�ck
   * @param administrativeVeto
   * @return
   * @throws PersistenceLayerException
   * @throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY
   */
  public String setDocumentationOfAdministrativeVeto(AdministrativeVeto administrativeVeto) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

  /**
   * Entfernt ein administratives Veto, gibt entferntes Veto zur�ck
   * @param administrativeVeto
   * @return
   * @throws XPRC_AdministrativeVetoDeallocationDenied
   * @throws PersistenceLayerException
   */
  public VetoInformation freeAdministrativeVeto(AdministrativeVeto administrativeVeto) throws XPRC_AdministrativeVetoDeallocationDenied, PersistenceLayerException;

  /**
   * Anzeige aller gesetzten Vetos
   * @return
   */
  public Collection<VetoInformation> listVetos();

  /**
   * TODO wof�r?
   * @param select
   * @param maxRows
   * @return
   * @throws PersistenceLayerException
   */
  public VetoSearchResult searchVetos(VetoSelectImpl select, int maxRows) throws PersistenceLayerException;

  
  /**
   * Gibt VetoManagementAlgorithmType zur�ck
   * @return
   */
  public VetoManagementAlgorithmType getAlgorithmType();

  /**
   * Ausgabe in CLI listExtendedSchedulerInfo
   * @return
   */
  public String showInformation();

}