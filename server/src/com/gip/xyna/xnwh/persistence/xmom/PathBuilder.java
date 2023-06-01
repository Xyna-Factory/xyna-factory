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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;

public class PathBuilder {
  
  public static final char PATH_SEPERATOR = '.';
  public static final char PATH_CLASS_PREFIX = '{';
  public static final char PATH_CLASS_SUFFIX = '}';
  public static final String PATH_LIST_MARKER = "[]";
  

  List<PathElement> path;
  
  
  public PathBuilder() {
    path = new ArrayList<>();
  }


  public String getPath() {
    StringBuilder sb = new StringBuilder();
    ListIterator<PathElement> iterator = getIteratorAtLastRoot();
    iterator.next(); // skip root
    while (iterator.hasNext()) {
      PathElement pe = iterator.next();
      if (pe instanceof ColumnElement) {
        sb.append(((ColumnElement) pe).getColumn());
        if (((ColumnElement) pe).isList()) {
          sb.append(PATH_LIST_MARKER);  
        }
        if (path.size() > iterator.nextIndex() + 1) { // kommt noch ein typ + andere column
                                                       // assumes a constant shift between type & path :-/
          sb.append(PATH_SEPERATOR);
        }
      }
    }
    return sb.toString();
  }
  
  public String getFqPath() {
    StringBuilder sb = new StringBuilder();
    ListIterator<PathElement> iterator = getIteratorAtLastRoot();
    iterator.next(); // skip root
    while (iterator.hasNext()) {
      PathElement pe = iterator.next();
      if (pe instanceof ColumnElement) {
        sb.append(((ColumnElement) pe).column);
        if (((ColumnElement) pe).isList()) {
          sb.append(PATH_LIST_MARKER);  
        }
        if (iterator.hasNext() &&
            path.get(iterator.nextIndex()) instanceof TypeElement &&
            path.size() > iterator.nextIndex()) {
          TypeElement<?> nextType = (TypeElement<?>) iterator.next();
          sb.append(PATH_CLASS_PREFIX).append(nextType.getOrignalFqName()).append(PATH_CLASS_SUFFIX);
          if (path.size() > iterator.nextIndex()) {
            sb.append(PATH_SEPERATOR);
          }
            
        }
      } else {
        //throw new RuntimeException("Invalid path!");
      }
    }
    return sb.toString();
  }
  
  
  public TypeElement<?> getLastPathRoot() {
    return (TypeElement<?>) path.get(getIndexOfLastRoot());
  }
  
  private ListIterator<PathElement> getIteratorAtLastRoot() {
    return path.listIterator(getIndexOfLastRoot());
  }
  
  int getIndexOfLastRoot() {
    int indexOfLastRoot = 0;
    for (int i = 0; i < path.size(); i++) {
      PathElement pe = path.get(i);
      if (pe instanceof TypeElement &&
          ((TypeElement<?>)pe).isRoot()) {
        indexOfLastRoot = i;
      }
    }
    return indexOfLastRoot;
  }
  
  
  public void enter(DOM gb, boolean isFlat, boolean isRootXMOM) {
    ModelledTypeElement te = new ModelledTypeElement();
    te.dom = gb;
    te.isFlat = isFlat;
    te.isRoot = isRootXMOM;
    path.add(te);
  }
  
  
  public void enter(String name, long revision) {
    NamedTypeElement nte = new NamedTypeElement();
    nte.originalFqName = name;
    nte.revision = revision;
    path.add(nte);
  }
  
  
  public void enter(String columnName, boolean isList) {
    ColumnElement ce = new ColumnElement(columnName, isList);
    path.add(ce);
  }
  
  
  public void exit() {
    path.remove(path.size() - 1);
  }
  
  
  // for super & subs
  public GenerationBase exchangeType(DOM exchangeWith) {
    TypeElement<?> te = getLastType(true, 0);
    if (te instanceof ModelledTypeElement) {
      return ((ModelledTypeElement)te).exchangeTypeIdentifier(exchangeWith);
    } else {
      throw new IllegalStateException("Can not exchange type of NamedTypeElement " + te.getOrignalFqName());
    }
  }
  
  
  TypeElement<?> getLastType(boolean allowFlat, int toSkip) {
    int skipped = 0;
    for (int i = path.size() -1; i >= 0; i--) {
      if (path.get(i) instanceof TypeElement &&
          (allowFlat || !((TypeElement<?>)path.get(i)).isFlat())) {
        if (skipped < toSkip) {
          continue;
        } else {
          return (TypeElement<?>)path.get(i);
        }
      }
    }
    return null;
  }
  
  
  public String getLastTypeName() {
    TypeElement<?> te = getLastType(true, 0);
    return te.getOrignalFqName();
  }
  
  
  public void setTableName(String table) {
    getLastType(true, 0).setTableName(table);
  }

  
  public DOM getLastGenerationBase() {
    TypeElement<?> te = getLastType(true, 0);
    if (te instanceof ModelledTypeElement) {
      return ((ModelledTypeElement)te).dom;
    } else {
      return null;
    }
  }
  
  
  public ColumnElement getLastColumnElement() {
    for (int i = path.size() -1; i >= 0; i--) {
      if (path.get(i) instanceof ColumnElement) {
        return (ColumnElement)path.get(i);
      }
    }
    return null;
  }
  
