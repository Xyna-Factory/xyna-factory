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
package com.gip.xyna.xprc.xfractwfe.generation;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.xfractwfe.formula.BaseType;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo.ModelledType;
import com.gip.xyna.xprc.xfractwfe.formula.VariableAccessPart;
import com.gip.xyna.xprc.xfractwfe.formula.VariableInstanceFunctionIncovation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.OperationInfo;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;



public class StepBasedVariable implements VariableInfo {

  private static final Logger logger = CentralFactoryLogging.getLogger(StepBasedVariable.class);

  private final VariableIdentification vi;
  private final AVariable var;
  private final String inputVarPath; //bei variablen referenzen in workflows kann man ne id und nen (source-)path angeben. dies ist hier der path
  private final StepBasedIdentification context;
  private AVariable typeCast;


  public StepBasedVariable(String inputVarPath, VariableIdentification vi, StepBasedIdentification context) {
    this.inputVarPath = inputVarPath;
    this.vi = vi;
    this.context = context;
    this.var = vi.getVariable();
  }


  /**
   * macht nichts mit der indexdef
   */
  public VariableInfo follow(List<VariableAccessPart> parts, int depth) throws XPRC_InvalidVariableMemberNameException {
    AVariable tempVar = getAVariable();
    //inputVarPath ist maximal bei der root-variable gesetzt
    if (inputVarPath.length() > 0) {
      String[] pathParts = inputVarPath.split("\\.");
      for (String part : pathParts) {
        if (tempVar.isPrototype()) {
          throw new XPRC_InvalidVariableMemberNameException(tempVar.getLabel(), part, new RuntimeException("Type is prototype."));
        }
        tempVar = follow(tempVar, part);
      }
    }

    for (int i = 0; i <= depth; i++) {
      VariableAccessPart part = parts.get(i);
      if (part.isMemberVariableAccess()) {
        if (tempVar.isPrototype()) {
          throw new XPRC_InvalidVariableMemberNameException(tempVar.getLabel(), part.getName(), new RuntimeException("Type is prototype."));
        }
        tempVar = follow(tempVar, part.getName());
      } else {
        if (tempVar.getDomOrExceptionObject() instanceof ExceptionGeneration) {
          throw new RuntimeException("Invocation of instance method on exception not supported!");
        }
        tempVar = checkInstanceInvocation((DOM) tempVar.getDomOrExceptionObject(), (VariableInstanceFunctionIncovation) part);
      }
    }
    VariableIdentification childVi = new VariableIdentification();
    childVi.variable = tempVar;
    childVi.scope = null; //FIXME siehe fixme unten
    return new StepBasedVariable("", childVi, context);
  }


  public static class InvalidInvocationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private final String operationName;
    private final String providerXmlName;


    public InvalidInvocationException(String operationName, String providerXmlName) {
      super("Operation " + operationName + " not found in " + providerXmlName);
      this.operationName = operationName;
      this.providerXmlName = providerXmlName;
    }


    public String getOperationName() {
      return operationName;
    }


