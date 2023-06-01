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
package com.gip.xyna.xprc.xpce.transaction.parameter;

import java.util.Map;


public final class TransactionParameter {
  
  private final String type;
  private final SafeguardParameter safeguard;
  private final Map<String, String> specificis;
  
  
  public TransactionParameter(String type, SafeguardParameter safeguard, Map<String, String> specificis) {
    this.type = type;
    this.safeguard = safeguard;
    this.specificis = specificis;
  }
  
  public String getTransactionType() {
    return type;
  }

  public SafeguardParameter getSafeguardParameter() {
    return safeguard;
  }

  public Map<String, String> getTransactionTypeSpecifics() {
    return specificis;
  }

  
}
