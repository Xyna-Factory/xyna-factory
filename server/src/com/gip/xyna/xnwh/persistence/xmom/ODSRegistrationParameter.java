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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.xnwh.persistence.xmom.PathBuilder.FqPathPart;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;

public class ODSRegistrationParameter {
  
  private final String fqxmlname;
  private final String fqpath;
  private final String odsname;
  private final boolean beforeDeployment;
  private long revision;
  private boolean isTableRegistration;
  private String tablename;
  private GenerationBaseCache cache;
  
  public ODSRegistrationParameter(DOM dom, String fqpath, String odsname, boolean beforeDeployment, GenerationBaseCache cache) {
    this.fqxmlname = dom.getOriginalFqName();
    this.revision = dom.getRevision();
    this.fqpath = fqpath;
    this.odsname = odsname;
    this.beforeDeployment = beforeDeployment;
    this.cache = cache;
    initTableRegistration(dom);
  }

  public ODSRegistrationParameter(String fqXmlName, Long revision, String fqpath, String tablename, String columnname, boolean beforeDeployment) {
    this.fqxmlname = fqXmlName;
    this.revision = revision;
    this.fqpath = fqpath;
    this.odsname = columnname == null || columnname.isBlank() ? tablename : columnname;
    this.tablename = tablename;
    this.beforeDeployment = beforeDeployment;
    isTableRegistration = columnname == null;
  }
  
  
  public String getFqxmlname() {
    return fqxmlname;
  }
  
  public long getRevision() {
    return revision;
  }
  
  public void adjustRevision(Long target) {
    this.revision = target;
  }
  
  public String getFqpath() {
    return fqpath;
  }
  
  public String getOdsName() {
    return odsname;
  }
  
  public boolean isBeforeDeployment() {
    return beforeDeployment;
  }
  
  public String getTableName() {
    if (isTableRegistration) {
      return odsname; // or throw 
    } else {
      return tablename;
    }
  }
  
  public boolean isTableRegistration() {
    return isTableRegistration;
  }
  
