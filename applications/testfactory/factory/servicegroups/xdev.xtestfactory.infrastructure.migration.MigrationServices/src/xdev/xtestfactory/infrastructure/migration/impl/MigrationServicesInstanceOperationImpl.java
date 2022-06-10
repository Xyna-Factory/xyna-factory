/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xdev.xtestfactory.infrastructure.migration.impl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
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

import org.apache.log4j.Logger;
import xact.templates.Document;
import xdev.xtestfactory.infrastructure.datatypes.Workspacename;
import xdev.xtestfactory.infrastructure.migration.TestCaseAndOrderInputSourceCombination;
import xdev.xtestfactory.infrastructure.migration.TestSeriesAndListOfTestCaseOISCombination;
import xdev.xtestfactory.infrastructure.migration.XTF5TestcaseCSV;
import xdev.xtestfactory.infrastructure.migration.XTF5TestcasetestseriesCSV;
import xdev.xtestfactory.infrastructure.migration.XTF5TestdurchfuerhungsplanCSV;
import xdev.xtestfactory.infrastructure.migration.XTF5TestseriesCSV;
import xdev.xtestfactory.infrastructure.migration.testcases.InputGenerationMapping;
import xdev.xtestfactory.infrastructure.migration.testcases.TestProcessMapping;
import xdev.xtestfactory.infrastructure.migration.testseries.FunctionObject;
import xdev.xtestfactory.infrastructure.migration.testseries.SeriesMigrationName;
import xdev.xtestfactory.infrastructure.migration.testseries.SeriesMigrationPath;
import xdev.xtestfactory.infrastructure.migration.testseries.ServiceReference;
import xdev.xtestfactory.infrastructure.migration.MigrationServicesSuperProxy;
import xdev.xtestfactory.infrastructure.migration.MigrationServices;


public class MigrationServicesInstanceOperationImpl extends MigrationServicesSuperProxy {

  private static final long serialVersionUID = 1L;

  public MigrationServicesInstanceOperationImpl(MigrationServices instanceVar) {
    super(instanceVar);
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
