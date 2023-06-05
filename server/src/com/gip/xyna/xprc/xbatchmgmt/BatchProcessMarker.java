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
package com.gip.xyna.xprc.xbatchmgmt;

import java.io.Serializable;



public class BatchProcessMarker implements Serializable{

  private static final long serialVersionUID = 1L;
  private final Long batchProcessId;
  private final boolean isBatchProcessMaster;

  
  public BatchProcessMarker(Long batchProcessId, boolean isBatchProcessMaster) {
    if( batchProcessId == null ) {
      throw new IllegalArgumentException("batchProcessId must not be null");
    }
    this.batchProcessId = batchProcessId;
    this.isBatchProcessMaster = isBatchProcessMaster;
  }
  
  @Override
  public String toString() {
    return "BatchProcessMarker("+batchProcessId+","+isBatchProcessMaster+")";
  }
  
  public Long getBatchProcessId() {
    return batchProcessId;
  }
  
  public boolean isBatchProcessMaster() {
    return isBatchProcessMaster;
  }
}
