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
package com.gip.xyna.xfmg.xfctrl.nodemgmt;

import java.io.Serializable;
import java.util.Set;

import com.gip.xyna.xmcp.OrderExecutionResponse;

public abstract class RemoteData implements Serializable {

  private static final long serialVersionUID = 1L;
  
  public static class RemoteDataOrderResponse extends RemoteData {

    private static final long serialVersionUID = 1L;
    private OrderExecutionResponse response;
    
    public RemoteDataOrderResponse(OrderExecutionResponse response) {
      this.response = response;
    }
    
    public OrderExecutionResponse getResponse() {
      return response;
    }
    
  }
  
  public static class RemoteDataApplicationChangeNotification extends RemoteData {

    private static final long serialVersionUID = 1L;
    private Set<String> applications;

    public RemoteDataApplicationChangeNotification(Set<String> applications) {
      this.applications = applications;
    }


    public Set<String> getApplications() {
      return applications;
    }
  }

}
