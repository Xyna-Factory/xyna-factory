/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation.xmom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase.ATT;


public class XmomNodeInfo {

  
  public static class XmomNodeInfoList {
    private List<XmomNodeInfo> list = new ArrayList<>();

    public List<XmomNodeInfo> getList() {
      return list;
    }
  }
  
  private String name;
  private Optional<String> value = Optional.empty();
  private Map<String, XmomNodeInfoList> childMap = new HashMap<>();
  private IdMapping idMapping;
  private Optional<IdValue> id = Optional.empty();
  private boolean ignore = false;
  
  
  public XmomNodeInfo(String name, IdMapping idMapping) {
    this.name = name;
    this.idMapping = idMapping;
  }
  
  public XmomNodeInfo(String name, String value, IdMapping idMapping) {
    this.name = name;
    initValue(name, Optional.ofNullable(value), idMapping);
    this.idMapping = idMapping;
  }
  
  public XmomNodeInfo(String name, Optional<String> value, IdMapping idMapping) {
    this.name = name;
    initValue(name, value, idMapping);
    this.idMapping = idMapping;
  }
  
  
  private void initValue(String name, Optional<String> value, IdMapping idMapping) {
    if (value.isEmpty()) {
      return;
    } else if (value.get() == null) {
      return;
    }
    if (value.get().isBlank()) {
      return;
    }
    if (!isIdName(name)) {
      this.value = value;
      return;
    }
    IdValue id = idMapping.getOrCreateIdValue(value.get());
    this.id = Optional.ofNullable(id);
  }
  
  
  public XmomNodeInfo createChild(String name) {
    return createChild(name, Optional.empty());
  }
  
  
  public XmomNodeInfo createChild(String name, String value) {
    return createChild(name, Optional.ofNullable(value));
  }
  
  
  public XmomNodeInfo createChild(String name, Optional<String> value) {
    XmomNodeInfo child = new XmomNodeInfo(name, value, this.getIdMapping());
    this.addChild(child);
    return child;
  }
  
  
  private boolean isIdName(String name) {
    if (ATT.ID.equals(name)) { return true; }
    if (ATT.REFID.equals(name)) { return true; }
    return false;
  }
  
  
  public void addChild(XmomNodeInfo info) {
    if (hasValue()) {
      throw new IllegalArgumentException("Xmom node with value must not have children");
    }
    //children.add(info);
    XmomNodeInfoList list = childMap.get(info.getName());
    if (list == null) {
      list = new XmomNodeInfoList();
      childMap.put(info.getName(), list);
    }
    list.getList().add(info);
  }
  
  
  public Optional<XmomNodeInfo> getChild(TreePathSegment seg) {
    XmomNodeInfoList list = childMap.get(seg.getName());
    if (list == null) {
      return Optional.empty();
    }
    if (seg.getIndex() >= list.getList().size()) {
      return Optional.empty();
    }
    XmomNodeInfo ret = list.getList().get(seg.getIndex());
    return Optional.ofNullable(ret);
  }

  
  public List<TreePathSegment> getAllChildPathSegments() {
    List<TreePathSegment> ret = new ArrayList<>();
    for (XmomNodeInfoList list : childMap.values()) {
      for (int i = 0; i < list.getList().size(); i++) {
        String name = list.getList().get(i).getName();
        TreePathSegment seg = new TreePathSegment(name, i);
        ret.add(seg);
      }
    }
    return ret;
  }
  
  
  public List<TreePathSegment> getChildrenWithName(String nameIn) {
    List<TreePathSegment> ret = new ArrayList<>();
    XmomNodeInfoList list = childMap.get(nameIn);
    for (int i = 0; i < list.getList().size(); i++) {
      String childName = list.getList().get(i).getName();
      TreePathSegment seg = new TreePathSegment(childName, i);
      ret.add(seg);
    }
    return ret;
  }
  
  
  protected Map<String, XmomNodeInfoList> getChildMap() {
    return childMap;
  }

  
  public boolean isIgnoreOrEmpty() {
    if (hasValue()) {
      return false;
    }
    for (XmomNodeInfoList list : childMap.values()) {
      for (XmomNodeInfo child : list.getList()) {
        if (!child.isIgnoreOrEmpty()) {
          return false;
        }
      }
    }
    return true;
  }
  
  
  public String getName() {
    return name;
  }

  
  public Optional<String> getValue() {
    if (id.isPresent()) {
      return Optional.ofNullable(id.get().getValue());
    }
    return value;
  }
  
  public String getValueOrEmpty() {
    Optional<String> val = getValue();
    if (val.isEmpty()) {
      return "";
    }
    return val.get();
  }
  
  public boolean hasValue() {
    return value.isPresent() || id.isPresent();
  }
  
  public boolean hasChildren() {
    return childMap.size() > 0;
  }

  public IdMapping getIdMapping() {
    return idMapping;
  }

  public Optional<IdValue> getIdValue() {
    return id;
  }

  public boolean isIgnore() {
    return ignore;
  }

  public void setIgnore(boolean ignore) {
    this.ignore = ignore;
    if (getIdValue().isPresent()) {
      getIdValue().get().handleIgnoreRef(ignore);
    }
  }

}
