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

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import xfmg.oas.generation.ApplicationGenerationParameter;
import xfmg.xfctrl.filemgmt.ManagedFileId;
import xfmg.oas.generation.ApplicationGenerationServiceOperation;
import xfmg.oas.generation.cli.generated.OverallInformationProvider;



public class ApplicationGenerationServiceOperationImpl implements ExtendedDeploymentTask, ApplicationGenerationServiceOperation {

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


  public ManagedFileId generateApplication(ApplicationGenerationParameter applicationGenerationParameter2) {
    String swagger = "";
    String target = "/tmp/resultFile";
    OpenAPIGenerator.main(new String[] {"generate", "-g", "xmom-data-model", "-i", swagger, "-o", target});
    return new ManagedFileId.Builder().instance();
  }

}