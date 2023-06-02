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
package com.gip.xyna.xdev.map.types;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xdev.map.typegen.GenerationParameter;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;


/**
 *
 */
public class DataTypeXmlHelper {

  private GenerationParameter generationParameter;

  public DataTypeXmlHelper(GenerationParameter generationParameter) {
    this.generationParameter = generationParameter;
  }

  public Datatype toDatatype(TypeInfo typeInfo) {
    List<Variable> vs = new ArrayList<Variable>();
    for( TypeInfoMember tim : typeInfo.getMembers() ) {
      vs.add( createVariable(tim) );
    }
    return Datatype.derived(typeInfo.getXmomType(), getBaseType(typeInfo), vs);
    //return Datatype.meta(typeInfo.getXmomType(), getBaseType(typeInfo), createMeta(importParameter), vs);
  }

  private Variable createVariable(TypeInfoMember tim) {
    //Meta meta = Meta.dataModel( XSDDataModel.member(this,parent,index));
    
    if( tim.isSimple() ) {
      return Variable.simple(tim.getVarName(), tim.getLabel(),
                             tim.getSimpleType(), tim.isList());
    } else {
      return Variable.complex(tim.getVarName(), tim.getLabel(),
                              tim.getComplexType().getXmomType(), tim.isList());
    }
  }

  private XmomType getBaseType(TypeInfo typeInfo) {
    if( typeInfo.getBaseTypeInfo() != null ) {
      return typeInfo.getBaseTypeInfo().getXmomType();
    } else {
      if( typeInfo.isTypeUsedByRootElement() ) {
        return generationParameter.getBaseTypeRoot();
      } else {
        return generationParameter.getBaseType();
      }
    }
  }

}
