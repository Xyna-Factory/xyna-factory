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
package com.gip.xyna.xmcp.xfcli.impl;



import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.appmgmt.RevisionOrderControl;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Startorder;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xprc.ResponseListener;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_OrderEntryCouldNotBeAcknowledgedException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.DeploymentManagement;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.OrderContext;
import com.gip.xyna.xprc.xpce.OrderContextServerExtension.AcknowledgableObject;
import com.gip.xyna.xprc.xsched.DefaultBackupAcknowledgableObject;
import com.gip.xyna.xprc.xsched.timeconstraint.TimeConstraint;



public class StartorderImpl extends XynaCommandImplementation<Startorder> {

  public void execute(OutputStream statusOutputStream, Startorder parameter) throws XynaException {

    if (XynaFactory.getInstance().isShuttingDown()) {
      writeLineToCommandLine(statusOutputStream, "Factory is shutting down, no orders may be started.");
      return;
    } else if (XynaFactory.getInstance().isStartingUp()) {
      writeLineToCommandLine(statusOutputStream, "Factory is starting up, no orders may be started.");
      return;
    }

    String orderType = parameter.getOrderType();

    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(orderType);
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    RuntimeContext runtimeContext = RevisionManagement.getRuntimeContext(parameter.getApplicationName(), parameter.getVersionName(), parameter.getWorkspaceName());
    xocp.getDestinationKey().setRuntimeContext(runtimeContext);

    try {
      handleTimeout(xocp, parameter);
      handleTimeconstraint(xocp, parameter);
      handlePriority(xocp, parameter);
      handleInputPayload( xocp, parameter );
    } catch( HandlingException e ) {
      writeLineToCommandLine(statusOutputStream, e.getMessage()+", aborting.");
      return;
    }    
    
    xocp.setIdOfLatestDeploymentKnownToOrder(DeploymentManagement.getInstance().getLatestDeploymentId());
    RevisionOrderControl.checkCliClosed(xocp.getDestinationKey().getApplicationName(), xocp.getDestinationKey().getVersionName());
    
    StartOrderResponseListener sorl = 
        new StartOrderResponseListener(parameter.getAcknowledge(), parameter.getSynchronous() );
    
    if( parameter.getAcknowledge() ) {
      xocp.setAcknowledgableObject(sorl.getAck());
    }
    
    long id = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().startOrder(xocp, sorl);
    writeLineToCommandLine(statusOutputStream, "Started order with id '" + id + "'.");
    
    if( parameter.getAcknowledge() ) {
      sorl.waitForAcknowledge();
      writeLineToCommandLine(statusOutputStream, "Scheduler acknowledged entry of order with id '" + id + "'.");
    }
    
    if( parameter.getSynchronous() ) {
      sorl.waitForExecution();
      if( parameter.getPrint() ) {
        writeLineToCommandLine(statusOutputStream, sorl.getResponse().toXml());
      }
    }

  }
 

  private void handleTimeout(XynaOrderCreationParameter xocp, Startorder parameter) throws HandlingException {
    if (parameter.getTimeout() == null) {
      return; //nichts zu tun
    }
    try {
      long timeout = Long.parseLong(parameter.getTimeout());
      xocp.setTimeConstraint(TimeConstraint.immediately().withSchedulingTimeout(timeout));
    } catch (NumberFormatException e) {
      throw new HandlingException("Unparsable timeout <" + parameter.getTimeout() + ">");
    }
  }
  
  private void handleTimeconstraint(XynaOrderCreationParameter xocp, Startorder parameter) throws HandlingException {
    if (parameter.getTimeconstraint() == null) {
      return; //nichts zu tun
    }
    try {
      String timeconstraint = parameter.getTimeconstraint();
      xocp.setTimeConstraint( TimeConstraint.valueOf(timeconstraint) );
    } catch (IllegalArgumentException e) {
      throw new HandlingException("Unparsable time  constraint <" + parameter.getTimeconstraint() + ">: "+e.getMessage());
    }
  }
  
  private void handlePriority(XynaOrderCreationParameter xocp, Startorder parameter) throws HandlingException {
    if (parameter.getPriority() == null) {
      return; //nichts zu tun
    }
    try {
      int prio = Integer.parseInt(parameter.getPriority());
      xocp.setPriority(prio);
    } catch (NumberFormatException e) {
      throw new HandlingException("Unparsable priority <" + parameter.getPriority() + ">");
    }
  }


  private void handleInputPayload(XynaOrderCreationParameter xocp, Startorder parameter) throws HandlingException, Ex_FileAccessException, XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if (parameter.getInputPayloadFile() == null) {
      xocp.setInputPayload( new Container() );
      return;
    }
    
