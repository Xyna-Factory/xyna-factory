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
package com.gip.xyna.xmcp.xguisupport.messagebus;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeDependencyContext;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;


public class Publisher {

  private static final Logger logger = CentralFactoryLogging.getLogger(Publisher.class);

  private String creator;
  
  public Publisher(String creator) {
    if (creator == null) {
      creator = " -unknown"; //leerzeichen vorne, um konfliktpotential zu reduzieren (es sollte keine usernamen geben, die mit leerzeichen beginnen)
    }
    this.creator = creator;
  }

  
  public void publishRuntimeContextCreate(RuntimeDependencyContext runtimeContext) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_CREATE, runtimeContext);
  }
  
  public void publishRuntimeContextCreate(String runtimeContextIndentification) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_CREATE, runtimeContextIndentification);
  }

  public void publishRuntimeContextUpdate(RuntimeDependencyContext runtimeContext) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_UPDATE, runtimeContext);
  }
  
  public void publishRuntimeContextUpdate(String runtimeContextIndentification) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_UPDATE, runtimeContextIndentification);
  }

  public void publishRuntimeContextDelete(RuntimeDependencyContext runtimeContext) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_DELETE, runtimeContext);
  }
  
  public void publishRuntimeContextDelete(String runtimeContextIndentification) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_RUNTIME_CONTEXT_DELETE, runtimeContextIndentification);
  }
  
  public void publishXMOMUpdate(RuntimeDependencyContext runtimeContext) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_MODELLER_UPDATE, runtimeContext);
  }
  
  public void publishXMOMUpdate(String runtimeContextIndentification) {
    publishRuntimeContextAction(PredefinedMessagePath.XYNA_MODELLER_UPDATE, runtimeContextIndentification);
  }
  
  
  
  public void publishRuntimeContextAction(PredefinedMessagePath path, RuntimeDependencyContext runtimeContext) {
    publishRuntimeContextAction(path, runtimeContext.getGUIRepresentation());
  }
  
  public void publishRuntimeContextAction(PredefinedMessagePath path, String publishRuntimeContextAction) {
    publish(path, publishRuntimeContextAction, new ArrayList<SerializablePair<String,String>>());
  }
  
  
  private void publish(PredefinedMessagePath messagePath, String runtimeContextRepresentation, List<SerializablePair<String, String>> payload) {
    MessageInputParameter mip = new MessageInputParameter(messagePath.getProduct(),
                                                          messagePath.getContext(),
                                                          runtimeContextRepresentation,
                                                          creator,
                                                          payload,
                                                          messagePath.isPersistent());
    try {
      XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().publish(mip);
    } catch (XynaException e) {
      logger.error("Failed to notify gui", e);
    }
  }
}
