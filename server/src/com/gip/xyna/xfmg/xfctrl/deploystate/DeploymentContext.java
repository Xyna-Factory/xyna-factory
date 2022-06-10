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
package com.gip.xyna.xfmg.xfctrl.deploystate;


import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

import com.gip.xyna.utils.collections.Optional;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase.XMOMType;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.DeploymentMode;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;


public class DeploymentContext {
  
  private final GenerationBaseCache cacheReference;
  private final Map<Long, Map<XMOMType, Map<String, DeploymentMode>>> additionalObjectsForCodeRegeneration;
  

  public DeploymentContext(GenerationBaseCache cacheReference) {
    this.cacheReference = cacheReference;
    this.additionalObjectsForCodeRegeneration = new HashMap<Long, Map<XMOMType, Map<String, DeploymentMode>>>();
  }
  

  public Map<Long, Map<XMOMType, Map<String, DeploymentMode>>> getAdditionalObjectsForCodeRegeneration() {
    return additionalObjectsForCodeRegeneration;
  }
  
  private static XMOMType getXMOMTypeByInstance(GenerationBase gb) {
    if (gb instanceof WF) {
      return XMOMType.WORKFLOW;
    } else if (gb instanceof DOM) {
      return XMOMType.DATATYPE;
    } else if (gb instanceof ExceptionGeneration) {
      return XMOMType.EXCEPTION;
    } else {
      throw new IllegalArgumentException("Unknown GenerationBase class: " + gb.getClass().getName());
    }
  }
  
  public Optional<DeploymentMode> getDeploymentMode(XMOMType type, String fqXmlName, long revision) {
    GenerationBase gb = cacheReference.getFromCache(fqXmlName, revision);
    if (gb != null && getXMOMTypeByInstance(gb).equals(type) && gb.getRevision() == revision) {
      return Optional.of(gb.getDeploymentMode());
    } else {
      Map<XMOMType, Map<String, DeploymentMode>> revisionMap = additionalObjectsForCodeRegeneration.get(revision);
      if (revisionMap == null) {
        return Optional.empty();
      }
      Map<String, DeploymentMode> subMap = revisionMap.get(type);
      if (subMap == null) {
        return Optional.empty();
      } else {
        return Optional.of(subMap.get(fqXmlName));
      }
    }
  }
  
  
  public void addObjectForCodeRegeneration(XMOMType type, String fqName, long revision) {
    Map<XMOMType, Map<String, DeploymentMode>> revisionMap = additionalObjectsForCodeRegeneration.get(revision);
    if (revisionMap == null) {
      revisionMap = new EnumMap<XMOMType, Map<String, DeploymentMode>>(XMOMType.class);
      additionalObjectsForCodeRegeneration.put(revision, revisionMap);
    }
    Map<String, DeploymentMode> subMap = revisionMap.get(type);
    if (subMap == null) {
      subMap = new HashMap<String, DeploymentMode>();
      revisionMap.put(type, subMap);
    }
    subMap.put(fqName, DeploymentMode.regenerateDeployedAllFeatures);
  }
  
  
  public static DeploymentContext empty() {
    return new DeploymentContext(new GenerationBaseCache());
  }
  
  public static DeploymentContext dummy() {
    return new DeploymentContext(null) {
      @Override
      public Optional<DeploymentMode> getDeploymentMode(XMOMType type, String fqName, long revision) {
        return Optional.<DeploymentMode>empty();
      }
      @Override
      public void addObjectForCodeRegeneration(XMOMType type, String fqName, long revision) { }
    };
  }
  
}

