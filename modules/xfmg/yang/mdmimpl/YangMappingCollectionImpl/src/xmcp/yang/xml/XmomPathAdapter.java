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


package xmcp.yang.xml;

import xmcp.yang.YangMappingPath;
import xmcp.yang.YangMappingPathElement;


public class XmomPathAdapter {
  
  public YangXmlPath adapt(YangMappingPath inputPath) {
    YangXmlPath ret = new YangXmlPath();
    if (inputPath == null) { return ret; }
    if (inputPath.getPath() == null) { return ret; }
    for (YangMappingPathElement item : inputPath.getPath()) {
      if ((item.getListIndex() != null) && (item.getListIndex() >= 0)) {
        ret.add(YangXmlPathElem.buildListIndexElem(item.getListIndex()));
        continue;
      }
      PathElemBuilder builder = YangXmlPathElem.builder().elemName(item.getElementName())
                                                         .namespace(item.getNamespace());
      if (ret.getPath().size() == inputPath.getPath().size() - 1) {
        builder.textValue(inputPath.getValue());
        builder.setIsListKeyLeaf(inputPath.getIsListKey());
      }
      ret.add(builder.build());
    }
    return ret;
  }
  
}
