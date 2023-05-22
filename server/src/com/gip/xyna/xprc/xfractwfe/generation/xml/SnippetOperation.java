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
package com.gip.xyna.xprc.xfractwfe.generation.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.EL;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable.VariableBuilder;


public class SnippetOperation extends Operation {

  private String name;
  private String label;
  private boolean isStatic = false;
  private boolean isFinal = false;
  private boolean isAbstract = false;
  private String documentation;
  private boolean hasBeenPersisted = true;
  private List<Variable> inputs;
  private List<Variable> outputs;
  public List<Variable> exceptions;
  public String sourceCode;
  public boolean isCancelable;
  public WF wf;
  private boolean requiresXynaOrder = false;
  
  
  protected SnippetOperation() {
  }
  
  private SnippetOperation(SnippetOperation operation) {
    this.name = operation.name;
    this.label = operation.label;
    this.inputs = clone(operation.inputs);
    this.outputs = clone(operation.outputs);
    this.exceptions = clone(operation.exceptions);
    this.sourceCode = operation.sourceCode;
    this.isCancelable = operation.isCancelable;
    this.wf = operation.wf;
    this.isStatic = operation.isStatic;
    this.isFinal = operation.isFinal;
    this.isAbstract = operation.isAbstract;
    this.documentation = operation.documentation;
    this.hasBeenPersisted = operation.hasBeenPersisted;
    this.requiresXynaOrder = operation.requiresXynaOrder;
  }
  
  private <T> List<T> clone(List<T> list) {
    if( list == null || list.isEmpty() ) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList( new ArrayList<T>(list) );
  }
  
  @Override
  protected void appendOperationContentToXML(XmlBuilder xml) {
    String documentation = getDocumentation();
    if ( (documentation != null && documentation.length() > 0) || (!hasBeenPersisted()) || (hasUnknownMetaTags()) ) {
      xml.startElement(EL.META);{
        if (documentation != null && documentation.length() > 0) {
          xml.element(EL.DOCUMENTATION, XMLUtils.escapeXMLValueAndInvalidChars(documentation, false, false));
        }

        if (!hasBeenPersisted()) {
          xml.element(EL.HAS_BEEN_PERSISTED, Boolean.FALSE.toString());
        }

        appendUnknownMetaTags(xml);
      } xml.endElement(EL.META);
    }

    if (getSourceCode() != null) {
      xml.startElement(EL.SOURCECODE); {
        xml.startElementWithAttributes(EL.CODESNIPPET); {
          xml.addAttribute(ATT.SNIPPETTYPE, "Java");
          if (isCancelable()) {
            xml.addAttribute(ATT.ISCANCELABLE, Boolean.TRUE.toString());
          }
          xml.endAttributesNoLineBreak();        // suppress line break to avoid having it shown in code snippet
          xml.append(getSourceCode());
        } xml.endElementNoIdent(EL.CODESNIPPET); // suppress indentation to avoid having it shown in code snippet
      } xml.endElement(EL.SOURCECODE);
    }

    if (getWfCall() != null) {
      xml.startElementWithAttributes(EL.WORKFLOW_CALL); {
        xml.addAttribute(ATT.REFERENCENAME, getWfCall().getOriginalSimpleName());
        xml.addAttribute(ATT.REFERENCEPATH, getWfCall().getOriginalPath());
      } xml.endAttributesAndElement();
    }
  }
  
  @Override
  public List<Variable> getInputs() {
    return inputs;
  }
  
  @Override
  public List<Variable> getOutputs() {
    return outputs;
  }
  
  @Override
  public boolean isStatic() {
    return isStatic;
  }
  
  @Override
  public boolean isFinal() {
    return isFinal;
  }
  
  @Override
  public boolean isAbstract() {
    return isAbstract;
  }
  
  @Override
  public boolean requiresXynaOrder() {
    return requiresXynaOrder;
  }
  
