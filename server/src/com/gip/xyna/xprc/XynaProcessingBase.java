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

package com.gip.xyna.xprc;



import com.gip.xyna.Department;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.exceptions.XPRC_CancelFailedException;
import com.gip.xyna.xprc.remotecallserialization.XynaXmomSerialization;
import com.gip.xyna.xprc.xbatchmgmt.BatchProcessManagement;
import com.gip.xyna.xprc.xfqctrl.XynaFrequencyControl;
import com.gip.xyna.xprc.xfractwfe.XynaPythonSnippetManagement;
import com.gip.xyna.xprc.xpce.WorkflowEngine;
import com.gip.xyna.xprc.xpce.XynaProcessCtrlExecution;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;
import com.gip.xyna.xprc.xsched.XynaScheduler;
import com.gip.xyna.xprc.xsched.ordercancel.CancelBean;



public abstract class XynaProcessingBase extends Department implements XynaProcessingPortal {

  public XynaProcessingBase() throws XynaException {
    super();
  }


  public abstract XynaScheduler getXynaScheduler();


  public abstract XynaProcessCtrlExecution getXynaProcessCtrlExecution();


  public abstract WorkflowEngine getWorkflowEngine();


  public abstract XynaProcessingODS getXynaProcessingODS();
  
  
  public abstract XynaFrequencyControl getFrequencyControl();


  public abstract BatchProcessManagement getBatchProcessManagement();

  
  public abstract XynaXmomSerialization getXmomSerialization();

  
  public abstract XynaPythonSnippetManagement getXynaPythonSnippetManagement();
  
  
  public abstract CancelBean cancelOrder(Long id, Long timeout, boolean waitForTimeout)
      throws XPRC_CancelFailedException;


  public abstract void stopGracefully() throws XynaException;


  public abstract OrderStatus getOrderStatus();
}
