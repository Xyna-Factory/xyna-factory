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
package com.gip.xyna.xprc.xprcods.orderarchive;



import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.TwoConnectionBean;
import com.gip.xyna.xprc.xprcods.orderarchive.selectorder.OrderInstanceSelect;



/**
 * methoden, die auf mehr als eine persistence-schicht aus (orderarchiv, orderbackup, orderdb) zugreifen.
 */
public interface OrderAPI {

  /**
   * gibt liste aller gew�hlten auftr�ge zur�ck
   */
  public OrderInstanceResult search(OrderInstanceSelect select, int maxRows) throws PersistenceLayerException;


  /**
   * holt gesamten Auftrag zur OrderId
   */
  public OrderInstanceDetails getCompleteOrder(long orderId) throws PersistenceLayerException,
                  XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;


  /**
   * liest auftrag aus orderdb und schreibt auftrag in orderarchive. auftrag wird aus db und backup gel�scht.
   * @return 
   */
  public TwoConnectionBean archive(XynaOrderServerExtension order) throws PersistenceLayerException;

}
