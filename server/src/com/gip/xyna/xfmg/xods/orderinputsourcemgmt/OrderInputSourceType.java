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
package com.gip.xyna.xfmg.xods.orderinputsourcemgmt;

import java.util.Map;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

/**
 * Erzeugung von Auftrags-Inputs:
 * Auftragsinputs können beliebig kompliziert erstellt werden
 *  - konstant
 *  - Workflow erzeugt Input teilweise aus Datenhaltung in DB mit verbrauchbaren Daten (Usecase XTF)
 * <ul>
 * <li>
 * {@link OrderInputSourceType} ist die Basisklasse (Dokumentation + Erzeugung von {@link OrderInputSource}
 * </li>
 * <li>
 * {@link OrderInputSource} Instanzen entsprechen den Einträgen die in der GUI sichtbar sind. Immer wenn ein Auftrag
 * bzgl eines Typs mit Parametrisierung gestartet werden soll, wird auf die zugehörige {@link OrderInputSource} Instanz zugegriffen und daraus eine
 * {@link OrderInputCreationInstance} erzeugt
 * </li>
 * <li>
 * {@link OrderInputCreationInstance} existiert pro Auftrag.
 * </li>
 * </ul>
 */
public interface OrderInputSourceType {

  public PluginDescription showDescription();


  /**
   * @param name Name der InputSource, der auch in der GUI als Identifikation verwendet wird
   * @param destinationKey Was für ein Auftragstyp soll mit dieser InputSource gestartet werden
   * @param parameters Was sind Typ-spezifische Parameter? (Z.B. bei Konstanten Inputs die Konstante)
   */
  public OrderInputSource createOrderInputSource(String name, DestinationKey destinationKey, Map<String, Object> parameters,
                                                       String documentation) throws XynaException;


  /**
   * refactored die übergebenen parameter in der map
   * @return true falls refactoring stattgefunden hat
   */
  public boolean refactorParameters(Map<String, Object> parameters, DependencySourceType refactoredObjectType, String fqNameOld, String fqNameNew) throws XynaException;
}
