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
package com.gip.xyna.utils.install.as.oracle;



import java.util.Properties;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.oracle.bpel.client.Locator;
import com.oracle.bpel.client.Server;
import com.oracle.bpel.client.ServerException;
import com.oracle.bpel.client.auth.ServerAuth;
import com.oracle.bpel.client.auth.ServerAuthFactory;



/**
 */
public abstract class XynaOracleTask extends Task {

  private String domain;
  private String bpelContainer;
  private int opmnPort = -1;
  private String host;
  private String password;
  private String userid;


  public void execute() throws BuildException {
    try {
      executeSub();
    }
    catch (NoClassDefFoundError ncdfe) {
      checkException(ncdfe);
    }
    catch (ServerException e) {
      checkException(e);
    }
  }


  protected void checkException(NoClassDefFoundError ncdfe) throws BuildException {
    if (ncdfe.getMessage().indexOf("NonSyncPrintWriter") > 0) {
      throw new BuildException("Missing library on ant's classpath: orabpel-common.jar");
    }
    else if (ncdfe.getMessage().indexOf("OpticException") > 0) {
      throw new BuildException("Missing library on ant's classpath: optic.jar");
    }
    throw new BuildException(ncdfe.getMessage());
  }


  protected void checkException(ServerException e) throws BuildException {
    if (e.getMessage().indexOf("RMIInitialContextFactory") > 0) {
      throw new BuildException("Missing library on ant's classpath: oc4jclient.jar");
    }
    else if ((e.getMessage().indexOf("ServerBean") > 0) && (e.getMessage().indexOf("ClassCastException") > 0)) {
      throw new BuildException("Missing library on ant's classpath: orabpel.jar");
    }
    else if ((e.getMessage().indexOf("EJBHome") > 0) && (e.getMessage().indexOf("NoClassDefFoundError") > 0)) {
      throw new BuildException("Missing library on ant's classpath: ejb.jar");
    }
    else if ((e.getMessage().indexOf("FinderBean not found") > 0) && (e.getMessage().indexOf("NameNotFoundException") > 0)) {
      throw new BuildException(
                               "Unable to access container " + getBpelContainer() + ". Please check if container is started.");
    }
    throw new BuildException(e.getMessage());
  }


  protected abstract void executeSub() throws ServerException;


  protected Properties getProperties() {
    Properties props = new java.util.Properties();
    props.put("orabpel.platform", "ias_10g");
    props.put("java.naming.factory.initial", "com.evermind.server.rmi.RMIInitialContextFactory");
    props.put("java.naming.provider.url",
              "opmn:ormi://" + getHost() + ":" + getOpmnPort() + ":" + getBpelContainer() + "/orabpel");
    props.put("java.naming.security.principal", getUserid());
    props.put("java.naming.security.credentials", getPassword());
    return props;
  }


  protected Locator getLocator() throws ServerException {
    return new Locator(getDomain(), getPassword(), getProperties());
  }


  public Server getServer() throws ServerException {
    ServerAuth auth = ServerAuthFactory.authenticate(getPassword(), getHost(), getProperties());
    return new Server(auth);
  }


  public void setDomain(String domain) {
    this.domain = domain;
  }


  /**
   * @return the domain
   */
  protected String getDomain() {
    if ((domain == null) || domain.equals("")) {
      throw new BuildException("Parameter 'domain' not set.");
    }
    return domain;
  }


  /**
   * @param opmnPort the opmnPort to set
   */
  public void setOpmnPort(int opmnPort) {
    this.opmnPort = opmnPort;
  }


  /**
   * @return the opmnPort
   */
  protected int getOpmnPort() {
    if (opmnPort < 0) {
      throw new BuildException("Parameter 'opmnPort' not set.");
    }
    return opmnPort;
  }


  /**
   * @param host the host to set
   */
  public void setHost(String host) {
    this.host = host;
  }


  /**
   * @return the host
   */
  protected String getHost() {
    if ((host == null) || (host.equals(""))) {
      throw new BuildException("Parameter 'host' not set.");
    }
    return host;
  }


  /**
   * @param password the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }


  /**
   * @return the password
   */
  protected String getPassword() {
    if (password == null) {
      throw new BuildException("Parameter 'password' not set");
    }
    return password;
  }


  /**
   * @param userid the userid to set
   */
  public void setUserid(String userid) {
    this.userid = userid;
  }


  /**
   * @return the userid
   */
  protected String getUserid() {
    if ((userid == null) || (userid.equals(""))) {
      throw new BuildException("Parameter 'userID' not set");
    }
    return userid;
  }


  /**
   * @param bpelContainer the bpelDomain to set
   */
  public void setBpelContainer(String bpelContainer) {
    this.bpelContainer = bpelContainer;
  }


  /**
   * @return the bpelContainer
   */
  protected String getBpelContainer() {
    if ((bpelContainer == null) || (bpelContainer.equals(""))) {
      throw new BuildException("Parameter 'bpelContainer' not set");
    }
    return bpelContainer;
  }

}