  private void initTableRegistration(DOM dom) {
    if (fqpath == null ||
        fqpath.isBlank()) {
      isTableRegistration = true;
    } else {
      AVariable aVar = resolvePath(dom, fqpath);
      isTableRegistration = aVar == null ? false : !aVar.isJavaBaseType() || aVar.isList();
      if (!isTableRegistration) {
        XMOMStorableStructureInformation xssi = XMOMStorableStructureCache.getInstance(revision).getStructuralInformation(fqxmlname);
        // find table name in structure cache
        String shortenedFqPath = shortenFqPath(fqpath);
        while (tablename == null) {
          if (shortenedFqPath.isBlank()) {
            tablename = xssi.getTableName();  
          } else {
            StorableColumnInformation sci = followPath(xssi, shortenedFqPath);
            if (sci == null || // could be a shortened flat-path
                sci.isFlattened()) { // could be a shortened self-collision
              // continue shortening until we arrive at an unflattened column 
              shortenedFqPath = shortenFqPath(shortenedFqPath);
            } else {
              tablename = sci.getStorableVariableInformation().getTableName();
            }
          }
        }
      }
    }
  }

  
  private String shortenFqPath(String fqpath) {
    List<FqPathPart> parts = splitFqPath(fqpath);
    if (parts.size() <= 1) {
      return "";
    } else {
      parts.remove(parts.size() - 1);
      return parts.stream().map(p -> p.getVarName() + "{" + p.getTypeName() + "}").collect(Collectors.joining("."));
    }
  }

  
  private AVariable resolvePath(DOM dom, String fqpath) {
    String unlistedFqPath = fqpath.replaceAll("\\[\\]", "");
    List<FqPathPart> parts = splitFqPath(unlistedFqPath);
    return resolvePathRecursivly(dom, parts, 0);
  }
  
  
  private static List<FqPathPart> splitFqPath(String fqpath) {
    ArrayList<String> parts = new ArrayList<>();
    StringBuilder partBuilder = new StringBuilder();
    boolean readingType = false;
    for (int i = 0; i < fqpath.length(); i++) {
      char currentChar = fqpath.charAt(i);
      switch (currentChar) {
        case '.' :
          if (!readingType) {
            parts.add(partBuilder.toString());
            partBuilder = new StringBuilder();
          } else {
            partBuilder.append(currentChar);
          }
          break;
        case '{' :
          partBuilder.append('{');
          readingType = true;
          break;
        case '}' :
          partBuilder.append('}');
          readingType = false;
          break;
        default :
          partBuilder.append(currentChar);
          break;
      }
    }
    parts.add(partBuilder.toString());
    return parts.stream().<FqPathPart>map(s -> PathBuilder.split(s)).collect(Collectors.toList());
  }

  
  private AVariable resolvePathRecursivly(DOM dom, List<FqPathPart> parts, int i) {
    FqPathPart currentPart = parts.get(i);
    
    AVariable currentVar = null;
    for (AVariable aVar : dom.getMemberVars()) {
      if (aVar.getVarName().equals(currentPart.getVarName())) {
        currentVar = aVar;
        break;
      }
    }
    if (currentVar == null) {
      return null;
    } else {
      if (i + 1 >= parts.size()) {
        return currentVar;
      } else {
        DOM nextDom = (DOM) currentVar.getDomOrExceptionObject();
        if(nextDom == null) {
          return null;
        }
        DOM specifiedType = findTypeInHierarchy(XMOMODSMappingUtils.getSuperRoot(nextDom), currentPart.getTypeName());
        AVariable result = resolvePathRecursivly(specifiedType, parts, i + 1);
        if (result != null) {
          return result;
        }
      }
    }
    return null;
  }
  
  
  private DOM findTypeInHierarchy(DOM dom, String type) {
    if (dom.getOriginalFqName().equals(type)) {
      return dom;
    } else {
      for (GenerationBase subType : dom.getSubTypes(cache)) {
        DOM result = findTypeInHierarchy((DOM) subType, type);
        if (result != null) {
          return result;
        }
      }
      return null;
    } 
  }
  
  
  public GenerationBaseCache getCache() {
    if (cache == null) {
      cache = new GenerationBaseCache();
    }
    return cache;
  }
  
  
  public static StorableColumnInformation followPath(StorableStructureInformation ssi, String fqPath) {
    fqPath = fqPath.replaceAll("\\[\\]", "");
    List<FqPathPart> pathParts = splitFqPath(fqPath);
    return followPathRecursivly(pathParts, 0, ssi);
  }
  
  
  public static StorableColumnInformation followPathRecursivly(List<FqPathPart> fqparts, int index, StorableStructureInformation ssi) {
    FqPathPart currentPart = fqparts.get(index);
    StorableColumnInformation col = resolve(fqparts, index, ssi);
    if (col == null) {
      //throw new IllegalArgumentException("Path-Part " + fqparts.get(index).varName + " could not be resolved ");
      return null;
    }
    if (index + 1 >= fqparts.size()) {
      return col;
    } else {
      StorableStructureInformation nextSsi = col.getStorableVariableInformation();
      StorableStructureInformation specifiedSsi = findTypeInHierarchy(nextSsi.getSuperRootStorableInformation(), currentPart.getTypeName());
      StorableColumnInformation result = followPathRecursivly(fqparts, index + 1, specifiedSsi);
      if (result != null) {
        return result;
      }
    }
    return null;
  }
  
  
  public static StorableColumnInformation resolve(List<FqPathPart> fqparts, int index, StorableStructureInformation ssi) {
    FqPathPart currentPart = fqparts.get(index);
    StorableColumnInformation col = ssi.getColumnInfo(currentPart.getVarName());
    if (col == null) {
      // check flattened
      int length = 2;
      search: while (index + length <= fqparts.size()) {
        String currentPath = fqparts.stream().skip(index).limit(index + length).map(p -> p.getVarName()).collect(Collectors.joining("."));
        for (StorableColumnInformation column : ssi.getColumnInfoAcrossHierarchy()) {
          if (column.isFlattened() && 
              column.getPath().equals(currentPath)) {
            col = column;
            break search;
          }
        }
        length++;
      }
      return col;
    } else {
      return col;
    }
  }

  
  private static StorableStructureInformation findTypeInHierarchy(StorableStructureInformation ssi, String type) {
    if (ssi.getFqClassNameForDatatype().equals(type)) {
      return ssi;
    } else {
      if (ssi.getSubEntries() != null) {
        for (StorableStructureIdentifier subType : ssi.getSubEntries()) {
          StorableStructureInformation result = findTypeInHierarchy(subType.getInfo(), type);
          if (result != null) {
            return result;
          }
        }
      }
      return null;
    }
  }
  

}