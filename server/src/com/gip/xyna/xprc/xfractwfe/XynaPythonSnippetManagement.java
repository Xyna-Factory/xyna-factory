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
package com.gip.xyna.xprc.xfractwfe;



import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.gip.xyna.Section;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.RevisionChangeUnDeploymentHandler;
import com.gip.xyna.xprc.xfractwfe.python.Context;
import com.gip.xyna.xprc.xfractwfe.python.JepInterpreterFactory;
import com.gip.xyna.xprc.xfractwfe.python.PythonInterpreter;
import com.gip.xyna.xprc.xfractwfe.python.PythonInterpreterFactory;
import com.gip.xyna.xprc.xfractwfe.python.PythonMdmGeneration;



public class XynaPythonSnippetManagement extends Section {

  private PythonInterpreterFactory factory;


  public XynaPythonSnippetManagement() throws XynaException {
    super();
  }

  public void invalidateRevisions(Collection<Long> revisions) {

    if (logger.isDebugEnabled()) {
      logger.debug("invalidating: " + revisions.size() + " revisions");
    }

    factory.invalidateRevisions(revisions);
  }

  public PythonInterpreter createPythonInterpreter(ClassLoader classloader) {
    if (!(classloader instanceof ClassLoaderBase)) {
      throw new RuntimeException("Unexpected createPythonInterpreter request. " + classloader + " does not inherit from ClassLoaderBase!");
    }
    return factory.createInterperter(((ClassLoaderBase) classloader));
  }


  @Override
  public String getDefaultName() {
    return "XynaPythonSnippetManagement";
  }


  @Override
  protected void init() throws XynaException {
    factory = new JepInterpreterFactory();
    factory.init();
    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
    .addDeploymentHandler(DeploymentHandling.PRIORITY_REMOTESERIALIZATION, new RevisionChangeUnDeploymentHandler(this::invalidateRevisions));

    XynaFactory.getInstance().getProcessing().getWorkflowEngine().getDeploymentHandling()
    .addUndeploymentHandler(DeploymentHandling.PRIORITY_REMOTESERIALIZATION, new RevisionChangeUnDeploymentHandler(this::invalidateRevisions));

  }

  public Object convertToPython(Object obj) {
    return factory.convertToPython(obj);
  }

  public Object convertToJava(Context context, String type, Object obj) {
    return factory.convertToJava(context, type, obj);
  }

  public Object invokeService(Context context, String fqn, String serviceName, List<Object> args) {
    return factory.invokeService(context, fqn, serviceName, args);
  }

  public Object invokeInstanceService(Context context, Object obj, String serviceName, List<Object> args) {
    return factory.invokeInstanceService(context, obj, serviceName, args);
  }
  
  public String createPythonMdm(Long revision, boolean withImpl, boolean typeHints) {
    return new PythonMdmGeneration().createPythonMdm(revision, withImpl, typeHints);
  }
  
  public void exportPythonMdm(Long revision, String destination) throws Exception {
    new PythonMdmGeneration().exportPythonMdm(revision, destination);
  }
  
  public String getLoaderSnippet() {
    return PythonMdmGeneration.LOAD_MODULE_SNIPPET;
  }
}
