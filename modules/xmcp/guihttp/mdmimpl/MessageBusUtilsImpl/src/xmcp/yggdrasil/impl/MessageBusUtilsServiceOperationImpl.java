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
package xmcp.yggdrasil.impl;


import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.SerializablePair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.util.xo.XynaObjectJsonBuilder;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xmcp.xguisupport.messagebus.transfer.MessageInputParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xmcp.yggdrasil.Event;
import xmcp.yggdrasil.Message;
import xmcp.yggdrasil.MessageBusUtilsServiceOperation;


public class MessageBusUtilsServiceOperationImpl implements ExtendedDeploymentTask, MessageBusUtilsServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(MessageBusUtilsServiceOperationImpl.class);

  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  public void publish(XynaOrderServerExtension correlatedXynaOrder, Message message) throws XynaException {
    long rootOrderRev = correlatedXynaOrder.getRootOrder().getRevision();
    String json = toJSON(message.getEvent(), rootOrderRev);
    List<SerializablePair<String, String>> payload = new ArrayList<SerializablePair<String, String>>();
    payload.add(new SerializablePair<String, String>("JSON Object", json));

    MessageInputParameter mip = new MessageInputParameter(message.getProduct(), message.getContext(), message.getCorrelation(), message.getEvent().getCreator(), payload, message.getPersistent());
    XynaFactory.getInstance().getXynaMultiChannelPortal().getMessageBusManagement().publish(mip);
  }

  private String toJSON(Event event, long revision) {
    StringWriter writer = new StringWriter();
    JsonBuilder jsonBuilder = new JsonBuilder(writer);
    XynaObjectJsonBuilder builder = new XynaObjectJsonBuilder(revision, jsonBuilder);
    builder.build(event);

    return writer.toString();
  }

}
