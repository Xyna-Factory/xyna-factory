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
package com.gip.xyna.xfmg.xods.orderinputsource.constant;



import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import com.gip.xyna.xmcp.RemoteXynaOrderCreationParameter;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class ConstantInputSourceType implements OrderInputSourceType {

  private static final String PARA_PAYLOAD_XML = "inputData";
  private static final String PARA_PRIO = "priority";
  private static final String PARA_CUSTOM = "customField";
  private static final String PARA_MONITORINGLEVEL = "monitoringLevel";

  private static final PluginDescription pluginDescription;
  private static final StringParameter<String> INPUT = StringParameter.typeString(PARA_PAYLOAD_XML)
      .documentation(Documentation.en("Input XMOM objects of order").de("XMOM Objekte für den Input des Auftrags").build()).mandatory()
      .pattern(Pattern.compile(".*", Pattern.DOTALL)).label("Input data of order").build();

  private static final StringParameter<Integer> PRIO = StringParameter
      .typeInteger(PARA_PRIO)
      .documentation(Documentation.en("Priority of order (highest = 10, lowest = 1)")
                         .de("Priorität des Auftrags von 1 (am niedrigsten) bis 10.").build()).optional().label("Priority of order")
      .build();

  @SuppressWarnings("unchecked")
  private static final StringParameter<String>[] CUSTOMS = new StringParameter[4];

  private static final StringParameter<Integer> MONITORING = StringParameter
      .typeInteger(PARA_MONITORINGLEVEL)
      .documentation(Documentation.en("MonitoringLevel of order (0, 5, 10, 15 or 20)")
                         .de("Monitoring Level des Auftrags (0, 5, 10, 15 oder 20)").build()).optional().label("Monitoring Level").build();

  static {
    List<StringParameter<?>> paras = new ArrayList<StringParameter<?>>();
    paras.add(INPUT);
    paras.add(PRIO);
    paras.add(MONITORING);
    //Custom fields
    for (int i = 0; i < 4; i++) {
      CUSTOMS[i] =
          StringParameter
              .typeString(PARA_CUSTOM + i)
              .documentation(Documentation.en("Custom Field " + i + ". Some userdefined value to be used to recognize orders.")
                                 .de("Custom Feld " + i + ".").build()).optional().label("Custom Field " + i).build();
      paras.add(CUSTOMS[i]);
    }

    try {


      pluginDescription =
          PluginDescription.create(PluginType.orderInputSource).description("Creation of constant order inputs")
              .label("Constant input generator type").name(ConstantInputSource.class.getSimpleName())
              .parameters(ParameterUsage.Create, paras).addDatatype(load("Constant.xml")).addForm(load("ConstantForm.xml")).build();
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
    InputStream resourceAsStream = FileUtils.getInputStreamFromResource("forms/" + fileName, ConstantInputSource.class.getClassLoader());
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
                                                 String documentation) throws XynaException {
    String payload = (String) parameters.get(PARA_PAYLOAD_XML);
    //checken, ob ein wrapper-element um payload notwendig ist.
    String check = "<Wrapper>" + payload + "</Wrapper>"; //TODO achtung, da könnte ein PI element in payload enthalten sein?!
    Document doc = XMLUtils.parseString(check);
    if (XMLUtils.getChildElements(doc.getDocumentElement()).size() == 1) {
      //alles gut
    } else {
      //fehlte offenbar ein wrapper
      payload = check;
    }

    Integer prio = (Integer) parameters.get(PARA_PRIO);
    RemoteXynaOrderCreationParameter xocp = new RemoteXynaOrderCreationParameter(destinationKey);

    if (prio != null) {
      xocp.setPriority(prio);
    }
    String custom0 = (String) parameters.get(PARA_CUSTOM + "0");
    xocp.setCustom0(custom0);
    String custom1 = (String) parameters.get(PARA_CUSTOM + "1");
    xocp.setCustom1(custom1);
    String custom2 = (String) parameters.get(PARA_CUSTOM + "2");
    xocp.setCustom2(custom2);
    String custom3 = (String) parameters.get(PARA_CUSTOM + "3");
    xocp.setCustom3(custom3);

    Integer monitoringLevel = (Integer) parameters.get(PARA_MONITORINGLEVEL);
    xocp.setMonitoringLevel(monitoringLevel);

    return new ConstantInputSource(xocp, payload);
  }


  public boolean refactorParameters(Map<String, Object> parameters, DependencySourceType refactoredObjectType, String fqNameOld,
                                    String fqNameNew) throws XynaException {
    if (refactoredObjectType == DependencySourceType.DATATYPE) {
      String payload = (String) parameters.get(PARA_PAYLOAD_XML);
      String simpleNameOld = GenerationBase.getSimpleNameFromFQName(fqNameOld);
      String pathOld = GenerationBase.getPackageNameFromFQName(fqNameOld);

      if (payload != null && payload.contains(pathOld) && payload.contains(simpleNameOld)) {
        String simpleNameNew = GenerationBase.getSimpleNameFromFQName(fqNameNew);
        String pathNew = GenerationBase.getPackageNameFromFQName(fqNameNew);

        String check = "<Wrapper>" + payload + "</Wrapper>"; //TODO achtung, da könnte ein PI element in payload enthalten sein?!
        Document doc = XMLUtils.parseString(check);

        List<Element> dataElements = XMLUtils.getChildElementsRecursively(doc.getDocumentElement(), GenerationBase.EL.DATA);

        boolean changed = false;
        for (Element e : dataElements) {
          boolean ref = false;
          String type = e.getAttribute(GenerationBase.ATT.TYPENAME);
          if (type == null || type.length() == 0) {
            type = e.getAttribute(GenerationBase.ATT.REFERENCENAME);
            ref = true;
            if (type == null || type.length() == 0) {
              //simple type dataelemente sehen so aus
              continue;
            }
          }

          if (type.equals(simpleNameOld)) {
            String path = e.getAttribute(GenerationBase.ATT.TYPEPATH);
            if (path == null || path.length() == 0) {
              path = e.getAttribute(GenerationBase.ATT.REFERENCEPATH);
              if (type == null || type.length() == 0) {
                continue;
              }
            }
            if (path.equals(pathOld)) {
              if (ref) {
                e.setAttribute(GenerationBase.ATT.REFERENCENAME, simpleNameNew);
                e.setAttribute(GenerationBase.ATT.REFERENCEPATH, pathNew);
              } else {
                e.setAttribute(GenerationBase.ATT.TYPENAME, simpleNameNew);
                e.setAttribute(GenerationBase.ATT.TYPEPATH, pathNew);
              }
              changed = true;
            }
          }

        }

        if (changed) {
          //neues xml in die parameter reinhängen
          dataElements = XMLUtils.getChildElementsByName(doc.getDocumentElement(), GenerationBase.EL.DATA);
          payload = "";
          for (Element dataEl : dataElements) {
            payload += XMLUtils.getXMLString(dataEl, false);
          }
          parameters.put(PARA_PAYLOAD_XML, payload);
          return true;
        }
      }
    }

    return false;
  }


}
