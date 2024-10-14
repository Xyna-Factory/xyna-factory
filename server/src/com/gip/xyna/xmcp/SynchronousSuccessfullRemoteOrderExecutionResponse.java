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

package com.gip.xyna.xmcp;



import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization;



public class SynchronousSuccessfullRemoteOrderExecutionResponse extends SuccesfullOrderExecutionResponse {


  private static final long serialVersionUID = -7128854812391247843L;
  private static final XynaXmomSerialization serialization = XynaFactory.getInstance().getProcessing().getXmomSerialization();
  
  private byte[] payload;
  private String fqn;
  private Long createdInRevision; //only valid for system creating the object. - required to store response


  public SynchronousSuccessfullRemoteOrderExecutionResponse(GeneralXynaObject response, Long id, Long revision) {
    super(id);
    createdInRevision = revision;
    if (response != null) {
      payload = serialization.serialize(revision, response);
      fqn = response.getClass().getCanonicalName();
    }
  }


  public byte[] getPayload() {
    return payload;
  }


  public String getFqn() {
    return fqn;
  }


  public Long getCreatedInRevision() {
    return createdInRevision;
  }
  
  @Override
  public boolean isSynchronousResponse() {
    return true;
  }
}