    Long revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
    if(parameter.getApplicationName() != null || parameter.getWorkspaceName() != null) {
      RevisionManagement revisionManagement =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
      try {
        revision = revisionManagement.getRevision(parameter.getApplicationName(), parameter.getVersionName(), parameter.getWorkspaceName());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        throw new HandlingException("Workspace or Application/Version not found.");
      }
    }
    GeneralXynaObject orderPayload = null;
    File inputPayloadFile = new File(parameter.getInputPayloadFile());
    if (!inputPayloadFile.exists()) {
      throw new HandlingException("Specified file for input payload <" + parameter.getInputPayloadFile()
                                  + "> does not exist.");
    }
    Document doc = XMLUtils.parse(inputPayloadFile);
    Element root = doc.getDocumentElement();
    List<Element> payloadObjects = XMLUtils.getChildElements(root);

    if (payloadObjects.size() == 1) {
      orderPayload = XynaObject.generalFromXml(XMLUtils.getXMLString(payloadObjects.get(0), false), revision);
    } else if (payloadObjects.size() > 1) {
      GeneralXynaObject[] xynaObjectPayloadList = new GeneralXynaObject[payloadObjects.size()];
      for (int i = 0; i < payloadObjects.size(); i++) {
        xynaObjectPayloadList[i] = XynaObject.generalFromXml(XMLUtils.getXMLString(payloadObjects.get(i), false), revision);
      }
      orderPayload = new Container(xynaObjectPayloadList);
    }
    xocp.setInputPayload( orderPayload );
  }

  private static class HandlingException extends Exception {
    private static final long serialVersionUID = 1L;

    public HandlingException(String message) {
      super(message);
    }

  }

  public static class StartOrderResponseListener extends ResponseListener {
    
    private static final long serialVersionUID = 1L;
    
    private transient CLIAcknowledgableObject ack;
    private transient CountDownLatch latch;
    private Throwable throwable;
    private GeneralXynaObject response;
    
    public StartOrderResponseListener(CLIAcknowledgableObject ack) {
      this.ack = ack;
    }
    
    public AcknowledgableObject getAck() {
      return ack;
    }

    public StartOrderResponseListener(boolean acknowledge, boolean synchronous) {
      if( acknowledge ) {
        this.ack = new CLIAcknowledgableObject();
      }
      if( synchronous ) {
        latch = new CountDownLatch(1);
      }
    }
    
    public void waitForAcknowledge() throws XynaException {
      if( ack != null ) {
        ack.waitForAcknowledge();
      }
      rethrow(throwable);
    }
    
    @Override
    public void onResponse(GeneralXynaObject response, OrderContext ctx) throws XNWH_RetryTransactionException {
      this.response = response;
      if( latch != null ) {
        latch.countDown();
      }
    }
    
    @Override
    public void onError(XynaException[] e, OrderContext ctx) throws XNWH_RetryTransactionException {
      if (e.length > 0) {
        throwable = e[0];
      } else {
        throwable = new RuntimeException("Missing exception");
      }
      if(ack != null) {
        //handle exceptions during planning -> vor Acknowledge
        ack.latch.countDown();
      }
      if( latch != null ) {
        latch.countDown();
      }
    }
        
    public void waitForExecution() throws XynaException {
      if( latch != null ) {
        try {
          latch.await();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
      rethrow(throwable);
    }
    
    public GeneralXynaObject getResponse() {
      return response;
    }

  }
  
  public static class CLIAcknowledgableObject extends DefaultBackupAcknowledgableObject {

    private static final long serialVersionUID = 4142428232319469624L;
    private final CountDownLatch latch;
    private Throwable t = null;
    

    CLIAcknowledgableObject() {
      latch = new CountDownLatch(1);
    }


    @Override
    public void acknowledgeSchedulerEntry(XynaOrderServerExtension xose)
        throws XPRC_OrderEntryCouldNotBeAcknowledgedException {
      try {
        super.acknowledgeSchedulerEntry(xose);
      } catch (Error e) {
        this.t = e;
        throw e;
      } catch (RuntimeException e) {
        this.t = e;
        throw e;
      } finally {
        latch.countDown();
      }
    }


    void waitForAcknowledge() throws XynaException {
      try {
        latch.await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      rethrow(t);
    }
    
  }
  
  public static void rethrow(Throwable t) throws XynaException {
    if (t != null) {
      if (t instanceof XynaException) {
        throw (XynaException) t;
      } else if (t instanceof Error) {
        throw (Error) t;
      } else if (t instanceof RuntimeException) {
        throw (RuntimeException) t;
      } else {
        throw new RuntimeException(t);
      }
    }
  }

  
}
