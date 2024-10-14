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
package com.gip.xyna.xact.filter.session.gb;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xact.filter.session.exceptions.UnknownObjectIdException;
import com.gip.xyna.xact.filter.util.AVariableIdentification.VarUsageType;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.WF.WFStep;

public class ObjectId {
  
  private final ObjectType type;
  private final String baseId;
  private final ObjectPart part;
  private final String objectId;
  private static final char SEPARATOR = '-';
  private static final char SUFFIX_SEPARATOR = '_';
  
  
  public ObjectId(ObjectType type, String baseId) {
    this.type = type;
    this.baseId = baseId;
    this.part = ObjectPart.all;
    this.objectId = createId(type, baseId, part );
  }
  
  public ObjectId(ObjectType type, String baseId, ObjectPart part) {
    this.type = type;
    this.baseId = baseId;
    this.part = part;
    this.objectId = createId(type, baseId, part );
  }
  
  private ObjectId(ObjectType type, String baseId, ObjectPart part, String objectId) {
    this.type = type;
    this.baseId = baseId;
    this.part = part;
    this.objectId = objectId;
  }
  
  private static String emptyIfNull(String s) {
    if (s == null) {
      return "";
    }
    return s;
  }

  @Override
  public String toString() {
    return type + " " + objectId +" ("+baseId+","+part+")";
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + objectId.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ObjectId other = (ObjectId) obj;
    if (!objectId.equals(other.objectId))
      return false;
    return true;
  }
  
  
  public ObjectType getType() {
    return type;
  }

  public String getBaseId() {
    return baseId;
  }

  public String getObjectId() {
    return objectId;
  }
  
  public ObjectPart getPart() {
    return part;
  }
  
  public static String[] split(String id) {
    return id.split(String.valueOf(SEPARATOR));
  }

