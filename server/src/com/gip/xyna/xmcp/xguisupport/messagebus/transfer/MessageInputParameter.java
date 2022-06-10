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

import java.util.List;

import com.gip.xyna.utils.collections.SerializablePair;



public class MessageInputParameter extends MessageBaseParameter {
  

  private static final long serialVersionUID = -7471034590865640291L;
  
  private boolean persistent = false;
  
  public MessageInputParameter(String product, String context, String correlation, String creator, List<SerializablePair<String, String>> payload, boolean persistent) {
    super(product, context, correlation, creator, payload);
    this.persistent = persistent;
  }
  
  
  public boolean isPersistent() {
    return persistent;
  }

  
}
