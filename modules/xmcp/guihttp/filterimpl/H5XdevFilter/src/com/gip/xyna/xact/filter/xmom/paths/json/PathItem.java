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
package com.gip.xyna.xact.filter.xmom.paths.json;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xact.filter.xmom.paths.Tags;

public class PathItem implements JsonSerializable {

  String label;
  String path;
  boolean isAbsolute;
  List<PathItem> children;
  
  @Override
  public void toJson(JsonBuilder jb) {
    jb.addOptionalStringAttribute(Tags.LABEL, label);
    jb.addStringAttribute(Tags.PATH, path);
    jb.addOptionalBooleanAttribute(Tags.IS_ABSOLUTE, isAbsolute);
    jb.addOptionalObjectListAttribute(Tags.CHILDREN, children );
  }


  public void setPath(String path) {
    this.path = path;
  }
  public void setAbsolute(boolean isAbsolute) {
    this.isAbsolute = isAbsolute;
  }
  public boolean isAbsolute() {
    return isAbsolute;
  }
  
  public void setLabel(String label) {
    this.label = label;
  }
  
  public void addChild(PathItem child) {
    if( this.children == null ) {
      this.children = new ArrayList<PathItem>();
    }
    this.children.add(child);
  }

  public void addChildren(List<PathItem> children) {
    if( this.children == null ) {
      this.children = new ArrayList<PathItem>();
    }
    this.children.addAll(children);
  }


  public String getPath() {
    return path;
  }


  
}
