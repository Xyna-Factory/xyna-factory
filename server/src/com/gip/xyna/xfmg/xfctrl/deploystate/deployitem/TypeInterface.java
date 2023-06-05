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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemIdentificationBase;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable.PrimitiveType;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.StepBasedVariable.StepBasedType;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification.VariableInfo;


public class TypeInterface extends DeploymentItemIdentificationBase implements DeploymentItemInterface {
  
  private final static TypeInterface ANY_TYPE = new TypeInterface("", (XMOMType) null, false) {
    public boolean matches(DeploymentItemInterface other) { return true; }
    public boolean isAssignableFrom(TypeInterface typeInterface) { return true; };
    public boolean resolve() { return true; };
  };
  
  protected Boolean javaBaseType;
  protected Boolean isList;
  protected Set<TypeOfUsage> typesOfUsage;

  protected TypeInterface(String name) {
    this(name, null);
  }
  
  protected TypeInterface(String name, XMOMType type) {
    super(type, name);
  }
  
  protected TypeInterface(String name, XMOMType type, boolean isList) {
    this(name, type);
    this.isList = isList;
  }
  
  protected TypeInterface(String name, boolean isJavaBaseType) {
    this(name);
    javaBaseType = isJavaBaseType;
  }
  
  protected TypeInterface(String name, boolean isJavaBaseType, boolean isList) {
    this(name, isJavaBaseType);
    this.isList = isList;
  }
  

  public boolean matches(DeploymentItemInterface other) {
    if (other instanceof TypeInterface) {
      TypeInterface otherType = (TypeInterface)other;
      if (otherType == ANY_TYPE) {
        return true;
      }
      if (this.getName().equals(otherType.getName())) {
        //type == null => ist mit jedem typ kompatibel
        if (getType() == null || otherType.getType() == null || getType() == otherType.getType()) {
          return isList == null || otherType.isList == null || isList.equals(otherType.isList); 
        } else {
          return false;
        }
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int h = super.hashCode();
    return Objects.hash(h, javaBaseType, isList);
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof TypeInterface)) {
      return false;
    }
    TypeInterface other = (TypeInterface)obj;
    if (!Objects.equals(javaBaseType,other.javaBaseType)) {
      return false;
    }
    if (!Objects.equals(isList,other.isList)) {
      return false;
    }
    return true;
  }


  public boolean isAssignableFrom(TypeInterface typeInterface) {
    return isAssignableFrom(typeInterface, false);
  }
  
  public boolean isAssignableFrom(TypeInterface typeInterface, boolean tolerantPrimitives) {
    if (javaBaseType == null || javaBaseType) {
      boolean check = typeInterface.javaBaseType == null || typeInterface.javaBaseType;
      if (XynaProperty.SUPPRESS_WARNINGS.get() || check) {
        PrimitiveType primitive = PrimitiveType.createOrNull(getName());
        if (primitive != null) {
          if (tolerantPrimitives && primitive == PrimitiveType.STRING) {
            //Strings sind zu allem kompatibel
            return true;
          }
          if (check) {
            PrimitiveType otherPrimitive = PrimitiveType.createOrNull(typeInterface.getName());
            if (otherPrimitive != null) {
              return isAssignable(primitive, otherPrimitive, tolerantPrimitives);
            }
          }
        }
      }
    }
    InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
    Optional<TypeInterface> typeInHierarchy = Optional.of(typeInterface);
    while (typeInHierarchy.isPresent()) {
      if (matches(typeInHierarchy.get())) {
        return true;
      } else {
        typeInHierarchy = resCtx.getSupertype(typeInHierarchy.get());
      }
    }
    return false;
  }
  
  
  private static boolean isAssignable(PrimitiveType one, PrimitiveType other, boolean tolerantPrimitives) {
    return true; //in ModelledExpression.JavaCodeGeneratorVisitor.transformation werden alle ineinander transformiert und damit kompatibel gemacht
  }


  public Boolean getJavaBaseType() {
    return javaBaseType;
  }
  
  public Boolean isList() {
    return isList;
  }
  
  public Set<TypeOfUsage> getTypesOfUsage() {
    if (typesOfUsage == null) {
      return Collections.emptySet();
    }
    return typesOfUsage;
  }

  public String getDescription() {
    return (getType() == null ? "" : (getType().toString() + " ")) + getName() + ((isList == null || !isList)? "" : "[]");
  }
  
  public static TypeInterface anyType() {
    return ANY_TYPE;
  }
  

  public static TypeInterface of(String fqName, String specialType) {
    TypeInterface to = TypeInterface.of(fqName);
    to.setSpecialType(specialType);
    return to;
  }

  public boolean resolve() {
    InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
    DeploymentItemState dis = resCtx.resolveProvider(this); //callsites eintragen
    if (isReservedObject()) {
      return true;
    } else {
      InterfaceResolutionContext.updateCtx(dis);
      try {
        if (dis.exists()) {
          return resCtx.getPublishedInterfaces().containsType(this);
        } else {
          return false;
        }
      } finally {
        InterfaceResolutionContext.revertCtx();
      }
    }
  }
  
  
  public boolean isJavaBaseType() {
    if (this == ANY_TYPE) {
      return true;
    } else if (javaBaseType == null) {
      return PrimitiveType.createOrNull(getName()) != null;
    } else {
      return javaBaseType;
    }
  }
  
  
  public boolean isUntyped() {
    return javaBaseType == null && (getName() == null || getName().isEmpty()); 
  }
  
  
  public static TypeInterface of(PrimitiveType type, boolean isList) {
    switch (type) {
      case EXCEPTION :
        return of(GenerationBase.getXmlNameForReservedClass(Exception.class), XMOMType.EXCEPTION, isList);
      case XYNAEXCEPTION :
        return of(GenerationBase.getXmlNameForReservedClass(XynaException.class), XMOMType.EXCEPTION, isList);
      case XYNAEXCEPTIONBASE :
        return of(GenerationBase.getXmlNameForReservedClass(XynaExceptionBase.class), XMOMType.EXCEPTION, isList);
      case ANYTYPE :
        return anyType();
      default :
        return of(type.getClassOfType(), true, isList);
    }
  }
  
