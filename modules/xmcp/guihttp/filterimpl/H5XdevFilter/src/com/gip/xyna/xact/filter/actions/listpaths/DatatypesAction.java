/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.actions.listpaths;



import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.actions.PathElements;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.XMOMGui;



public class DatatypesAction extends ListPathsAction {

  private static final Type[] filterTypes = new Type[] {ObjectIdentifierJson.Type.dataType, ObjectIdentifierJson.Type.exceptionType};


  public DatatypesAction(XMOMGui xmomGui) {
    super(xmomGui, Type.dataType, "dataTypes", PathElements.DATA_TYPES);
  }


  @Override
  public String getTitle() {
    return "XMOM-Datatypes";
  }


  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH, "all datatypes");
  }


  @Override
  protected Type[] getFilterTypes() {
    return filterTypes;
  }

}

