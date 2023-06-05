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
package com.gip.xyna.xfmg.xods.orderinputsource.workflow;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Documentation;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSource;
import com.gip.xyna.xfmg.xods.orderinputsourcemgmt.OrderInputSourceType;
import com.gip.xyna.xmcp.PluginDescription;
import com.gip.xyna.xmcp.PluginDescription.ParameterUsage;
import com.gip.xyna.xmcp.PluginDescription.PluginType;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class WorkflowInputSourceType implements OrderInputSourceType {

  private static final String PARA_ORDERTYPE = "generatingOrderType";

  private static final PluginDescription pluginDescription;
  private static final StringParameter<String> ORDERTYPE = StringParameter
      .typeString(PARA_ORDERTYPE)
      .documentation(Documentation.en("This is the ordertype corresponding to the workflow to be used to create the input.")
                         .de("Der Ordertype des Hilfs-Workflows zum Erzeugen der Inputdaten.").build()).mandatory()
      .label("Ordertype to create input with").build();


  static {
    List<StringParameter<?>> paras = new ArrayList<StringParameter<?>>();
    paras.add(ORDERTYPE);

    try {
      pluginDescription =
          PluginDescription
              .create(PluginType.orderInputSource)
              .description("Creation of order input by executing another workflow. The output of that workflow is then used as input for the ordertype to be started originally. The other workflow must contain xprc.xpce.OrderCreationParameter as output.")
              .label("Workflow input generator type").name(WorkflowInputSourceType.class.getSimpleName())
              .parameters(ParameterUsage.Create, paras).addDatatype(load("WorkflowInputSource.xml"))
              .addForm(load("WorkflowInputSourceForm.xml")).build();
    } catch (IOException e) {
      throw new RuntimeException("could not initialize plugin description", e);
    } catch (Ex_FileAccessException e) {
      throw new RuntimeException("could not initialize plugin description", e);
    }

  }


  public PluginDescription showDescription() {
    return pluginDescription;
  }


  private static String load(String fileName) throws IOException, Ex_FileAccessException {
    InputStream resourceAsStream =
        FileUtils.getInputStreamFromResource("forms/" + fileName, WorkflowInputSourceType.class.getClassLoader());
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


  public OrderInputSource createOrderInputSource(String name, DestinationKey destinationKey, Map<String, Object> parameters,
                                                 String documentation) {
    DestinationKey destinationToCreateInputWith =
        new DestinationKey((String) parameters.get(PARA_ORDERTYPE), destinationKey.getRuntimeContext());
    XynaOrderCreationParameter xocpForWorkflowToCreateInputWith = new XynaOrderCreationParameter(destinationToCreateInputWith);
    return new WorkflowInputSource(destinationKey, xocpForWorkflowToCreateInputWith);
  }


  public boolean refactorParameters(Map<String, Object> parameters, DependencySourceType refactoredObjectType, String fqNameOld,
                                    String fqNameNew) throws XynaException {
    if (refactoredObjectType == DependencySourceType.ORDERTYPE) {
      String ot = (String) parameters.get(PARA_ORDERTYPE);
      if (ot.equals(fqNameOld)) {
        parameters.put(PARA_ORDERTYPE, fqNameNew);
        return true;
      }
    }
    return false;
  }

}
