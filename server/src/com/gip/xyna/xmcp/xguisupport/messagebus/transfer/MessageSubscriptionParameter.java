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
package com.gip.xyna.xmcp.xguisupport.messagebus.transfer;

import java.io.Serializable;



public class MessageSubscriptionParameter implements Serializable {

  private static final long serialVersionUID = 4209231871153821458L;
  
  private Long id;
  private String product;
  private String context;
  private String filter;
  
  
  public MessageSubscriptionParameter(Long id, String product, String context, String filter) {
    this.id = id;
    this.product = product;
    this.context = context;
    this.filter = filter;
  }
  
  
  public Long getId() {
    return id;
  }
  
  
  public String getProduct() {
    return product;
  }
  
  
  public String getContext() {
    return context;
  }
  
  
  public String getFilter() {
    return filter;
  }

  
}
