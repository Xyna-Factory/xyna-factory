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

package com.gip.xyna.xprc.xpce.dispatcher;



import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xfmg.exceptions.XFMG_MDMObjectClassLoaderNotFoundException;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaOrderServerExtension.ExecutionType;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidEntryInList;
import com.gip.xyna.xprc.exceptions.XPRC_OperationNotFoundInDatatypeException;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.JavaCall;
import com.gip.xyna.xprc.xfractwfe.base.XynaProcess;
import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;
import com.gip.xyna.xprc.xsched.ordercancel.KillStuckProcessBean;



/**
 * ruft services ohne workflow-wrapper auf.
 * beim aufruf von nicht-statischen methoden (instanzmethoden) ist der erste inputparameter die instanz, auf
 * der die methode aufgerufen wird
 */
public class ServiceDestination extends DestinationValue {

  private static final long serialVersionUID = -846940175737227040L;

  private final String originalFqDatatypeName;
  private final String fqDatatypeClassname;
  private final String operationName;
  private final String serviceName;
  private static final ConcurrentMap<Long, ServiceDestinationWrapper> runningServiceExecutions =
      new ConcurrentHashMap<Long, ServiceDestinationWrapper>();

  private static final Logger logger = CentralFactoryLogging.getLogger(ServiceDestination.class);


  public ServiceDestination(String originalFqDatatypeName, String serviceName, String operationName,
                            String fqClassName) throws XFMG_MDMObjectClassLoaderNotFoundException,
      XPRC_OperationNotFoundInDatatypeException {
    super(createFqName(originalFqDatatypeName, serviceName, operationName));
    this.originalFqDatatypeName = originalFqDatatypeName;
    this.operationName = operationName;
    this.fqDatatypeClassname = fqClassName;
    this.serviceName = serviceName;
  }

  private static class FractalProcessStepServiceDestination extends FractalProcessStep<ServiceDestinationWrapper> implements JavaCall {
    private static final long serialVersionUID = 1L;
    
    private final ServiceDestination destination;
    private GeneralXynaObject input;
    private GeneralXynaObject output;
    private ChildOrderStorage childOrderStorage;

    private transient XynaOrderServerExtension xo;
    
    public FractalProcessStepServiceDestination(ServiceDestination destination) {
      super(1);
      this.destination = destination;
      this.childOrderStorage = new ChildOrderStorage(this);
    }
    
    public void setInputVars(GeneralXynaObject input) throws XynaException {
      this.input = input;
    }


    public GeneralXynaObject getOutput() {
      return output;
    }
        
    @Override
    protected FractalProcessStep<ServiceDestinationWrapper>[] getChildren(int i) {
      return null;
    }


    @Override
    protected int getChildrenTypesLength() {
      return 0;
    }


    @Override
    public GeneralXynaObject[] getCurrentIncomingValues() {
      return null; //inputs und outputs brauchen nur als inputs und outputs vom gesamten auftrag in die auditdaten aufgenommen werden
    }

    @Override
    public GeneralXynaObject[] getCurrentOutgoingValues() {
      return null;
    }


    @Override
    public Integer getXmlId() {
      return null;
    }
    
