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
package com.gip.xyna.xprc.xprcods.orderarchive;

import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.ProcessStep;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.ProcessStepHandlerType;

/**
 * schnittstelle fuer die nicht archivierten auftraege
 */
public interface OrderDBAPI {

  /**
   * fuegt auftrag hinzu
   * @param order
   */
  public void insert(XynaOrderServerExtension order) throws PersistenceLayerException;


  public void updateStatus(XynaOrderServerExtension order, OrderInstanceStatus status, ODSConnection con) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  public void updateAuditData(XynaOrderServerExtension order, ProcessStep pstep, ProcessStepHandlerType auditDataType)
                  throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  public void updateStatusOnError(XynaOrderServerExtension order, OrderInstanceStatus status) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
}
