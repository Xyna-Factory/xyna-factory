/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xmcp.xfcli.scriptentry;

import java.io.File;
import java.io.StringWriter;
import java.util.ArrayList;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xmcp.xfcli.scriptentry.support.ApplicationManagement;
import com.gip.xyna.xmcp.xfcli.scriptentry.support.ApplicationManagement.CreateApplicationDefinitionXmlParameter;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;

import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.WorkspaceContent;
import xmcp.gitintegration.impl.FactoryContentCreator;
import xmcp.gitintegration.impl.WorkspaceContentCreator;

public class CreateApplicationXml {

  /**
   * creates an application.xml from a given workspace.xml and application-name
   * Input:
   *   path to workspace.xml
   *   path to factory.xml, or "false" to skip
   *   application name
   *   version name
   *   stub (true/false)
   *   load XMOM files from saved folder? (true/false)
   *   factory version
   */
  public static void main(String[] args) {
    
    if(args.length != 7) {
      System.out.println("Expected 7 arguments, but got " + args.length);
      System.out.println("  path to workspace.xml");
      System.out.println("  path to factory.xml or false to skip factory content (capacities)");
      System.out.println("  name of the application to build");
      System.out.println("  application version");
      System.out.println("  build stub application (true/false)");
      System.out.println("  use files from saved/XMOM instead of XMOM folder (relative to workspace.xml)");
      System.out.println("  factory version");
      System.exit(1);
    }
    
    String workspaceXmlFile = args[0];
    String factoryXmlFile = args[1];
    String applicationName = args[2];
    String versionName = args[3];
    boolean isStub = Boolean.valueOf(args[4]);
    boolean fromSaved = Boolean.valueOf(args[5]);
    String factoryVersion = args[6];
    
    System.out.println("Creating application.xml for " + applicationName + "/" + versionName + " from " + workspaceXmlFile);
    System.out.println("  factoryXml: " + factoryXmlFile);
    System.out.println("  isStub: " + isStub);
    System.out.println("  fromSaved: " + fromSaved);
    System.out.println("  factoryVersion: " + factoryVersion);
    
    WorkspaceContentCreator creator = new WorkspaceContentCreator();
    WorkspaceContent workspaceContent = creator.createWorkspaceContentFromFile(new File(workspaceXmlFile)); 
    FactoryContent factoryContent = createFactoryContent(factoryXmlFile);
    ApplicationXmlEntry result = null;
    
    CreateApplicationDefinitionXmlParameter parameter = new CreateApplicationDefinitionXmlParameter();
    parameter.setApplicationName(applicationName);
    parameter.setVersionName(versionName);
    parameter.setCreateStub(isStub);
    parameter.setContent(workspaceContent);
    parameter.setFactoryContent(factoryContent);
    parameter.setBasePath(workspaceXmlFile);
    parameter.setFromSaved(fromSaved);
    
    try {
      ApplicationManagement appMgmt = new ApplicationManagement(factoryVersion);
      result = appMgmt.createApplicationDefinitionXml(parameter);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
    
    writeResult(result, workspaceXmlFile);
    
    
    System.out.println("Done");
  }
  
  private static FactoryContent createFactoryContent(String factoryXmlFile) {
    FactoryContent factoryContent;
    if(!factoryXmlFile.equals("false")) {
      FactoryContentCreator factoryContentCreator = new FactoryContentCreator();
      factoryContent = factoryContentCreator.createFactoryContentFromFile(new File(factoryXmlFile));
    } else {
      factoryContent = new FactoryContent();
      factoryContent.setFactoryContentItems(new ArrayList<>());
    }
    return factoryContent;
  }
  
  private static void writeResult(ApplicationXmlEntry result, String workspaceXmlFile) {
    StringWriter sw = new StringWriter();
    try {
      XMLUtils.saveDomToWriter(sw, result.buildXmlDocument());
      XMLUtils.saveDom(new File(new File(workspaceXmlFile).getParent(), "application.xml"), result.buildXmlDocument());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