    @Override
    public void executeInternally() throws XynaException {
      //TODO performance: method cachen (siehe auskommentierter code). achtung: dann benötigt man einen cache-refresh mechanismus, der beim classreloading des services zuschlägt. vgl bug 15802
      Method operationMethod = destination.getOperationOfService(xo);
      boolean firstParameterIsXynaOrder = operationMethod.getParameterTypes().length > 0 && operationMethod.getParameterTypes()[0] == XynaOrderServerExtension.class;
      initEventSource();
      try {
        Object result;
        try {
          if (input instanceof Container) {
            Container inputContainer = (Container) input;
            Object[] params = new Object[inputContainer.size()];
            for (int i = 0; i < inputContainer.size(); i++) {
              params[i] = inputContainer.get(i);
            }
            if (Modifier.isStatic(operationMethod.getModifiers())) {
              if (firstParameterIsXynaOrder) {
                Object[] params2 = new Object[params.length + 1];
                System.arraycopy(params, 0, params2, 1, params.length);
                params2[0] = xo;
                result = operationMethod.invoke(null, params2);
              } else {
                result = operationMethod.invoke(null, params);
              }
            } else {
              ChildOrderStorageStack childOrderStorageStack = ChildOrderStorage.childOrderStorageStack.get();
              childOrderStorageStack.add(childOrderStorage);
              try {
                if (params.length == 1) {
                  if (firstParameterIsXynaOrder) {
                    result = operationMethod.invoke(params[0], xo);
                  } else {
                    result = operationMethod.invoke(params[0]);
                  }
                } else {
                  if (firstParameterIsXynaOrder) {
                    Object ob = params[0];
                    params[0] = xo;
                    result = operationMethod.invoke(ob, params);
                  } else {
                    Object[] params2 = new Object[params.length - 1];
                    System.arraycopy(params, 1, params2, 0, params2.length);
                    result = operationMethod.invoke(params[0], params2);
                  }
                }
              } finally {
                childOrderStorageStack.remove();
              }
            }
          } else {
            if (Modifier.isStatic(operationMethod.getModifiers())) {
              if (firstParameterIsXynaOrder) {
                result = operationMethod.invoke(null, xo, input);
              } else {
                result = operationMethod.invoke(null, input);
              }
            } else {
              ChildOrderStorageStack childOrderStorageStack = ChildOrderStorage.childOrderStorageStack.get();
              childOrderStorageStack.add(childOrderStorage);
              try {
                if (firstParameterIsXynaOrder) {
                  result = operationMethod.invoke(input, xo);
                } else {
                  result = operationMethod.invoke(input);
                }
              } finally {
                childOrderStorageStack.remove();
              }
            }
          }
        } catch (IllegalArgumentException e) {
          throw new RuntimeException("Error while executing service destination (" + e.getMessage() + ")", e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Error while executing service destination (" + e.getMessage() + ")", e);
        } catch (InvocationTargetException e) {
          Throwable t = e.getTargetException();
          if (t instanceof XynaException) {
            throw (XynaException) t;
          }
          if (t instanceof RuntimeException) {
            throw (RuntimeException) t;
          }
          if (t instanceof Error) {
            throw (Error) t;
          }
          throw new RuntimeException(null, e.getTargetException());
        }

        if (result instanceof GeneralXynaObject) {
          output = (GeneralXynaObject) result;
        } else if (result == null) {
          output = new Container();
        } else if (result instanceof List) {
          List<?> resultAsList = (List<?>) result;
          if (resultAsList.size() == 0) {
            // FIXME 
            output = null;
          } else {
            if (resultAsList.get(0) instanceof GeneralXynaObject) {
              GeneralXynaObject gxo = (GeneralXynaObject) resultAsList.get(0);
              output = new GeneralXynaObjectList(resultAsList, gxo.getClass());
            } else {
              throw new XPRC_InvalidEntryInList(GeneralXynaObject.class.getSimpleName(), "null");
            }
          }
        } else {
          throw new RuntimeException("Unexpected result class of service operation execution: "
              + (result != null ? result.getClass() : "null"));
        }
      } finally {
        clearEventSource();
      }
    }


    @Override
    public void compensateInternally() throws XynaException {
    }

    public List<XynaOrderServerExtension> getChildOrders() {
      return childOrderStorage.getChildXynaOrders();
    }

  }

  private static class ServiceDestinationWrapper extends XynaProcess {

    private static final long serialVersionUID = 1L;
    private final FractalProcessStep<?>[] allSteps;
    private FractalProcessStepServiceDestination step;
    
    public ServiceDestinationWrapper(ServiceDestination dest) {
      this.step = new FractalProcessStepServiceDestination(dest);
      allSteps = new FractalProcessStep<?>[] {step};
    }


    public void setInputVars(GeneralXynaObject input) throws XynaException {
      step.setInputVars(input);
    }


    public GeneralXynaObject getOutput() {
      return step.getOutput();
    }


    public FractalProcessStep<?>[] getStartSteps() {
      return allSteps;
    }


