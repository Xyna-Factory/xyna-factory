/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xmcp.oas.fman.codedservice;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCall;

import xmcp.oas.fman.datatypes.EndpointImplementationCreationData;
import xmcp.oas.fman.datatypes.OasEndpointService;
import xmcp.oas.fman.tools.GeneratedOasApiType;
import xmcp.oas.fman.tools.ImplementedOasApiType;
import xmcp.oas.fman.tools.OasApiType;
import xmcp.oas.fman.tools.OasGuiContext;
import xmcp.oas.fman.tools.OperationGroup;
import xmcp.oas.fman.tools.RtcData;

public class CSGetImplServices {

  public List<? extends OasEndpointService> execute(EndpointImplementationCreationData data) {
    List<OasEndpointService> result = new ArrayList<>();
    OasGuiContext context = new OasGuiContext();
    OasApiType genType = new GeneratedOasApiType(data.getAPIDatatype(), new RtcData(data.getGeneratedRtcRevision()));
    OasApiType implType = new ImplementedOasApiType(data.getImplementationDatatypeFqn(), new RtcData(data.getImplementationRtcRevision()));
    OperationGroup allOps = new OperationGroup(genType, context);
    OperationGroup implOps =  new OperationGroup(implType, context);
    DOM dom;
    try {
    dom = DOM.getOrCreateInstance(data.getImplementationDatatypeFqn(), new GenerationBaseCache(), data.getImplementationRtcRevision());
    dom.parseGeneration(true, false);
    } catch(Exception e) {
      throw new RuntimeException(e);
    }
    
    List<String> opsSorted = new ArrayList<>(allOps.getOperations());
    opsSorted.sort((x, y) -> x.compareTo(y));
    int index = 0;
    for(String op : opsSorted) {
      Operation operation = getOperation(dom, op);
      boolean isImplemented = implOps.getOperations().contains(op);
      OasEndpointService.Builder builder = new OasEndpointService.Builder();
      builder.status(isImplemented ? "OK" : "NOK");
      builder.serviceName(operation.getLabel());
      builder.implementationWorkflowFqn(getImplWfFqn(operation));
      builder.index(String.valueOf(index));
      builder.name(op);
      result.add(builder.instance());
      index++;
    }
    
    return result;
  }
  
  private String getImplWfFqn(Operation operation) {
    if(operation instanceof WorkflowCall) {
      return ((WorkflowCall)operation).getWf().getOriginalFqName();
    }
    return "";
  }
  
  private Operation getOperation(DOM dom, String opName) {
    try {
      return dom.getOperationByName(opName, true);
    } catch (XPRC_OperationUnknownException e) {
    }
    return null;
  }

}
