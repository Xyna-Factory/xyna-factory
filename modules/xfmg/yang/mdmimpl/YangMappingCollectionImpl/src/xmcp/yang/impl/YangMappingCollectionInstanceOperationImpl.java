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
import java.util.List;

import xact.templates.Document;
import xmcp.yang.YangMappingCollection;
import xmcp.yang.YangMappingCollectionInstanceOperation;
import xmcp.yang.YangMappingCollectionSuperProxy;
import xmcp.yang.xml.CsvPathsAndNspsWithIds;
import xmcp.yang.xml.YangXmlPathList;


public class YangMappingCollectionInstanceOperationImpl extends YangMappingCollectionSuperProxy implements YangMappingCollectionInstanceOperation {

  private static final long serialVersionUID = 1L;
  
  protected List<String> _mappings = new ArrayList<>();
  protected List<String> _namespaces = new ArrayList<>();
  
  
  public YangMappingCollectionInstanceOperationImpl(YangMappingCollection instanceVar) {
    super(instanceVar);
  }

  public Document createXml() {
    CsvPathsAndNspsWithIds csv = CsvPathsAndNspsWithIds.builder().csvPaths(_mappings).namespaces(_namespaces).build();
    YangXmlPathList pathlist = YangXmlPathList.fromCsv(csv);
    String xml = pathlist.toXml();
    Document ret = new Document();
    ret.setText(xml);
    return ret;
  }

  public List<String> getMappings() {
    return _mappings;
  }

  public List<String> getNamespaces() {
    return _namespaces;
  }

  
  public YangMappingCollection merge(YangMappingCollection input) {
    if (input == null) { return getInstanceVar(); }
    CsvPathsAndNspsWithIds csv1 = CsvPathsAndNspsWithIds.builder().csvPaths(_mappings).namespaces(_namespaces).build();
    CsvPathsAndNspsWithIds csv2 = CsvPathsAndNspsWithIds.builder().csvPaths(input.getMappings()).
                                                                   namespaces(input.getNamespaces()).build();
    CsvPathsAndNspsWithIds csv3 = csv1.merge(csv2);
    _mappings = csv3.getCsvPathList();
    _namespaces = csv3.getNamespaceWithIdList();
    getInstanceVar().setMappingCount(_mappings.size());
    return getInstanceVar();
  }


  @Override
  public Object clone() {
    // Parameter to constructor below (instance-var) is irrelevant since it will be replaced later with the cloned instance-var
    // (this clone()-method here is supposed to be called only implicitly when cloning the XMOM-class to which this impl-class belongs,
    // i.e. the instance-var used below)
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
