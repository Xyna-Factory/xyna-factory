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
package com.gip.xyna.coherence.coherencemachine.interconnect.rmi;



import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectCallee;
import com.gip.xyna.coherence.coherencemachine.interconnect.InterconnectProtocol;



/**
 * RMI spezifische implementierung des callees f�r das interconnect protokoll. ben�tigt noch weiteres interface+impl
 * weil rmi-schnittstelle von remote-interface ableiten muss. alle requests die von aussen kommen, werden an den parent
 * weitergeleitet.
 */
public class InterconnectCalleeRMI extends InterconnectCallee {

  public static final String RMI_NAME = "InterconnectCalleeRMI";

  private Object rmiImpl; //leitet von remote interface ab, vom Typ Object wegen classloading
  private final RMIConnectionParametersServer rmiParameters;
  private RMIConnectionClientParameters clientParameters;

  private InterconnectRMIClassLoader responsibleClassloader;

  private Method initMethod;
  private Method shutdownMethod;


  public static final String INTERCONNECT_CALLEE_CLASSNAME =
      "com.gip.xyna.coherence.coherencemachine.interconnect.rmi.InterconnectCalleeRemoteInterfaceImpl";


  public InterconnectCalleeRMI(InterconnectProtocol calleeImpl, RMIConnectionParametersServer rmiParameters,
                               InterconnectRMIClassLoader responsibleClassloader) {
    super(calleeImpl);
    //TODO prio4: implement RMI node connection: create RMI Binding Name by Node ID
    this.rmiParameters = rmiParameters;
    this.responsibleClassloader = responsibleClassloader;
  }


  @Override
  public void initInternally() {
    Class<?> klazz;
    try {
      klazz = responsibleClassloader.loadClass(INTERCONNECT_CALLEE_CLASSNAME);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Missing required class: " + e.getMessage(), e);
    }
    String RMI_ERROR_MESSATE = "Failed to instantiate RMI remote interface";
    try {
      rmiImpl = klazz.getConstructors()[0].newInstance(this, RMI_NAME, rmiParameters.getPort());
    } catch (IllegalArgumentException e) {
      throw new RuntimeException(RMI_ERROR_MESSATE + ": " + e.getMessage(), e);
    } catch (SecurityException e) {
      throw new RuntimeException(RMI_ERROR_MESSATE + ": " + e.getMessage(), e);
    } catch (InstantiationException e) {
      throw new RuntimeException(RMI_ERROR_MESSATE + ": " + e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(RMI_ERROR_MESSATE + ": " + e.getMessage(), e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException(RMI_ERROR_MESSATE + ": " + e.getMessage(), e);
    }

    for (Method m : klazz.getMethods()) {
      if (m.getName().equals("init")) {
        initMethod = m;
      } else if (m.getName().equals("shutdown")) {
        shutdownMethod = m;
      }
      if (initMethod != null && shutdownMethod != null) {
        break;
      }
    }
    if (initMethod == null) {
      throw new RuntimeException("Could not find init method");
    }
    if (shutdownMethod == null) {
      throw new RuntimeException("Could not find shutdown method");
    }

    try {
      initMethod.invoke(rmiImpl);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Failed to call init method: " + e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to call init method: " + e.getMessage(), e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Failed to call init method: " + e.getMessage(), e);
    }

    clientParameters = new RMIConnectionClientParameters(rmiParameters, RMI_NAME);

  }


  @Override
  public void shutdown() {
    try {
      shutdownMethod.invoke(rmiImpl);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Failed to call shutdown method: " + e.getMessage(), e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Failed to call shutdown method: " + e.getMessage(), e);
    } catch (InvocationTargetException e) {
      throw new RuntimeException("Failed to call shutdown method: " + e.getMessage(), e);
    }
  }


  public RMIConnectionParametersServer getConnectionParameters() {
    return this.rmiParameters;
  }


  public RMIConnectionClientParameters getClientParameters() {
    return clientParameters;
  }

}
