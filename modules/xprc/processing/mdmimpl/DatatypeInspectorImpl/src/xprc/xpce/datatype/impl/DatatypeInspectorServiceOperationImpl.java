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
package xprc.xpce.datatype.impl;

import base.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.BasicProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.EventType;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeEvent;
import com.gip.xyna.xdev.ProjectCreationOrChangeProvider.ProjectCreationOrChangeListener;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import xprc.xpce.datatype.InspectionParameter;
import xprc.xpce.datatype.NamedVariableMember;
import xprc.xpce.datatype.NamedXMOMMember;
import xprc.xpce.datatype.DatatypeInspector;
import xprc.xpce.datatype.DatatypeInspectorServiceOperation;


public class DatatypeInspectorServiceOperationImpl implements ExtendedDeploymentTask, DatatypeInspectorServiceOperation, ProjectCreationOrChangeListener {
  
  private static GenerationBaseCache cache;
  
  private final static String LISTENER_ID = "DatatypeInspector_" + System.currentTimeMillis();


  public xprc.xpce.datatype.DatatypeInspector inspectDatatype(GeneralXynaObject anyType) {
    try {
      Class<?> clazz = anyType.getClass();
      Long revision = com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement.getRevisionByClass(clazz);
      String fqXmlName = clazz.getAnnotation(com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation.class).fqXmlName();
      DatatypeInspector di = new DatatypeInspector();
      Method interimGetter = DatatypeInspector.class.getDeclaredMethod("getImplementationOfInstanceMethods");
      interimGetter.setAccessible(true);
      Object interim = interimGetter.invoke(di);
      Method implGetter = interim.getClass().getDeclaredMethod("getInstanceOperationInstance");
      implGetter.setAccessible(true);
      DatatypeInspectorInstanceOperationImpl instanceImpl = (DatatypeInspectorInstanceOperationImpl) implGetter.invoke(interim);
      instanceImpl.setDOM((DOM)getGeneration(fqXmlName, revision));
      instanceImpl.setXynaObject(anyType);
      return di;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }



  public DatatypeInspector inspectDatatypeByName(XynaOrderServerExtension correlatedXynaOrder, Text fqXmlName) {
    try {
      ClassLoader factoryClassLoader =
              com.gip.xyna.XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
                  .getMDMClassLoader(fqXmlName.getText(), correlatedXynaOrder.getRootOrder().getRevision(), true);
      GeneralXynaObject gxo = (GeneralXynaObject)factoryClassLoader.loadClass(fqXmlName.getText()).newInstance();
      return inspectDatatype(gxo);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  
  DOM getGeneration(String fqXmlName, long revision) throws XPRC_InheritedConcurrentDeploymentException, AssumedDeadlockException, XPRC_MDMDeploymentException, XPRC_InvalidPackageNameException {
    DOM dom = DOM.getOrCreateInstance(fqXmlName, cache, revision);
    dom.parseGeneration(true, false, false);
    return dom;
  }
  
  public void projectCreatedOrModified(Collection<? extends ProjectCreationOrChangeEvent> event, Long revision, String commitMsg) {
    // TODO we could restrict to VPN-Workspace/Application and all it's users (parents)
    for (ProjectCreationOrChangeEvent pcoce : event) {
      if (pcoce.getType().equals(EventType.SERVICE_DEPLOY)) {
        GenerationBase gb = cache.getFromCache(((BasicProjectCreationOrChangeEvent)pcoce).getObjectIdentifier(), revision);
        if (gb != null) {
          cache.remove(gb);
        }
      }
    }
  }
  
  public void onDeployment() throws XynaException {
    cache = new GenerationBaseCache();
    ProjectCreationOrChangeProvider.getInstance().addListener(LISTENER_ID, this);
  }

  public void onUndeployment() throws XynaException {
    cache = null;
    ProjectCreationOrChangeProvider.getInstance().removeListener(LISTENER_ID);
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
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

}