    public FractalProcessStep<?>[] getAllSteps() {
      return allSteps;
    }

    public FractalProcessStep<?>[] getAllLocalSteps() {
      return allSteps;
    }


    @Override
    public String getOriginalName() {
      return null;
    }


    @Override
    protected void initializeMemberVars() {
      step.init(this);
    }


    @Override
    protected void onDeployment() throws XynaException {

    }


    @Override
    protected void onUndeployment() throws XynaException {

    }

    public void setXynaOrder(XynaOrderServerExtension xo) {
      step.xo = xo;
    }

  }


  public final GeneralXynaObject exec(final XynaOrderServerExtension xo) throws XynaException {
    final long orderId = xo.getId();
    final GeneralXynaObject input = xo.getInputPayload();
    ServiceDestinationWrapper xp = (ServiceDestinationWrapper) xo.getExecutionProcessInstance();
    if (xp == null) {
      xp = new ServiceDestinationWrapper(this);
      xp.setInputVars(input);
      xo.setExecutionProcessInstance(xp);
    }
    xp.setXynaOrder(xo);
    runningServiceExecutions.put(orderId, xp);
    try {
      return xp.execute(xo.getInputPayload(), xo);
    } finally {
      runningServiceExecutions.remove(orderId);
    }
  }


  @Override
  public final boolean isPoolable() {
    return false;
  }


  private static String createFqName(String path, String serviceName, String operationName) {
    return path + "." + serviceName + "." + operationName;
  }
  
  
  /**
   * @return array {fqDatatypeName, serviceName, operationName}
   */
  public static String[] splitFqName(String fqName) {
    String[] ret = new String[3];
    int idx = fqName.lastIndexOf(".");
    ret[2] = fqName.substring(idx + 1);
    fqName = fqName.substring(0, idx);
    idx = fqName.lastIndexOf(".");
    ret[1] = fqName.substring(idx + 1);
    ret[0] = fqName.substring(0, idx);
    return ret;
  }


  private Method getOperationOfService(XynaOrderServerExtension xo)
      throws XFMG_MDMObjectClassLoaderNotFoundException, XPRC_OperationNotFoundInDatatypeException {
    
    Class<?> c;
    try {
      c = Class.forName(fqDatatypeClassname);
    } catch (ClassNotFoundException e1) {
      //mdmclassloader probieren
      try {
        Long revision = resolveRevision(xo.getRootOrder().getRevision());
        MDMClassLoader mdmClassLoader = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getClassLoaderDispatcher()
        .getMDMClassLoader(fqDatatypeClassname, revision, true);
        c = mdmClassLoader.loadClass(fqDatatypeClassname);
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e); //sollte nicht passieren, weil dann eher MDMObjectClassLoaderNotFoundException kommen würde.
      }
    }
    for (Method m : c.getMethods()) {
      if (m.getName().equals(operationName)) {
        return m;
      }
    }
    throw new XPRC_OperationNotFoundInDatatypeException(operationName, fqDatatypeClassname);
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
  }


  public String getOriginalFqDataTypeName() {
    return this.originalFqDatatypeName;
  }


  public String getServiceName() {
    return this.serviceName;
  }


  public String getOperation() {
    return operationName;
  }


  public boolean containsExecutionFromOrder(long orderId) {
    return runningServiceExecutions.containsKey(orderId);
  }


  public void terminateThreadOfRunningServiceExecution(long orderId, boolean threadShouldBeStoppedForcefully,
                                                       AbortionCause reason) {

    XynaProcess xp = runningServiceExecutions.get(orderId);

    if (xp != null) {
      xp.abortRunningWF(true, new KillStuckProcessBean(orderId, threadShouldBeStoppedForcefully, reason), new AtomicBoolean(false));
    }
  }


  @Override
  public ExecutionType getDestinationType() {
    return ExecutionType.SERVICE_DESTINATION;
  }
  
  public boolean equals(Object obj) {
    //wenn die destination gleich ist, kann sie trotzdem deployed werden müssen, weil sie auf alte classloader zeigt (vgl bug 15802)
    return this == obj;
  }

}
