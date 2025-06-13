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

import java.util.ArrayList;
import java.util.List;


public class PathElemBuilder {

  private String _elemName = null;
  private String _namespace = null;
  private String _textValue = null;
  private List<ListKey> _listKeys = new ArrayList<>();
  
  
  public YangXmlPathElem build() {
    return new YangXmlPathElem(_elemName, _namespace, _textValue, _listKeys);
  }
  
  public PathElemBuilder elemName(String elemName) {
    this._elemName = elemName;
    return this;
  }
  
  public PathElemBuilder namespace(String namespace) {
    this._namespace = namespace;
    return this;
  }
  
  public PathElemBuilder textValue(String textValue) {
    this._textValue = textValue;
    return this;
  }
    
  public PathElemBuilder addListKey(ListKeyBuilder builder) {
    _listKeys.add(builder.build());
    return this;
  }
  
  public PathElemBuilder addListKey(ListKey lk) {
    _listKeys.add(lk);
    return this;
  }
  
}
