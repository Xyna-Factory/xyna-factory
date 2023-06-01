/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import javax.enterprise.deploy.shared.ModuleType;

import javax.enterprise.deploy.spi.DeploymentManager;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;

import oracle.oc4j.admin.deploy.spi.DataSourceInfo;
import oracle.oc4j.admin.deploy.spi.TargetModuleIDImpl;
import oracle.oc4j.admin.deploy.spi.factories.Oc4jDeploymentFactory;

import org.apache.tools.ant.BuildException;

import org.apache.tools.ant.taskdefs.condition.Condition;

public class ExistsDatasource implements Condition {

  private String datasourceName;
  private String username;
  private String password;
  private String hostName;
  private int opmnPort;
  private String oc4j;

  /**
   * überprüft, ob die application "appName" im durch die anderen Parameter beschriebenen oc4j deployed ist.
   * @return
   * @throws BuildException
   */
  public boolean eval() throws BuildException {
    String uri = "deployer:oc4j:opmn://" + hostName + ":" + opmnPort + "/" + oc4j;
    //Valid connection_uris are:
    // 1. To target all OC4J instances that belongs to an OC4J group in an Oracle Application Ser
    // ver Cluster:
    // deployer:cluster:[ormis:]opmn://opmnHost[:opmnPort]/oc4jGroupName

    // 2. To target a specific OC4J instance within a cluster:
    // deployer:oc4j:[ormis:]opmn://opmnHost[:opmnPort]/oc4jInstanceName
    // deployer:oc4j:[ormis:]opmn://opmnHost[:opmnPort]/asInstanceName/oc4jInstanceName

    // 3. To target a standalone OC4J server:
    // deployer:oc4j:oc4jHost:rmiPort
    // deployer:oc4j:ormis:oc4jHost:ormisPort

    // opmnPort is the OPMN request port specified in opmn.xml.
    // If omitted, defaults to 6003.
    Oc4jDeploymentFactory o = new Oc4jDeploymentFactory();
    try {
      DeploymentManager dm = o.getDeploymentManager(uri, username, password);
      Target[] targets = dm.getTargets();
      for (Target t: targets) {
        /*        if (t instanceof TargetImpl) {
            TargetImpl ti = (TargetImpl)t;
          }*/
        //System.out.println(t.getDescription() + "  " + t.getName() + " " + t.getClass().getName());
        TargetModuleID[] tms = dm.getAvailableModules(ModuleType.EAR, new Target[] { t });
        for (TargetModuleID tm: tms) {
          if (tm instanceof TargetModuleIDImpl) {
            TargetModuleIDImpl tmi = (TargetModuleIDImpl)tm;
            if (tmi.getObjectName().getKeyProperty("name").equals("default")) {
              DataSourceInfo[] dsilist = tmi.getDataSources();
              for (DataSourceInfo dsi: dsilist) {
                if (dsi.getDisplayName().equals(datasourceName)) {
                  return true;
                }
              }
            }
            //              System.out.println(tmi.getObjectName().getKeyProperty("name"));
          } else {
            throw new BuildException("Unexpected Class for TargetModuleID: " +
                tm.getClass().getName());
          }
          //System.out.println(tm.getModuleID() + "  " + tm.getWebURL() + " " + tm.getClass().getName());
        }
        break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }

  public String getDatasourceName() {
    return datasourceName;
  }

  public static void main(String[] args) {
    try {
      ExistsDatasource ea = new ExistsDatasource();
      ea.datasourceName = "schedulerDS";
      ea.username = "oc4jadmin";
      ea.password = "oracle10";
      ea.hostName = "gipsun185";
      ea.opmnPort = 6003;
      ea.oc4j = "processing";
      System.out.println(ea.eval());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
  }

  public String getHostName() {
    return hostName;
  }

  public int getOpmnPort() {
    return opmnPort;
  }

  public String getOc4j() {
    return oc4j;
  }

  private static void checkEmpty(String name, String val) {
    if (val == null || val.length() == 0) {
      throw new BuildException("Required attribute \"" + name + "\" is missing.");
    }
  }

  public void setDatasourceName(String datasourceName) {
    checkEmpty("datasourceName", datasourceName);
    this.datasourceName = datasourceName;
  }

  public void setUsername(String username) {
    checkEmpty("username", username);
    this.username = username;
  }

  public void setPassword(String password) {
    checkEmpty("password", password);
    this.password = password;
  }

  public void setHostName(String hostName) {
    checkEmpty("hostName", hostName);
    this.hostName = hostName;
  }

  public void setOpmnPort(int opmnPort) {
    //checkEmpty("opmnPort", opmnPort);
    this.opmnPort = opmnPort;
  }

  public void setOc4j(String oc4j) {
    checkEmpty("oc4j", oc4j);
    this.oc4j = oc4j;
  }
}