  public PathBuilder clone() {
    PathBuilder clone = new PathBuilder();
    clone.path.addAll(path);
    return clone;
  }
  
  
  interface PathElement {
    
  }
  
  
  interface TypeElement<I extends Object> extends PathElement {

    String getTableName();

    long getRevision();

    String getOrignalFqName();

    boolean isFlat();
    
    boolean isRoot();

    void setTableName(String relevantField);
    
    I exchangeTypeIdentifier(I newIdentifier);
    
  }
  
  
  static class ModelledTypeElement implements TypeElement<DOM> {
    
    DOM dom;
    boolean isFlat;
    boolean isRoot;
    private String tableName;

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }

    public DOM exchangeTypeIdentifier(DOM newIdentifier) {
      DOM oldDom = dom;
      dom = newIdentifier;
      return oldDom;
    }

    public boolean isFlat() {
      return isFlat;
    }

    public String getOrignalFqName() {
      return dom.getOriginalFqName();
    }

    public long getRevision() {
      return dom.getRevision();
    }

    public boolean isRoot() {
      return isRoot;
    }
    
  }
  
  
  static class NamedTypeElement implements TypeElement<String> {
    
    String originalFqName;
    long revision;
    String tableName;

    public String getTableName() {
      return tableName;
    }

    public void setTableName(String tableName) {
      this.tableName = tableName;
    }

    public String exchangeTypeIdentifier(String newIdentifier) {
      String oldOriginalFqName = originalFqName;
      originalFqName = newIdentifier;
      return oldOriginalFqName;
    }

    public boolean isFlat() {
      return false;
    }

    public String getOrignalFqName() {
      return originalFqName;
    }

    public long getRevision() {
      return revision;
    }

    public boolean isRoot() {
      return false;
    }
    
  }
  
  
  static class ColumnElement implements PathElement {
    
    String column;
    boolean isList;
    
    public ColumnElement(String column, boolean isList) {
      this.column = column;
      this.isList = isList;
    }
    
    public boolean isList() {
      return isList;
    }
    
    public String getColumn() {
      return column;
    }
    
  }
  
  
  static class FqPathPart {
    
    private final String varName;
    private final String typeName;
    
    private FqPathPart(String varName) {
      this(varName, null);
    }
    
    private FqPathPart(String varName, String typeName) {
      this.varName = varName;
      this.typeName = typeName;
    }
    
    public String getVarName() {
      return varName;
    }
    
    public String getTypeName() {
      return typeName;
    }
    
  }
  
  public static FqPathPart split(String fqPart) {
    FqPathPart part;
    if (fqPart.contains(PathBuilder.PATH_CLASS_PREFIX+"")) {
      part = new FqPathPart(fqPart.substring(0, fqPart.indexOf(PathBuilder.PATH_CLASS_PREFIX)),
                            fqPart.substring(fqPart.indexOf(PathBuilder.PATH_CLASS_PREFIX) + 1,
                                             fqPart.indexOf(PathBuilder.PATH_CLASS_SUFFIX)));
    } else {
      part = new FqPathPart(fqPart);
    }
    return part;
  }

}