  @Override
  public String getDocumentation() {
    return documentation;
  }
  
  @Override
  public boolean hasBeenPersisted() {
    return hasBeenPersisted;
  }
  
  public String getName() {
    return name;
  }
  
  public String getLabel() {
    return label;
  }
  
  public List<Variable> getExceptions() {
    return exceptions;
  }
  
  public String getSourceCode() {
    return sourceCode;
  }
  
  public boolean isCancelable() {
    return isCancelable;
  }
  
  public WF getWfCall() {
    return wf;
  }
  
  public static SnippetOperationBuilder create(String name) {
    return new SnippetOperationBuilder(name);
  }

  public static class SnippetOperationBuilder {
    SnippetOperation operation;
    
    public SnippetOperationBuilder(String name) {
      operation = new SnippetOperation();
      operation.name = name;
    }

    public SnippetOperationBuilder label(String label) {
      operation.label = label;
      return this;
    }
    
    public SnippetOperationBuilder input(Variable variable) {
      getOrCreateInputs().add(variable);
      return this;
    }
    public SnippetOperationBuilder input(VariableBuilder variable) {
      getOrCreateInputs().add(variable.build());
      return this;
    }
    
    public SnippetOperationBuilder output(Variable variable) {
      getOrCreateOutputs().add(variable);
      return this;
    }
    
    public SnippetOperationBuilder output(VariableBuilder variable) {
      getOrCreateOutputs().add(variable.build());
      return this;
    }
    
    public SnippetOperationBuilder exception(Variable variable) {
      getOrCreateExceptions().add(variable);
      return this;
    }
    
    public SnippetOperationBuilder exception(VariableBuilder variable) {
      getOrCreateExceptions().add(variable.build());
      return this;
    }
    
    public SnippetOperationBuilder isStatic(boolean isStatic) {
      operation.isStatic = isStatic;
      return this;
    }
    
    public SnippetOperationBuilder isFinal(boolean isFinal) {
      operation.isFinal = isFinal;
      return this;
    }
    
    public SnippetOperationBuilder isAbstract(boolean isAbstract) {
      operation.isAbstract = isAbstract;
      return this;
    }
    
    public SnippetOperationBuilder documentation(String documentation) {
      operation.documentation = documentation;
      return this;
    }
    
    public SnippetOperationBuilder hasBeenPersisted(boolean hasBeenPersisted) {
      operation.hasBeenPersisted = hasBeenPersisted;
      return this;
    }
    
    private List<Variable> getOrCreateInputs() {
      if( operation.inputs == null ) {
        operation.inputs = new ArrayList<Variable>();
      }
      return operation.inputs;
    }
    
    private List<Variable> getOrCreateOutputs() {
      if( operation.outputs == null ) {
        operation.outputs = new ArrayList<Variable>();
      }
      return operation.outputs;
    }
    
    private List<Variable> getOrCreateExceptions() {
      if( operation.exceptions == null ) {
        operation.exceptions = new ArrayList<Variable>();
      }
      return operation.exceptions;
    }
    
    public SnippetOperationBuilder sourceCode(String sourceCode) {
      operation.sourceCode = sourceCode;
      return this;
    }
    
    public SnippetOperationBuilder isCancelable(boolean isCancelable) {
      operation.isCancelable = isCancelable;
      return this;
    }
    
    public SnippetOperationBuilder wfCall(WF wf) {
      operation.wf = wf;
      return this;
    }
    
    public SnippetOperationBuilder requiresXynaOrder() {
      operation.requiresXynaOrder = true;
      return this;
    }
    
    public SnippetOperation build() {
      return new SnippetOperation(operation);
    }
    
  }

  @Override
  public String getId() {
    return null;
  }

  @Override
  public boolean hasUnknownMetaTags() {
    return false;
  }

  @Override
  public void appendUnknownMetaTags(XmlBuilder xml) {}

}
