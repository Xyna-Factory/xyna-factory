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

import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.parameters.InstantiateRepositoryAccessParameters;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Instantiaterepositoryaccessinstance;



public class InstantiaterepositoryaccessinstanceImpl extends XynaCommandImplementation<Instantiaterepositoryaccessinstance> {

  public void execute(OutputStream statusOutputStream, Instantiaterepositoryaccessinstance payload) throws XynaException {
    RevisionManagement revisionManagement = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
    Long revision = revisionManagement.getRevision(null, null, payload.getWorkspaceName());

    InstantiateRepositoryAccessParameters params = new InstantiateRepositoryAccessParameters();
    
    params.setRepositoryAccessInstanceName(payload.getInstancename());
    params.setRepositoryAccessName(payload.getTypename());
    
    Map<String, String> parameterMap;
    if (payload.getParameter() != null) {
      parameterMap = StringParameter.paramListToMap(Arrays.asList(payload.getParameter()));
      params.setParameterMap(parameterMap);
    }
    
    params.setXmomAccessName(payload.getXmomAccessName());
    params.setCodeAccessName(payload.getCodeAccessName());
    
    XynaFactory.getInstance()
               .getXynaDevelopment()
               .getXynaLibraryDevelopment()
               .getRepositoryAccessManagement()
               .instantiateRepositoryAccessInstance(params, revision);
  }

}
