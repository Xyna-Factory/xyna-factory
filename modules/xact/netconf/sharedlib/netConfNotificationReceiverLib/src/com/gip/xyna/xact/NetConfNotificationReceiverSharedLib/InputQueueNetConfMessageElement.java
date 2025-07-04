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


public class InputQueueNetConfMessageElement {

  private String MessageID;
  private String ConnectionID;
  private String RDID;
  private String NetConfMessage;
  private boolean valid;


  InputQueueNetConfMessageElement(String ConnectionID, String RDID, String MessageID, String NetConfMessage, boolean valid) {
    try {
      this.ConnectionID = ConnectionID;
      this.RDID = RDID;
      this.MessageID = MessageID;
      this.NetConfMessage = NetConfMessage;
      this.valid = valid;
    } catch (Throwable t) {
      this.ConnectionID = "invalid";
      this.NetConfMessage = "Initialization of InputQueueNetConfMessageElement Instance failed";
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


  public String getNetConfMessage() throws Throwable {
    return this.NetConfMessage;
  }


  public String getMessageID() throws Throwable {
    return this.MessageID;
  }

}
