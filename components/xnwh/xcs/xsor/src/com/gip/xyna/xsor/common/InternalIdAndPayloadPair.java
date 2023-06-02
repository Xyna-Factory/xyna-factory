/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package com.gip.xyna.xsor.common;

import com.gip.xyna.xsor.protocol.XSORPayload;


public class InternalIdAndPayloadPair {

  private final int internalId;
  private final XSORPayload payload;
  
  public InternalIdAndPayloadPair(int internalId, XSORPayload payload) {
    this.internalId = internalId;
    this.payload = payload;
  }
  
  public XSORPayload getPayload() {
    return payload;
  }
  
  public int getInternalId() {
    return internalId;
  }
  
}
