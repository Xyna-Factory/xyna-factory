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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.StringUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.exceptions.XDEV_UNSUPPORTED_FEATURE;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_EmptyVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidExceptionVariableXmlMissingTypeNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLMissingListValueException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXmlChoiceHasNoInputException;
import com.gip.xyna.xprc.exceptions.XPRC_JAVATYPE_UNSUPPORTED;
import com.gip.xyna.xprc.exceptions.XPRC_MEMBER_DATA_NOT_IDENTIFIED;
import com.gip.xyna.xprc.exceptions.XPRC_MISSING_ATTRIBUTE;
import com.gip.xyna.xprc.exceptions.XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.exceptions.XPRC_PrototypeDeployment;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xfractwfe.base.ChoiceLane;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.SubclassChoiceObject;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.TokenType;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.XFLLexem;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.MapTree;
import com.gip.xyna.xprc.xfractwfe.generation.ScopeStep.VariableIdentification;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;



public class StepChoice extends Step implements Distinction, FormulaContainer {

  private static final Logger logger = CentralFactoryLogging.getLogger(StepChoice.class);

  private static final String _METHODNAME_DECIDE_ORIG = "decide";
  protected static final String METHODNAME_DECIDE;
  
  static {
    //methoden namen auf diese art gespeichert können von obfuscation tools mit "refactored" werden.
    try {
      METHODNAME_DECIDE = SubclassChoiceObject.class.getDeclaredMethod(_METHODNAME_DECIDE_ORIG, Object.class, Class[].class, ChoiceLane[].class).getName();
    } catch (Exception e) {
      throw new RuntimeException("Method " + _METHODNAME_DECIDE_ORIG + " not found", e);
    }
  }

  public static final String BASECHOICE_PACKAGE = "server";
  public static final String BASECHOICE_TYPE_SUBCLASSES = "BaseChoiceTypeSubclasses";
  public static final String BASECHOICE_TYPE_FORMULA = "BaseChoiceTypeFormula";
  public static final String BASECHOICE_SUBCLASSES = BASECHOICE_PACKAGE + "." + BASECHOICE_TYPE_SUBCLASSES;
  public static final String BASECHOICE_FORMULA = BASECHOICE_PACKAGE + "." + BASECHOICE_TYPE_FORMULA;

  public static final String ANY_TYPE_PATH = "base";
  public static final String ANY_TYPE_NAME = "AnyType";
  public static final String ANY_TYPE_FQN = ANY_TYPE_PATH + "." + ANY_TYPE_NAME;
  public static final String OUTER_CONDITION_TYPE_CHOICE = "instanceof(%0%)==?";

  public static final String FORMULA_GUI_DELIMITER = "?";

  private List<AVariable> inputVars; //achtung: varnamen und ids sind nicht gesetzt, weil die nur dummy-datatypen sind
  private InputConnections input;
  private List<Step> children = new ArrayList<Step>(); // sequences unter cases
  private List<CaseInfo> cases;
  private String[] modelledexpressions;
  private String fqChoiceClassName;
  private String choiceClassName;
  private boolean isUsingBaseChoiceObject;
  private boolean isConditionalBranch;
  private HashMap<String, CaseInfo> caseInfos; // maps from stepId of child to caseInfo
  private String outerCondition;
 
  // subclass basechoice specific:
  private DomOrExceptionGenerationBase[] subclassDoms;
  private TreeSet<DomOrExceptionGenerationBase> subclassesAsTree;
  private final HashMap<Integer, Integer> mapSortedIdToOriginalId = new HashMap<Integer, Integer>();

  // subclass formula specific:
  private List<ModelledExpression> parsedFormulas;
  
  // conditional branch specific
  private final List<CaseInfo> casesWithoutDefault = new ArrayList<CaseInfo>();
  private final Map<String, ModelledExpression> mapCaseIdToParsedFormula = new HashMap<String, ModelledExpression>();
 
  //TODO wird von Dataflow hineingeschrieben
  private List<AVariable> calculatedOutput;

  public StepChoice(ScopeStep parentScope, GenerationBase creator) {
    super(parentScope, creator);
  }

  @Override
  public void visit( StepVisitor visitor ) {
    visitor.visitStepChoice( this );
  }


  public class ChoiceBranchInfo extends BranchInfo {
    @Override
    public Step getMainStep() {
      CaseInfo mainCase = getMainCase();
      for (Step child : StepChoice.this.getChildSteps()) {
        if (caseInfos.get(child.getStepId()) == mainCase) {
          return child;
        }
      }

      return null;
    }

    @Override
    public Step getExecutedStep() {
      for (CaseInfo caseInfo : getCases()) {
        for (Step child : StepChoice.this.getChildSteps()) {
          if ( (caseInfos.get(child.getStepId()) == caseInfo) && (child.hasBeenExecuted(false)) ) {
            return child;
          }
        }
      }

      return null;
    }

    @Override
    public boolean isFakeBranchForOldGUI() {
      if (getDistinctionType() != DistinctionType.TypeChoice) {
        return false;
      }

      CaseInfo mainCase = getMainCase();
      return ANY_TYPE_FQN.equals(mainCase.getComplexName());
    }
  }


  /**
   * @param e ein "Choice" Element
   */
  public void parseXML(Element e) throws XPRC_InvalidPackageNameException {

    parseId(e);
    // selbst definierte choice => complexname/complexpath TODO
    fqChoiceClassName =
        GenerationBase.transformNameForJava(e.getAttribute(GenerationBase.ATT.TYPEPATH),
                                            e.getAttribute(GenerationBase.ATT.TYPENAME));
    choiceClassName = GenerationBase.getSimpleNameFromFQName(fqChoiceClassName);
    // rekursiv xml parsen + dom objekt?
    isUsingBaseChoiceObject = e.getAttribute(GenerationBase.ATT.TYPEPATH).equals(BASECHOICE_PACKAGE);
    if (!isUsingBaseChoiceObject) {
      return;
    }

    parseUnknownMetaTags(e, Arrays.asList(EL.CONDITIONAL_BRANCHING_OUTER_CONDITION_ELEMENT));
    Element metaElement = XMLUtils.getChildElementByName(e, GenerationBase.EL.META);
    if (metaElement != null) {
      Element outerConditionElement = XMLUtils.getChildElementByName(metaElement, GenerationBase.EL.CONDITIONAL_BRANCHING_OUTER_CONDITION_ELEMENT);
      if (outerConditionElement != null) {
        isConditionalBranch = true;
        outerCondition = outerConditionElement.getTextContent();
      }
    }

    List<Element> inputs = XMLUtils.getChildElementsByName(e, GenerationBase.EL.CHOICEINPUT);
    input = new InputConnections(inputs.size());
    inputVars = new ArrayList<AVariable>();
    for (int i = 0; i < inputs.size(); i++) {
      Element inputEl = inputs.get(i);
      Element source = XMLUtils.getChildElementByName(inputEl, GenerationBase.EL.SOURCE);
      if (source != null) {
        input.parseSourceElement(source, i);
      }
      Element d = XMLUtils.getChildElementByName(inputEl, GenerationBase.EL.DATA);
      AVariable v = null;
      if (d != null) {
        v = new DatatypeVariable(creator);
        v.parseXML(d);
      } else {
        d = XMLUtils.getChildElementByName(inputEl, GenerationBase.EL.EXCEPTION);
        if (d != null) {
          v = new ExceptionVariable(creator);
          v.parseXML(d);
        }
      }
      inputVars.add(v);
    }
    
    caseInfos = new HashMap<String,CaseInfo>();

    List<Element> caseEls = XMLUtils.getChildElementsByName(e, GenerationBase.EL.CASE);
    if (isConditionalBranch) {
      cases = new ArrayList<StepChoice.CaseInfo>();
      for (int i = 0; i < caseEls.size(); i++) {
        Element caseEl = caseEls.get(i);
        String isDefault = caseEl.getAttribute(GenerationBase.ATT.ISDEFAULTCASE);
        CaseInfo ci = null;
        if (isDefault == null || isDefault.length() <= 0 || isDefault.equals("false")) {
          ci = new CaseInfo(caseEl.getAttribute(GenerationBase.ATT.ID),
                                     caseEl.getAttribute(GenerationBase.ATT.CASENAME),
                                     caseEl.getAttribute(GenerationBase.ATT.CASECOMPLEXNAME),
                                     caseEl.getAttribute(GenerationBase.ATT.CASEALIAS),
                                     this);
        } else {
          ci = new CaseInfo(caseEl.getAttribute(GenerationBase.ATT.ID),
                                     caseEl.getAttribute(GenerationBase.ATT.CASENAME),
                                     caseEl.getAttribute(GenerationBase.ATT.CASEALIAS),
                                     this);
        }
        cases.add(ci);
        StepSerial sf = new StepSerial(getParentScope(), creator);
        sf.parseXML(caseEl);
        Step child = sf.getProxyForCatch(); //immer nur ein "sequence" step
        children.add(child);
        caseInfos.put(child.getStepId(), ci);
      }
      
    } else {
      cases = new ArrayList<StepChoice.CaseInfo>();
      for (int i = 0; i < caseEls.size(); i++) {
        Element caseEl = caseEls.get(i);
        cases.add(
            new CaseInfo(caseEl.getAttribute(GenerationBase.ATT.ID), // TODO: Warum wurde das bisher immer auf null gesetzt?
                         caseEl.getAttribute(GenerationBase.ATT.CASENAME),
                         caseEl.getAttribute(GenerationBase.ATT.CASECOMPLEXNAME),
                         caseEl.getAttribute(GenerationBase.ATT.CASEALIAS),
                         this));
        StepSerial sf = new StepSerial(getParentScope(), creator);
        sf.parseXML(caseEl);
        Step child = sf.getProxyForCatch(); //immer nur ein "sequence" step
        children.add(child);
        caseInfos.put(child.getStepId(), cases.get(i));
      }
    }
    
    // ggfs subclasses rekursion
    if (isUsingBaseChoiceObject) {
      if (fqChoiceClassName.equals(BASECHOICE_SUBCLASSES)) {
        subclassDoms = new DomOrExceptionGenerationBase[cases.size()];
        for (int i = 0; i < cases.size(); i++) {
          DomOrExceptionGenerationBase dom = null;
          if (inputVars.get(0) instanceof DatatypeVariable) {
            dom = creator.getCachedDOMInstanceOrCreate(cases.get(i).getComplexName(), creator.revision);
          } else if (inputVars.get(0) instanceof ExceptionVariable) {
            dom = creator.getCachedExceptionInstanceOrCreate(cases.get(i).getComplexName(), creator.revision);
          }
          subclassDoms[i] = dom;
        }
        
        // Condition-String zusammenbauen (fuer zeta audit)
        outerCondition = OUTER_CONDITION_TYPE_CHOICE;
      } else if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
        parseFormulas();
      }
    }

