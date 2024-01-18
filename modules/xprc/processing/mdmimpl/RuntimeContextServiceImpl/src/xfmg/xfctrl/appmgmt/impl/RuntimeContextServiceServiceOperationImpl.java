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
package xfmg.xfctrl.appmgmt.impl;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.Text;
import xfmg.xfctrl.appmgmt.RuntimeContextServiceServiceOperation;
import xprc.xpce.RuntimeContext;


public class RuntimeContextServiceServiceOperationImpl implements ExtendedDeploymentTask, RuntimeContextServiceServiceOperation {

  private static final Logger logger = CentralFactoryLogging.getLogger(RuntimeContextServiceServiceOperationImpl.class);


  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  //filter
  public List<? extends RuntimeContext> filterRuntimeContextsByContains(List<? extends Text> filter, List<? extends RuntimeContext> rtcs) {
    if (filter == null) {
      return rtcs;
    }
    //leere filter entfernen
    for (int i = filter.size() - 1; i >= 0; i--) {
      Text t = filter.get(i);
      if (t == null || t.getText() == null || t.getText().isEmpty()) {
        filter.remove(i);
      }
    }
    if (filter.size() == 0) {
      return rtcs;
    }
    //code etwas komplizierter, weil im raum stand so zu sortieren, dass zuerst die filter1-matches, dann filter2-matches etc kommen.
    Set<RuntimeContext> allRuntimeContexts = new HashSet<>(rtcs);
    List<RuntimeContext> r = new ArrayList<>();
    for (Text t : filter) {
      String filterString = t.getText();
      Iterator<RuntimeContext> it = allRuntimeContexts.iterator();
      while (it.hasNext()) {
        RuntimeContext rc = it.next();
        if (match(rc, filterString)) {
          it.remove();
          r.add(rc);
        }
      }
    }

    //sortiere lexikographisch
    Collections.sort(r, new Comparator<RuntimeContext>() {

      @Override
      public int compare(RuntimeContext o1, RuntimeContext o2) {
        String n1 = getName(o1);
        String n2 = getName(o2);
        return n1.compareTo(n2);
      }

    });
    return r;
  }


  private boolean match(RuntimeContext rc, String filterString) {
    return getName(rc).contains(filterString);
  }


  private static String getName(RuntimeContext rc) {
    if (rc instanceof xprc.xpce.Workspace) {
      return ((xprc.xpce.Workspace) rc).getName();
    } else if (rc instanceof xprc.xpce.Application) {
      return ((xprc.xpce.Application) rc).getName();
    } else {
      throw new RuntimeException("unexpected rtc type: " + rc.getClass().getName());
    }
  }


  private static String getVersionName(RuntimeContext rc) {
    if (rc instanceof xprc.xpce.Workspace) {
      return null;
    } else if (rc instanceof xprc.xpce.Application) {
      return ((xprc.xpce.Application) rc).getVersion();
    } else {
      throw new RuntimeException("unexpected rtc type: " + rc.getClass().getName());
    }
  }

  public RuntimeContext getOwnRuntimeContext(XynaOrderServerExtension correlatedXynaOrder) {
    try {
      long rootOrderRev = correlatedXynaOrder.getRootOrder().getRevision();
      com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext rtc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
          .getRevisionManagement().getRuntimeContext(rootOrderRev);

      return convert(rtc);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException(e);
    }
  }


  public List<? extends RuntimeContext> getRuntimeContextDependenciesRecursively(RuntimeContext rtc) {
    String applicationName = null;
    String versionName = null;
    String workspaceName = null;
    if (rtc instanceof xprc.xpce.Application) {
      xprc.xpce.Application app = (xprc.xpce.Application) rtc;
      applicationName = app.getName();
      versionName = app.getVersion();
    } else if (rtc instanceof xprc.xpce.Workspace) {
      xprc.xpce.Workspace ws = (xprc.xpce.Workspace) rtc;
      workspaceName = ws.getName();
    } else {
      throw new RuntimeException("unexpected runtimecontext " + rtc);
    }
    long revision;
    try {
      revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement()
          .getRevision(applicationName, versionName, workspaceName);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      throw new RuntimeException("runtimecontext not found: " + applicationName + ", " + versionName + "," + workspaceName, e);
    }
    Set<Long> dependencies = new HashSet<>();
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
        .getDependenciesRecursivly(revision, dependencies);

    List<RuntimeContext> l = new ArrayList<>();
    for (Long dep : dependencies) {
      try {
        com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext rd =
            XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(dep);
        l.add(convert(rd));
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        logger.debug(null, e);
      }
    }
    return l;
  }


  private RuntimeContext convert(com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext rtc) {
    if (rtc instanceof Workspace) {
      Workspace ws = (Workspace) rtc;
      return new xprc.xpce.Workspace.Builder().name(ws.getName()).instance();
    } else if (rtc instanceof Application) {
      Application app = (Application) rtc;
      return new xprc.xpce.Application.Builder().name(app.getName()).version(app.getVersionName()).instance();
    } else {
      throw new RuntimeException("unexpected rtc found: " + rtc.getGUIRepresentation());
    }
  }


  @Override
  public List<? extends RuntimeContext> filterRuntimeContextsForLatestVersion(List<? extends RuntimeContext> rtcs) {
    Map<String, RuntimeContext> rtcToLastestVersion = new HashMap<>();
    for (RuntimeContext rtc : rtcs) {
      if (!rtcToLastestVersion.containsKey(getName(rtc)) ||
          getVersionName(rtcToLastestVersion.get(getName(rtc))).compareToIgnoreCase(getVersionName(rtc)) < 0) {
        rtcToLastestVersion.put(getName(rtc), rtc);
      }
    }

    List<RuntimeContext> result = new ArrayList<>();
    for (Entry<String, RuntimeContext> rtcEntry : rtcToLastestVersion.entrySet()) {
      result.add(rtcEntry.getValue());
    }

    return result;
  }

}
