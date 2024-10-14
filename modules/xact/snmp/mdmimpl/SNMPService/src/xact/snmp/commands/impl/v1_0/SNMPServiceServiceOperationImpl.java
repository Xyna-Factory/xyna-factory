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
package xact.snmp.commands.impl.v1_0;



import java.util.ArrayList;
import java.util.List;

import xact.snmp.OID;
import xact.snmp.OIDs;
import xact.snmp.RetryModel;
import xact.snmp.SNMPConnectionData;
import xact.snmp.SimpleRetryModel;
import xact.snmp.VarBinding;
import xact.snmp.VarBindings;
import xact.snmp.commands.SNMPServiceServiceOperation_v1_0;
import xact.snmp.exception.SNMPConnectionException;
import xact.snmp.exception.SNMPResponseException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;



public class SNMPServiceServiceOperationImpl implements ExtendedDeploymentTask, SNMPServiceServiceOperation_v1_0 {

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
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
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


  private xact.snmp.commands.impl.SNMPServiceServiceOperationImpl newImpl = new xact.snmp.commands.impl.SNMPServiceServiceOperationImpl();


  public List<? extends VarBinding> get(List<? extends VarBinding> varBinding, SNMPConnectionData connectionData)
      throws SNMPConnectionException, SNMPResponseException {
    OIDs oids = new OIDs();
    for (VarBinding vb : varBinding) {
      oids.addToOID(vb.getOID());
    }
    VarBindings varBindings = newImpl.get(oids, connectionData, new SimpleRetryModel(0, 0));
    List<VarBinding> varBinds = (List<VarBinding>) varBindings.getVarBinding();
    if (varBinds == null) {
      return new ArrayList<VarBinding>();
    } else {
      return varBinds;
    }
  }


  public void setWithRetry(List<? extends VarBinding> varBindList, SNMPConnectionData connectionData, RetryModel retryModel)
      throws SNMPConnectionException, SNMPResponseException {
    VarBindings varBindings = new VarBindings();
    for (VarBinding vb : varBindList) {
      varBindings.addToVarBinding(vb);
    }
    newImpl.set(varBindings, connectionData, retryModel);
  }


  public void trap(List<? extends VarBinding> varBinding, SNMPConnectionData connectionData, OID trapOID) throws SNMPConnectionException,
      SNMPResponseException {
    VarBindings varBindings = new VarBindings();
    for (VarBinding vb : varBinding) {
      varBindings.addToVarBinding(vb);
    }
    newImpl.trap(trapOID, varBindings, connectionData, new SimpleRetryModel(0, 0));
  }


  public List<? extends VarBinding> walk(VarBinding varBinding, SNMPConnectionData sNMPConnectionData) throws SNMPConnectionException,
      SNMPResponseException {
    VarBindings varBindings = newImpl.walk(sNMPConnectionData, varBinding.getOID());
    List<VarBinding> varBinds = (List<VarBinding>) varBindings.getVarBinding();
    if (varBinds == null) {
      return new ArrayList<VarBinding>();
    } else {
      return varBinds;
    }
  }


  public void set(List<? extends VarBinding> varBinding, SNMPConnectionData connectionData) throws SNMPConnectionException,
      SNMPResponseException {
    setWithRetry(varBinding, connectionData, new SimpleRetryModel(0, 0));
  }
}
