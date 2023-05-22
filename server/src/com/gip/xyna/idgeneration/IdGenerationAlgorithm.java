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

package com.gip.xyna.idgeneration;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


/**
 * Interface, um verschiedene IdGeneration-Algorithmen im IDGenerator verwenden zu k�nnen.
 * IDGenerator stellt sicher, dass auf die Methoden dieses Interfaces nicht konkurrierend 
 * zugegriffen wird, daher sind keine weiteren Locks etc. n�tig.
 */
public interface IdGenerationAlgorithm {

  /**
   * IdGenerationAlgorithm intialisieren
   */
  void init() throws PersistenceLayerException;

  /**
   * IdGenerationAlgorithm beenden
   */
  void shutdown() throws PersistenceLayerException;

  /**
   * @param realm 
   * @return neue eindeutige Id
   */
  long getUniqueId(String realm);

  long getIdLastUsedByOtherNode(String realm);

  void storeLastUsed(String realm);
  

}
