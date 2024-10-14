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
package com.gip.xyna.xact.filter.xmom.datatypes.json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DOM.OperationInformation;

import xmcp.yggdrasil.plugin.Context;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceInformation;

public class Utils {
  
  private static final RuntimeContextDependencyManagement rcdm =  XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  private static final RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();
  private static final FQNameJson ANYTYPEFQNAME =
      new FQNameJson(GenerationBase.ANYTYPE_REFERENCE_PATH, GenerationBase.ANYTYPE_REFERENCE_NAME);

  public static Map<String, GenerationBaseObject> getParents(GenerationBaseObject parent) {
    Map<String, GenerationBaseObject> parents = new HashMap<String, GenerationBaseObject>();
    GenerationBaseObject grandParent = parent;
    while( grandParent != null ) {
      parents.put( grandParent.getOriginalFqName(), grandParent);
      grandParent = grandParent.getParent();
    }
    return parents;
  }

  public static List<DatatypeMemberXo> createDtMembers(List<AVariable> memberVars, GenerationBaseObject gbo, Map<String, GenerationBaseObject> parents) {
    List<DatatypeMemberXo> list = new ArrayList<>(memberVars.size());
    ExtendedContextBuilder contextBuilder = new ExtendedContextBuilder(gbo);
    for (int varIdx = 0; varIdx < memberVars.size(); varIdx++) {
      AVariable var = memberVars.get(varIdx);

      PersistenceInformation pi = null;
      XMOMStorableStructureInformation si = null;
      boolean isStorable = false;
      if (gbo.getType() == XMOMType.DATATYPE) {
        DOM dom = gbo.getDOM();
        pi = dom.getPersistenceInformation();
        si = XMOMStorableStructureCache.getInstance(dom.getRevision()).getStructuralInformation(dom.getFqClassName());
        isStorable = dom.isInheritedFromStorable();
      }


      DatatypeMemberXo member = new DatatypeMemberXo(var, ObjectId.createMemberVariableId(varIdx), pi, si, isStorable, contextBuilder);
      list.add(member);

      GenerationBase varCreator = var.getCreator();
      if( ! varCreator.equals(gbo.getGenerationBase() ) ) {
        ObjectIdentifierJson inheritedFrom = parents.get(varCreator.getOriginalFqName()).getObjectIdentifierJson();
        member.setInheritedFrom( inheritedFrom );
      }
    }
    return list;
  }


  public static List<DatatypeMethodXo> createDtMethods(DOM dom, GenerationBaseObject gbo) {
    List<DatatypeMethodXo> methods = new ArrayList<>();
    ExtendedContextBuilder contextBuilder = new ExtendedContextBuilder(gbo);
    OperationInformation[] operationInformations = dom.collectOperationsOfDOMHierarchy(true);
    for (int operationIdx = 0; operationIdx < operationInformations.length; operationIdx++) {
      OperationInformation oi = operationInformations[operationIdx];
      DatatypeMethodXo dtMethod = new DatatypeMethodXo(oi.getOperation(), gbo, ObjectId.createMemberMethodId(operationIdx), contextBuilder);
      if(!oi.getDefiningType().equals(dom)) { // Override or inherited
        boolean overrides = false;
        for(Operation operation : dom.getOperations()) {
          if(operation.hasEqualSignature(oi.getOperation())) { // Override
            overrides = true;
            break;
          }
        }
        dtMethod.setOverrides(overrides);
        if(!overrides) { // inherited
          dtMethod.setInheritedFrom(oi.getOperation().getParent());
        }
      }
      methods.add(dtMethod);
    }
    return methods;
  }
  
  
  public static ObjectIdentifierJson createAnyTypeIdentifier(Long revision) {
    ObjectIdentifierJson result = new ObjectIdentifierJson();

    Long correctRtc = rcdm.getRevisionDefiningXMOMObject(GenerationBase.CORE_EXCEPTION, revision);
    
    if(correctRtc == null) {
      return null;
    }

    RuntimeContextJson rtc = null;
    RuntimeContextJson currentRtc = null;

    try {
      RuntimeContext runtimeContext = rm.getRuntimeContext(correctRtc);
      rtc = new RuntimeContextJson(runtimeContext);
      currentRtc = new RuntimeContextJson(rm.getRuntimeContext(revision));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return null;
    }
    
    result.setDataTypeLabel("AnyType");
    result.setLabel("AnyType");
    result.setType(Type.dataType);
    
    result.setFQName(ANYTYPEFQNAME);
    result.setOriginRuntimeContext(rtc);
    result.setRuntimeContext(currentRtc);
    
    return result;
  }


  public static class ExtendedContextBuilder {

    private Context.Builder builder;
    private XMOMType type;


    public ExtendedContextBuilder(GenerationBaseObject gbo) {
      builder = new Context.Builder();
      builder.fQN(gbo.getOriginalFqName());
      builder.runtimeContext(com.gip.xyna.xact.filter.util.Utils.getXpceRtc(gbo.getRuntimeContext()));
      type = gbo.getType();
    }

    
    public Context instantiateContext(String location, String objectId) {
      builder.location(location);
      builder.objectId(objectId);
      return builder.instance().clone();
    }

    public Context.Builder getBuilder() {
      return builder;
    }


    public void setBuilder(Context.Builder builder) {
      this.builder = builder;
    }


    public XMOMType getType() {
      return type;
    }


    public void setType(XMOMType type) {
      this.type = type;
    }
  }

}
