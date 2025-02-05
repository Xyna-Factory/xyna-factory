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
package xmcp.xypilot.impl.locator;

import java.util.List;
import java.util.Optional;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.StepMapping;

import xmcp.processmodeller.datatypes.Area;
import xmcp.processmodeller.datatypes.Item;
import xmcp.processmodeller.datatypes.TextArea;
import xmcp.processmodeller.datatypes.datatypemodeller.MemberVariable;
import xmcp.processmodeller.datatypes.datatypemodeller.Method;
import xmcp.xypilot.MemberReference;
import xmcp.xypilot.XMOMItemReference;
import xmcp.xypilot.impl.gen.model.DomModel;
import xmcp.xypilot.impl.gen.model.ExceptionModel;
import xmcp.xypilot.impl.gen.model.ExceptionVariableModel;
import xmcp.xypilot.impl.gen.model.MappingModel;
import xmcp.xypilot.impl.gen.model.DomVariableModel;
import xmcp.xypilot.impl.gen.model.DomMethodModel;
import xmcp.xypilot.impl.gen.util.DomUtils;
import xmcp.xypilot.impl.gen.util.FilterCallbackInteractionUtils;

public class DataModelLocator {
  
  public static final String datatypesTypeName = "datatypes";
  public static final String serviceGroupsTypeName = "servicegroups";
  
  private static final String docuTextAreaName = "documentation";
  private static final String implTextAreaName = "implementation";

  public static MappingModel getMappingModel(MemberReference memberReference, XynaOrderServerExtension order) throws XynaException {
    StepMapping targetMapping = FilterCallbackInteractionUtils.getMapping(memberReference, order);
    return new MappingModel(targetMapping);
  }

  public static DomModel getDomModel(XMOMItemReference xmomItemReference, XynaOrderServerExtension order) throws XynaException {
    DOM dom = FilterCallbackInteractionUtils.getDatatypeDom(xmomItemReference, order, datatypesTypeName);
    DomModel result = new DomModel(dom);
    result.setLatestDocumentation(dom.getDocumentation());
    return result;
  }

  public static ExceptionModel getExceptionModel(XMOMItemReference xmomItemReference, XynaOrderServerExtension order) throws XynaException {
    ExceptionGeneration exception = FilterCallbackInteractionUtils.getException(xmomItemReference, order);
    ExceptionModel result = new ExceptionModel(exception);
    result.setLatestDocumentation(exception.getDocumentation());
    return result;
  }

  public static DomMethodModel getDomMethodModel(XMOMItemReference xmomItemReference, XynaOrderServerExtension order, String id, String type) throws XynaException {
    DOM dom = FilterCallbackInteractionUtils.getDatatypeDom(xmomItemReference, order, type);
    Item item = FilterCallbackInteractionUtils.getDatatypeItemByAreaOrItemId(xmomItemReference, order, id, type);
    if (item == null || !(item instanceof Method)) {
      throw new XynaException("Method not found!");
    }
    Method method = ((Method)item);
    Operation targetMethod = dom.getOperationByName(method.getName());
    DomMethodModel result =  new DomMethodModel(dom, targetMethod);
    result.setLatestDocumentation(getTextContentAreaByName(method.getAreas(), docuTextAreaName));
    result.setLatestImplementation(getTextContentAreaByName(method.getAreas(), implTextAreaName));
    return result;
  }

  public static DomVariableModel getDomVariableModel(XMOMItemReference xmomItemReference, XynaOrderServerExtension order, String id) throws XynaException {
    DOM dom = FilterCallbackInteractionUtils.getDatatypeDom(xmomItemReference, order, datatypesTypeName);
    Item item = FilterCallbackInteractionUtils.getDatatypeItemByAreaOrItemId(xmomItemReference, order, id, datatypesTypeName);
    if (item == null || !(item instanceof MemberVariable)) {
      throw new XynaException("Variable not found!");
    }
    MemberVariable memberVar = ((MemberVariable) item);
    AVariable targetVariable = DomUtils.getVariableByName(dom, ((MemberVariable) item).getName());
    DomVariableModel result =  new DomVariableModel(dom, targetVariable);
    result.setLatestDocumentation(getTextContentAreaByName(memberVar.getAreas(), docuTextAreaName));
    return result;
  }

  public static ExceptionVariableModel getExceptionVariableModel(XMOMItemReference xmomItemReference, XynaOrderServerExtension order, String id) throws XynaException {
    ExceptionGeneration exception = FilterCallbackInteractionUtils.getException(xmomItemReference, order);
    Item item = FilterCallbackInteractionUtils.getExceptionItemByAreaOrItemId(xmomItemReference, order, id);
    if (item == null || !(item instanceof MemberVariable)) {
      throw new XynaException("Variable not found!");
    }
    MemberVariable memberVar = ((MemberVariable) item);
    AVariable targetVariable = DomUtils.getVariableByName(exception, memberVar.getName());
    ExceptionVariableModel result = new ExceptionVariableModel(exception, targetVariable);
    result.setLatestDocumentation(getTextContentAreaByName(memberVar.getAreas(), docuTextAreaName));
    return result;
  }
    
  private static String getTextContentAreaByName(List<? extends Area> areas, String name) {
    if(areas == null) {
      return "";
    }
    Optional<? extends Area> area = areas.stream().filter(x -> name.equals(x.getName())).findAny();
    if(area.isEmpty() || !(area.get() instanceof TextArea)) {
      return "";
    }
    return ((TextArea)area.get()).getText();
  }
}