  public static ObjectId parse(String objectId) throws UnknownObjectIdException {
    int idx = objectId.indexOf('_');
    ObjectPart part = ObjectPart.all;
    String id = objectId;
    if( idx > 0 ) {
      part = ObjectPart.parse(objectId.substring(idx+1));
      id = objectId.substring(0, idx);
    }
    
    
    if( ObjectIdPrefix.workflow.match(id) ) {
      String baseId = ObjectIdPrefix.workflow.getBaseId(id);
      return new ObjectId(ObjectType.workflow, baseId, part, objectId);
    }
    
    if( ObjectIdPrefix.step.match(id) ) {
      String baseId = ObjectIdPrefix.step.getBaseId(id);
      return new ObjectId(ObjectType.step, baseId, part, objectId);
    }
    if( ObjectIdPrefix.labelArea.match(id) ) {
      String baseId = ObjectIdPrefix.labelArea.getBaseId(id);
      return new ObjectId(ObjectType.labelArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.documentationArea.match(id) ) {
      String baseId = ObjectIdPrefix.documentationArea.getBaseId(id);
      return new ObjectId(ObjectType.documentationArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.variable.match(id) ) {
      String baseId = ObjectIdPrefix.variable.getBaseId(id);
      return new ObjectId(ObjectType.variable, baseId, part, objectId);
    }
    if( ObjectIdPrefix.formulaArea.match(id) ) {
      String baseId = ObjectIdPrefix.formulaArea.getBaseId(id);
      return new ObjectId(ObjectType.formulaArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.formula.match(id) ) {
      String baseId = ObjectIdPrefix.formula.getBaseId(id);
      return new ObjectId(ObjectType.expression, baseId, part, objectId);
    }
    if( ObjectIdPrefix.branchArea.match(id) ) {
      String baseId = ObjectIdPrefix.branchArea.getBaseId(id);
      return new ObjectId(ObjectType.branchArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.caseArea.match(id) ) {
      String baseId = ObjectIdPrefix.caseArea.getBaseId(id);
      return new ObjectId(ObjectType.caseArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.distinctionBranch.match(id) ) {
      String baseId = ObjectIdPrefix.distinctionBranch.getBaseId(id);
      return new ObjectId(ObjectType.distinctionBranch, baseId, part, objectId);
    }
    if( ObjectIdPrefix.distinctionCase.match(id) ) {
      String baseId = ObjectIdPrefix.distinctionCase.getBaseId(id);
      return new ObjectId(ObjectType.distinctionCase, baseId, part, objectId);
    }
    if( ObjectIdPrefix.exceptionMessageArea.match(id) ) {
      String baseId = ObjectIdPrefix.exceptionMessageArea.getBaseId(id);
      return new ObjectId(ObjectType.exceptionMessageArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.exceptionMessage.match(id) ) {
      String baseId = ObjectIdPrefix.exceptionMessage.getBaseId(id);
      return new ObjectId(ObjectType.exceptionMessage, baseId, part, objectId);
    }
    if( ObjectIdPrefix.typeInfoArea.match(id) ) {
      String baseId = ObjectIdPrefix.typeInfoArea.getBaseId(id);
      return new ObjectId(ObjectType.typeInfoArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.memberVarArea.match(id) ) {
      String baseId = ObjectIdPrefix.memberVarArea.getBaseId(id);
      return new ObjectId(ObjectType.memberVarArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.memberVar.match(id) ) {
      String baseId = ObjectIdPrefix.memberVar.getBaseId(id);
      return new ObjectId(ObjectType.memberVar, baseId, part, objectId);
    }
    if( ObjectIdPrefix.memberMethodsArea.match(id) ) {
      String baseId = ObjectIdPrefix.memberMethodsArea.getBaseId(id);
      return new ObjectId(ObjectType.memberMethodsArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.memberMethod.match(id) ) {
      String baseId = ObjectIdPrefix.memberMethod.getBaseId(id);
      return new ObjectId(ObjectType.memberMethod, baseId, part, objectId);
    }
    if( ObjectIdPrefix.overriddenMethodsArea.match(id) ) {
      String baseId = ObjectIdPrefix.overriddenMethodsArea.getBaseId(id);
      return new ObjectId(ObjectType.overriddenMethodsArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.queryFilterCriterion.match(id) ) {
      String baseId = ObjectIdPrefix.queryFilterCriterion.getBaseId(id);
      return new ObjectId(ObjectType.queryFilterCriterion, baseId, part, objectId);
    }
    if( ObjectIdPrefix.querySortCriterion.match(id) ) {
      String baseId = ObjectIdPrefix.querySortCriterion.getBaseId(id);
      return new ObjectId(ObjectType.querySortCriterion, baseId, part, objectId);
    }
    if( ObjectIdPrefix.querySortingArea.match(id) ) {
      String baseId = ObjectIdPrefix.querySortingArea.getBaseId(id);
      return new ObjectId(ObjectType.querySortingArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.queryFilterArea.match(id) ) {
      String baseId = ObjectIdPrefix.queryFilterArea.getBaseId(id);
      return new ObjectId(ObjectType.queryFilterArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.querySelectionMasksArea.match(id) ) {
      String baseId = ObjectIdPrefix.querySelectionMasksArea.getBaseId(id);
      return new ObjectId(ObjectType.querySelectionMasksArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.querySelectionMask.match(id) ) {
      String baseId = ObjectIdPrefix.querySelectionMask.getBaseId(id);
      return new ObjectId(ObjectType.querySelectionMask, baseId, part, objectId);
    }
    if( ObjectIdPrefix.serviceGroupLib.match(id) ) {
      String baseId = ObjectIdPrefix.serviceGroupLib.getBaseId(id);
      return new ObjectId(ObjectType.serviceGroupLib, baseId, part, objectId);
    }
    if( ObjectIdPrefix.serviceGroupSharedLib.match(id) ) {
      String baseId = ObjectIdPrefix.serviceGroupSharedLib.getBaseId(id);
      return new ObjectId(ObjectType.serviceGroupSharedLib, baseId, part, objectId);
    }
    if( ObjectIdPrefix.methodVarArea.match(id) ) {
      String baseId = ObjectIdPrefix.methodVarArea.getBaseId(id);
      return new ObjectId(ObjectType.methodVarArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.libs.match(id) ) {
      String baseId = ObjectIdPrefix.libs.getBaseId(id);
      return new ObjectId(ObjectType.libs, baseId, part, objectId);
    }
    if( ObjectIdPrefix.orderInputSource.match(id) ) {
      String baseId = ObjectIdPrefix.orderInputSource.getBaseId(id);
      return new ObjectId(ObjectType.orderInputSource, baseId, part, objectId);
    }
    if( ObjectIdPrefix.exception.match(id) ) {
      String baseId = ObjectIdPrefix.exception.getBaseId(id);
      return new ObjectId(ObjectType.exception, baseId, part, objectId);
    }
    if( ObjectIdPrefix.datatype.match(id) ) {
      String baseId = ObjectIdPrefix.datatype.getBaseId(id);
      return new ObjectId(ObjectType.datatype, baseId, part, objectId);
    }
    if( ObjectIdPrefix.servicegroup.match(id) ) {
      String baseId = ObjectIdPrefix.servicegroup.getBaseId(id);
      return new ObjectId(ObjectType.servicegroup, baseId, part, objectId);
    }
    if( ObjectIdPrefix.memberDocumentationArea.match(id) ) {
      String baseId = ObjectIdPrefix.memberDocumentationArea.getBaseId(id);
      return new ObjectId(ObjectType.memberDocumentationArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.operationDocumentationArea.match(id) ) {
      String baseId = ObjectIdPrefix.operationDocumentationArea.getBaseId(id);
      return new ObjectId(ObjectType.operationDocumentationArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.operation.match(id) ) {
      String baseId = ObjectIdPrefix.operation.getBaseId(id);
      return new ObjectId(ObjectType.operation, baseId, part, objectId);
    }
    if( ObjectIdPrefix.implementationArea.match(id) ) {
      String baseId = ObjectIdPrefix.implementationArea.getBaseId(id);
      return new ObjectId(ObjectType.implementationArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.remoteDestinationArea.match(id)) {
      String baseId = ObjectIdPrefix.remoteDestinationArea.getBaseId(id);
      return new ObjectId(ObjectType.remoteDestinationArea, baseId, part, objectId);
    }
    if( ObjectIdPrefix.remoteDestinationParameter.match(id)) {
      String baseId = ObjectIdPrefix.remoteDestinationParameter.getBaseId(id);
      return new ObjectId(ObjectType.remoteDestinationParameter, baseId, part, objectId);
    }
    if( ObjectIdPrefix.remoteDestination.match(id)) {
      String baseId = ObjectIdPrefix.remoteDestination.getBaseId(id);
      return new ObjectId(ObjectType.remoteDestination, baseId, part, objectId);
    }

    if (ObjectIdPrefix.clipboardEntry.match(id)) {
      String baseId = ObjectIdPrefix.clipboardEntry.getBaseId(id);
      return new ObjectId(ObjectType.clipboardEntry, baseId);
    }

    if (ObjectIdPrefix.warning.match(id)) {
      String baseId = ObjectIdPrefix.warning.getBaseId(id);
      return new ObjectId(ObjectType.warning, baseId);
    }

    if (ObjectIdPrefix.reference.match(id)) {
      String baseId = ObjectIdPrefix.reference.getBaseId(id);
      return new ObjectId(ObjectType.reference, baseId);
    }

    throw new UnknownObjectIdException(objectId);
  }

  public static ObjectId createStepId(Step step) {
    if (step.getStepId() != null || step instanceof WFStep) {
      return new ObjectId(ObjectType.step, step.getStepId());
    } else {
      return new ObjectId(ObjectType.step, createHashId(step));
    }
  }

  public static ObjectId createClipboardId(int uniqueId) {
    return new ObjectId(ObjectType.clipboardEntry, "" + uniqueId);
  }

  public static ObjectId createWarningId(int uniqueId) {
    return new ObjectId(ObjectType.warning, "" + uniqueId);
  }
  
  public static ObjectId createReferenceId(String reference) {
    return new ObjectId(ObjectType.reference, reference);
  }

  public static ObjectId createStepId(Step step, ObjectPart part) {
    if (step.getStepId() != null || step instanceof WFStep) {
      return new ObjectId(ObjectType.step, step.getStepId(), part);
    } else {
      return new ObjectId(ObjectType.step, createHashId(step), part);
    }
  }

  public static ObjectId createExceptionHandlingId(Step step) {
    ObjectType type = step instanceof WFStep ? ObjectType.exceptionHandlingWf : ObjectType.exceptionHandling;

    if (step.getStepId() != null || step instanceof WFStep) {
      return new ObjectId(type, step.getStepId());
    } else {
      return new ObjectId(type, createHashId(step));
    }
  }

  public static ObjectId createRemoteDestinationParameterId(Step step, int idx) {
    return new ObjectId(ObjectType.remoteDestinationParameter, step.getStepId() + SEPARATOR + String.valueOf(idx));
  }


  public static ObjectId createRemoteDestinationAreaId(Step step) {
    return new ObjectId(ObjectType.remoteDestinationArea, step.getStepId());
  }


  public static ObjectId createRemoteDestinationId(Step step) {
    return new ObjectId(ObjectType.remoteDestination, step.getStepId());
  }


  public static String createId(ObjectType type) {
    return createId(type, null);
  }

  public static String createId(ObjectType type, String baseId) {
    return ObjectIdPrefix.of(type).getPrefix() + emptyIfNull(baseId);
  }
  
  public static String createId(ObjectType type, String baseId, ObjectPart part) {
    return ObjectIdPrefix.of(type).getPrefix() + emptyIfNull(baseId) + part.getSuffix();
  }
  
  public static String createId(ObjectId id, VarUsageType usage) {
    return createId(id.getType(), id.getBaseId(), ObjectPart.forUsage(usage));
  }

  public static String createLabelAreaId(String baseId) {
    return ObjectIdPrefix.labelArea.getPrefix() + emptyIfNull(baseId);
  }

  public static String createDocumentationAreaId(String baseId) {
    return ObjectIdPrefix.documentationArea.getPrefix() + emptyIfNull(baseId);
  }

  public static String createBranchId(String baseId, String branchNo) {
    return ObjectIdPrefix.distinctionBranch.getPrefix() + emptyIfNull(baseId) + SEPARATOR + branchNo;
  }

  public static String createCaseAreaId(String baseId, String branchNo) {
    return ObjectIdPrefix.caseArea.getPrefix() + emptyIfNull(baseId) + SEPARATOR + branchNo;
  }

  public static String createCaseId(String baseId, String caseNo) {
    return ObjectIdPrefix.distinctionCase.getPrefix() + emptyIfNull(baseId) + SEPARATOR + caseNo + ObjectPart.input.getSuffix();
  }

  public static String createVariableId(String baseId, VarUsageType usage, int idx) {
    return ObjectIdPrefix.variable.getPrefix() + emptyIfNull(baseId) + SEPARATOR + getVarUsage(usage) + idx;
  }

  public static String createMemberVariableId(int idx) {
    return ObjectIdPrefix.memberVar.getPrefix() + idx;
  }

  public static String createMemberMethodId(int idx) {
    return ObjectIdPrefix.memberMethod.getPrefix() + idx;
  }

  public static String createFormulaId(String baseId, VarUsageType usage, int idx) {
    return ObjectIdPrefix.formula.getPrefix() + emptyIfNull(baseId) + SEPARATOR + idx + ObjectPart.forUsage(usage).getSuffix();
  }
  
  public static String createFilterCriterionId(String baseId, VarUsageType usage, int idx) {
    return ObjectIdPrefix.queryFilterCriterion.getPrefix() + emptyIfNull(baseId) + SEPARATOR + idx + ObjectPart.forUsage(usage).getSuffix();
  }
  
  public static String createSortCriterionId(String baseId, VarUsageType usage, int idx) {
    return ObjectIdPrefix.querySortCriterion.getPrefix() + emptyIfNull(baseId) + SEPARATOR + idx + ObjectPart.forUsage(usage).getSuffix();
  }
  
  public static String createSelectionMaskId(String baseId, VarUsageType usage, int idx) {
    return ObjectIdPrefix.querySelectionMask.getPrefix() + emptyIfNull(baseId) + SEPARATOR + idx + ObjectPart.forUsage(usage).getSuffix();
  }
  
  public static String createServiceGroupLibId(int idx) {
    return ObjectIdPrefix.serviceGroupLib.getPrefix() + idx;
  }
  
  public static String createServiceGroupSharedLibId(int idx) {
    return ObjectIdPrefix.serviceGroupSharedLib.getPrefix() + idx;
  }
  
  public static String createMemberDocumentationAreaId(String baseId) {
    return ObjectIdPrefix.memberDocumentationArea.getPrefix() + emptyIfNull(baseId);
  }
  
  public static String createOperationDocumentationAreaId(String baseId) {
    return ObjectIdPrefix.operationDocumentationArea.getPrefix() + emptyIfNull(baseId);
  }
  
  public static String createOperationImplementationAreaId(String baseId) {
    return ObjectIdPrefix.implementationArea.getPrefix() + emptyIfNull(baseId);
  }
  
  public static String createMetaTagId(int idx) {
    return ObjectIdPrefix.metaTag.getPrefix() + idx;
  }
  
  public static String createIdForCase(String baseId, String branchId, String caseId) {
    return baseId + SEPARATOR + ObjectIdPrefix.distinctionBranch +  branchId + SEPARATOR + ObjectIdPrefix.distinctionCase + caseId;
  }
  
  private static String getVarUsage(VarUsageType usage) {
    switch( usage ) {
    case input:
      return "in";
    case output:
      return "out";
    case thrown:
      return "thrown";
    default:
    }
    return usage.name();
  }

  private static String createHashId(Step step) {
    return "U" + System.identityHashCode(step);
  }

  public static Pair<VarUsageType, Integer> parseVariableInfo(ObjectId oi) {
    int idx = oi.getObjectId().indexOf(SEPARATOR);
    String end = oi.getObjectId().substring(idx+1);
    if( end.startsWith("in") ) {
      return Pair.of(VarUsageType.input, Integer.valueOf( end.substring(2)));
    }
    if( end.startsWith("out") ) {
      return Pair.of(VarUsageType.output, Integer.valueOf( end.substring(3)));
    }
    if( end.startsWith("thrown") ) {
      return Pair.of(VarUsageType.thrown, Integer.valueOf( end.substring(6)));
    }
    throw new IllegalArgumentException(oi.getObjectId()+" has no variable info");
  }

  public static int parseMemberVarNumber(ObjectId oi) {
    if (!oi.getObjectId().startsWith(ObjectIdPrefix.memberVar.prefix)) {
      throw new IllegalArgumentException(oi.getObjectId() + " is of invalid format for a member variable (does not start with " + ObjectIdPrefix.memberVar.prefix + ")");
    }

    return Integer.parseInt(oi.getObjectId().substring(ObjectIdPrefix.memberVar.prefix.length()));
  }

  public static int parseMemberMethodNumber(ObjectId oi) {
    if (!oi.getObjectId().startsWith(ObjectIdPrefix.memberMethod.prefix)) {
      throw new IllegalArgumentException(oi.getObjectId() + " is of invalid format for a member method (does not start with " + ObjectIdPrefix.memberMethod.prefix + ")");
    }

    return Integer.parseInt(oi.getObjectId().substring(ObjectIdPrefix.memberMethod.prefix.length()));
  }

  public static int parseLibNumber(ObjectId oi) {
    if (!oi.getObjectId().startsWith(ObjectIdPrefix.serviceGroupLib.prefix)) {
      throw new IllegalArgumentException(oi.getObjectId() + " is of invalid format for a member method (does not start with " + ObjectIdPrefix.serviceGroupLib.prefix + ")");
    }

    return Integer.parseInt(oi.getObjectId().substring(ObjectIdPrefix.serviceGroupLib.prefix.length()));
  }

  public static int parseSharedLibNumber(ObjectId oi) {
    if (!oi.getObjectId().startsWith(ObjectIdPrefix.serviceGroupSharedLib.prefix)) {
      throw new IllegalArgumentException(oi.getObjectId() + " is of invalid format for a member method (does not start with " + ObjectIdPrefix.serviceGroupSharedLib.prefix + ")");
    }
    
    return Integer.parseInt(oi.getObjectId().substring(ObjectIdPrefix.serviceGroupSharedLib.prefix.length()));
  }

  public static int parseFormulaNumber(ObjectId oi) {
    try {
      int beginIdx = oi.getObjectId().indexOf(SEPARATOR) + 1;
      int endIdx = oi.getObjectId().indexOf(SUFFIX_SEPARATOR);
      return Integer.valueOf(oi.getObjectId().substring(beginIdx, endIdx));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Cannot parse formula number for " + oi.getObjectId());
    }
  }

  public static int parseBranchNumber(ObjectId oi) {
    try {
      int idx = oi.getObjectId().indexOf(SEPARATOR);
      return Integer.valueOf(oi.getObjectId().substring(idx + 1));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Cannot parse branch number for " + oi.getObjectId());
    }
  }

  public static int parseCaseNumber(ObjectId oi) {
    try {
      int beginIdx = oi.getObjectId().indexOf(SEPARATOR) + 1;
      int endIdx = oi.getObjectId().indexOf(SUFFIX_SEPARATOR);
      return Integer.valueOf(oi.getObjectId().substring(beginIdx, endIdx));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Cannot parse case number for " + oi.getObjectId());
    }
  }

  public static int parseCaseAreaNumber(ObjectId oi) {
    try {
      int idx = oi.getObjectId().indexOf(SEPARATOR);
      return Integer.valueOf(oi.getObjectId().substring(idx + 1));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Cannot parse case area number for " + oi.getObjectId());
    }
  }


  public enum ObjectIdPrefix {
    workflow("wf") {
      public boolean match(String objectId) {
        return objectId.equals(prefix);
      }
      public String getBaseId(String id) {
        return null;
      }
    }, 
    step("step") {
      public boolean match(String objectId) {
         return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    variable("var") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        int idx = id.indexOf(SEPARATOR);
        if (prefix.length() < idx) {
          // input/output of step
          return id.substring(prefix.length(), idx);
        } else {
          // input/output of workflow, hence no base-id
          return null;
        }
      }
    },
    service("service") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        int idx = id.indexOf(SEPARATOR);
        return id.substring(prefix.length(), idx);
      }
    },
    labelArea("labelArea") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    documentationArea("documentationArea") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    branchArea("branchArea") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    caseArea("caseArea") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        int idx = id.indexOf(SEPARATOR);
        return id.substring(prefix.length(), idx);
      }
    },
    formulaArea("formulaArea") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    formula("formula") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        int idx = id.indexOf(SEPARATOR);
        return id.substring(prefix.length(), idx);
      }
    },
    distinctionBranch("branch") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    distinctionCase("case") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    exceptionMessage("exceptionMessage"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    exceptionMessageArea("exceptionMessageArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return null;
      }
    },
    storableProperty("storableProperty"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    typeInfoArea("typeInfoArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return null;
      }
    },
    memberVarArea("memberVarArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return null;
      }
    },
    memberVar("memberVar") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return null;
      }
    },
    memberMethodsArea("memberMethodsArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return null;
      }
    },
    memberMethod("memberMethod") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    overriddenMethodsArea("overriddenMethodsArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    queryFilterCriterion("queryFilterCriterion"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    querySortCriterion("querySortCriterion"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    querySortingArea("querySortingArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    queryFilterArea("queryFilterArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    querySelectionMasksArea("querySelectionMasksArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    querySelectionMask("querySelectionMask"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length(), id.indexOf(SEPARATOR));
      }
    },
    libs("libs"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return null;
      }
    },
    serviceGroupLib("serviceGroupLib"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    serviceGroupSharedLib("serviceGroupSharedLib"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    methodVarArea("methodVarArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    operation("operation"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix) 
            && !objectId.startsWith(ObjectIdPrefix.operationDocumentationArea.getPrefix());
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    datatype("datatype"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    exception("exception"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    exceptionHandling("exceptionHandling") {
      public boolean match(String objectId) {
         return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    exceptionHandlingWf("exceptionHandlingWf") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    orderInputSource("orderInputSource"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    servicegroup("servicegroup"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    memberDocumentationArea("memberDocumentationArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    operationDocumentationArea("operationDocumentationArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    implementationArea("implementationArea") {
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    remoteDestinationArea("remoteDestinationArea"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }      
    },
    remoteDestination("remoteDestination"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }      
    },
    remoteDestinationParameter("remoteDestinationParameter"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    clipboardEntry("clipboard"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    warning("warning"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    reference("reference"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    },
    metaTag("metaTag"){
      public boolean match(String objectId) {
        return objectId.startsWith(prefix);
      }
      public String getBaseId(String id) {
        return id.substring(prefix.length());
      }
    }
    ;

    protected String prefix;

    private ObjectIdPrefix(String prefix) {
      this.prefix = prefix;
    }

    public abstract boolean match(String objectId);
    
    public String getPrefix() {
      return prefix;
    }

    public abstract String getBaseId(String id);
    
    public static ObjectIdPrefix of(ObjectType type) {
      switch( type ) {
      case step:
        return step;
      case workflow:
        return workflow;
      case variable:
        return variable;
      case memberVar:
        return memberVar;
      case service:
        return service;
      case distinctionBranch:
        return distinctionBranch;
      case distinctionCase:
        return distinctionCase;
      case branchArea:
        return branchArea;
      case caseArea:
        return caseArea;
      case formulaArea:
        return formulaArea;
      case expression:
        return formula;
      case exceptionMessage:
        return exceptionMessage;
      case exceptionMessageArea:
        return exceptionMessageArea;
      case storableProperty:
        return storableProperty;
      case typeInfoArea:
        return typeInfoArea;
      case memberVarArea:
        return memberVarArea;
      case memberMethodsArea:
        return memberMethodsArea;
      case overriddenMethodsArea:
        return overriddenMethodsArea;
      case queryFilterCriterion:
        return queryFilterCriterion;
      case querySortCriterion:
        return querySortCriterion;
      case querySortingArea:
        return querySortingArea;
      case queryFilterArea:
        return queryFilterArea;
      case querySelectionMasksArea:
        return querySelectionMasksArea;
      case querySelectionMask:
        return querySelectionMask;
      case serviceGroupLib:
        return serviceGroupLib;
      case serviceGroupSharedLib:
        return serviceGroupSharedLib;
      case methodVarArea:
        return methodVarArea;
      case operation:
        return operation;
      case datatype:
        return datatype;
      case orderInputSource:
        return orderInputSource;
      case servicegroup:
        return servicegroup;
      case exception:
        return exception;
      case exceptionHandling:
        return exceptionHandling;
      case exceptionHandlingWf:
        return exceptionHandlingWf;
      case memberDocumentationArea:
        return memberDocumentationArea;
      case operationDocumentationArea:
        return operationDocumentationArea;
      case remoteDestination:
        return remoteDestination;
      case remoteDestinationArea:
        return remoteDestinationArea;
      case remoteDestinationParameter:
        return remoteDestinationParameter;
      case clipboardEntry:
        return clipboardEntry;
      case warning:
        return warning;
      case reference:
        return reference;
      default:
        return null;
      }
    }

    
  }
  
  public enum ObjectPart {
    input,
    output,
    thrown,
    all;

    public static ObjectPart parse(String string) {
      if( string.isEmpty() ) {
        return null;
      }
      return valueOf(string); //FIXME Exception
    }

    public static ObjectPart forUsage(VarUsageType usage) {
      switch( usage ) {
      case input:
        return input;
      case output:
        return output;
      case thrown:
        return thrown;
      default:
        throw new IllegalArgumentException("no ObjectPart for "+usage);
      }
    }

    public String getSuffix() {
      if( this == all ) {
        return "";
      } else {
        return SUFFIX_SEPARATOR + name();
      }
     }

    public VarUsageType asUsage() {
      switch (this) {
        case input : return VarUsageType.input;
        case output : return VarUsageType.output;
        case thrown : return VarUsageType.thrown;
        default : throw new IllegalArgumentException(name() + " is not a valid usage.");
      }
    }
    
  }


}
