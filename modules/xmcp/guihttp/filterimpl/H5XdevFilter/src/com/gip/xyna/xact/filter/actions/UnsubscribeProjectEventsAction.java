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
public class UnsubscribeProjectEventsAction extends XmomGuiAction {

  private static final String BASE_PATH = "/" + PathElements.PROJECT_EVENTS_UNSUBSCRIBE;
  
  public UnsubscribeProjectEventsAction(XMOMGui xmomGui) {
    super(xmomGui, 1, Operation.UnsubscribeProjectPollEvents, null, true);
  }

  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH)
        && url.getPathLength() == 2
        && method == Method.POST;
  }

  @Override
  public String getTitle() {
    return "ProjectEvents-clear";
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH, "ProjectEventsClear"); //FIXME
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }

}

