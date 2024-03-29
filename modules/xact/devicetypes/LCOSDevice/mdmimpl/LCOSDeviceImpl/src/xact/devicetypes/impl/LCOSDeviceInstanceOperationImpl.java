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
package xact.devicetypes.impl;

import base.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
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
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import xact.connection.Command;
import xact.connection.CommandResponseTuple;
import xact.connection.DetectedError;
import xact.connection.ManagedConnection;
import xact.connection.RuleBasedDeviceType;
import xact.templates.Document;
import xact.templates.DocumentType;
import xact.devicetypes.LCOSDeviceSuperProxy;
import xact.devicetypes.LCOSDeviceInstanceOperation;
import xact.devicetypes.LCOSDevice;


public class LCOSDeviceInstanceOperationImpl extends LCOSDeviceSuperProxy implements LCOSDeviceInstanceOperation {

  private static final long serialVersionUID = 1L;
  
  private final static Pattern[] errorPatterns = {
                  Pattern.compile(".*\n\\s*Path name wrong: .*", Pattern.DOTALL),
                  Pattern.compile(".*\n\\s*Value invalid: .*", Pattern.DOTALL),
                  Pattern.compile("[tT][aA][bB] .*\n\\s*Ignoring unknown column .*", Pattern.DOTALL)
  };

  public LCOSDeviceInstanceOperationImpl(LCOSDevice instanceVar) {
    super(instanceVar);
  }

  public void detectCriticalError(CommandResponseTuple response, DocumentType documentType) throws DetectedError {
    for (Pattern errorPattern : errorPatterns) {
      Matcher errorMatcher = errorPattern.matcher(response.getResponse().getContent());
      if (errorMatcher.matches()) {
        throw new DetectedError(response.getCommand(), response.getResponse(), new IllegalArgumentException(errorPattern.pattern()));
      }
    }
  }

  public Command enrichCommand(Command command) {
    String content = command.getContent();
    if (content.contains("[[")) {
        for (int i=0; i < 33; i++) {
            content = content.replaceAll("\\[\\[" + i + "\\]\\]", String.valueOf((char) i));
        }
    }
    Command retVal = new Command(content);
    return retVal;
  }

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }
  
}
