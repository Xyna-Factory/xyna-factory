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

package com.gip.xyna.xact.filter.util;



import java.util.Arrays;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.util.xo.MetaInfo;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.AssumedDeadlockException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;



public class AVariableObjectVisitor extends EmptyJsonVisitor<AVariable> {

  //TODO: move to EmptyJsonVisitor
  public final static String META_TAG = "$meta";
  public final static String WRAPPED_LIST_TAG = "$list";
  public final static String OBJECT_TAG = "$object";
  public final static String PRIMITIVE_TAG = "$primitive";
  public final static String META_LABEL_TAG = "$label";
  public final static String META_METHOD_TAG = "$method";
  public final static String LABEL_TAG = "label";
  public final static String DOCU_TAG = "docu";
  public final static String DOLLAR_DOCU_TAG = "$docu";
  public final static String IS_ABSTRACT_TAG = "abstract";
  public static final String OUTPUTS_LIST_TAG = "returns";
  public static final String INPUTS_LIST_TAG = "params";


  private AVariable result;
  private Long revision;
  private MetaInfo info;
  private GenerationBaseCache cache;
  private RevisionManagement revMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();


  public AVariableObjectVisitor() {
    this(new GenerationBaseCache());
  }


  private AVariableObjectVisitor(GenerationBaseCache cache) {
    this.cache = cache;
  }


  @Override
  public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
    if (label.equals(META_TAG)) {
      return MetaInfo.getJsonVisitor();
    } else {
      return new AVariableObjectVisitor(cache);
    }
  }


  @Override
  public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
    AVariable obj = getObject();
    DatatypeVariable child = new DatatypeVariable(revision);
    PrimitiveType primType = getPrimitiveType(label);
    child.create(primType);
    child.setValue(value);
    child.setVarName(label);
    child.setHasConstantValue(true);
    obj.addChild(child);
    obj.setHasConstantValue(false);
  }

  
  private AVariable findMember(String label) {
    AVariable obj = getObject();
    DomOrExceptionGenerationBase doe = obj.getDomOrExceptionObject();
    try {
      doe.parse(false);
    } catch (XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
    }

    List<AVariable> candidates = obj.getDomOrExceptionObject().getAllMemberVarsIncludingInherited();
    for (AVariable candidate : candidates) {
      if (candidate.getVarName().equals(label)) {
        return candidate;
      }
    }
    throw new RuntimeException("could not determine type of " + label);
  }
  

  private PrimitiveType getPrimitiveType(String label) {
    return findMember(label).getJavaTypeEnum();
  }


  @Override
  public void list(String label, List<String> values, Type type) throws UnexpectedJSONContentException {
    AVariable object = getObject();
    DatatypeVariable child = new DatatypeVariable(revision);
    PrimitiveType primType = getPrimitiveType(label);
    child.create(primType);
    child.setValues(values.toArray(new String[0]));
    child.setVarName(label);
    child.setIsList(true);
    child.setHasConstantValue(true);
    object.addChild(child);
  }

  
  private AVariable createMemberAVar(String label) {
    if(getPrimitiveType(label) != null) {
      DatatypeVariable result = new DatatypeVariable(revision);
      PrimitiveType primType = getPrimitiveType(label);
      result.create(primType);
      result.setVarName(label);
      return result;
    }
    
    AVariable candidate = findMember(label);
    AVariable result = AVariable.createAVariable("", candidate.getDomOrExceptionObject(), candidate.isList());
    result.setId(null);
    result.setVarName(label);
    return result;
  }

  @Override
  public void object(String label, Object value) throws UnexpectedJSONContentException {
    if (label.equals(META_TAG)) {
      info = (MetaInfo) value;
      getObject(); //create object
    } else {
      AVariable obj = getObject();
      if(value instanceof AVariable) {
        obj.addChild((AVariable)value);
        ((AVariable)value).setVarName(label);
      }
      obj.setHasConstantValue(false);
    }
  }


  @Override
  public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
    AVariable object = getObject();
    for (Object child : values) {
      if (!(child instanceof AVariable)) {
        continue;
      }
      object.addChild((AVariable) child);
    }
    
    if (!label.equals(WRAPPED_LIST_TAG)) {
      object.setVarName(label);
    }
    object.setIsList(true);
    object.setHasConstantValue(true);
  }


  @Override
  public void emptyList(String label) throws UnexpectedJSONContentException {
    AVariable object = getObject();
    if (label.equals(WRAPPED_LIST_TAG)) {
      //entire thing is a list
      object.setIsList(true);
    } else {
      //a member is a list
      AVariable child = createMemberAVar(label);
      child.setVarName(label);
      child.setIsList(true);
      child.addChild(object); //empty list instead of null!
      child.removeChildren(Arrays.asList(new AVariable[] {object}));
      child.setValues(new String[0]);
      object.addChild(child);
      object.setHasConstantValue(false);
    }
  }


  private AVariable getObject() {
    if (result != null) {
      return result;
    }
    if (info == null) {
      throw new RuntimeException("MetaInfo not present!");
    }
    String fqn = info.getFqName();
    GenerationBase doe = null;
    try {
      Long revision = revMgmt.getRevision(info.getRuntimeContext());
      this.revision = revision;
      doe = DomOrExceptionGenerationBase.getOrCreateInstance(fqn, cache, revision);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }

    result = AVariable.createAVariable("", (DomOrExceptionGenerationBase) doe, false);
    String varName = result.getVarName();
    result.setId(null);
    result.setVarName(varName);
    result.setLabel(doe.getLabel());
    result.addChild(result); //empty list instead of null!
    result.removeChildren(Arrays.asList(new AVariable[] {result}));
    

    return result;
  }


  @Override
  public AVariable get() {
    return result;
  }


  @Override
  public AVariable getAndReset() {
    AVariable result = this.result;
    reset();
    return result;
  }


  private void reset() {
    result = null;
  }

}
