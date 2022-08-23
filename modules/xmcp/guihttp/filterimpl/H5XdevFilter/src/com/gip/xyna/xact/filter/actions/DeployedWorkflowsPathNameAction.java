/*----------------------------------------------------
 * Xyna 6.1 (Black Edition)
 * Xyna GUI Support
 *----------------------------------------------------
 * Copyright GIP AG 2015
 * (http://www.gip.com)
 * Hechtsheimer Str. 35-37
 * 55131 Mainz
 *----------------------------------------------------
 * $Revision: 313343 $
 * $Date: 2022-08-19 11:53:44 +0200 (Fri, 19 Aug 2022) $
 *----------------------------------------------------
 */
package com.gip.xyna.xact.filter.actions;

import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.XmomGuiAction;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;


/**
 *
 */
public class DeployedWorkflowsPathNameAction extends XmomGuiAction {

  private static final String BASE_PATH = "/" + PathElements.DEPLOYED + "/" + PathElements.WORKFLOWS;
  
  public DeployedWorkflowsPathNameAction(XMOMGui xmomGui) {
    super(xmomGui, 2, 3, Operation.ViewDeployed, Type.workflow, false);
  }

  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) 
        && url.getPathLength() == 4 
        && method == Method.GET;
  }

  @Override
  public String getTitle() {
    return "DEPLOYED-Workflows-TypePath-TypeName";
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH+"/h5/TestSerial", "h5.TestSerial");
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }

}

