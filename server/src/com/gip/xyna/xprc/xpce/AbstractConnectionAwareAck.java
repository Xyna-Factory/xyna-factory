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

package com.gip.xyna.xprc.xpce;



import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.StorableClassList;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension.AcknowledgableObject;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceBackup;



public abstract class AbstractConnectionAwareAck implements AcknowledgableObject {

  private static final long serialVersionUID = 1L;
  private static final StorableClassList BACKUP_STORABLES = new StorableClassList(OrderInstanceBackup.class);

  private final boolean connectionPresent;
  private transient ODSConnection con;


  public AbstractConnectionAwareAck(ODSConnection con) {
    this.con = con;
    this.connectionPresent = con != null;
  }

  public StorableClassList backupStorables() {
    return BACKUP_STORABLES;
  }

  public final ODSConnection getConnection() {
    return con;
  }


  protected final boolean isConnectionPresent() {
    return connectionPresent;
  }

  public void setConnection(ODSConnection con) {
    this.con = con;
  }

  public void handleErrorAtPlanning(XynaOrderServerExtension xose, Throwable throwable) {
  }

}
