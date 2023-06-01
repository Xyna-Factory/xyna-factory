/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.xdev.xfractmod.xmdm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionMap;


/**
 *
 */
public class XynaObjectDeploymentListenerManagement {

  public interface XynaObjectDeploymentListener {
    public void deploy(String fqClassName, long revision, Class<? extends XynaObject> clazz);
    public void undeploy(String fqClassName, long revision, Class<? extends XynaObject> clazz);
  }
  
  public static abstract class FilteredXynaObjectDeploymentListener implements XynaObjectDeploymentListener {
    
    private XynaObjectDeploymentListener wrapped;
    public FilteredXynaObjectDeploymentListener(XynaObjectDeploymentListener wrapped) {
      this.wrapped = wrapped;
    }
    public abstract boolean isRelevant(String fqClassName);
    
    public void deploy(String fqClassName, long revision, Class<? extends XynaObject> clazz) {
      if( isRelevant(fqClassName) ) {
        wrapped.deploy(fqClassName, revision, clazz);
      }
    }
    public void undeploy(String fqClassName, long revision, Class<? extends XynaObject> clazz) {
      if( isRelevant(fqClassName) ) {
        wrapped.undeploy(fqClassName, revision, clazz);
      }
    }
  }
  
  public static class SetFilteredXynaObjectDeploymentListener extends FilteredXynaObjectDeploymentListener {
    private HashSet<String> relevantFqClassNames;
    
    public SetFilteredXynaObjectDeploymentListener(Collection<String> relevantFqClassNames, XynaObjectDeploymentListener wrapped) {
      super(wrapped);
      this.relevantFqClassNames = new HashSet<String>(relevantFqClassNames);
    }
    public boolean isRelevant(String fqClassName) {
      return relevantFqClassNames.contains(fqClassName);
    }
  }
  
  
  private final RevisionMap<List<XynaObjectDeploymentListener>> listeners;
  private final List<XynaObjectDeploymentListener> listenersForAllRevisions;
  
  public XynaObjectDeploymentListenerManagement() {
    listeners = RevisionMap.createList(XynaObjectDeploymentListener.class);
    listenersForAllRevisions = new ArrayList<XynaObjectDeploymentListener>();
  }
  
  public boolean registerXynaObjectDeploymentListener(long revision, XynaObjectDeploymentListener listener) {
    return registerXynaObjectDeploymentListener(listeners.getOrCreate(revision), listener);
  }
  
  public boolean unregisterXynaObjectDeploymentListener(long revision, XynaObjectDeploymentListener listener) {
    return unregisterXynaObjectDeploymentListener(listeners.get(revision), listener);
  }
  
  public boolean registerXynaObjectDeploymentListener(XynaObjectDeploymentListener listener) {
    return registerXynaObjectDeploymentListener(listenersForAllRevisions, listener);
  }
 
  public boolean unregisterXynaObjectDeploymentListener(XynaObjectDeploymentListener listener) {
    return unregisterXynaObjectDeploymentListener(listenersForAllRevisions, listener);
  }
  
  private synchronized boolean registerXynaObjectDeploymentListener(List<XynaObjectDeploymentListener> listeners, XynaObjectDeploymentListener listener) {
    if( listeners.contains(listener) ) {
      return false; //Listener ist bereits eingetragen
    }
    listeners.add(listener);
    return true;
  }
  
  private synchronized boolean unregisterXynaObjectDeploymentListener(List<XynaObjectDeploymentListener> listeners, XynaObjectDeploymentListener listener) {
    if( listeners == null ) {
      return false; //ganze Revision fehlt
    }
    return listeners.remove(listener);
  }

  public void notifyDeploy(Class<? extends XynaObject> clazz) {
    String fqClassName = clazz.getName();
    long revision = getRevision(clazz);
    if (revision == Integer.MAX_VALUE) {
      return;
    }
    notifyDeploy(listenersForAllRevisions, fqClassName, revision, clazz);
    notifyDeploy(listeners.get(revision), fqClassName, revision, clazz);
  }
  
  private void notifyDeploy(List<XynaObjectDeploymentListener> listeners,
                            String fqClassName, long revision, Class<? extends XynaObject> clazz) {
    if( listeners == null ) {
      return; //nichts zu tun
    }
    for (XynaObjectDeploymentListener l: listeners ) {
      l.deploy(fqClassName, revision, clazz);
    }
  }

  public void notifyUndeploy(Class<? extends XynaObject> clazz) {
    String fqClassName = clazz.getName();
    long revision = getRevision(clazz);
    if (revision == Integer.MAX_VALUE) {
      return;
    }
    notifyUndeploy(listenersForAllRevisions, fqClassName, revision, clazz);
    notifyUndeploy(listeners.get(revision), fqClassName, revision, clazz);
  }
  
  private void notifyUndeploy(List<XynaObjectDeploymentListener> listeners,
                            String fqClassName, long revision, Class<? extends XynaObject> clazz) {
    if( listeners == null ) {
      return; //nichts zu tun
    }
    for (XynaObjectDeploymentListener l: listeners ) {
      l.undeploy(fqClassName, revision, clazz);
    }
  }
  
  private long getRevision(Class<? extends XynaObject> clazz) {
    return RevisionManagement.getRevisionByClass(clazz);
  }

}