    parameterPerLane = new List[caseEls.size()];
    for (int i = 0; i < caseEls.size(); i++) {
      parseParameter(caseEls.get(i));
      parameterPerLane[i] = getParameterList();
    }
    parseParameter(e);
  }

  private List<Parameter>[] parameterPerLane;

  public boolean laneHasParameter(int laneIdx, List<Integer> foreachIndices, int retryCounter) {
    if (parameterPerLane[laneIdx] == null) {
      return false;
    }
    if ((foreachIndices == null) && (retryCounter < 0)) {
      return true;
    }
    for (Parameter p : parameterPerLane[laneIdx]) {
      if (((foreachIndices == null) || (p.foreachIndicesEqual(foreachIndices)))
          && ((retryCounter < 0) || (p.getRetryCounter() == retryCounter))) {
        return true;
      }
    }
    return false;
  }

  public void parseFormulas() {
    List<ModelledExpression> expressions = new ArrayList<ModelledExpression>();
    modelledexpressions = new String[cases.size()];
    for (int i = 0; i < cases.size(); i++) {
      if (cases.get(i) != null && cases.get(i).getComplexName() != null && cases.get(i).getComplexName().length() > 0) {
        if (getDistinctionType() == DistinctionType.ConditionalChoice) {
          // einzige Condition wird als Condition des gesamten Choices angesehen (fuer zeta audit)
          outerCondition = cases.get(i).getComplexName();
        }
        
        ModelledExpression expression;
        try {
          expression = ModelledExpression.parse(this, cases.get(i).getComplexName());
        } catch (XPRC_ParsingModelledExpressionException e1) {
          modelledexpressions[i] = cases.get(i).getComplexName();
          //validate checkt das nochmal
          continue;
        }
        expressions.add(expression);
        
        if (isConditionalBranch) {
          casesWithoutDefault.add(cases.get(i));
          mapCaseIdToParsedFormula.put(cases.get(i).getId(), expression);
        }
      }
    }
    
    parsedFormulas = expressions;
  }


  private List<DomOrExceptionGenerationBase> getSortedSubClassDomsAndCasesByClassAndCalculateIdMapping() {

    if (subclassDoms == null) {
      throw new IllegalStateException("Subclass DOM objects have to be set before sorting them");
    }
    
    if( subclassesAsTree != null ) {
      return new ArrayList<DomOrExceptionGenerationBase>(subclassesAsTree);
    }
    
    subclassesAsTree =
        new TreeSet<DomOrExceptionGenerationBase>(new Comparator<DomOrExceptionGenerationBase>() {

          public int compare(DomOrExceptionGenerationBase o1, DomOrExceptionGenerationBase o2) {
            DomOrExceptionGenerationBase potentialParent = o1.getSuperClassGenerationObject();
            while (potentialParent != null) {
              if (potentialParent == o2) {
                return -1;
              }
              potentialParent = potentialParent.getSuperClassGenerationObject();
            }
            return 1;
          }
        });
    for (int i = 0; i < subclassDoms.length; i++) {
      subclassesAsTree.add(subclassDoms[i]);
    }

    // we have to keep track of the changes
    Iterator<DomOrExceptionGenerationBase> iter = subclassesAsTree.iterator();
    int j = 0;
    while (iter.hasNext()) {
      DomOrExceptionGenerationBase next = iter.next();
      innerLoop : for (int i = 0; i < subclassDoms.length; i++) {
        if (next == subclassDoms[i]) {
          mapSortedIdToOriginalId.put(j, i);
          break innerLoop;
        }
      }
      j++;
    }

    
    return new ArrayList<DomOrExceptionGenerationBase>(subclassesAsTree);
  }


  protected void getImports(HashSet<String> imports) {
    // ggfs subclasses
    if (isUsingBaseChoiceObject) {
      if (fqChoiceClassName.equals(BASECHOICE_SUBCLASSES)) {
        imports.add(SubclassChoiceObject.class.getName());
        imports.add(ChoiceLane.class.getName());
        for (int i = 0; i < subclassDoms.length; i++) {
          imports.add(subclassDoms[i].getFqClassName()); // fqclassname
        }
      } else if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
        imports.add(ChoiceLane.class.getName());
        imports.add(ChildOrderStorage.class.getName());
        imports.add(ChildOrderStorageStack.class.getCanonicalName());
      }
    }
  }


  private long calculateSerialVersionUID() {
    List<Pair<String, String>> types = new ArrayList<Pair<String, String>>();
    for (int i = 0; i < cases.size(); i++) {
      String varName = "choiceLane_";
      if (cases.get(i).getAlias() == null) {
        if (cases.get(i).isDefault()) {
          varName += "default";
        } else {
          if (isConditionalBranch) {
            varName += cases.get(i).getId();
          } else {
            varName += Integer.toString(i);
          }
        }
        types.add(Pair.of(varName, ChoiceLane.class.getName()));
      } else {
        if (isConditionalBranch) {
          varName += cases.get(i).getId();
        } else {
          varName += i;
        }
      }
      types.add(Pair.of(varName, ChoiceLane.class.getName()));
    }

    if (isUsingBaseChoiceObject) {
      if (fqChoiceClassName.equals(BASECHOICE_SUBCLASSES)) {
        types.add(Pair.of("choicelanes", "A$" + ChoiceLane.class.getName()));
      }
    }

    if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
      types.add(Pair.of(StepFunction.VARNAME_CHILDORDERSTORAGE, ChildOrderStorage.class.getName()));
    }
    return GenerationBase.calcSerialVersionUID(types);
  }

  private void generateChoiceLane(CodeBuffer cb, int childId) {
    cb.add("private " + ChoiceLane.class.getSimpleName() + " choiceLane_");
    if (cases.get(childId).isDefault()) {
      cb.add("default");
    } else {
      if (isConditionalBranch) {
        cb.add(cases.get(childId).getId());
      } else {
        cb.add(Integer.toString(childId));
      }
    }
    cb.add(" = new " + ChoiceLane.class.getSimpleName() + "() {").addLB();
    cb.addLine("private static final long serialVersionUID = -1L");
    cb.addLine("public void ", METHODNAME_EXECUTE, "() throws " , XynaException.class.getName(), " {");
    cb.addLine(METHODNAME_EXECUTE_CHILDREN, "(" + childId + ")");
    cb.addLine("}").addLB();
    cb.addLine("};");
  }

  protected void generateJavaInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
      throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException,
      XPRC_ParsingModelledExpressionException {

    // TODO code in StepParallel etc ähnlich => extraktion von teilcode

    cb.addLine("private static class " + getClassName() + " extends " + FractalProcessStep.class.getSimpleName() + "<"
                   + getParentScope().getClassName() + "> {").addLB();

    cb.addLine("private static final long serialVersionUID = ", String.valueOf(calculateSerialVersionUID()), "L");

    //ChoiceLanes generieren
    for (int i = 0; i < cases.size(); i++) {
      if (cases.get(i).getAlias() == null) {
        generateChoiceLane(cb, i);
      }
    }

    //nun noch die ChoiceLanes für Cases mit Alias generieren (diese erst nach den anderen Cases
    //generieren, damit sich für im alten Format serialisierte Workflows (Alias als Referenz)
    //die Reihenfolge der ChoiceLane-Klassen nicht ändert)
    for (int i = 0; i < cases.size(); i++) {
      if (cases.get(i).getAlias() != null) {
        generateChoiceLane(cb, i);
      }
    }

    cb.addLB();
    if (isUsingBaseChoiceObject) {
      if (fqChoiceClassName.equals(BASECHOICE_SUBCLASSES)) {
        getSortedSubClassDomsAndCasesByClassAndCalculateIdMapping(); //TODO muss das hier schon geschehen?
        cb.add("private " + ChoiceLane.class.getSimpleName() + "[] choicelanes = new "
            + ChoiceLane.class.getSimpleName() + "[]{");
        for (int i = 0; i < cases.size(); i++) {
          cb.addListElement("choiceLane_" + mapSortedIdToOriginalId.get(i));
        }
        cb.add("};").addLB();
      } else if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
        cb.addLine("protected void ", METHODNAME_REINITIALIZE, "() {");
        cb.addLine("super.", METHODNAME_REINITIALIZE, "()");
        if (isConditionalBranch) {
          for (Entry<String, ModelledExpression> entry : mapCaseIdToParsedFormula.entrySet()) {
            cb.addLine("cachedEvaluation_", entry.getKey(), " = null");
          }
        } else {
          cb.addLine("cachedEvaluation = null");
        }
        cb.addLine("}").addLB();
        
        if (isConditionalBranch) {
          for (Entry<String, ModelledExpression> entry : mapCaseIdToParsedFormula.entrySet()) {
            cb.addLine("private Boolean cachedEvaluation_", entry.getKey(), " = null").addLB();
            cb.addLine("private boolean evalFormula_", entry.getKey(), "() throws ", XynaException.class.getSimpleName(), " {");
            cb.addLine("if (cachedEvaluation_", entry.getKey(), " == null) {");              
            cb.addLine(ChildOrderStorageStack.class.getSimpleName(), " ", StepFunction.VARNAME_CHILDORDERSTORAGE_STACK, " = ",
                       ChildOrderStorage.class.getSimpleName(), ".", FIELDNAME_CHILD_ORDER_STORAGE_STACK, ".get()");
            cb.addLine(StepFunction.VARNAME_CHILDORDERSTORAGE_STACK, ".", METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD, "(", StepFunction.VARNAME_CHILDORDERSTORAGE, ")");
            cb.addLine("try {");
            cb.add("cachedEvaluation_", entry.getKey(), " = ");
            entry.getValue().initTypesOfParsedFormula(importedClassesFqStrings, new MapTree());
            entry.getValue().writeNonAssignmentExpressionToBuffer(cb);
            cb.addLB();
            cb.addLine("} catch (", Throwable.class.getName() ," e) {");
            cb.addLine("if (e instanceof ", RuntimeException.class.getName(), ") { throw (RuntimeException)e; }");
            cb.addLine("throw new ", XDEV_PARAMETER_NAME_NOT_FOUND.class.getName() ,"(\" - \" ,e)");
            cb.addLine("} finally {");
            cb.addLine(StepFunction.VARNAME_CHILDORDERSTORAGE_STACK, ".", METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE, "()");
            cb.addLine("}");
            cb.addLine("}"); //end if
            cb.addLine("return cachedEvaluation_", entry.getKey());
            cb.addLine("}").addLB();
          }
        } else {
          cb.addLine("private Boolean cachedEvaluation = null").addLB();
          cb.addLine("private boolean evalFormula() throws ", XynaException.class.getSimpleName(), " {");
          cb.addLine("if (cachedEvaluation == null) {");
          cb.addLine(ChildOrderStorageStack.class.getSimpleName(), " ", StepFunction.VARNAME_CHILDORDERSTORAGE_STACK, " = ",
                     ChildOrderStorage.class.getSimpleName(), ".", FIELDNAME_CHILD_ORDER_STORAGE_STACK, ".get()");
          cb.addLine(StepFunction.VARNAME_CHILDORDERSTORAGE_STACK, ".", METHODNAME_CHILD_ORDER_STORAGE_STACK_ADD, "(", StepFunction.VARNAME_CHILDORDERSTORAGE, ")");
          cb.addLine("try {");
          cb.add("cachedEvaluation = ");
          parsedFormulas.get(0).initTypesOfParsedFormula(importedClassesFqStrings, new MapTree());
          parsedFormulas.get(0).writeNonAssignmentExpressionToBuffer(cb);
          cb.addLB();
          cb.addLine("} catch (", Throwable.class.getName() ," e) {");
          cb.addLine("if (e instanceof ", RuntimeException.class.getName(), ") { throw (RuntimeException)e; }");
          cb.addLine("throw new ", XDEV_PARAMETER_NAME_NOT_FOUND.class.getName() ,"(\" - \" ,e)");
          cb.addLine("} finally {");
          cb.addLine(StepFunction.VARNAME_CHILDORDERSTORAGE_STACK, ".",  METHODNAME_CHILD_ORDER_STORAGE_STACK_REMOVE, "()");
          cb.addLine("}");
          cb.addLine("}"); //end if
          cb.addLine("return cachedEvaluation");
          cb.addLine("}").addLB();
        }
      }
    }
    
    if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
      cb.addLine("private ", ChildOrderStorage.class.getSimpleName(), " ", StepFunction.VARNAME_CHILDORDERSTORAGE);
      cb.addLB();
      cb.addLine("public List<", XynaOrderServerExtension.class.getSimpleName(), "> getChildOrders() {"); // TODO JavaCall.getChildOrders ?
      cb.addLine("return ", StepFunction.VARNAME_CHILDORDERSTORAGE,".", METHODNAME_GET_XYNA_CHILD_ORDERS, "()");
      cb.addLine("}").addLB();
    }
    cb.addLine("public void ",METHODNAME_INIT, "(", getParentScope().getClassName(), " p) {");
    cb.addLine("super.", METHODNAME_INIT, "(p)");
    if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
      cb.addLine(StepFunction.VARNAME_CHILDORDERSTORAGE, " = new ", ChildOrderStorage.class.getSimpleName(), "(this)");
    }
    cb.addLine("}").addLB();

    cb.addLB().addLine("public " + getClassName() + "() {");
    cb.addLine("super(" + getIdx() + ")");
    cb.addLine("}").addLB();
    
    appendExecuteInternally(cb, importedClassesFqStrings);
    
    appendParameterValueGetters(cb, importedClassesFqStrings);

    // generate a mapping to the refId
    generatedGetRefIdMethod(cb);

    //compensation
    cb.addLine("public void ", METHODNAME_COMPENSATE_INTERNALLY, "() throws ", XynaException.class.getName(), " {").addLine("}").addLB(); //TODO compensation

    //getChildren
    cb.addLine("protected " + FractalProcessStep.class.getSimpleName() + "<" + getParentScope().getClassName()
        + ">[] ", METHODNAME_GET_CHILDREN, "(int i) {");
    for (int i = 0; i < children.size(); i++) {
      cb.addLine("if (i == " + i + ") {");
      
      String alias = "";
      if (cases.get(i).getAlias() != null) {
        //für Cases, die einen anderen Step referenzieren, den Alias-Step verwenden
        alias = "alias";
      }
      
      cb.addLine("return new " + FractalProcessStep.class.getSimpleName() + "[]{", METHODNAME_GET_PARENT_SCOPE, "()."
          + children.get(i).getVarName() + alias + "};");
      cb.addLine("}");
    }
    cb.addLine("return null").addLine("}").addLB();

    //getChildrenLength
    cb.addLine("protected int ", METHODNAME_GET_CHILDREN_TYPES_LENGTH, "() {");
    cb.addLine("return " + children.size());
    cb.addLine("}").addLB();
    cb.addLine("}").addLB();
  }

  protected void appendExecuteInternally(CodeBuffer cb, HashSet<String> importedClassesFqStrings) throws XPRC_InvalidVariableIdException {
    cb.addLine("public void ", METHODNAME_EXECUTE_INTERNALLY, "() throws ", XynaException.class.getName(), " {");
    if (isUsingBaseChoiceObject) {
      if (fqChoiceClassName.equals(BASECHOICE_SUBCLASSES)) {
        VariableIdentification vi = getParentScope().identifyVariable(input.getVarIds()[0]);
        AVariable v = vi.variable;

        cb.add(SubclassChoiceObject.class.getSimpleName() + ".",METHODNAME_DECIDE, "(" + vi.getScopeGetter(getParentScope()));
        cb.add(v.getGetter(input.getPaths()[0]) + ", new Class[]{");
        for (DomOrExceptionGenerationBase obj : getSortedSubClassDomsAndCasesByClassAndCalculateIdMapping() ) {
          cb.addListElement(obj.getFqClassName() + ".class");
        }
        cb.add("}, choicelanes)").addLB();
      } else if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
        if (isConditionalBranch) {
          if (casesWithoutDefault.size() > 0) {
            cb.add("if (");
          }
          for (int i = 0; i < casesWithoutDefault.size(); i++) {
            String caseId = casesWithoutDefault.get(i).getId();
            cb.add("evalFormula_" + caseId + "()) {").addLB();
            cb.addLine("choiceLane_" + caseId, ".execute()");
            cb.add("} else ");
            if (i + 1 < casesWithoutDefault.size()) {
              cb.add("if (");
            } else {
              cb.add("{").addLB();
            }
          }
          cb.addLine("choiceLane_default.execute()");
          if (casesWithoutDefault.size() > 0) {
            cb.addLine("}");
          }
        } else {
          //im complexname jeder lane (cases[i]) steht eine formel. wenn diese true auswertet, soll die lane ausgeführt werden
          //ansonsten nicht
          cb.addLine("if (evalFormula()) {");
          for (int i = 0; i < cases.size(); i++) {
            CaseInfo ci = cases.get(i);
            if (ci.getName().equals("true")) {
              cb.addLine("choiceLane_" + i, ".execute()");
              break;
            }
          }
          cb.addLine("} else {");
          // es gibt zweite choicelane für false-fall
          for (int i = 0; i < cases.size(); i++) {
            CaseInfo ci = cases.get(i);
            if (ci.getName().equals("false")) {
              cb.addLine("choiceLane_" + i, ".execute()");
              break;
            }
          }
          cb.addLine("}");
        }
      }

    } else {
      //TODO benutzerdefinierte choice objects...
    }
    cb.addLine("}").addLB();
  }
  
  private void appendParameterValueGetters(CodeBuffer cb, HashSet<String> importedClassesFqStrings)
      throws XPRC_InvalidVariableIdException {
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_INCOMING_VALUES, input.getVarIds(), input.getPaths(), cb, importedClassesFqStrings);
    generateJavaForIncomingOutgoingValues(METHODNAME_GET_CURRENT_OUTGOING_VALUES, new String[0], null, cb, importedClassesFqStrings);
  }


  @Override
  protected List<GenerationBase> getDependencies() {
    List<GenerationBase> dependentDoms = new ArrayList<GenerationBase>();
    if (isBaseSubclassChoice()) {
      if (subclassDoms != null) {
        for (int i = 0; i < subclassDoms.length; i++) {
          if (subclassDoms[i] != null) {
            dependentDoms.add(subclassDoms[i]);
          }
        }
      }
    }
    return dependentDoms;
  }

  
  public boolean isBaseSubclassChoice() {
    return isUsingBaseChoiceObject && fqChoiceClassName.equals(BASECHOICE_SUBCLASSES);
  }
  
  public List<String> getComplexCaseNames() {
    List<String> complexNames = new ArrayList<String>();
    for (CaseInfo caze : cases) {
      complexNames.add(caze.getComplexName());
    }
    return complexNames;
  }

  @Override
  protected List<ExceptionVariable> getExceptionVariables() {
    return null;
  }


  @Override
  protected List<Service> getServices() {
    return null;
  }


  @Override
  protected List<ServiceVariable> getServiceVariables() {
    return null;
  }
  
  
  @Override
  protected void removeVariable(AVariable var) {
    throw new RuntimeException("unsupported to remove variable " + var + " from step " + this);
  }


  @Override
  public List<Step> getChildSteps() {
    ArrayList<Step> allSteps = new ArrayList<Step>(children);
    return Collections.unmodifiableList(allSteps);
  }


  @Override
  public boolean replaceChild(Step oldChild, Step newChild) {
    for (int childNo = 0; childNo < children.size(); childNo++) {
      if (children.get(childNo) == oldChild) {
        children.set(childNo, newChild);
        return true;
      }
    }

    return false;
  }


  @Override
  public boolean isExecutionDetached() {
    return false;
  }


  @Override
  public Step getContainerStepForGui() {
    return this;
  }


  @Override
  protected boolean compareImplementation(Step oldStep) {
    if (oldStep == null || !(oldStep instanceof StepChoice)) {
      return true;
    }
    StepChoice oldChoiceStep = (StepChoice) oldStep;

    if (!fqChoiceClassName.equals(oldChoiceStep.fqChoiceClassName)) {
      return true;
    }

    if (!Arrays.equals(input.getVarIds(), oldChoiceStep.input.getVarIds()) || !Arrays.equals(input.getPaths(), oldChoiceStep.input.getPaths())
//        || !Arrays.equals(cases, oldChoiceStep.cases)) {
      || !cases.equals(oldChoiceStep.cases)) {
      return true;
    }

    if (children != null && oldChoiceStep.children != null) {
      if (children.size() != oldChoiceStep.children.size()) {
        return true;
      }
      for (int i = 0; i < children.size(); i++) {
        if (children.get(i).compareImplementation(oldChoiceStep.children.get(i))) {
          return true;
        }
      }
    } else if (children == null ^ oldChoiceStep.children == null) {
      return true;
    }

    return false;
  }


  @Override
  public String[] getInputVarIds() {
    return input.getVarIds();
  }


  @Override
  public String[] getInputVarPaths() {
    return input.getPaths();
  }

  public List<AVariable> getInputVars() {
    if(inputVars == null)
      return new ArrayList<AVariable>();
    return inputVars;
  }

  public void addInputVar(int index, AVariable inputVar) {
    if (getDistinctionType() == DistinctionType.TypeChoice) {
      throw new UnsupportedOperationException("This operation is not supported for type choices.");
    }

    inputVars.add(index, inputVar);
  }
  
  public void removeInput(AVariable inputVar) {
    
    int index = inputVars.indexOf(inputVar);
    if(index == -1)
      throw new RuntimeException("Variable to remove not found!");
    
    
    //update InputConnection
    //remove and shorten input (remove entry at index)
    InputConnections newInput = new InputConnections(inputVars.size()-1);
    for(int i=0; i<index; i++)
      newInput.getVarIds()[i] = input.getVarIds()[i];
    for(int i= index + 1; i< input.getVarIds().length; i++)
      newInput.getVarIds()[i-1] = input.getVarIds()[i];
      
    input = newInput;
    inputVars.remove(inputVar);
    
  }

  public String[] getOutputVarIds() {
    
    if(calculatedOutput == null)
      return new String[0];
    
    String[] result = new String[calculatedOutput.size()];
    
    for(int i=0; i<calculatedOutput.size(); i++)
      result[i] = calculatedOutput.get(i).getId();
    
    return result;
//    return super.getOutputVarIds(); // TODO: determine output var ids of executed branch (needed for dataflow in monitor)
  }

  public List<AVariable> getOutputVars() {
    if(calculatedOutput == null)
      return new ArrayList<AVariable>();
      
    return calculatedOutput;
    //return new ArrayList<AVariable>(); // TODO: determine output vars of executed branch (needed for dataflow in monitor)
  }

  public void setTypeChoiceVar(AVariable inputVar) {
    if (getDistinctionType() != DistinctionType.TypeChoice) {
      throw new UnsupportedOperationException("This operation is only supported for type choices.");
    }

    // only one input variable for type choice
    input = new InputConnections(1);
    inputVars.clear();
    inputVars.add(inputVar);
    createTypeChoiceBranches(true); // TODO: if new type has common sub-types, keep cases
  }

  private void createTypeChoiceBranches(boolean clearExisting) {
    if (getDistinctionType() != DistinctionType.TypeChoice) {
      return;
    }

    AVariable choiceVar = inputVars.get(0);
    Set<GenerationBase> subTypes = determineSubTypes(choiceVar);
    GenerationBase doe;
    
    if (clearExisting) {
      children.clear();
      cases.clear();
      caseInfos.clear();
      subclassDoms = new DomOrExceptionGenerationBase[0]; //array expends when calling addCaseToTypes()
    }
    
    int caseIndex = cases.size();

    // add branch for the variable of the type choice
    List<CaseInfo> newCases = new ArrayList<CaseInfo>();
    String fqn = choiceVar.getOriginalPath() + "." + choiceVar.getOriginalName();
    if (!doesCaseExist(fqn)) {
      CaseInfo newCase = new CaseInfo(creator.getNextXmlId().toString(), choiceVar.getLabel(), fqn, "", this, false);
      cases.add(newCase);
      newCases.add(newCase);
      doe = choiceVar.getDomOrExceptionObject();
      addCaseToTypes(doe);
    }

    // add branches for all sub-types in the hierarchy tree of the variable
    for (GenerationBase subType : subTypes) {
      if (!doesCaseExist(subType.getOriginalFqName())) {
        CaseInfo newCase = new CaseInfo(creator.getNextXmlId().toString(), subType.getLabel(), subType.getOriginalFqName(), "", this, false);
        cases.add(newCase);
        newCases.add(newCase);
        
        if(!(subType instanceof DomOrExceptionGenerationBase))
          throw new RuntimeException("Could not create type: " + subType.getOriginalFqName());
        
        addCaseToTypes(subType);
      }
    }

    // refresh data structures
    if (clearExisting) {
      caseInfos = new HashMap<String, CaseInfo>();
    }
    for (int caseNr = 0; caseNr < newCases.size(); caseNr++) {
      addStepSerialForBranch(caseNr + caseIndex, newCases.get(caseNr));
    }
  }
  
  private  Set<GenerationBase> determineSubTypes(AVariable var) {
    DomOrExceptionGenerationBase domOrException = var.getDomOrExceptionObject();
    Set<GenerationBase> subTypes = null;
        if(domOrException != null)
          subTypes = domOrException.getSubTypes(domOrException.cacheReference);
        else
          subTypes = new HashSet<GenerationBase>(); //AnyType -- TODO: every Type?

    // filter out types that are not in current RTC or dependent RTCs

    Set<Long> dependencies = new HashSet<Long>();
    RuntimeContextDependencyManagement rcdMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    rcdMgmt.getDependenciesRecursivly(var.revision, dependencies);
    Set<Long> usableRevisions = new HashSet<Long>(dependencies);
    usableRevisions.add(var.revision);

    Set<GenerationBase> usableSubTypes = new HashSet<GenerationBase>();
    for (GenerationBase subType: subTypes) {
      for (long revision : usableRevisions) {
        if (revision == subType.revision) {
          usableSubTypes.add(subType);
        }
      }
    }

    return usableSubTypes;
  }

  private boolean doesCaseExist(String complexName) {
    for (CaseInfo curCase : cases) {
      if (complexName.equals(curCase.getComplexName())) {
        return true;
      }
    }

    return false;
  }

  public InputConnections getInputConnections() {
    return input;
  }
  
  @Override
  protected Set<String> getAllUsedVariableIds() {
    List<AVariable> userOutputs = getUserdefinedOutput();
    List<String> allUsedVariableIds = new ArrayList<String>();
    
    //add user Outputs. otherwise they get removed.
    for(AVariable v : userOutputs) {
      allUsedVariableIds.add(v.getId());
    }
    
    //add input variables
    for(String id : input.getVarIds()) {
      allUsedVariableIds.add(id);
    }
    
    //add targets
    //required for hidden FilterConditions in Conditional Branchings that would otherwise be removed
    for(String id : getTargetIds()) {
      allUsedVariableIds.add(id);
    }
    
    return createVariableIdSet(allUsedVariableIds);
  }
  
  
  public List<ModelledExpression> getParsedFormulas() {
    return parsedFormulas;
  }

  public DomOrExceptionGenerationBase[] getSubclassDoms() {
    return subclassDoms;
  }
  
  public DomOrExceptionGenerationBase getBaseDomForSubClassChoice() {
    if (inputVars == null || inputVars.size() <= 0) {
      return null;
    } else {
      return inputVars.get(0).domOrException;
    }
  }


  @Override
  public void validate() throws XPRC_InvalidXmlChoiceHasNoInputException, XPRC_EmptyVariableIdException, XPRC_PrototypeDeployment,
      XPRC_ParsingModelledExpressionException, XPRC_InvalidXMLMissingListValueException, XPRC_MISSING_ATTRIBUTE, XPRC_JAVATYPE_UNSUPPORTED,
      XPRC_OBJECT_EXISTS_BUT_TYPE_DOES_NOT_MATCH, XPRC_InvalidExceptionVariableXmlMissingTypeNameException, XPRC_MEMBER_DATA_NOT_IDENTIFIED  {
    if (!isUsingBaseChoiceObject) {
      throw new RuntimeException(new XDEV_UNSUPPORTED_FEATURE("userdefined choices in workflowmodelling"));
    }
    for (int i = 0; i < input.length(); i++) {
      if (input.getVarIds()[i] == null || input.getVarIds()[i].trim().length() == 0) {
        throw new XPRC_EmptyVariableIdException(GenerationBase.EL.CHOICEINPUT + "." + GenerationBase.EL.SOURCE + " " + choiceClassName);
      }
      if (inputVars.get(i) == null) {
        throw new XPRC_InvalidXmlChoiceHasNoInputException(fqChoiceClassName);
      }
      inputVars.get(i).validate();
    }
    if (isUsingBaseChoiceObject) {
      if (fqChoiceClassName.equals(BASECHOICE_SUBCLASSES)) {
        if (inputVars.size() == 0) {
          throw new XPRC_InvalidXmlChoiceHasNoInputException(fqChoiceClassName);
        }
      }
    }
    if (modelledexpressions != null) {
      for (int i = 0; i < cases.size(); i++) {
        if (modelledexpressions[i] != null) {
          try {
            ModelledExpression.parse(this, modelledexpressions[i]);
          } catch (XPRC_ParsingModelledExpressionException e) {
            throw e;
          }
          throw new RuntimeException("Could not parse expression before");
        }
      }
    }

    if (isUsingBaseChoiceObject) {
      if (fqChoiceClassName.equals(BASECHOICE_SUBCLASSES)) {
        for (int i = 0; i < cases.size(); i++) {
          if (subclassDoms[i] == null) {
            throw new RuntimeException("Subclass Type not found: " + cases.get(i).getComplexName());
          }
        }
      }
    }
  }
  
  /**
   * Für jeden Step, der einen anderen Step referenziert wird eine Alias-Step Variable generiert.
   * @param cb
   */
  protected void generateAliasSteps(CodeBuffer cb) {
    for (int i = 0; i < cases.size(); i++) {
      if (cases.get(i).getAlias() != null) {
        //Step-Id des referenzierten Steps ermitteln
        int aliasStepId = -1;
        for (int j = 0; j < cases.size(); j++) {
          String alias = isConditionalBranch ? cases.get(j).getId() : cases.get(j).getComplexName();
          if (cases.get(i).getAlias().equals(alias)) {
            aliasStepId = j;
            break;
          }
        }
        
        //Variable für Alias-Step (vom Typ des referenzierten Steps) generieren.
        //Die eigene xmlId dem Konstruktor der referenzierten Step-Klasse übergeben.
        if (aliasStepId > -1) {
          cb.addLine("private ", children.get(aliasStepId).getClassName(), " ", children.get(i).getVarName(), "alias = new ", children.get(aliasStepId).getClassName(), "(", String.valueOf(children.get(i).getXmlId()), ");");
          
          //die referenzierte Step-Klasse muss dann den entsprechenden Konstruktor bekommen
          if (children.get(aliasStepId) instanceof StepSerial) {
            ((StepSerial) children.get(aliasStepId)).setParameteriseXmlId(true);
          }
        }
      }
    }
  }
  
  /**
   * Fügt alle Alias-Step-Variablennamen hinzu.
   */
  protected void addAliasStepVarNames(CodeBuffer cb) {
    for (int i = 0; i < cases.size(); i++) {
      if (cases.get(i).getAlias() != null) {
        cb.addListElement(children.get(i).getVarName() + "alias");
      }
    }
  }
  
  @Override
  public DistinctionType getDistinctionType() {
    if (isConditionalBranch) {
      return DistinctionType.ConditionalBranch;
    } else if (fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
      return DistinctionType.ConditionalChoice;
    } else {
      return DistinctionType.TypeChoice;
    }
  }
  
  public CaseInfo getCaseInfo(Step child) {
    return caseInfos.get(child.getStepId());
  }

  public CaseInfo getCaseInfo(int caseNo) { // TODO: geht nicht einfach return cases.get(caseNo)?
    List<Step> cases = getChildSteps();
    return caseInfos.get(cases.get(caseNo).getStepId());
  }

  @Override
  public List<CaseInfo> getHandledCases() {
    return cases;
  }

  @Override
  public List<CaseInfo> getUnhandledCases(boolean considerRetryAsHandled) {
    return new ArrayList<CaseInfo>(); // TODO: maybe later when unhandled cases should be shown in GUI
  }

  public boolean addCase(int destinationBranchNo, int insertIndex, String expression, String name) {
    List<BranchInfo> branches = getBranches();
    if (destinationBranchNo >= branches.size()) {
      // invalid branch index
      return false;
    }

    BranchInfo destinationBranch = branches.get(destinationBranchNo);
    String alias = destinationBranch.getBranchName(); // new case is going to be an alias for the main case of the branch
    CaseInfo caseInfo = new CaseInfo(creator.getNextXmlId().toString(), name, expression, alias, this, false);
    
    // determine insert position in global cases-array (which contains the cases of all branches)
    int insertPosition;
    if ( (insertIndex < 0) || (insertIndex >= destinationBranch.getCases().size()) ) {
      // insert case at the end
      insertPosition = getNextCaseIndexAfterBranch(destinationBranchNo); // TODO: besser davor?
    } else {
      insertPosition = cases.indexOf((destinationBranch.getCases().get(insertIndex)));
    }

    cases.add(insertPosition, caseInfo);
    addStepSerialForBranch(insertPosition, caseInfo, false, false);
    addTypeForCase(insertPosition, expression);
    parseFormulas();

    return true;
  }
  
  private void removeCaseFromTypes(int index) {
    
    //updating subclassDoms only applies to TypeChoice
    if(getDistinctionType() != DistinctionType.TypeChoice)
      return;
    
    DomOrExceptionGenerationBase[] newSubclassDoms = new DomOrExceptionGenerationBase[subclassDoms.length-1];
    System.arraycopy(subclassDoms, 0, newSubclassDoms, 0, index);
    
    if(subclassDoms.length > index+1)
      System.arraycopy(subclassDoms, index+1, newSubclassDoms, index, subclassDoms.length - index - 1);

    subclassDoms = newSubclassDoms;
  }
  
  private void addCaseToTypes(GenerationBase gb) {
    DomOrExceptionGenerationBase[] newSubclassDoms = new DomOrExceptionGenerationBase[subclassDoms.length+1];
    System.arraycopy(subclassDoms, 0, newSubclassDoms, 0, subclassDoms.length);
    newSubclassDoms[subclassDoms.length] = (DomOrExceptionGenerationBase)gb;
    
    subclassDoms = newSubclassDoms;
  }

  private void addTypeForCase(int index, String type) {
    if (getDistinctionType() != DistinctionType.TypeChoice) {
      return;
    }

    DomOrExceptionGenerationBase dom = null;
    try {
      dom = creator.getCachedDOMInstanceOrCreate(type, creator.revision);
    } catch (Exception e1) {
      try {
        dom = creator.getCachedExceptionInstanceOrCreate(type, creator.revision);
      } catch (Exception e2) {
        throw new RuntimeException("Type for case could not be determined.", e2);
      }
    }

    DomOrExceptionGenerationBase[] newSubclassDoms = new DomOrExceptionGenerationBase[subclassDoms.length+1];
    System.arraycopy(subclassDoms, 0, newSubclassDoms, 0, index);
    newSubclassDoms[index] = dom;

    if (subclassDoms.length > index) {
      System.arraycopy(subclassDoms, index, newSubclassDoms, index+1, subclassDoms.length-index);
    }

    subclassDoms = newSubclassDoms;
  }

  public CaseInfo removeCase(int caseNo) {
    CaseInfo caseToRemove = cases.get(caseNo);
    return removeCase(caseToRemove);
  }

  public CaseInfo removeCase(CaseInfo caseToRemove) {
    int oldCaseIndex = cases.indexOf(caseToRemove);
    removeCaseFromTypes(oldCaseIndex);

    // find step of case to remove
    String oldStepId = null;
    for (String childId : caseInfos.keySet()) {
      CaseInfo caseInfo = caseInfos.get(childId);
      if (caseToRemove == caseInfo) {
        oldStepId = childId; // TODO: break
      }
    }

    Step oldStep = getChild(oldStepId);
    String branchName = caseInfos.get(oldStepId).getBranchName(); // TODO: vorne caseToRemove

    // remove step and references to it
    CaseInfo caseInfo = caseInfos.get(oldStepId); // ueberfluessig - stattdessen caseToRemove verwenden
    children.remove(oldStep);
    caseInfos.remove(oldStepId);

    if (caseInfo.isMainCaseOfItsBranch()) {
      // make another case the main case of the branch and the remaining cases aliases to this one
      List<Step> aliasChildren = getAliasChildren(branchName);
      CaseInfo newMainCase = null;
      for (int childNo = 0; childNo < aliasChildren.size(); childNo++) {
        Step curChild = aliasChildren.get(childNo);
        CaseInfo curChildCase = caseInfos.get(curChild.getStepId());
  
        if (childNo == 0) {
          // make case the new main case of the branch
          newMainCase = curChildCase;
          newMainCase.setAlias(null);
  
          // replace child for the case with the one containing the steps of the branch
          children.add(children.indexOf(curChild), oldStep);
          children.remove(curChild);
          caseInfos.remove(curChild.getStepId());
          caseInfos.put(oldStep.getStepId(), newMainCase);

          // move new main case to position of old main case (necessary for branches to stay in the same order)
          Collections.swap(cases, oldCaseIndex, cases.indexOf(newMainCase));
        } else {
          curChildCase.setAlias(newMainCase.getBranchName());
        }
      }
    }

    cases.remove(caseToRemove);
    parseFormulas();

    return caseToRemove;
  }

  public boolean decoupleCase(int caseNo) {
    if (getDistinctionType() == DistinctionType.ConditionalChoice) {
      return false;
    }

    CaseInfo caseToDecouple = cases.get(caseNo);
    int oldBranchNo = getBranchNo(caseNo);
    int newBranchNo = oldBranchNo + 1; // spawn new branch for decoupled type next to old one
    removeCase(caseToDecouple);

    if (getDistinctionType() == DistinctionType.TypeChoice) { 
      // if branch has only one case left, another assign is necessary for input to be used
      BranchInfo oldBranch = getBranches().get(oldBranchNo);
      if (oldBranch.getCases().size() <= 1) {
        StepSerial oldBranchSteps = (StepSerial)oldBranch.getMainStep();
        StepAssign sa = new StepAssign(getParentScope(), creator);
        sa.createEmpty();
        oldBranchSteps.addChild(0, sa);
      }
    }

    addBranch(newBranchNo, caseToDecouple.getComplexName(), caseToDecouple.getName());
    int newCaseNo = cases.indexOf(getBranches().get(newBranchNo).getMainCase());
    addTypeForCase(newCaseNo, caseToDecouple.getComplexName());

    return true;
  }

  private List<Step> getAliasChildren(String branchName) {
    List<Step> aliasChildren = new ArrayList<Step>();
    for (Step child : children) {
      if (caseInfos.get(child.getStepId()).getBranchName().equals(branchName)) {
        aliasChildren.add(child);
      }
    }

    return aliasChildren;
  }

  private Step getChild(String stepId) {
    for (Step child : children) {
      if (child.getStepId().equals(stepId)) {
        return child;
      }
    }

    return null;
  }

  public int getCaseNo(Step step) {
    List<Step> childSteps = getChildSteps();
    for (int childNo = 0; childNo < childSteps.size(); childNo++) {
      if (childSteps.get(childNo) == step) {
        return childNo;
      }
    }

    return -1;
  }

  public void addBranch(int branchNo, String expression, String name) {
    List<BranchInfo> branches = getBranches();

    int insertPosition;
    if ( (branchNo < 0) || (branchNo >= branches.size()) ) {
      insertPosition = cases.size();
    } else if (branchNo == 0) {
      insertPosition = 0;
    } else {
      insertPosition = getNextCaseIndexAfterBranch(branchNo-1);
    }

    // make sure default case (if existing) stays on last position
    if ( (cases.size() > 0) && (cases.get(cases.size()-1).isDefault()) && (insertPosition == cases.size()) ) {
      insertPosition--;
    }

    CaseInfo caseInfo = new CaseInfo(creator.getNextXmlId().toString(), name, expression, "", this, false);
    cases.add(insertPosition, caseInfo);
    addStepSerialForBranch(insertPosition, caseInfo);

    parseFormulas();
  }

  @Override
  public void addMissingBranches() {
    createTypeChoiceBranches(false);
  }

  public BranchInfo removeBranch(int index) {
    if (getDistinctionType() == DistinctionType.ConditionalChoice) {
      // removing branches is not supported for conditional choice
      return null;
    }

    List<BranchInfo> branches = getBranches();
    if (branches.size() <= index) {
      // invalid index
      return null;
    }

    BranchInfo branchToRemove = branches.get(index);
    if (branchToRemove.getMainCase().isDefault()) {
      // the default case must not be deleted
      return null;
    }

    for (CaseInfo caseToRemove : branchToRemove.getCases()) {
      removeCase(caseToRemove);
    }

    return branchToRemove;
  }

  public int getBranchNo(Step step) {
    CaseInfo stepCaseInfo = caseInfos.get(step.getXmlId().toString());
    if (stepCaseInfo == null) {
      return -1;
    }

    int curBranchNo = 0;
    for (BranchInfo curBranch : getBranches()) {
      if (curBranch.getCases().contains(stepCaseInfo)) {
        return curBranchNo;
      }

      curBranchNo++;
    }

    return -1;
  }

  public int getBranchNo(int caseNo) {
    CaseInfo caseInfo = cases.get(caseNo);
    List<BranchInfo> branches = getBranches();
    for (int branchNo = 0; branchNo < branches.size(); branchNo++) {
      if (branches.get(branchNo).getCases().contains(caseInfo)) {
        return branchNo;
      }
    }

    return -1;
  }

  private int getNextCaseIndexAfterBranch(int branchNo) {
    BranchInfo branch = getBranches().get(branchNo);
    List<CaseInfo> casesInBranch = branch.getCases();
    CaseInfo lastCaseInBranch = branch.getCases().get(casesInBranch.size() - 1);

    return (cases.indexOf(lastCaseInBranch) + 1);
  }

  private List<BranchInfo> getBranches() {
    // create map with an entry for every branch (linked map to retain original order)
    Map<String, BranchInfo> branchesByName = new LinkedHashMap<String, BranchInfo>();
    for (CaseInfo curCase : cases) {
      if (curCase.isMainCaseOfItsBranch()) {
        branchesByName.put(curCase.getBranchName(), new ChoiceBranchInfo());
      }
    }

    // add cases to their branches
    for (CaseInfo curCase : cases) {
      branchesByName.get(curCase.getBranchName()).addCase(curCase);
    }

    return new ArrayList<>(branchesByName.values());
  }

  /**
   * Converts the inner data structures with cases that can have aliases into branches that have n cases (n &gt; 0).
   *
   * A branch represents a section in the Conditional Branching that can contain steps.
   * The list of cases per branch represents the conditions under which the steps are executed (linked with OR).
   *
   * @return one list entry per branch with each having one list entry per case
   */
  @Override
  public List<BranchInfo> getBranchesForGUI() {
    List<BranchInfo> allBranches = getBranches();

    // for type choice filter out initial branch with AnyType that is only present for downwards compatibility
    List<BranchInfo> filteredBranches = new ArrayList<BranchInfo>();
    for (BranchInfo branch : allBranches) {
      if (!branch.isFakeBranchForOldGUI()) {
        filteredBranches.add(branch);
      }
    }

    return filteredBranches;
  }

  public String getOuterCondition() {
    return outerCondition;
  }

  @Override
  public String getOuterConditionForGUI() {
    String outerCondition = getOuterCondition();
    if ( (getDistinctionType() == DistinctionType.TypeChoice) &&
         (outerCondition.equals(OUTER_CONDITION_TYPE_CHOICE)) ) {
      // supress AnyType that is initially present in newly created TypeChoices for downwards compatibility with flash gui
      return "";
    } else {
      return outerCondition;
    }
  }
  
  public void setCalculatedOutput(List<AVariable> calculatedOutput) {
    this.calculatedOutput = calculatedOutput;
  }

  public List<AVariable> getCalculatedOutput() {
    return calculatedOutput;
  }
  
  public List<AVariable> getUserdefinedOutput() {
    List<AVariable> vars = new ArrayList<>();
    String xmlIdString = String.valueOf(getXmlId());
    for (ServiceVariable serviceVar : getParentWFObject().getWfAsStep().childStep.getServiceVariables()) {
      if (serviceVar.getSourceIds().contains(xmlIdString) && serviceVar.isUserOutput()) {
        vars.add(serviceVar);
      }
    }
    for (AVariable exVar : getParentWFObject().getWfAsStep().childStep.getExceptionVariables()) {
      if (exVar.getSourceIds().contains(xmlIdString) && exVar.isUserOutput()) {
        vars.add(exVar);
      }
    }
    //Reihenfolge stabil halten
    Collections.sort(vars, new Comparator<AVariable>() {

      @Override
      public int compare(AVariable o1, AVariable o2) {
        int c = o1.getFQClassName().compareTo(o2.getFQClassName());
        if (c == 0) {
          c = o1.isList == o2.isList ? 0 : o1.isList ? 1 : -1;
          if (c == 0) {
            c = o1.getId().compareTo(o2.getId());
          }
        }
        return c;
      }
      
    });
    return vars;
  }

  public void addUserdefinedOutput(AVariable userOutputVar) {
    userOutputVar.getSourceIds().add(String.valueOf(getXmlId()));
    userOutputVar.setIsUserOutput(true);

    getParentScope().getChildStep().addVar(userOutputVar);
  }

  public void removeUserdefinedOutput(AVariable userOutputVar) {
    getParentScope().getChildStep().removeVar(userOutputVar);
  }

  public void create(String condition, DistinctionType type) throws XPRC_InvalidPackageNameException {
    setXmlId(creator.getNextXmlId());
    cases = new ArrayList<CaseInfo>();
    isUsingBaseChoiceObject = true;

    switch (type) {
      case ConditionalChoice:
        fqChoiceClassName = BASECHOICE_FORMULA;
        choiceClassName = BASECHOICE_TYPE_FORMULA;
        isConditionalBranch = false;

        input = new InputConnections(0);
        inputVars = new ArrayList<AVariable>();

        cases.add(new CaseInfo(creator.getNextXmlId().toString(), "true",  " ", condition, this, false));
        cases.add(new CaseInfo(creator.getNextXmlId().toString(), "false", "",  "",        this, false));
        outerCondition = condition;
        break;

      case ConditionalBranch:
        fqChoiceClassName = BASECHOICE_FORMULA;
        choiceClassName = BASECHOICE_TYPE_FORMULA;
        isConditionalBranch = true;

        input = new InputConnections(0);
        inputVars = new ArrayList<AVariable>();

        cases.add(new CaseInfo(creator.getNextXmlId().toString(), "default", "", this));
        outerCondition = (condition != null && condition.length() > 0) ? condition : "?";
        break;

      case TypeChoice:
        fqChoiceClassName = BASECHOICE_SUBCLASSES;
        choiceClassName = BASECHOICE_TYPE_SUBCLASSES;
        isConditionalBranch = false;

        input = new InputConnections(1);
        inputVars = new ArrayList<AVariable>();
        DatatypeVariable inputVar = new DatatypeVariable(creator);
        inputVar.create(ANY_TYPE_PATH, ANY_TYPE_NAME);
        inputVar.setId(creator.getNextXmlId().toString());
        inputVars.add(inputVar);

        cases.add(new CaseInfo(creator.getNextXmlId().toString(), ANY_TYPE_NAME, ANY_TYPE_FQN, "", this, false));
        subclassDoms = new DomOrExceptionGenerationBase[1];
        subclassDoms[0] = null;
        outerCondition = (condition != null && condition.length() > 0) ? condition : OUTER_CONDITION_TYPE_CHOICE;
        break;

      default:
        throw new IllegalStateException("DistinctionType must be a choice-type, but is: " + type);
    }

    caseInfos = new HashMap<String, CaseInfo>();
    for (int caseNr = 0; caseNr < cases.size(); caseNr++) {
      addStepSerialForBranch(caseNr, cases.get(caseNr));
    }

    parseFormulas();
  }

  private void addStepSerialForBranch(int index, CaseInfo caseInfo) {
    addStepSerialForBranch(index, caseInfo, true, getDistinctionType() == DistinctionType.TypeChoice);
  }

  private void addStepSerialForBranch(int index, CaseInfo caseInfo, boolean addFirstStepAssign, boolean addSecondStepAssign) {
    StepSerial sf = new StepSerial(getParentScope(), creator);
    sf.createEmpty();
    Step child = sf.getProxyForCatch(); // immer nur ein "sequence" step

    if (addFirstStepAssign) {
      StepAssign sa = new StepAssign(getParentScope(), creator);
      sa.createEmpty();
      sf.addChild(0, sa);
    }

    if (addSecondStepAssign) {
      // add another StepAssign for Type choice
      StepAssign sa = new StepAssign(getParentScope(), creator);
      sa.createEmpty();
      sf.addChild(1, sa);
    }

    children.add(index, child);
    caseInfos.put(child.getStepId(), caseInfo);
  }
  
  
  /**
   * Returns the index of "?" in condition, or -1 if there is no "?" in condition.
   * Only "?" that are not inside a literal are considered.
   */
  public static int calcIndexOfFormulaDelimiter(String condition) {
    int result = 0;
    List<XFLLexem> conditionLexed = XFLLexer.lex(condition, true);

    for (XFLLexem lex : conditionLexed) {
      if (!lex.getType().equals(TokenType.LITERAL) && lex.getToken().contains(StepChoice.FORMULA_GUI_DELIMITER)) {
        result += lex.getToken().indexOf(StepChoice.FORMULA_GUI_DELIMITER);
        return result;
      } else {
        result += lex.getToken().length();
      }
    }

    //no valid "?" in condition
    return -1;
  }
  
  

  public void replaceOuterCondition(String outerCondition) {
    if ( (getDistinctionType() == DistinctionType.ConditionalBranch) && (calcIndexOfFormulaDelimiter(outerCondition) == -1) ) {
      outerCondition += StepChoice.FORMULA_GUI_DELIMITER;
    }

    if (!fqChoiceClassName.equals(BASECHOICE_FORMULA)) {
      return; // only supported for Conditional Choice and Conditional Branching
    }

    if (getDistinctionType() == DistinctionType.ConditionalChoice) {
      if (outerCondition.length() == 0) {
        outerCondition = " ";
      }
      if ( (cases.size() > 0) && (cases.get(0) != null) ) {
        cases.get(0).setComplexName(outerCondition);
      }
    } else { // Conditional Branching
      String oldOuterCondition = getOuterCondition();
      int oldQuestionMarkPos = calcIndexOfFormulaDelimiter(oldOuterCondition);
      String oldBeforeQuestionMark = oldOuterCondition.substring(0, oldQuestionMarkPos);
      String oldAfterQuestionMark = oldOuterCondition.substring(oldQuestionMarkPos+1);

      String newOuterCondition = outerCondition;
      int newQuestionMarkPos = calcIndexOfFormulaDelimiter(newOuterCondition);
      String newBeforeQuestionMark = newOuterCondition.substring(0, newQuestionMarkPos);
      String newAfterQuestionMark = newOuterCondition.substring(newQuestionMarkPos+1);

      // update formulas in cases
      for (CaseInfo caseInfo : cases) {
        if (caseInfo == null) {
          continue;
        }

        String newComplexName = null;
        if (caseInfo.getComplexName() != null) {
          newComplexName = StringUtils.replaceFirst(caseInfo.getComplexName(), oldBeforeQuestionMark, newBeforeQuestionMark);
          newComplexName = StringUtils.replaceLast(newComplexName, oldAfterQuestionMark, newAfterQuestionMark);
        } else if (!caseInfo.isDefault()) {
          newComplexName = newBeforeQuestionMark + newAfterQuestionMark;
        }

        caseInfo.setComplexName(newComplexName);
      }
    }

    this.outerCondition = outerCondition;
    parseFormulas();
  }

  @Override
  public void addFormula(int index, String expression) {
    replaceOuterCondition(expression);
  }

  @Override
  public String getFormula(int index) {
    return getOuterCondition();
  }

  @Override
  public int getFormulaCount() {
    return 1;
  }

  public void replaceExpression(int caseNo, String expression) {
    // remove delimiters that have been added to simplify handling in GUI
    String complexName = expression.replaceAll(Pattern.quote(FORMULA_GUI_DELIMITER_START), "")
                                   .replaceAll(Pattern.quote(FORMULA_GUI_DELIMITER_END), "");

    cases.get(caseNo).setComplexName(complexName);
    parseFormulas();
  }

  private List<String> getTargetIds() {
    List<String> targetIds = new ArrayList<String>();
    if ( (getChildSteps() == null) || (getChildSteps().size() == 0) ) {
      return targetIds;
    }

    if(calculatedOutput == null)
      return targetIds;
    
    for(AVariable avar : calculatedOutput) {
      targetIds.add(avar.getId());
    }

    return targetIds;
  }
  
  //TODO: check addInputVar?
  public AVariable createInputVariableForBranch(int index) {
    
    if(getDistinctionType() != DistinctionType.TypeChoice)
      throw new RuntimeException("Input Variables can only be created for Type Choice.");
    
    AVariable result = null;
    
    //determine type
    DomOrExceptionGenerationBase doe = subclassDoms[index];
    
    //set id, label, ReferenceName, referencePath, VariableName and isList (-> false)
    if (doe instanceof DOM) {
      result = new ServiceVariable(new DatatypeVariable(getCreator(), doe.getRevision()));
      ((DatatypeVariable)result).setOriginalClassName(doe.getOriginalSimpleName());
      ((DatatypeVariable)result).setOriginalPath(doe.getOriginalPath());
    } else if (doe instanceof ExceptionGeneration) {
      result = new ExceptionVariable(getCreator(), doe.getRevision());
      try {
        ((ExceptionVariable)result).init(doe.getOriginalPath(), doe.getOriginalSimpleName());
      } catch (XPRC_InvalidPackageNameException e) {
        logger.warn("Could not initialize exception variable.", e);
      }
    } else {
      return null;
    }
    
    String assignId = getStepAssignId(index);

    result.setIsList(false);
    result.setId(String.valueOf(getParentWFObject().getNextXmlId()));
    result.setFQClassName(doe.getFqClassName());
    result.setGenerationBaseObject(doe);
    result.setLabel(doe.getLabel());
    result.setSourceIds(assignId);
    //result.setClassName(...);
    
    //step is one of our Branches
    return result;
  }
  
  
  /*
   * returns id of first step assign
   */
  private String getStepAssignId(int index) {
    Step serial = children.get(index);
    List<Step> candidates = serial.getChildSteps();
    if (candidates.size() < 2 || !(candidates.get(0) instanceof StepAssign)) {
      return null; //no two step assigns
    }

    return candidates.get(0).getStepId();
  }


  //creates AVariable and adds it to parent StepSerial - also sets sourceID
  public AVariable createOutputVariable(DomOrExceptionGenerationBase gb, boolean isList) {
    AVariable result = null;
    if(gb != null) {
      result = AVariable.createAVariable(String.valueOf(creator.getNextXmlId()), gb, isList);
      result.setLabel(gb.getLabel());
    }
    else {
      //create anyType
      result = new ServiceVariable(creator);
      result.setIsList(isList);
      result.setFQClassName("AnyType");
      result.setLabel("Any Type");
      result.setIsList(isList); //unlike inputVar
      result.setFQClassName("base.AnyType");
      ((ServiceVariable)result).setOriginalClassName("AnyType");
      ((ServiceVariable)result).setOriginalPath("base");
      result.setId(creator.getNextXmlId().toString());
    }

    result.setSourceIds(String.valueOf(getXmlId()));
    StepSerial ss = getParentScope().getChildStep();
    ss.addVar(result);
    return result;
  }

  @Override
  public void appendXML(XmlBuilder xml) {
    xml.startElementWithAttributes(EL.CHOICE); {
      Integer xmlId = getXmlId();
      if (xmlId != null) {
        xml.addAttribute(ATT.ID, xmlId.toString());
      }
      Pair<String, String> pathAndName = GenerationBase.getPathAndNameFromJavaName(fqChoiceClassName);
      xml.addAttribute(ATT.TYPEPATH, pathAndName.getFirst());
      xml.addAttribute(ATT.TYPENAME, pathAndName.getSecond());
      xml.endAttributes();
      
      // <Source>
      Set<String> appendedIds = new HashSet<String>();
      for (String id : input.getVarIds()) {
        if (appendedIds.contains(id)) {
          continue;
        }
        
        appendSource(xml, id, false, false, false);
        appendedIds.add(id);
      }
      
      // <Target>
      for (String targetId : getTargetIds()) {
        appendTarget(xml, targetId, false);
      }
      
      // <Meta>
      if ( (isConditionalBranch) || (hasUnknownMetaTags()) ) {
        xml.startElement(EL.META); {
          if (isConditionalBranch) {
            xml.element(EL.CONDITIONAL_BRANCHING_OUTER_CONDITION_ELEMENT, XMLUtils.escapeXMLValueAndInvalidChars(outerCondition, false, false));
          }

          appendUnknownMetaTags(xml);
        } xml.endElement(EL.META);
      }
      
      // <Input>
      for (int varNr = 0; varNr < inputVars.size(); varNr++) {
        xml.startElement(EL.CHOICEINPUT); {
          inputVars.get(varNr).appendXML(xml);
          String id = input.getVarIds()[varNr];
          appendSource(xml, id, input.getUserConnected()[varNr], input.getConstantConnected()[varNr], false, input.getUnknownMetaTags().get(varNr));
        } xml.endElement(EL.CHOICEINPUT);
      }
      
      // <Case>
      for (int caseNr = 0; caseNr < cases.size(); caseNr++) {
        xml.startElementWithAttributes(EL.CASE); {
          xml.addAttribute(ATT.ID, cases.get(caseNr).getId());
          xml.addAttribute(ATT.CASENAME, XMLUtils.escapeXMLValue(cases.get(caseNr).getName(), true, false));
          xml.addAttribute(ATT.CASEALIAS, XMLUtils.escapeXMLValue(cases.get(caseNr).getAlias(), true, false));
          if (cases.get(caseNr).isDefault()) {
            xml.addAttribute(ATT.ISDEFAULTCASE, Boolean.toString(cases.get(caseNr).isDefault()));
          }
          
          String premise = XMLUtils.escapeXMLValue(cases.get(caseNr).getComplexName());
          if ( (premise != null) && (premise.length() > 0) ) {
            xml.addAttribute(ATT.CASECOMPLEXNAME, premise);
          }
          xml.endAttributes();
          
          // <Function>
          children.get(caseNr).appendXML(xml);
        } xml.endElement(EL.CASE);
      }
    } xml.endElement(EL.CHOICE);
  }
  
  //an old workflow does not give IDs to cases
  //this method allows updating entries - giving them a valid ID
  public Map<String, CaseInfo> getCaseInfos(){
    return caseInfos;
  }

}
