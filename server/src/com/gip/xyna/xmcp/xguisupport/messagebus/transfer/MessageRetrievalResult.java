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
package com.gip.xyna.xmcp.xguisupport.messagebus.transfer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;


public class MessageRetrievalResult implements Serializable {

  private static final long serialVersionUID = -712776577253591916L;
  
  private final List<MessageOutputParameter> messages;
  private final long lastCheckedId;
  private static final Comparator<MessageOutputParameter> COMPARATOR_MESSAGES = new Comparator<MessageOutputParameter>() {

    public int compare(MessageOutputParameter o1, MessageOutputParameter o2) {
      if (o1.getId().equals(o2.getId())) {
        return 0;
      }
      if (o1.getId() > o2.getId()) {
        return 1;
      }
      return -1;
    }

  };
  
  public MessageRetrievalResult(Set<MessageOutputParameter> messages, long lastCheckedId) {
    List<MessageOutputParameter> l = new ArrayList<MessageOutputParameter>(messages);
    Collections.sort(l, COMPARATOR_MESSAGES);
    this.messages = l;
    this.lastCheckedId = lastCheckedId;
  }

  
  public List<MessageOutputParameter> getMessages() {
    return messages;
  }

  
  public long getLastCheckedId() {
    return lastCheckedId;
  }

}
