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

import xmcp.yang.TraceYangMappingCollection;
import xmcp.yang.TraceYangMappingCollectionInstanceOperation;
import xmcp.yang.TraceYangMappingCollectionSuperProxy;
import xmcp.yang.YangMappingCollection;
import xmcp.yang.xml.CsvPathsAndNspsWithIds;
import xmcp.yang.xml.IdOfNamespaceMap;
import xmcp.yang.xml.YangXmlPathList;


public class TraceYangMappingCollectionInstanceOperationImpl extends TraceYangMappingCollectionSuperProxy implements TraceYangMappingCollectionInstanceOperation {

  private static final long serialVersionUID = 1L;

  public TraceYangMappingCollectionInstanceOperationImpl(TraceYangMappingCollection instanceVar) {
    super(instanceVar);
  }

  public YangMappingCollection merge(YangMappingCollection yangMappingCollection1) {
    YangMappingCollection ret = super.merge(yangMappingCollection1);
    /*
    getInstanceVar().setMappingList(new ArrayList<String>(ret.getMappings()));
    getInstanceVar().setNamespaceList(new ArrayList<String>(ret.getNamespaces()));
    */
    
    CsvPathsAndNspsWithIds csv = CsvPathsAndNspsWithIds.builder().csvPaths(_mappings).namespaces(_namespaces).build();
    YangXmlPathList pathlist = YangXmlPathList.fromCsv(csv);
    //CsvPathsAndNspsWithIds ret = new CsvPathsAndNspsWithIds(pathlist);
    IdOfNamespaceMap map = new IdOfNamespaceMap();
    List<String> xPathList = pathlist.toXPathList(map);
    List<String> namespaceWithIdList = map.toPrefixNamespacePairList();
    getInstanceVar().setMappingList(xPathList);
    getInstanceVar().setNamespaceList(namespaceWithIdList);
    return ret;
  }
  
  
  @Override
  public Object clone() {
    // Parameter to constructor below (instance-var) is irrelevant since it will be replaced later with the cloned instance-var
    // (this clone()-method here is supposed to be called only implicitly when cloning the XMOM-class to which this impl-class belongs,
    // i.e. the instance-var used below)
    TraceYangMappingCollectionInstanceOperationImpl ret = new TraceYangMappingCollectionInstanceOperationImpl(getInstanceVar());
    cloneContent(ret);
    return ret;
  }
  
}