    public String getProviderXMLName() {
      return providerXmlName;
    }

  }


  private AVariable follow(AVariable parent, String memberVarName) throws XPRC_InvalidVariableMemberNameException {
    if (parent.isJavaBaseType()) {
      throw new XPRC_InvalidVariableMemberNameException("<not a complex type>", memberVarName);
    }
    List<AVariable> memberVars = parent.getDomOrExceptionObject().getAllMemberVarsIncludingInherited();
    for (AVariable memberVar : memberVars) {
      if (memberVar.getVarName().equals(memberVarName)) {
        return memberVar;
      }
    }
    throw new XPRC_InvalidVariableMemberNameException(parent.getDomOrExceptionObject().getOriginalFqName(), memberVarName);
  }


  //FIXME diese methode ist in StepBasedIdentification kopiert
  public AVariable checkInstanceInvocation(DOM datatypeWithOperations, VariableInstanceFunctionIncovation instanceMethod) {
    String fqName = datatypeWithOperations.getOriginalFqName();
    if (!datatypeWithOperations.exists()) {
      throw new RuntimeException(fqName + " not found.");
    }
    while (datatypeWithOperations != null) {
      for (List<Operation> serviceEntries : datatypeWithOperations.getServiceNameToOperationMap().values()) {
        for (Operation operation : serviceEntries) {
          if (operation.getName().equals(instanceMethod.getName()) && !operation.isStatic()) {
            List<AVariable> resultValues = operation.getOutputVars();

            List<TypeInfo> typesOfInputVars = new ArrayList<TypeInfo>();
            for (AVariable aVar : operation.getInputVars()) {
              typesOfInputVars.add(getTypeInfo(aVar, false));
            }
            instanceMethod.setInputParameterTypes(typesOfInputVars);
            if (operation instanceof CodeOperation) {
              instanceMethod.setRequiresXynaOrder(((CodeOperation) operation).requiresXynaOrder());
            }
            if (resultValues.size() > 1) {
              throw new RuntimeException("Instance methods with more than 1 result are not supported: " + instanceMethod.getName());
            } else if (resultValues.size() == 0) {
              // what to do, return null? (would throw NPW on getVariableType)
            } else {
              resultValues.get(0).markAsFunctionResult();
              return resultValues.get(0);
            }
          }
        }
      }
      datatypeWithOperations = datatypeWithOperations.getSuperClassGenerationObject(); //im supertype schauen
    }
    throw new InvalidInvocationException(instanceMethod.getName(), fqName);
  }


  public static class StepBasedType implements ModelledType {

    private final DomOrExceptionGenerationBase dom;
    private final StepBasedIdentification context;


    public StepBasedType(DomOrExceptionGenerationBase dom, StepBasedIdentification context) {
      this.dom = dom;
      this.context = context;
    }


    public String getFqClassName() {
      return dom.getFqClassName();
    }


    public String getFqXMLName() {
      return dom.getOriginalFqName();
    }


    public DomOrExceptionGenerationBase getGenerationType() {
      return dom;
    }


    public boolean isSuperClassOf(ModelledType otherType) {
      if (otherType instanceof StepBasedType) {
        StepBasedType otherTypeDom = (StepBasedType) otherType;
        return DomOrExceptionGenerationBase.isSuperClass(dom, otherTypeDom.dom);
      }
      throw new RuntimeException();
    }


    public String getSimpleClassName() {
      return dom.getSimpleClassName();
    }


    public List<VariableInfo> getAllMemberVarsIncludingInherited() {
      List<VariableInfo> members = new ArrayList<VariableInfo>();
      for (AVariable v : dom.getAllMemberVarsIncludingInherited()) {
        VariableIdentification childVi = new VariableIdentification();
        childVi.variable = v;
        members.add(new StepBasedVariable("", childVi, null));
      }
      return members;
    }


    @Override
    public int hashCode() {
      return dom.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof StepBasedType)) {
        return false;
      }
      return dom.equals(((StepBasedType) obj).dom);
    }


    public List<OperationInfo> getAllInstanceOperationsIncludingInherited() {
      List<OperationInfo> operations = new ArrayList<OperationInfo>();
      if (dom instanceof DOM) {
        for (OperationInformation operation : ((DOM) dom).collectOperationsOfDOMHierarchy(false)) {
          operations.add(new StepBasedOperation(operation.getOperation(), context));
        }
        return operations;
      } else {
        return Collections.emptyList();
      }
    }


    public String generateEmptyConstructor() throws XPRC_InvalidPackageNameException {
      AVariable var;
      if (dom instanceof DOM) {
        var = new DatatypeVariable(context.step.creator);
        var.domOrException = dom;
        var.originalClassName = dom.getOriginalSimpleName();
        var.originalPath = dom.getOriginalPath();
        var.setFQClassName(dom.getFqClassName());
      } else {
        var = new ExceptionVariable(context.step.creator);
        ((ExceptionVariable) var).init(dom.getOriginalPath(), dom.getOriginalSimpleName());
      }
      var.children = new ArrayList<AVariable>();
      CodeBuffer cb = new CodeBuffer("");
      var.generateConstructor(cb, Collections.<String> emptySet(), true);
      return cb.toString(false);
    }


    public boolean isAbstract() {
      return dom.isAbstract();
    }


    public Set<ModelledType> getSubTypesRecursivly() {
      GenerationBaseCache gba = new GenerationBaseCache();
      Set<StepBasedType> types = new TreeSet<>(new Comparator<StepBasedType>() {

        public int compare(StepBasedType o1, StepBasedType o2) {
          return o1.dom.getOriginalFqName().compareTo(o2.dom.getOriginalFqName());
        }
      });
      getSubTypesRecursivlyInternaly(gba, types);
      return new HashSet<ModelledType>(types);
    }


    private void getSubTypesRecursivlyInternaly(GenerationBaseCache gba, Set<StepBasedType> visitedTypes) {
      StepBasedType thisType = new StepBasedType(dom, context);
      if (visitedTypes.add(thisType)) {
        Set<GenerationBase> subTypes = dom.getSubTypes(gba);
        for (GenerationBase subType : subTypes) {
          StepBasedType sbt = new StepBasedType((DomOrExceptionGenerationBase) subType, context);
          sbt.getSubTypesRecursivlyInternaly(gba, visitedTypes);
        }
      }
    }

  }


  public static class StepBasedOperation implements OperationInfo {

    private final Operation operation;
    private final List<VariableInfo> resultTypes;


    private StepBasedOperation(Operation operation, StepBasedIdentification context) {
      this.operation = operation;
      if (operation.getOutputVars().size() > 0) {
        resultTypes = new ArrayList<VariableInfo>();
        for (AVariable aVar : operation.getOutputVars()) {
          VariableIdentification childVi = new VariableIdentification();
          childVi.variable = aVar;
          childVi.scope = null; //FIXME siehe fixme unten
          resultTypes.add(new StepBasedVariable("", childVi, context));
        }
      } else {
        resultTypes = Collections.emptyList();
      }
    }


    public String getOperationName() {
      return operation.getName();
    }


    public List<VariableInfo> getResultTypes() {
      return resultTypes;
    }

  }


  //FIXME diese methode ist in StepBasedIdentification kopiert
  private TypeInfo getTypeInfo(AVariable var, boolean ignoreList) {
    if (!ignoreList && var.isList()) {
      return new TypeInfo(BaseType.LIST);
    }
    if (var.getDomOrExceptionObject() != null) {
      return new TypeInfo(new StepBasedType(var.getDomOrExceptionObject(), context), false);
    } else if (var.getJavaTypeEnum() != null) {
      if (var.getJavaTypeEnum() == PrimitiveType.ANYTYPE) {
        return TypeInfo.ANY;
      } else {
        return new TypeInfo(BaseType.valueOfJavaName(var.getJavaTypeEnum().getClassOfType()));
      }
    } else {
      logger.debug("Unknown type of variable name=" + var.getVarName() + ", id=" + var.getId() + ", children=" + var.getChildren().size());
      return new TypeInfo(BaseType.STRING);
    }
  }


  public TypeInfo getTypeInfo(boolean ignoreList) {
    return getTypeInfo(getAVariable(), ignoreList);
  }


  public String getJavaCodeForVariableAccess() {
    //FIXME aufrufe auf kind-variablen funktionieren nicht, weil vi null ist!
    //es fehlen auch die getter für den path zum kind, der nicht im inputVarPath steckt....
    String getter = "";
    if (inputVarPath.length() > 0) {
      getter = getAVariable().getGetter(inputVarPath);
    } else {
      getter = getAVariable().getVarName();
    }

    String result = context.getPathTo(vi) + getter;

    return result;
  }


  public boolean isIdentifieableVariable() {
    return context.getPathTo(vi) != null;
  }


  public AVariable getAVariable() {
    if (typeCast != null) {
      return typeCast;
    } else {
      return var;
    }
  }


  public String getVarName() {
    return getAVariable().getVarName();
  }


  public void castTo(TypeInfo type) {
    if (var instanceof ExceptionVariable) {
      typeCast = new ExceptionVariable((ExceptionVariable) var);
      ((ExceptionVariable) typeCast)
          .setExceptionGeneration((ExceptionGeneration) ((StepBasedType) type.getModelledType()).getGenerationType());
    } else {
      typeCast = new DatatypeVariable((DatatypeVariable) var);
    }
    typeCast.domOrException = ((StepBasedType) type.getModelledType()).getGenerationType();
    if (typeCast.domOrException != null) {
      typeCast.setClassName(typeCast.domOrException.getSimpleClassName());
      typeCast.setFQClassName(typeCast.domOrException.getFqClassName());
    }
  }
}
