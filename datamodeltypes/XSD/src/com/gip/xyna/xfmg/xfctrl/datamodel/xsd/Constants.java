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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd;

import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.FQName;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.MemberType;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfo.Type;
import com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types.TypeInfoMember;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Datatype;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;

public class Constants {

  private static final XmomType XMOM_BASE = new XmomType("xdnc.model.xsd","XSDBaseModel", "XSDBase Model");
  private static final String UTIL_PATH = "xfmg.xfctrl.datamodel.xsd";
  
  private static final XmomType XMOM_ANYTYPE = new XmomType(UTIL_PATH, "XSDAnyType", "XSD Any Type");
  public final static String FIELD_ANYTYPE_NAMESPACE = "namespace";
  public final static String FIELD_ANYTYPE_TYPE_NAME = "typeName";
  public final static String FIELD_ANYTYPE_STRING_VALUE = "stringValue";
  public final static String FIELD_ANYTYPE_VALUE = "value";
  
  
  public static XmomType getBase_XmomType() {
    return XMOM_BASE;
  }

  public static String getUtilPath() {
    return UTIL_PATH;
  }

  public static XmomType getAnyType_XmomType() {
    return XMOM_ANYTYPE;
  }
  
  public static String getAnyType_FqName() {
    return XMOM_ANYTYPE.getFQTypeName();
  }
  
  public static Datatype createDatatype() {
    Datatype datatype = Datatype.
        create(XMOM_ANYTYPE).
        basetype(XMOM_BASE).
        variable(Variable.create(FIELD_ANYTYPE_NAMESPACE).label("Namespace").simpleType(PrimitiveType.STRING)).
        variable(Variable.create(FIELD_ANYTYPE_TYPE_NAME).label("Type Name").simpleType(PrimitiveType.STRING)).
        variable(Variable.create(FIELD_ANYTYPE_STRING_VALUE).label("String Value").simpleType(PrimitiveType.STRING)).
        variable(Variable.create(FIELD_ANYTYPE_VALUE).label("Value").complexType(XMOM_BASE)).
        build();
    return datatype;
  }

  

  public static TypeInfo createTypeInfo() {
    String namespace = null; //liegen in keinem richtigen Namespace
    
    TypeInfo typeInfoBase = new TypeInfo( Type.Complex, new FQName(namespace, XMOM_BASE.getName() ) );
    
    TypeInfo typeInfo = new TypeInfo( Type.Any, new FQName(namespace, XMOM_ANYTYPE.getName() ) );
    
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_NAMESPACE),MemberType.Element).optional(false).
                       simpleType(PrimitiveType.STRING).build());
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_TYPE_NAME),MemberType.Element).optional(false).
                       simpleType(PrimitiveType.STRING).build());
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_STRING_VALUE),MemberType.Element).optional(false).
                       simpleType(PrimitiveType.STRING).build());
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_VALUE),MemberType.Element).optional(false).
                       complexType(typeInfoBase).build());
    return typeInfo;
  }

  public static TypeInfo createBase_TypeInfo() {
    String namespace = null; //liegen in keinem richtigen Namespace
    TypeInfo typeInfo = new TypeInfo(Type.Complex, new FQName(namespace, XMOM_BASE.getName() ) );
    typeInfo.setXmomType(XMOM_BASE);
    return typeInfo;
  }

  public static TypeInfo createAnyType_TypeInfo() {
    String namespace = null; //liegen in keinem richtigen Namespace
    
    TypeInfo typeInfoBase = createBase_TypeInfo();
    
    TypeInfo typeInfo = new TypeInfo( Type.Any, new FQName(namespace, XMOM_ANYTYPE.getName() ) );
    
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_NAMESPACE),MemberType.Element).optional(false).
                       simpleType(PrimitiveType.STRING).build());
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_TYPE_NAME),MemberType.Element).optional(false).
                       simpleType(PrimitiveType.STRING).build());
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_STRING_VALUE),MemberType.Element).optional(false).
                       simpleType(PrimitiveType.STRING).build());
    typeInfo.addMemberAndIncrementPosition(
        TypeInfoMember.create(new FQName(namespace, FIELD_ANYTYPE_VALUE),MemberType.Element).optional(false).
                       complexType(typeInfoBase).build());
    
    typeInfo.setXmomType(XMOM_ANYTYPE);
    for( TypeInfoMember tim : typeInfo.getMembers() ) {
      tim.setVarName(tim.getName().getName());
    }

    return typeInfo;
  }

 

}
