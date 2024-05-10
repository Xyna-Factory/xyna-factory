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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyNode;
import com.gip.xyna.xfmg.xfctrl.dependencies.DependencyRegister.DependencySourceType;
import com.gip.xyna.xprc.exceptions.XPRC_InheritedConcurrentDeploymentException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;


public abstract class DomOrExceptionGenerationBase extends GenerationBase implements HasDocumentation {

  private String documentation = "";
  private boolean isAbstract = false;
  private boolean cacheSubTypes = false;
  private Set<GenerationBase> subTypeCache;
  protected List<String> unknownMetaTags;

  public String getDocumentation() {
    return documentation;
  }

  public void setDocumentation(String documentation) {
    this.documentation = documentation;
  }

  @Override
  public boolean isAbstract() {
    return isAbstract;
  }

  public void setIsAbstract(boolean isAbstract) {
    this.isAbstract = isAbstract;
  }
  
  public void setCacheSubTypes(boolean cacheSubTypes) {
    this.cacheSubTypes = cacheSubTypes;
    if (!cacheSubTypes) {
      subTypeCache = null;
    }
  }
  
  public List<String> getUnknownMetaTags() {
    return unknownMetaTags;
  }
  
  public void setUnknownMetaTags(List<String> unknownMetaTags) {
    this.unknownMetaTags = unknownMetaTags;
  }

  protected DomOrExceptionGenerationBase(String originalClassName, String fqClassName, Long revision) {
    super(originalClassName, fqClassName, revision);
  }
  
  protected DomOrExceptionGenerationBase(String originalClassName, String fqClassName, GenerationBaseCache cache, Long revision, String realType, XMLSourceAbstraction inputSource) {
    super(originalClassName, fqClassName, cache, revision, realType, inputSource);
  }
  
  /**
   * reihenfolge so wie im xml angegeben
   */
  public abstract List<AVariable> getMemberVars();
  
  public abstract boolean replaceMemberVar(AVariable oldVar, AVariable newVar);
  
  public abstract void addMemberVar(int index, AVariable var);
  
  public abstract boolean removeMemberVar(AVariable var);
  
  public abstract AVariable removeMemberVar(int index);
  
  /**
   * reihenfolge: membervars von oberklasse zuerst. (je höher in hierarchie, desto früher werden die vars angegeben).
   * bei gleicher hierarchiestufe gilt die reihenfolge aus dem jeweiligen xml. 
   */
  public abstract List<AVariable> getAllMemberVarsIncludingInherited();
  public abstract Set<GenerationBase> getDirectlyDependentObjects();
  public abstract DomOrExceptionGenerationBase getSuperClassGenerationObject();
  public abstract boolean hasSuperClassGenerationObject();

  public static boolean isSuperClass(DomOrExceptionGenerationBase superType, DomOrExceptionGenerationBase subType) {
    if (subType.equals(superType)) {
      return true;
    }
    DomOrExceptionGenerationBase superClassOfSubType = subType.getSuperClassGenerationObject();
    if (superClassOfSubType == null) {
      return false;
    }
    return isSuperClass(superType, superClassOfSubType);
  }

  public long calculateSerialVersionUID() {
    List<Pair<String, String>> types = new ArrayList<Pair<String, String>>();
    for (AVariable v : getMemberVars()) {
      types.add(Pair.of(v.getVarName(), v.getUniqueTypeName()));
    }
    return GenerationBase.calcSerialVersionUID(types);
  }

  public Set<GenerationBase> getSubTypes(GenerationBaseCache parseAdditionalCache) {
    return getSubTypes(parseAdditionalCache, true);
  }

  public Set<GenerationBase> getSubTypes(GenerationBaseCache parseAdditionalCache, boolean resolveFromGlobalCache) {
    if (cacheSubTypes) {
      if (subTypeCache == null) {
        subTypeCache = getSubTypesInternally(parseAdditionalCache, resolveFromGlobalCache);
      }
      return subTypeCache;
    } else {
      return getSubTypesInternally(parseAdditionalCache, resolveFromGlobalCache);
    }
  }
    
  private Set<GenerationBase> getSubTypesInternally(GenerationBaseCache parseAdditionalCache, boolean resolveFromGlobalCache) {
    //dependencyregister bemühen. das mag zu beginn nicht vollständig sein, aber zu dem zeitpunkt sind die subtypen dann halt noch nicht bekannt. ok

    Set<GenerationBase> ret = new HashSet<GenerationBase>();

    //FIXME performance!!!! eigtl wäre es hier schön, wenn man die xmomdatabase verwenden könnte. man benötigt aber die informationen aus dem deployed ordner
    Set<DependencyNode> dependencies =
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getDependencyRegister()
            .getDependencies(getOriginalFqName(), getDependencySourceType(), revision, false);

    for (DependencyNode dn : dependencies) {
      if (dn.getType() == getDependencySourceType() && !dn.getUniqueName().equals(getOriginalFqName())) {
        addIfSubType(ret, dn, parseAdditionalCache, resolveFromGlobalCache, true);
      }
    }

    return ret;
  }
  
  private void addIfSubType(Set<GenerationBase> ret, DependencyNode dn, GenerationBaseCache parseAdditionalCache,
                                         boolean resolveFromGlobalCache, boolean recursive) {
    DomOrExceptionGenerationBase domOrException;
    try {
      //globalcached funktioniert hier nicht gut, weil der deploymentmode dann falsch gesetzt wird
      if (this instanceof ExceptionGeneration) {
        domOrException = ExceptionGeneration.getOrCreateInstance(dn.getUniqueName(), parseAdditionalCache, dn.getRevision());;
      } else {
        domOrException = DOM.getOrCreateInstance(dn.getUniqueName(), parseAdditionalCache, dn.getRevision());
      }
      domOrException.parseGeneration(true, false, false);
    } catch (XPRC_InvalidPackageNameException | XPRC_InheritedConcurrentDeploymentException | AssumedDeadlockException | XPRC_MDMDeploymentException e) {
      throw new RuntimeException(e);
    }
    if (isSuperClass(this, domOrException)) {
      if (resolveFromGlobalCache) {
        try {
          if (this instanceof ExceptionGeneration) {
            domOrException = getCachedExceptionInstanceOrCreate(dn.getUniqueName(), dn.getRevision());
          } else {
            domOrException = getCachedDOMInstanceOrCreate(dn.getUniqueName(), dn.getRevision());
          }
        } catch (XPRC_InvalidPackageNameException e) {
          throw new RuntimeException(e);
        }
      }
      if (recursive) {
        //cache checken/verwenden
        ret.addAll(domOrException.getSubTypes(parseAdditionalCache, resolveFromGlobalCache));
      }
      ret.add(domOrException);
    }
  }

  protected abstract DependencySourceType getDependencySourceType();

}
