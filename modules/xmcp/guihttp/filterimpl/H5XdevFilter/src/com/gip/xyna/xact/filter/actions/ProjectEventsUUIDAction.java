/*----------------------------------------------------
 * Xyna 6.1 (Black Edition)
 * Xyna GUI Support
 *----------------------------------------------------
 * Copyright GIP AG 2015
 * (http://www.gip.com)
 * Hechtsheimer Str. 35-37
 * 55131 Mainz
 *----------------------------------------------------
 * $Revision: 330010 $
 * $Date: 2023-09-11 21:19:58 +0200 (Mon, 11 Sep 2023) $
 *----------------------------------------------------
 */
package com.gip.xyna.xact.filter.actions;

import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.XmomGuiAction;
import com.gip.xyna.xact.filter.session.XMOMGui;
import com.gip.xyna.xact.filter.session.XMOMGuiRequest.Operation;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;


/**
 *
 */
public class ProjectEventsUUIDAction extends XmomGuiAction {

  private static final String BASE_PATH = "/" + PathElements.PROJECT_EVENTS;
  
  public ProjectEventsUUIDAction(XMOMGui xmomGui) {
    super(xmomGui, 1, Operation.GetProjectPollEvents, null, false);
  }

  @Override
  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH) 
        && url.getPathLength() == 2
        && method == Method.GET;
  }

  @Override
  public String getTitle() {
    return "GetProjectEvents";
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH, "GetProjectEvents"); //FIXME
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }

}
