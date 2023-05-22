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
package com.gip.xyna.xprc.xsched.orderseries;


/**
 *
 */
public class OSMRemoteEndpointImpl implements OSMRemoteInterface {

  OSMLocalImpl localImpl;
  int ownBinding;
  
  public OSMRemoteEndpointImpl(OSMLocalImpl localImpl, int ownBinding) {
    this.localImpl = localImpl;
    this.ownBinding = ownBinding;
  }
  
  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface#updateSuccessor(int, java.lang.String, java.lang.String, long, boolean)
   */
  public Result updateSuccessor(int binding, String successorCorrId, String predecessorCorrId, long predecessorOrderId,
                                boolean cancel) {
    if( binding == ownBinding ) {
      return localImpl.updateSuccessor(binding, successorCorrId, predecessorCorrId, predecessorOrderId, cancel);
    } else {
      return Result.NotFound;
    }
  }

  /* (non-Javadoc)
   * @see com.gip.xyna.xprc.xsched.orderseries.OSMRemoteInterface#updatePredecessor(int, java.lang.String, java.lang.String, long)
   */
  public Result updatePredecessor(int binding, String predecessorCorrId, String successorCorrId, long successorOrderId) {
    if( binding == ownBinding ) {
      return localImpl.updatePredecessor(binding, predecessorCorrId, successorCorrId, successorOrderId);
    } else {
      return Result.NotFound;
    }
  }

}
