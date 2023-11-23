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
package xfmg.oas.generation.impl;



import org.openapitools.codegen.OpenAPIGenerator;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ApplicationPartImportMode;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationManagementImpl.ImportApplicationParameter;
import com.gip.xyna.xfmg.xfctrl.nodemgmt.rtctxmgmt.LocalRuntimeContextManagementSecurity;
import com.gip.xyna.xfmg.xopctrl.managedsessions.SessionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xfmg.oas.generation.ApplicationGenerationParameter;
import xfmg.oas.generation.ApplicationGenerationServiceOperation;
import xfmg.oas.generation.cli.generated.OverallInformationProvider;
import xfmg.oas.generation.cli.impl.BuildoasapplicationImpl;



public class ApplicationGenerationServiceOperationImpl implements ExtendedDeploymentTask, ApplicationGenerationServiceOperation {

  private static final LocalRuntimeContextManagementSecurity localLrcms =
      new LocalRuntimeContextManagementSecurity();
  private static final SessionManagement sessionManagement = 
      XynaFactory.getInstance().getFactoryManagement().getXynaOperatorControl().getSessionManagement();
  
  public void onDeployment() throws XynaException {
    OverallInformationProvider.onDeployment();
  }


  public void onUndeployment() throws XynaException {
    OverallInformationProvider.onUndeployment();
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }

  @Override
  public void generateApplication(XynaOrderServerExtension correlatedXynaOrder, ApplicationGenerationParameter applicationGenerationParameter2) {
    String swagger = applicationGenerationParameter2.getOpenAPISpecificationPath();
    String target = "/tmp/Order_" + correlatedXynaOrder.getId();

    try {
      OpenAPIGenerator.main(new String[] {"validate", "-i", swagger, "--recommend"});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    
    BuildoasapplicationImpl oasAppBuilder = new BuildoasapplicationImpl();
    
    String id;
    
    id = oasAppBuilder.createOasApp("xmom-data-model", target + "_datatypes", swagger);
    importApplication(correlatedXynaOrder, id);
    
    if (applicationGenerationParameter2.getGenerateProvider()) {
      id = oasAppBuilder.createOasApp("xmom-server", target + "_provider", swagger);
      importApplication(correlatedXynaOrder, id);
    }
    if (applicationGenerationParameter2.getGenerateClient()) {
      id = oasAppBuilder.createOasApp("xmom-client", target + "_client", swagger);
      importApplication(correlatedXynaOrder, id);
    }
  }
  
  private void importApplication(XynaOrderServerExtension correlatedXynaOrder, String id) {
    try {
      String user = sessionManagement.resolveSessionToUser(correlatedXynaOrder.getSessionId());
      ImportApplicationParameter iap = ImportApplicationParameter.with(ApplicationPartImportMode.EXCLUDE,
                                                                       ApplicationPartImportMode.EXCLUDE,
                                                                       true,
                                                                       true,
                                                                       user);
      localLrcms.importApplication(correlatedXynaOrder.getCreationRole(), iap, id);

    } catch (Exception ex) {
      throw new RuntimeException(ex.getMessage(), ex);
    }
  }
}
