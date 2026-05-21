/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package com.gip.xyna.xact.NetConfNotificationReceiverSharedLib;


public class OutputQueueNetConfOperationElement {

  private String ConnectionID;
  private String RDID;
  private String MessageID;
  private String NetConfOperation;
  private boolean valid;


  OutputQueueNetConfOperationElement(String ConnectionID, String RDID, String MessageID, String NetConfOperation) {
    try {
      this.ConnectionID = ConnectionID;
      this.RDID = RDID;
      this.MessageID = MessageID;
      this.NetConfOperation = NetConfOperation;
      this.valid = true;
    } catch (Throwable t) {
      this.valid = false;
    }
  }


  public boolean isValid() throws Throwable {
    return valid;
  }


  public String getConnectionID() throws Throwable {
    return this.ConnectionID;
  }


  public String getRDID() throws Throwable {
    return this.RDID;
  }


  public String getMessageID() throws Throwable {
    return this.MessageID;
  }


  public String getNetConfOperation() throws Throwable {
    return this.NetConfOperation;
  }

}
