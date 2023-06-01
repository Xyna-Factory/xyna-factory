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

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public class RemoteCallXynaOrderCreationParameter extends XynaOrderCreationParameter {

  private static final long serialVersionUID = 7549429441296601965L;
  private static final XynaXmomSerialization serialization = XynaFactory.getInstance().getProcessing().getXmomSerialization();
  
  private byte[] payloadBytes;
  private String fqn;

  public RemoteCallXynaOrderCreationParameter(DestinationKey dk, GeneralXynaObject... inputPayload) {
    super(dk, inputPayload);
  }
  
  
  public RemoteCallXynaOrderCreationParameter(XynaOrderCreationParameter xocp) {
    super(xocp);
    setInputPayload(xocp.getInputPayload());
    removeXynaObjectInputPayload();
  }
  
  
  private Long getRevision() {
    try {
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(getDestinationKey().getRuntimeContext());
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      CentralFactoryLogging.getLogger(RemoteCallXynaOrderCreationParameter.class)
          .warn("Could not find revision for destinationKey trying WorkingSet", e);
      return RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    }
  }
  
  @Override
  public void setInputPayload(GeneralXynaObject payload) {
    if (payload != null) {
      payloadBytes = serialization.serialize(getRevision(), payload);
    }
  }
  
  
  @Override
  public void setInputPayload(GeneralXynaObject... payload) {
    GeneralXynaObject objToSet;

    if (payload == null || (payload.length == 1 && payload[0] == null)) {
      objToSet = null;
      fqn = null;
    } else if (payload.length == 1) {
      objToSet = payload[0];
      fqn = XynaXmomSerialization.getFqnXmlName(objToSet.getClass());
    } else {
      Container c = new Container(payload);
      objToSet = c;
      fqn = Container.class.getCanonicalName();
    }
    payloadBytes = serialization.serialize(getRevision(), objToSet);

  }
  
  
  public void convertInputPayload() {
    if(fqn == null) { //sending a (single) null-value
      super.setInputPayload(new GeneralXynaObject[] { null });
    }
    else if(payloadBytes == null) { //no input
      super.setInputPayload(new Container());
    } else {
      GeneralXynaObject resultObj = serialization.deserialize(getRevision(), fqn, payloadBytes);
      super.setInputPayload(resultObj);
    }
    payloadBytes = null;
  }


  /**
   * Entfernen der XynaObject-InputPayload, damit die RemoteXorderCreationParameter
   * auch wieder auﬂerhalb der Factory deserialisiert werden kann
   */
  public void removeXynaObjectInputPayload() {
    super.setInputPayload((GeneralXynaObject) null);
  }
}
