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

package com.gip.xyna.xfmg.xods.orderinputsource.xtf;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSource;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceType;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.dispatcher.FractalWorkflowDestination;



public class XTFInputSourceType implements OrderInputSourceType {

  public static final String KEY_ORDERTYPE_INPUTGENERATOR = "orderTypeOfGeneratingWorkflow";
  public static final String KEY_ORDERTYPE_TESTCASEID = "testCaseID";
  public static final String KEY_ORDERTYPE_TESTCASENAME = "testCaseName";
  public static final String KEY_EXECUTING_USER = "executingUser";
  public static final String XTF_CLEANUP_WF_NAME = "xdev.xtestfactory.infrastructure.services.TestFactoryCleanup";
  public static final String XTF_PLANNING_WF_NAME = "xdev.xtestfactory.infrastructure.services.TestFactoryPlanning";

  private static final PluginDescription pluginDescription;


  private static final StringParameter<String> ORDERTYPE = StringParameter.typeString(KEY_ORDERTYPE_INPUTGENERATOR)
      .documentation(Documentation.en("This is the ordertype corresponding to the workflow to be used to create the input.")
          .de("Der Ordertype des Hilfs-Workflows zum Erzeugen der Inputdaten.").build())
      .optional().label("Ordertype to create input with").build();

  private static final StringParameter<String> TESTCASEID = StringParameter.typeString(KEY_ORDERTYPE_TESTCASEID)
      .documentation(Documentation.en("This is the Test Case ID of the xtf Test Case executed.")
          .de("Die Testfallid von dem Testfall, der ausgefuehrt wird.").build())
      .mandatory().label("Test Case ID of corresponding Test Case").build();

  private static final StringParameter<String> TESTCASENAME = StringParameter.typeString(KEY_ORDERTYPE_TESTCASENAME)
      .documentation(Documentation.en("This is the Test Case Name of the xtf Test Case executed.")
          .de("Der Name des Testfalls, der ausgefuehrt wird.").build())
      .mandatory().label("Test Case Name of corresponding Test Case").build();


  static {
    List<StringParameter<?>> paras = new ArrayList<StringParameter<?>>();
    paras.add(ORDERTYPE);
    paras.add(TESTCASEID);
    paras.add(TESTCASENAME);

    try {
      pluginDescription = PluginDescription.create(PluginType.orderInputSource)
          .description("Creation of order input by executing another workflow. The output of that workflow is then used as input for the ordertype to be started originally. The other workflow must contain xprc.xpce.OrderCreationParameter as output.")
          .label("Workflow input generator type").name(XTFInputSourceType.class.getSimpleName()).parameters(ParameterUsage.Create, paras)
          .addDatatype(load("XTFInputSource.xml")).addForm(load("XTFInputSourceForm.xml")).build();
    } catch (IOException e) {
      throw new RuntimeException("could not initialize plugin description", e);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("could not initialize plugin description", e);
    }

  }


  public OrderInputSource createOrderInputSource(String name, DestinationKey destinationKey, Map<String, Object> parameters,
                                                 String documentation) {
    DestinationKey destinationToCreateInputWith = null;
    String testcaseid = "";
    String testcasename = "";

    if (parameters.containsKey(KEY_ORDERTYPE_INPUTGENERATOR) && parameters.get(KEY_ORDERTYPE_INPUTGENERATOR) != null
        && String.valueOf(parameters.get(KEY_ORDERTYPE_INPUTGENERATOR)).length() > 0) {
      destinationToCreateInputWith =
          new DestinationKey((String) parameters.get(KEY_ORDERTYPE_INPUTGENERATOR), destinationKey.getRuntimeContext());
    } else {
      destinationToCreateInputWith = null;
    }
    if (parameters.containsKey(KEY_ORDERTYPE_TESTCASEID)) {
      testcaseid = (String) parameters.get(KEY_ORDERTYPE_TESTCASEID);
    }
    if (parameters.containsKey(KEY_ORDERTYPE_TESTCASENAME)) {
      testcasename = (String) parameters.get(KEY_ORDERTYPE_TESTCASENAME);
    }


    if (destinationKey.getOrderType() != null && destinationKey.getOrderType().length() > 0) {
      long revision;
      try {
        revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
            .getRevision(destinationKey.getRuntimeContext());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e1) {
        throw new RuntimeException("Error getting revision of destinationKey:", e1);
      }

      DestinationValue dv = null;
      
      RuntimeContextDependencyManagement rcdm =
          XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();

      Long XTFrevision = rcdm.getRevisionDefiningXMOMObject(XTF_PLANNING_WF_NAME, revision);

      //Planning Workflow exists since XTF new features project. 
      //To ensure downward compatibility with older XTF versions, check if workflow exists.
      if (XTFrevision != null) {

        dv = new FractalWorkflowDestination(XTF_PLANNING_WF_NAME, XTFrevision);

        try {
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaPlanning().getPlanningDispatcher()
              .setCustomDestination(destinationKey, dv);
        } catch (PersistenceLayerException e) {
          throw new RuntimeException("Error setting planningworkflow for ordertype: ", e);
        }

      }

      revision = rcdm.getRevisionDefiningXMOMObjectOrParent(XTF_CLEANUP_WF_NAME, revision);
      dv = new FractalWorkflowDestination(XTF_CLEANUP_WF_NAME, revision);

      try {
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getXynaCleanup().getCleanupEngineDispatcher()
            .setCustomDestination(destinationKey, dv);
      } catch (PersistenceLayerException e) {
        throw new RuntimeException("Error setting cleanupworkflow for ordertype: ", e);
      }
    }


    Optional<XynaOrderCreationParameter> xocpForWorkflowToCreateInputWith = (destinationToCreateInputWith == null ? Optional
        .<XynaOrderCreationParameter> empty() : Optional.of(new XynaOrderCreationParameter(destinationToCreateInputWith)));
    return new XTFInputSource(destinationKey, xocpForWorkflowToCreateInputWith, testcaseid, testcasename);
  }


  public PluginDescription showDescription() {
    return pluginDescription;
  }


  private static String load(String fileName) throws IOException, Ex_FileAccessException {
    InputStream resourceAsStream = FileUtils.getInputStreamFromResource("forms/" + fileName, XTFInputSourceType.class.getClassLoader());
    if (resourceAsStream == null) {
      throw new Ex_FileAccessException(fileName);
    }
    try {
      InputStreamReader r = new InputStreamReader(resourceAsStream);
      BufferedReader br = new BufferedReader(r);
      StringBuilder sb = new StringBuilder();
      while (true) {
        String line = br.readLine();
        if (line == null) {
          break;
        }
        sb.append(line).append("\n");
      }
      return sb.toString();
    } finally {
      resourceAsStream.close();
    }
  }


  public boolean refactorParameters(Map<String, Object> parameters, DependencySourceType refactoredObjectType, String fqNameOld,
                                    String fqNameNew)
      throws XynaException {
    if (refactoredObjectType == DependencySourceType.ORDERTYPE) {
      String ot = (String) parameters.get(KEY_ORDERTYPE_INPUTGENERATOR);
      if (ot != null && ot.equals(fqNameOld)) {
        parameters.put(KEY_ORDERTYPE_INPUTGENERATOR, fqNameNew);
        return true;
      }
    }
    return false;
  }


}
