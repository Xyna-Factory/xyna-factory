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
package com.gip.xyna.xprc.xfractwfe.formula;



import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.OperationInfo;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;



public class TypeInfo {

  public interface ModelledType {

    String getFqClassName();

    boolean isSuperClassOf(ModelledType otherType);
    
    String getFqXMLName();    

    String getSimpleClassName();

    List<VariableInfo> getAllMemberVarsIncludingInherited();
    
    List<OperationInfo> getAllInstanceOperationsIncludingInherited();
    
    String generateEmptyConstructor() throws XPRC_InvalidPackageNameException;

    boolean isAbstract();
    
    Set<ModelledType> getSubTypesRecursivly();

  }


  public static final TypeInfo UNKOWN = new TypeInfo(2);
  public static final TypeInfo ANY = new TypeInfo(1);
  public static final TypeInfo NULL = new TypeInfo(3);
  public static final TypeInfo ANYNUMBER = new TypeInfo(4);
  private BaseType t;
  private ModelledType dom;
  private final boolean isList;
  private int specialtype; //1 = any, 2 = unknown, 3=null, 4 == any number


  private TypeInfo(int specialtype) {
    this.specialtype = specialtype;
    isList = false;
  }

  public TypeInfo(BaseType t, boolean isList) {
    this.t = t;
    this.isList = isList;
  }

  public TypeInfo(BaseType t) {
    this.t = t;
    isList = t == BaseType.LIST;
  }


  public TypeInfo(PrimitiveType primitiveType) {
    switch (primitiveType) {
      case BOOLEAN:
      case BOOLEAN_OBJ:
      case BYTE:
      case BYTE_OBJ:
      case DOUBLE:
      case DOUBLE_OBJ:
      case INT:
      case INTEGER:
      case LONG:
      case LONG_OBJ:
      case STRING:
        t = BaseType.valueOfJavaName(primitiveType.getClassOfType());
        break;
      default :
        throw new RuntimeException("unsupported");
    }
    isList = false;     
  }


  public TypeInfo(ModelledType dom, boolean isList) {
    this.dom = dom;
    this.isList = isList;
  }


  /**
   * einer von boolean/boolean_obj, byte/byte_obj, double/double_obj, int/integer, long/long_obj, string. LIST
   */
  public boolean isBaseType() {
    return t != null;
  }


  public BaseType getBaseType() {
    return t;
  }


  public boolean isModelledType() {
    return dom != null;
  }


  public ModelledType getModelledType() {
    return dom;
  }


  public boolean isAny() {
    return specialtype == 1;
  }


  public boolean isUnknown() {
    return specialtype == 2;
  }


  public boolean isNull() {
    return specialtype == 3;
  }


  public boolean isList() {
    return isList;
  }


  public boolean isAnyNumber() {
    return specialtype == 4 || (t != null && t.isNumber());
  }


  public String getJavaName() {
    if (t != null) {
      return t.getJavaClass();
    }
    if (dom != null) {
      if (isList) {
        return "List";
      }
      return dom.getFqClassName();
    }
    if (isNull()) {
      return "null";
    }
    if (isAny()) {
      return "Object";
    }
    if (isList()) {
      return "List";
    }
    if (isAnyNumber()) {
      return "Number";
    }
    throw new RuntimeException();
  }


  public boolean isPrimitive() {
    return specialtype == 4 || (t != null && t.isPrimitive());
  }

  @Override
  public int hashCode() {
    return Objects.hash(dom,isList,specialtype,t);
  }
  
  //EQUALS-Methoden

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass()) {
      return false;
    }
    TypeInfo other = (TypeInfo) obj;
    if (dom == null) {
      if (other.dom != null)
        return false;
    } else if (!dom.equals(other.dom))
      return false;
    if (isList != other.isList)
      return false;
    if (specialtype != other.specialtype)
      return false;
    if (t != other.t)
      return false;
    return true;
  }


  public boolean equals(TypeInfo otherType, boolean ignoreDifferenceInPrimitveOrNotPrimitive) {
    if (otherType.isBaseType()) {
      return equals(otherType.getBaseType(), ignoreDifferenceInPrimitveOrNotPrimitive);
    }

    if (otherType.isModelledType()) {
      if (isModelledType()) {
        if (otherType.getModelledType() == getModelledType()) {
          return true;
        }
        if (otherType.getModelledType().getFqXMLName().equals(getModelledType().getFqXMLName())) {
          return true;
        }
      }
      return false;
    }

    return otherType.specialtype == specialtype;
  }


  public boolean equals(BaseType baseType, boolean ignoreDifferenceInPrimitveOrNotPrimitive) {
    if (baseType == t) {
      return true;
    }

    if (ignoreDifferenceInPrimitveOrNotPrimitive && isBaseType()) {
      return t.isSameTypeIgnoringPrimitiveness(baseType);
    }

    return false;
  }


}