  public static TypeInterface of(AVariable aVar, XMOMType type) {
    return of(aVar.getOriginalPath() + '.' + aVar.getOriginalName(), type, aVar.isList());
  }
  
  public static TypeInterface of(String fqName) {
    return new TypeInterface(fqName);
  }
  
  public static TypeInterface of(String fqName, boolean isJavaBaseType, boolean isList) {
    return new TypeInterface(fqName, isJavaBaseType, isList);
  }
  
  public static TypeInterface of(String fqName, XMOMType type) {
    return of(fqName, type, false);
  }
  
  public static TypeInterface of(String fqName, XMOMType type, boolean isList) {
    return new TypeInterface(fqName, type, isList);
  }
  
  public static TypeInterface of(GenerationBase gb) {
    return new TypeInterface(gb.getOriginalFqName(), XMOMType.getXMOMTypeByGenerationInstance(gb));
  }
  
  public static TypeInterface of(GenerationBase gb, TypeOfUsage type) {
    TypeInterface ti = new TypeInterface(gb.getOriginalFqName(), XMOMType.getXMOMTypeByGenerationInstance(gb));
    if (ti.typesOfUsage == null) {
      ti.typesOfUsage = new HashSet<TypeOfUsage>(1);
    }
    ti.typesOfUsage.add(type);
    return ti;
  }
  

  public static TypeInterface of(SupertypeInterface supertype) {
    if (supertype.getType() == null) {
      return TypeInterface.of(supertype.getName());
    } else {
      return TypeInterface.of(supertype.getName(), supertype.getType());
    }
  }

  public static TypeInterface of(Variable variable) {
    boolean isList = variable.getBaseVariable().getTypeInfo(false).isList();
    TypeInfo typeInfo = variable.getBaseVariable().getTypeInfo(true);
    return of(typeInfo, isList);
  }
  
  public static TypeInterface of(VariableInfo varInfo) {
    boolean isList = varInfo.getTypeInfo(false).isList();
    return of(varInfo.getTypeInfo(true), isList);
  }
  
  public static TypeInterface of(TypeInfo typeInfo, boolean isList) {
    if (typeInfo.isModelledType()) {
      return TypeInterface.of(((StepBasedType)typeInfo.getModelledType()).getGenerationType().getOriginalFqName(), false, isList);
    } else {
      if (typeInfo.getJavaName().equals("Object")) {
        return TypeInterface.anyType();
      } else if (typeInfo.getJavaName().equals("List")) {
        return TypeInterface.anyType(); //FIXME
      } else if (typeInfo.getJavaName().equals("null")) {
        return TypeInterface.ANY_TYPE; //ist das sinnvoll??
      }
      return TypeInterface.of(typeInfo.getJavaName(), true, isList);
    }
  }
  
  public static TypeInterface of(TypeInterface type, Boolean asList) {
    if (type.javaBaseType == null) {
      if (asList == null) {
        return new TypeInterface(type.getName());
      } else {
        return new TypeInterface(type.getName(), null, asList);
      }
    } else {
      if (asList == null) {
        return new TypeInterface(type.getName(), type.javaBaseType);
      } else {
        return new TypeInterface(type.getName(), type.javaBaseType, asList);
      }
    }
  }
  
  
  public static TypeInterface of(AVariable aVar) throws AvariableNotResolvableException {
    return of(aVar, TypeOfUsage.EMPLOYMENT);
  }
  
  public static class AvariableNotResolvableException extends Exception {

    private static final long serialVersionUID = 1L;
    
    private final DeploymentItemInterface problem;
    
    public AvariableNotResolvableException(DeploymentItemInterface problem) {
      this.problem = problem;
    }

    public DeploymentItemInterface getProblem() {
      return problem;
    }
    
  }

  
  public static TypeInterface of(AVariable aVar, TypeOfUsage type) throws AvariableNotResolvableException {
    TypeInterface newType;
    if (aVar.isJavaBaseType()) {
      if (aVar.getJavaTypeEnum() == null) {
        throw new AvariableNotResolvableException(new UnresolvableInterface(type, aVar.getVarName(), null));
      }
      newType = of(aVar.getJavaTypeEnum(), aVar.isList());
    } else {
      newType = of(aVar.getOriginalPath() + '.' + aVar.getOriginalName(), aVar instanceof ExceptionVariable ? XMOMType.EXCEPTION : XMOMType.DATATYPE, aVar.isList());
    }
    if (type != null) {
      if (newType.typesOfUsage == null) {
        newType.typesOfUsage = new HashSet<TypeOfUsage>(1);
      }
      newType.typesOfUsage.add(type);
    }
    return newType;
  }

}
