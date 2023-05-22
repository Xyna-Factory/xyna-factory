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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;


public class UnresolvableInterface implements DeploymentItemInterface {
  
  private final TypeOfUsage typeOfUsage;
  private final String id;
  private final Integer stepId;
  
  public UnresolvableInterface(TypeOfUsage typeOfUsage, String id, Integer stepId) {
    this.typeOfUsage = typeOfUsage;
    this.id = id;
    if (stepId == null) {
      this.stepId = -1;
    } else {
      this.stepId = stepId;
    }
  }

  public boolean resolve() {
    return false;
  }
  
  @Override
  public int hashCode() {
    return 31 * (1 + typeOfUsage.ordinal() + id.hashCode() + stepId);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof UnresolvableInterface)) {
      return false;
    }
    UnresolvableInterface other = (UnresolvableInterface) obj;
    return typeOfUsage.equals(other.typeOfUsage) &&
           id.equals(other.id) &&
           stepId.equals(other.stepId);
  }

  public static DeploymentItemInterface get(TypeOfUsage type, String id, Integer stepId) {
    return new UnresolvableInterface(type, id, stepId);
  }
  
  
  @Override
  public String toString() {
    return "UNRESOLVABLE " + typeOfUsage.name() + " " + id;
  }

  
  public String getDescription() {
    if (id != null && id.length() > 0) {
      return typeOfUsage.name() + " " + id;
    }
    return typeOfUsage.name();
  }
  
  
  public TypeOfUsage getTypeOfUsage() {
    return typeOfUsage;
  }

  
  public String getId() {
    return id;
  }
  
  
  public Integer getStepId() {
    return stepId;
  }


  public static enum TypeOfUsage {
    
    /**
     * Input of a Service
     */
    INPUT, 
    /**
     * Output of a Service
     */
    OUTPUT, 
    /**
     * Member of a Datatype
     */
    EMPLOYMENT, 
    SERVICE_REFERENCE, 
    MODELLED_EXPRESSION, 
    ORDERTYPE, 
    /**
     * Supertype of a Datatype
     */
    SUPERTYPE;
  }
  
  
  public static class TypeMissmatch extends UnresolvableInterface {
    
    private final TypeInterface sourceType;
    private final TypeInterface targetType;
    private final boolean isCausedByTypeCast;
    
    public TypeMissmatch(TypeInterface sourceType, TypeInterface targetType, Integer xmlId, boolean isCausedByTypeCast) {
      super(TypeOfUsage.MODELLED_EXPRESSION, "", xmlId);
      this.sourceType = sourceType;
      this.targetType = targetType;
      this.isCausedByTypeCast = isCausedByTypeCast;
    }
    
    public TypeInterface getSourceType() {
      return sourceType;
    }
    
    public TypeInterface getTargetType() {
      return targetType;
    }
    
    @Override
    public int hashCode() {
      return super.hashCode() + sourceType.getName().hashCode() + targetType.getName().hashCode() + (isCausedByTypeCast ? 1 : 0);
    }
    
    @Override
    public String getDescription() {
      return isCausedByTypeCast ? targetType.getDescription() : super.getDescription();
    }

    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj instanceof TypeMissmatch) {
        if (super.equals(obj)) {
          TypeMissmatch other = (TypeMissmatch) obj;
          return sourceType.getName().equals((other.sourceType.getName())) &&
                 targetType.getName().equals((other.targetType.getName())) && 
                 other.isCausedByTypeCast == isCausedByTypeCast;          
        } else {
          return false;
        }
      } else {
        return false;
      }
    }

    public boolean isCausedByTypeCast() {
      return isCausedByTypeCast;
    }
    
  }
  
  
  public static class MissingVarId extends UnresolvableInterface {
    
    public MissingVarId(TypeOfUsage type, Integer xmlId) {
      super(type, "", xmlId);
    }
    
  }
  
  
  public static class PrototypeElement extends UnresolvableInterface {
    
    public PrototypeElement(TypeOfUsage type, Integer xmlId) {
      super(type, "", xmlId);
    }
    
  }
  
}
