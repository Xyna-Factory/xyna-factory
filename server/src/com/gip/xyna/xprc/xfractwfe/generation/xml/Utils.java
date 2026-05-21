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

package com.gip.xyna.xprc.xfractwfe.generation.xml;

import java.util.Collections;
import java.util.List;

import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.CodeOperation;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.JavaOperation;
import com.gip.xyna.xprc.xfractwfe.generation.PythonOperation;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCallInService;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.SnippetOperation.SnippetOperationBuilder;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable.VariableBuilder;

public class Utils {
  public static XmomType getXmomType(GenerationBase base) {
    if(base == null) {
      return new XmomType(GenerationBase.ANYTYPE_REFERENCE_PATH, GenerationBase.ANYTYPE_REFERENCE_NAME, "AnyType", false);
    }
    return new XmomType(base.getOriginalPath(), base.getOriginalSimpleName(), base.getLabel(), base.isAbstract());
  }
  
  public static XmomType getBaseType(DomOrExceptionGenerationBase domOrExceptionGenerationBase) {
    DomOrExceptionGenerationBase parent = domOrExceptionGenerationBase instanceof DOM ?
        ((DOM) domOrExceptionGenerationBase).getSuperClassGenerationObject() : ((ExceptionGeneration) domOrExceptionGenerationBase).getSuperClassGenerationObject();
    if (parent == null) {
      return null;
    }

    return getXmomType(parent);
  }
  
  public static Variable createVariable(AVariable var) {
    VariableBuilder vb;
    if (var instanceof ExceptionVariable) {
      vb = Variable.createException(var.getVarName());
    } else {
      vb = Variable.create(var.getVarName());
    }
    vb.label(var.getLabel()).
       isList(var.isList()).
       id(var.getId()).
       documentation(var.getDocumentation()).
       persistenceTypes(var.getPersistenceTypes());

    if (var.isJavaBaseType()) {
      vb.simpleType(var.getJavaTypeEnum());
    } else if (var.isPrototype()) {
      String variableName = labelToJavaName(var.getLabel(), false);
      String referenceName = labelToJavaName(var.getLabel(), true);
      vb.abstractType(variableName, referenceName);
    } else {
      vb.complexType( getXmomType(var.getDomOrExceptionObject()) );
    }

    vb.meta(Meta.unknownMetaTags(var.getUnknownMetaTags()));

    if (var instanceof DatatypeVariable) {
      DatatypeVariable dtVar = (DatatypeVariable)var;
      vb.restrictions(dtVar.getRestrictions());
    }

    return vb.build();
  }

  public static Operation createOperation(com.gip.xyna.xprc.xfractwfe.generation.Operation operation) {
    return createOperation(operation, false);
  }

  public static Operation createOperation(com.gip.xyna.xprc.xfractwfe.generation.Operation operation, boolean escapeSourceCode) {
    SnippetOperationBuilder sob = SnippetOperation.create(operation.getName());
    sob.label(operation.getLabel())
       .isStatic(operation.isStatic())
       .isFinal(operation.isFinal())
       .isAbstract(operation.isAbstract())
       .documentation(operation.getDocumentation())
       .hasBeenPersisted(operation.hasBeenPersisted())
       .unknownMetaTags(operation.getUnknownMetaTags());

    for (AVariable inputVar : operation.getInputVars()) {
      sob.input(createVariable(inputVar));
    }
    for (AVariable outputVar : operation.getOutputVars()) {
      sob.output(createVariable(outputVar));
    }
    for (ExceptionVariable exceptionVar : operation.getThrownExceptions()) {
      sob.exception(createVariable(exceptionVar));
    }

    if (operation instanceof CodeOperation) {
      CodeOperation codeOperation = (CodeOperation) operation;
      sob.codeLanguage(codeOperation.getCodeLanguage());
      if (codeOperation.requiresXynaOrder()) {
        sob.requiresXynaOrder();
      }

      String sourceCode;
      if (escapeSourceCode) {
        String strippedImpl = codeOperation.getImpl() != null ? codeOperation.getImpl().strip() : null;
        sourceCode = XMLUtils.escapeXMLValueAndInvalidChars(strippedImpl, false, false);
      } else {
        sourceCode = codeOperation.getImpl();
      }

      sob.sourceCode(sourceCode)
         .isCancelable(codeOperation.isStepEventListener());
    }

    if (operation instanceof WorkflowCallInService) {
      sob.wfCall(((WorkflowCallInService) operation).getWf());
    }

    return sob.build();
  }

  public static Meta createMeta(DomOrExceptionGenerationBase dtOrException) {
    Meta meta = new Meta();
    meta.setDocumentation(dtOrException.getDocumentation());
    meta.setUnknownMetaTags(dtOrException.getUnknownMetaTags());
    
    if (dtOrException instanceof DOM) {
      DOM dom = (DOM)dtOrException;
      meta.setIsServiceGroupOnly(dom.isServiceGroupOnly());
      meta.setPersistenceInformation(dom.getPersistenceInformation());
    }

    return meta;
  }
  
  public static String labelToJavaName(String label, boolean startWithUpperCase) {
    return createUniqueJavaName(Collections.emptyList(), label, startWithUpperCase);
  }

  static final int MAX_UNIQUE_CHECK_COUNT = 10_000;

  /**
   * Creates a java name based on the given label that isn't already contained in the given list of used names.
   * 
   * This is accomplished by adding an integer number at the end in case of a name conflict.
   */
  public static String createUniqueJavaName(List<String> usedNames, String label, boolean startWithUpperCase) {

    if (label == null) {
      return null;
    }

    String[]  parts = label.split("[\\W]+");
    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (String s : parts) {
      if (s.length() == 0) {
        continue;
      }

      if (first) {
        if (Character.isDigit(s.charAt(0))) {
          sb.append("_");
        }

        if (startWithUpperCase) {
          sb.append(Character.toUpperCase(s.charAt(0)));
        } else {
          sb.append(Character.toLowerCase(s.charAt(0)));
        }

        first = false;
      } else {
        sb.append(Character.toUpperCase(s.charAt(0)));
      }

      sb.append(s.substring(1));
    }

    String javaName = sb.toString();

    String uniqueJavaName = javaName;
    boolean isUnique = false;
    int suffixIdx = -1;

    int runCount = 0;

    while (!isUnique) {
      runCount++;
      if (usedNames.contains(uniqueJavaName) || GenerationBase.isReservedName(uniqueJavaName)) {
        suffixIdx++;
        uniqueJavaName = javaName + suffixIdx;
      } else {
        isUnique = true;
      }

      if (runCount >= MAX_UNIQUE_CHECK_COUNT) {
        throw new RuntimeException("Maximum Unique-Check-Count reached.");
      }
    }

    return uniqueJavaName;
  }

  public static String changeCaseFirstChar(String string, boolean startWithUpperCase) {
    if ( (string == null) || (string.length() == 0) ) {
      return string;
    }

    if (startWithUpperCase) {
      return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    } else {
      return Character.toLowerCase(string.charAt(0)) + string.substring(1);
    }
  }

}
