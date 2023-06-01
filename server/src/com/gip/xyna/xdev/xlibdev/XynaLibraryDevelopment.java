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

package com.gip.xyna.xdev.xlibdev;



import com.gip.xyna.Section;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xlibdev.codeaccess.CodeAccessManagement;
import com.gip.xyna.xdev.xlibdev.repository.RepositoryManagement;
import com.gip.xyna.xdev.xlibdev.repositoryaccess.RepositoryAccessManagement;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.fileprovider.EclipseProjectTemplateFileProvider;
import com.gip.xyna.xdev.xlibdev.xmomaccess.XMOMAccessManagement;



public class XynaLibraryDevelopment extends Section {

  public static final String DEFAULT_NAME = "Xyna Library Development";


  public XynaLibraryDevelopment() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  public void init() throws XynaException {
    deployFunctionGroup(new Support4Eclipse());
    deployFunctionGroup(new EclipseProjectTemplateFileProvider());
    deployFunctionGroup(new CodeAccessManagement());
    deployFunctionGroup(new RepositoryAccessManagement());
    deployFunctionGroup(new XMOMAccessManagement());
    deployFunctionGroup(new RepositoryManagement());
  }

  public EclipseProjectTemplateFileProvider getEclipseProjectTemplateFileProvider() {
    return (EclipseProjectTemplateFileProvider) getFunctionGroup(EclipseProjectTemplateFileProvider.DEFAULT_NAME);
  }
  
  public CodeAccessManagement getCodeAccessManagement() {
    return (CodeAccessManagement) getFunctionGroup(CodeAccessManagement.DEFAULT_NAME);
  }
  
  public RepositoryAccessManagement getRepositoryAccessManagement() {
    return (RepositoryAccessManagement) getFunctionGroup(RepositoryAccessManagement.DEFAULT_NAME);
  }

  public XMOMAccessManagement getXMOMAccessManagement() {
    return (XMOMAccessManagement) getFunctionGroup(XMOMAccessManagement.DEFAULT_NAME);
  }
  
  public RepositoryManagement getRepositoryManagement() {
    return (RepositoryManagement) getFunctionGroup(RepositoryManagement.DEFAULT_NAME);
  }
}
