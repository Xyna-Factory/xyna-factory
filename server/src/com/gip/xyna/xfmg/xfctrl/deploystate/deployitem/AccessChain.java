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
package com.gip.xyna.xfmg.xfctrl.deploystate.deployitem;

import java.util.Objects;

import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemInterface;
import com.gip.xyna.xfmg.xfctrl.deploystate.DeploymentItemState;
import com.gip.xyna.xfmg.xfctrl.deploystate.InconsistencyProvider;
import com.gip.xyna.xfmg.xfctrl.deploystate.InconsistencyProvider.InconsistencyProviderWithTypeInference;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeOfUsage;
import com.gip.xyna.xfmg.xfctrl.deploystate.deployitem.UnresolvableInterface.TypeMissmatch;


public class AccessChain extends TypeInterface {
  
  protected final TypeInterface rootType;
  protected AccessPart rootPart;
  
  AccessChain(TypeInterface rootType) {
    super(null);
    this.rootType = rootType;
  }
  
  AccessChain(TypeInterface rootType, AccessPart rootPart) {
    this(rootType);
    this.rootPart = rootPart;
  }
  
  
  @Override
  public boolean matches(DeploymentItemInterface other) {
    ResolutionResult result = rootPart.resolve(rootType);
    if (result.wasSuccessfull()) {
      return result.resolvedType.matches(other);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    int h = super.hashCode();
    return Objects.hash(h, rootType, rootPart);
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof AccessChain)) {
      return false;
    }
    AccessChain other = (AccessChain)obj; 
    if (rootType != null) {
      if (other.rootType != null) {
        if (!rootType.equals(other.rootType)) {
          return false;
        }
      } else {
        return false;
      }
    }
    if (rootPart != null) {
      if (other.rootPart != null) {
        if (!rootPart.equals(other.rootPart)) {
          return false;
        }
      } else {
        return false;
      }
    }
    return true;
  }


  @Override
  public String getName() {
    if (rootPart == null) {
      return rootType.getName();
    } else {
      try {
        return rootPart.resolve(rootType).resolvedType.getName();
      } catch (NullPointerException npe) {
        // can happen if we get asked for a description on Inconsistency<init>
        return "?";
      }
    }
  }
  
  
  public void append(AccessPart part) {
    if (rootPart == null) {
      rootPart = part;
    } else {
      rootPart.append(part);
    }
  }
  
  @Override
  public boolean resolve() {
    if (rootPart == null) {
      return false;
    }
    ResolutionResult result = rootPart.resolve(rootType);
    return result.wasSuccessfull();
  }
  
  
  public ResolutionResult resolveChain() {
    if (rootPart == null) {
      return new ResolutionResult(rootType);
    }
    return rootPart.resolve(rootType);
  }
 
  
  public TypeInterface getRootType() {
    return rootType;
  }

  
  public AccessPart getRootPart() {
    return rootPart;
  }
  
  
  public String getDescription() {
    return getName();
  }
  
  
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(rootType.getName());
    if (rootPart != null) {
      sb.append('.')
        .append(rootPart.toString());
    }
    return sb.toString();
  }
  
  
  public static interface AccessPart extends DeploymentItemInterface {
    
    public ResolutionResult resolve(TypeInterface type);
    
    public void setNext(AccessPart next);
    
    public AccessPart getNext();
    
    public boolean hasNext();
    
    public void append(AccessPart next);
    
    
  }
  
  public static abstract class LinkedAccessPart implements AccessPart {
    
    protected AccessPart next;
    
    public ResolutionResult resolve(TypeInterface type) {
      ResolutionResult result = resolveInternally(type);
      if (!result.wasSuccessfull()) {
        return result;
      }
      if (hasNext()) {
        if (result.resolvedType.isList()) {
          //resolvedType ist nur dann listenwertig, wenn kein listen-access ([...]) folgt (das wird bereits in resolveinternally aufgelöst)
          //die accesschain darf bei einem ausdruck, der listenwertig endet, also keinen folgenden accesspart haben 
          return new ResolutionResult(InterfaceEmployment.of(type, next), false);
        } else {
          return next.resolve(result.resolvedType);
        }
      } else {
        return result;
      }
    }
    
    public abstract ResolutionResult resolveInternally(TypeInterface type);
    
    public void setNext(AccessPart next) {
      this.next = next;
    }
    
    public void append(AccessPart next) {
      AccessPart current = this;
      while (current.hasNext()) {
        current = current.getNext();
      }
      current.setNext(next);
    }
    
    public AccessPart getNext() {
      return next;
    }
    
    public boolean hasNext() {
      return next != null;
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(next);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof LinkedAccessPart)) {
        return false;
      }
      LinkedAccessPart other = (LinkedAccessPart)obj;
      if (next != null) {
        if (other.next != null) {
          if (!next.equals(other.next)) {
            return false;
          }
        } else {
          return false;
        }
      }
      return true;
    }
    
  }
  
  
  public static abstract class InterfaceWrappingAccessPart<I extends DeploymentItemInterface> extends LinkedAccessPart {
    
    protected final I wrapped;
    
    
    InterfaceWrappingAccessPart(I wrapped) {
      this.wrapped = wrapped;
    }
    

    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + ((wrapped == null) ? 0 : wrapped.hashCode());
      return result;
    }



    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (getClass() != obj.getClass())
        return false;
      InterfaceWrappingAccessPart other = (InterfaceWrappingAccessPart) obj;
      if (wrapped == null) {
        if (other.wrapped != null)
          return false;
      } else if (!wrapped.equals(other.wrapped))
        return false;
      if (!super.equals(obj))
        return false;
      return true;
    }



    public boolean resolve() {
      return wrapped.resolve();
    }


    public String getDescription() {
      return wrapped.getDescription();
    }
    
    
    public I unwrap() {
      return wrapped;
    }
    
  }
  
  
  public static AccessChain of(TypeInterface rootType, AccessPart rootPart) {
    return new AccessChain(rootType, rootPart);
  }
  
  public static class MemberVarAccessPart extends InterfaceWrappingAccessPart<MemberVariableInterface> {

    boolean withListAccess;
    // TODO more like expectSingleAndReturnAsList 
    // we would currently accept a list as the second parameter of an appendToList wouldn't we? 
    boolean asList; 
    
    private MemberVarAccessPart(MemberVariableInterface wrapped, boolean withListAccess) {
      super(wrapped);
      this.withListAccess = withListAccess;
    }
    
    private MemberVarAccessPart(MemberVariableInterface wrapped, boolean withListAccess, boolean asList) {
      this(wrapped, withListAccess);
      this.asList = asList;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = super.hashCode();
      result = prime * result + (asList ? 1231 : 1237);
      result = prime * result + (withListAccess ? 1231 : 1237);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (getClass() != obj.getClass())
        return false;
      MemberVarAccessPart other = (MemberVarAccessPart) obj;
      if (asList != other.asList)
        return false;
      if (withListAccess != other.withListAccess)
        return false;
      if (!super.equals(obj))
        return false;
      return true;
    }

    @Override
    public ResolutionResult resolveInternally(TypeInterface type) {
      if (type == null) {
        return null;
      }
      InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
      DeploymentItemState dis = resCtx.resolveProvider(type);
      if (!dis.exists()) {
        return new ResolutionResult(InterfaceEmployment.of(type, wrapped), !hasNext());
      }
      InterfaceResolutionContext.updateCtx(dis);
      try {
        MemberVariableInterface member = (MemberVariableInterface) resCtx.findInPublishedInterfacesInHierarchyOfType(wrapped);
        if (member != null) {
          ResolutionResult result;
          if (withListAccess) {
            result = new ResolutionResult(TypeInterface.of(member.getType(), false));
          } else {
            result = new ResolutionResult(member.getType());
          }
          if (asList) {
            return new ResolutionResult(TypeInterface.of(result.resolvedType, true));
          } else {
            return result;
          }
        }
        return new ResolutionResult(InterfaceEmployment.of(type, wrapped), !hasNext());
      } finally {
        InterfaceResolutionContext.revertCtx();
      }
    }
    
    public static MemberVarAccessPart of(MemberVariableInterface wrapped, boolean withListAccess) {
      return new MemberVarAccessPart(wrapped, withListAccess);
    }
    
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(wrapped.getName());
      if (hasNext()) {
        sb.append('.')
          .append(next.toString());
      }
      return sb.toString();
    }
    
  }
  
  
  public static class InstanceMethodAccessPart extends InterfaceWrappingAccessPart<OperationInterface> {

    private InstanceMethodAccessPart(OperationInterface wrapped) {
      super(wrapped);
    }

    @Override
    public ResolutionResult resolveInternally(final TypeInterface type) {
      if (type == null) {
        return new ResolutionResult(UnresolvableInterface.get(TypeOfUsage.MODELLED_EXPRESSION, "UNKNOWN", 0), !hasNext());
      }
      InterfaceResolutionContext resCtx = InterfaceResolutionContext.resCtx.get();
      DeploymentItemState dis = resCtx.resolveProvider(type);
      if (!dis.exists()) {
        return new ResolutionResult(InterfaceEmployment.of(type, wrapped), !hasNext());
      }
      InterfaceResolutionContext.updateCtx(dis);
      try {
        /*
         * TODO unschön, hier jedesmal (aus performancegründen) clonen zu müssen. 
         * verbesserungsidee:
         * 1) instanzmethoden haben den ersten parameter nie in der signatur - dazu müsste man bei alle step-function aufrufen
         *    und bei den service-definition den ersten parameter entsprechend entfernen
         */
        OperationInterface clonedOperation = wrapped.clone();
        clonedOperation.getInput().add(0, type);
        
        OperationInterface op = (OperationInterface) resCtx.findInPublishedInterfacesInHierarchyOfType(clonedOperation);
        if (op != null) {
          if (op.getOutput().size() > 0) {
            if (op.getOutput().size() > 1) {
              // TODO XFL parsing should have not allowed this? warn about it or consider it as unresolvable
            }
            return new ResolutionResult(op.getOutput().get(0));
          } else {
            // TODO return void
            return new ResolutionResult(TypeInterface.of("void"));
          }
        }
        return new ResolutionResult(InterfaceEmployment.of(type, wrapped), !hasNext());
      } finally {
        InterfaceResolutionContext.revertCtx();
      }
    }

    
    public static InstanceMethodAccessPart of(OperationInterface wrapped) {
      return new InstanceMethodAccessPart(wrapped);
    }
    
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(wrapped.getName())
        .append("()");
      if (hasNext()) {
        sb.append('.')
          .append(next.toString());
      }
      return sb.toString();
    }
    
  }
  
  
  public static class AccessChainedAssignment implements DeploymentItemInterface, InconsistencyProviderWithTypeInference {
    
    private final AccessChain source;
    private final AccessChain target;
    private final Integer xmlId;
    
    private AccessChainedAssignment(AccessChain source, AccessChain target, Integer xmlId) {
      this.source = source;
      this.target = target;
      this.xmlId = xmlId;
    }

    public boolean resolve() {
      ResolutionResult sourceResult = source.resolveChain();
      if (sourceResult.wasSuccessfull()) {
        ResolutionResult targetResult = target.resolveChain();
        if (targetResult.wasSuccessfull()) {
          return sourceResult.resolvedType.isAssignableFrom(targetResult.resolvedType, true) ||
                 targetResult.resolvedType.isAssignableFrom(sourceResult.resolvedType, true); // UpCast
        } else {
          return false;
        }
      } else {
        return false;
      }
    }

    public AccessChain getSource() {
      return source;
    }
    
    public AccessChain getTarget() {
      return target;
    }
    
    public String getDescription() {
      return target.getDescription() + " = " + source.getDescription();
    }
    
    public Integer getXmlId() {
      return xmlId;
    }

    public DeploymentItemInterface getInconsistency() {
      return getInconsistency(false);
    }
    
    public DeploymentItemInterface getInconsistency(boolean inferTypes) {
      ResolutionResult sourceResult = source.resolveChain();
      if (sourceResult.wasSuccessfull()) {
        ResolutionResult targetResult = target.resolveChain();
        if (targetResult.wasSuccessfull()) {
          if (!sourceResult.resolvedType.isAssignableFrom(targetResult.resolvedType, true) &&
              !targetResult.resolvedType.isAssignableFrom(sourceResult.resolvedType, true)) { // UpCast
            return new TypeMissmatch(sourceResult.resolvedType, targetResult.resolvedType, xmlId, false);
          } else {
            //inkonsistenz
            throw new RuntimeException(source + " seems to be assignable to " + target + ". This is not expected here.");
          }
        } else {
          if (inferTypes && targetResult.inconsistencyInLastPart) {
            tryToInferEmployedMemberVariableType(targetResult, sourceResult);
          }
          return targetResult.inconsistency;
        }
      } else {
        if (inferTypes) {
          tryToInferEmployedMemberVariableType(sourceResult, target);
        }
        return sourceResult.inconsistency;
      }
    }
    
    private void tryToInferEmployedMemberVariableType(ResolutionResult unresolvedResult, AccessChain unevaluatedChain) {
      if (unresolvedResult.inconsistencyInLastPart &&
          unresolvedResult.inconsistency instanceof InterfaceEmployment) {
        ResolutionResult targetResult = unevaluatedChain.resolveChain();
        if (targetResult.wasSuccessfull()) {
          tryToInferEmployedMemberVariableType(unresolvedResult, targetResult);
        }
      } 
    }
    
    private void tryToInferEmployedMemberVariableType(ResolutionResult unresolvedResult, ResolutionResult resolvedResult) {
      MemberVariableInterface untypedInterface;
      if (unresolvedResult.inconsistency instanceof InterfaceEmployment) {
        InterfaceEmployment ie = (InterfaceEmployment)unresolvedResult.inconsistency;
        if (ie.unwrap() instanceof MemberVariableInterface) {
          ie.cloneWrapped(); //unten stehendes type-infer muss auf clone passieren, weil die information von der auflösbarkeit der typen abhängt. die ist nicht immer gegeben.
          untypedInterface = (MemberVariableInterface) ie.unwrap();
        } else {
          return;
        }
      } else {
        return;
      }
      if (untypedInterface.isUntyped()) {
        untypedInterface.inferType(resolvedResult.resolvedType);
      }
    }
    
    public String toString() {
      return "(" + xmlId + ") <" + source.toString() + "> must be compatible to type of <" + target.toString() + ">"; 
    }

    @Override
    public int hashCode() {
      return Objects.hash(source,target,xmlId);
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (!(obj instanceof AccessChainedAssignment)) {
        return false;
      }
      AccessChainedAssignment other = (AccessChainedAssignment)obj;
      return xmlId.equals(other.xmlId) &&
             target.equals(other.target) &&
             source.equals(other.source);
    }
    
  }
  
  
  public static class StaticChainResultType implements DeploymentItemInterface, InconsistencyProvider {
    
    private final TypeInterface source;
    private final AccessChain target;
    private final Integer xmlId;
    private final boolean isCausedByTypeCast;
    
    private StaticChainResultType(TypeInterface source, AccessChain target, Integer xmlId, boolean isCausedByTypeCast) {
      this.source = source;
      this.target = target;
      this.xmlId = xmlId;
      this.isCausedByTypeCast = isCausedByTypeCast;
    }

    public boolean resolve() {
      ResolutionResult targetResult = target.resolveChain();
      if (targetResult.wasSuccessfull()) {
        if (source.matches(targetResult.resolvedType)) {
          return true;
        }
        return source.isAssignableFrom(targetResult.resolvedType, true) ||
               targetResult.resolvedType.isAssignableFrom(source, true);
      } else {
        return false;
      }
    }

    public String getDescription() {
      return target.getDescription();
    }
    
    public Integer getXmlId() {
      return xmlId;
    }
    
    public TypeInterface getSource() {
      return source;
    }
    
    public AccessChain getTarget() {
      return target;
    }

    public DeploymentItemInterface getInconsistency() {
      ResolutionResult targetResult = target.resolveChain();
      if (targetResult.wasSuccessfull()) {
        if (!source.isAssignableFrom(targetResult.resolvedType, true) &&
            !targetResult.resolvedType.isAssignableFrom(source, true)) { // UpCast
          return new TypeMissmatch(source, targetResult.resolvedType, xmlId, isCausedByTypeCast);
        } else {
          return new UnresolvableInterface(TypeOfUsage.MODELLED_EXPRESSION, "It appears we were unresolvable but do not deliver a missmatch?", xmlId);
        }
      } else {
        return targetResult.inconsistency;
      }
    }

    public String toString() {
      return "(" + xmlId + ") " + target.toString() + " must be of type " + source.toString();
    }

    public boolean isCausedByTypeCast() {
      return isCausedByTypeCast;
    }
    
  }
  
  
  public static class ResolutionResult {
    
    private TypeInterface resolvedType;
    private DeploymentItemInterface inconsistency;
    private boolean inconsistencyInLastPart;
    
    ResolutionResult(TypeInterface resolvedType) {
      this.resolvedType = resolvedType;
    }
    
    ResolutionResult(DeploymentItemInterface inconsistency, boolean inLastPart) {
      this.inconsistency = inconsistency;
      this.inconsistencyInLastPart = inLastPart;
    }
    
    public boolean wasSuccessfull() {
      return resolvedType != null; 
    }
    
    public TypeInterface getResolvedType() {
      return resolvedType;
    }
    
  }
  

  public static DeploymentItemInterface of(AccessChain source, AccessChain target, Integer xmlId) {
    return new AccessChainedAssignment(source, target, xmlId);
  }
  
  public static DeploymentItemInterface of(TypeInterface source, AccessChain target, Integer xmlId, boolean isCausedByTypeCast) {
    return new StaticChainResultType(source, target, xmlId, isCausedByTypeCast);
  }
  
}
