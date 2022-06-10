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

package com.gip.xyna.xact.filter.session.repair;



import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCallInService;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;

import xmcp.processmodeller.datatypes.RepairEntry;



public class DOMRepair extends DOMandExceptionRepair<DOM> {
  

  @Override
  protected DOM getDomOrExceptionGenerationBaseObj(GenerationBaseObject obj) {
    return obj.getDOM();
  }


  @Override
  protected void replaceParent(DOM obj) {
    // set parent to null
    obj.replaceParent(null);
  }


  @Override
  public boolean responsible(GenerationBaseObject obj) {
    return obj.getGenerationBase() instanceof DOM;
  }


  @Override
  public List<RepairEntry> repair(GenerationBaseObject gbo) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    result.addAll(super.repair(gbo));
    result.addAll(repairServices(gbo, true));
    return result;
  }


  @Override
  public List<RepairEntry> getRepairEntries(GenerationBaseObject gbo) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    result.addAll(super.getRepairEntries(gbo));
    result.addAll(repairServices(gbo, false));
    return result;

  }


  private List<RepairEntry> repairServices(GenerationBaseObject gbo, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    DOM dom = getDomOrExceptionGenerationBaseObj(gbo);
    try {
      dom.parse(false);
    } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
      throw new RuntimeException("datatype could not be parsed");
    }
    List<Operation> operations = dom.getOperations();
    Operation operation;

    for (int i = 0; i < operations.size(); i++) {
      operation = operations.get(i);
      result.addAll(convertOperationVariables(operation, i, apply));
      result.addAll(updateOperationSignature(dom, operation, apply));
      result.addAll(convertReferenceServiceToAbstract(dom, operation, i, apply));
    }

    return result;
  }


  private List<RepairEntry> convertReferenceServiceToAbstract(DOM dom, Operation operation, int opIdx, boolean apply) {
    if (!(operation instanceof WorkflowCallInService)) {
      return Collections.emptyList();
    }

    if (operation.isAbstract()) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();
    WorkflowCallInService op = (WorkflowCallInService) operation;
    if (!op.getWf().exists()) {
      RepairEntry entry = new RepairEntry();
      entry.setDescription("Referenced workflow not found");
      entry.setId(ObjectId.createMemberMethodId(opIdx));
      entry.setLocation(operation.getLabel());
      entry.setResource(dom.getFqClassName());
      entry.setType("Member method turned abstract");
      result.add(entry);

      if (apply) {
        operation.setAbstract(true);
      }
    } else if (!Utils.isValidWorkflowReference(dom, operation, op.getWf())) {
      RepairEntry entry = new RepairEntry();
      entry.setDescription("Referenced workflow has incompatible inputs/Outputs");
      entry.setId(ObjectId.createMemberMethodId(opIdx));
      entry.setLocation(operation.getLabel());
      entry.setResource(dom.getFqClassName());
      entry.setType("Member method turned abstract");
      result.add(entry);

      if (apply) {
        operation.setAbstract(true);
      }
    } else if (!workflowThrowIsValid(operation, op.getWf())) {
      RepairEntry entry = new RepairEntry();
      List<ExceptionVariable> opEvs = operation.getThrownExceptions();
      StringBuilder sb = new StringBuilder();
      for(ExceptionVariable opEv : opEvs) {
        sb.append(opEv.getFQClassName());
        sb.append(", ");
      }
      
      //remove last " ,"
      if (sb.length() > 2) {
        sb.setLength(sb.length() - 2);
      }
      
      entry.setDescription("Referenced workflow has incompatible signature. Thrown exceptions do not match. Workflow may throw: " + sb.toString());
      entry.setId(ObjectId.createMemberMethodId(opIdx));
      entry.setLocation(operation.getLabel());
      entry.setResource(dom.getFqClassName());
      entry.setType("Member method turned abstract");
      result.add(entry);

      if (apply) {
        operation.setAbstract(true);
      }
    }

    return result;
  }


  private boolean workflowThrowIsValid(Operation operation, WF wf) {
    List<ExceptionVariable> opEvs = operation.getThrownExceptions();
    List<ExceptionVariable> wfEvs = wf.getAllThrownExceptions();

    //if no exceptions are allowed to be thrown and the workflow throws something
    //unless everything the workflow throws is a reservedServerObject
    //-> workflow may still throw XynaException, ServerException and Exception
    //   even though the service does not specify that.
    if (opEvs.size() == 0 && wfEvs.size() > 0 && wfEvs.stream().anyMatch(x -> !DOM.isReservedServerObjectByFqClassName(x.getFQClassName())))
      return false;

    for (ExceptionVariable ev : wfEvs) {
      //do not check reserved server objects like XynaException, ServerException and Exception
      if(DOM.isReservedServerObjectByFqClassName(ev.getFQClassName())) {
        continue;
      }
      if (!opEvs.stream().anyMatch(x -> compatibleExceptionVariables(ev, x))) {
        return false;
      }
    }


    return true;
  }


  //true if ev2 can be used where ev1 is
  private boolean compatibleExceptionVariables(ExceptionVariable ev1, ExceptionVariable ev2) {
    DomOrExceptionGenerationBase doe1 = ev1.getDomOrExceptionObject();
    DomOrExceptionGenerationBase doe2 = ev2.getDomOrExceptionObject();

    if (doe1 == null || doe2 == null) {
      return false;
    }

    return DomOrExceptionGenerationBase.isSuperClass(doe1, doe2) && DomOrExceptionGenerationBase.isSuperClass(doe2, doe2);
  }


  private List<RepairEntry> updateOperationSignature(DOM dom, Operation operation, boolean apply) {

    if (!operation.isInheritedOrOverriden(dom)) {
      return Collections.emptyList();
    }

    List<RepairEntry> result = new ArrayList<RepairEntry>();
    Operation superOperation = null;

    try {
      superOperation = dom.getSuperClassGenerationObject().getOperationByName(operation.getName(), true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    if (!operation.hasEqualSignature(superOperation)) {

      RepairEntry entry = new RepairEntry();
      int operationIndex = -1;
      OperationInformation[] operationInformations = dom.collectOperationsOfDOMHierarchy(true);
      for (int operationIdx = 0; operationIdx < operationInformations.length; operationIdx++) {
        OperationInformation oi = operationInformations[operationIdx];
        if (oi.getOperation().equals(operation)) {
          operationIndex = operationIdx;
        }
      }


      entry.setDescription("Inherited Operation signature does not match.");
      entry.setId(ObjectId.createMemberMethodId(operationIndex));
      entry.setResource(dom.getFqClassName());
      entry.setType("Operation Signature change");
      result.add(entry);

      if (apply) {
        operation.takeOverSignature(superOperation);
      }

    }


    return result;
  }


  private List<RepairEntry> convertOperationVariables(Operation operation, int operationIndex, boolean apply) {
    List<RepairEntry> result = new ArrayList<RepairEntry>();
    List<RepairEntry> re;
    List<AVariable> varList;
    String location;
    Function<Integer, String> createId;


    varList = operation.getInputVars();
    location = operation.getLabel();
    final int operationId = operationIndex;
    createId = (id) -> ObjectId.createMemberMethodId(operationId);
    re = XMOMRepair.convertAVariableList(location, varList, createId, apply);
    result.addAll(re);
    varList = operation.getOutputVars();
    re = XMOMRepair.convertAVariableList(location, varList, createId, apply);
    result.addAll(re);

    List<ExceptionVariable> exceptionVars = operation.getThrownExceptionsForMod();
    for (int i = exceptionVars.size() - 1; i >= 0; i--) {
      ExceptionVariable evc = exceptionVars.get(i);

      if (XMOMRepair.variableHasToBeConverted(evc)) {
        RepairEntry entry = new RepairEntry();
        entry.setDescription("Exception invalid.");
        entry.setId(createId.apply(i));
        entry.setLocation(location);
        entry.setType("Exception variable removal.");
        result.add(entry);

        if (apply) {
          exceptionVars.remove(evc);
        }
      }
    }


    return result;
  }

}
