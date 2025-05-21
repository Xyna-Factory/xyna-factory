/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xmcp.yang.impl;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xact.templates.Document;
import xmcp.yang.YangMappingCollection;
import xmcp.yang.YangMappingCollectionInstanceOperation;
import xmcp.yang.YangMappingCollectionSuperProxy;


public class YangMappingCollectionInstanceOperationImpl extends YangMappingCollectionSuperProxy implements YangMappingCollectionInstanceOperation {

  private static final long serialVersionUID = 1L;
  
  protected List<String> _mappings = new ArrayList<>();
  protected Set<String> _namespaces = new HashSet<>();
  
  
  public YangMappingCollectionInstanceOperationImpl(YangMappingCollection instanceVar) {
    super(instanceVar);
  }

  public Document createXml() {
    return null;
  }

  public List<String> getMappings() {
    return _mappings;
  }

  public List<String> getNamespaces() {
    return new ArrayList<String>(_namespaces);
  }

  
  public xmcp.yang.YangMappingCollection merge(xmcp.yang.YangMappingCollection input) {
    if (input == null) { return getInstanceVar(); }
    List<String> mappings = input.getMappings();
    if (mappings != null) {
      _mappings.addAll(mappings);
      getInstanceVar().setMappingCount(_mappings.size());
    }
    List<String> namespaces = input.getNamespaces();
    if (namespaces != null) {
      _namespaces.addAll(namespaces);
    }
    return getInstanceVar();
  }


  @Override
  public Object clone() {
    YangMappingCollectionInstanceOperationImpl ret = new YangMappingCollectionInstanceOperationImpl(getInstanceVar());
    cloneContent(ret);
    return ret;
  }
  
  protected void cloneContent(YangMappingCollectionInstanceOperationImpl cloned) {
    cloned._mappings.addAll(_mappings);
    cloned._namespaces.addAll(_namespaces);
  }
  
  
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

}
