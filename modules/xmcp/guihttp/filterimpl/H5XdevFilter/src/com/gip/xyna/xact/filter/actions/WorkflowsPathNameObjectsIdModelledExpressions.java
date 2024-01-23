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
package com.gip.xyna.xact.filter.actions;



import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.XmomGuiAction;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;



public class WorkflowsPathNameObjectsIdModelledExpressions extends XmomGuiAction {

  private static final String BASE_PATH = "/" + PathElements.XMOM + "/" + PathElements.WORKFLOWS;


  public WorkflowsPathNameObjectsIdModelledExpressions(XMOMGui xmomGui) {
    super(xmomGui, 2, 3, 5, Operation.ModelledExpressions, Type.workflow, false);
  }


  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) 
        && url.getPathLength() == 7 
        && url.getPathElement(4).equals(PathElements.OBJECTS)
        && url.getPathElement(6).equals(PathElements.MODELLED_EXPRESSIONS)
        && method == Method.GET;
  }


  @Override
  public String getTitle() {
    return "XMOM-Workflows-TypePath-TypeName-Objects-Id-modelledExpressions";
  }


  @Override
  public void appendIndexPage(HTMLPart body) {
  }


  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }
}
