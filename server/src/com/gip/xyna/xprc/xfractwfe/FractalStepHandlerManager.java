/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xfractwfe.base.DeploymentHandling;
import com.gip.xyna.xprc.xfractwfe.base.Handler;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationValue;
import com.gip.xyna.xprc.xpce.monitoring.EngineSpecificStepHandlerManager;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderArchive.ProcessStepHandlerType;


public class FractalStepHandlerManager extends FunctionGroup implements EngineSpecificStepHandlerManager {

  public static final String DEFAULT_NAME = "StepHandlerManager";
  private final static Object NO_DYNAMIC_HANDLER_IDENTIFICATION = new Object();
  

  private ReentrantReadWriteLock lock;

  Map<DestinationKey, Map<ProcessStepHandlerType, List<Handler>>> handlers;
  private Map<String, DynamicStepHandlerFactory> factories;
  private ConcurrentMap<DestinationKey, ConcurrentMap<ProcessStepHandlerType, ConcurrentMap<Object, List<Handler>>>> dynamicHandlerCache;


  static {
    XynaFactoryPath path1 = new XynaFactoryPath(XynaProcessing.class, XynaFractalWorkflowEngine.class,
                                                DeploymentHandling.class);
    ArrayList<XynaFactoryPath> pathes = new ArrayList<XynaFactoryPath>();
    pathes.add(path1);
    addDependencies(FractalStepHandlerManager.class, pathes);
  }


  public FractalStepHandlerManager() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    if (logger.isDebugEnabled()) {
      logger.debug("Initializing " + FractalStepHandlerManager.class);
    }
    handlers = new HashMap<DestinationKey, Map<ProcessStepHandlerType, List<Handler>>>();
    factories = new HashMap<String, FractalStepHandlerManager.DynamicStepHandlerFactory>();
    dynamicHandlerCache = new ConcurrentHashMap<DestinationKey, ConcurrentMap<ProcessStepHandlerType, ConcurrentMap<Object, List<Handler>>>>();
    lock = new ReentrantReadWriteLock();
  }


  @Override
  protected void shutdown() throws XynaException {

  }


  public void addHandler(DestinationKey destinationKey, DestinationValue dv, ProcessStepHandlerType ht, Handler handler) {

    lock.writeLock().lock();
    try {
      Map<ProcessStepHandlerType, List<Handler>> map = handlers.get(destinationKey);
      if (map == null) {
        map = new HashMap<ProcessStepHandlerType, List<Handler>>();
        List<Handler> hList = new ArrayList<Handler>();
        hList.add(handler);
        map.put(ht, hList);
        handlers.put(destinationKey, map);
      } else {
        List<Handler> hList = map.get(ht);
        if (hList == null) {
          hList = new ArrayList<Handler>();
          map.put(ht, hList);
        }
        hList.add(handler);
      }
      clearCache();
    } finally {
      lock.writeLock().unlock();
    }

    clearFractalWorkflowInstancePools(dv, dv.resolveRevision(destinationKey));

  }


  public List<Handler> getHandlers(ProcessStepHandlerType ht, XynaOrderServerExtension xose) {
    lock.readLock().lock();
    try {
      List<Handler> cachedHandlers = tryGetHandlerFromCache(ht, xose);
      if (cachedHandlers != null) {
        return cachedHandlers;
      } else {
        DestinationKey destinationKey = xose.getDestinationKey();
        Map<ProcessStepHandlerType, List<Handler>> relevantHandlers = handlers.get(destinationKey);
        List<Handler> result;
        if (relevantHandlers != null) {
          result = relevantHandlers.get(ht);
          if (result == null) {
            result = new ArrayList<Handler>();
          } else {
            // copy
            result = new ArrayList<Handler>(result);
          }
        } else {
          result = new ArrayList<Handler>();
        }
        for (DynamicStepHandlerFactory factory : factories.values()) {
          Handler newHandler = factory.createHandler(xose, ht);
          if (newHandler != null) {
            result.add(newHandler);
          }
        }
        insertIntoCache(ht, xose, result);
        return result;
      }
    } finally {
      lock.readLock().unlock();
    }
  }


  private List<Handler> tryGetHandlerFromCache(ProcessStepHandlerType ht, XynaOrderServerExtension xose) {
    DestinationKey dk = xose.getDestinationKey();
    Object dynamicIdentification = createDynamicIdentification(xose);
    ConcurrentMap<ProcessStepHandlerType, ConcurrentMap<Object, List<Handler>>> dkMap = dynamicHandlerCache.get(dk);
    if (dkMap == null) {
      return null;
    }
    ConcurrentMap<Object, List<Handler>> htMap = dkMap.get(ht);
    if (htMap == null) {
      return null;
    }
    List<Handler> handler = htMap.get(dynamicIdentification);
    return handler;
  }


  private void insertIntoCache(ProcessStepHandlerType ht, XynaOrderServerExtension xose, List<Handler> result) {
    DestinationKey dk = xose.getDestinationKey();
    Object dynamicIdentification = createDynamicIdentification(xose);
    if (!dynamicHandlerCache.containsKey(dk)) {
      dynamicHandlerCache.putIfAbsent(dk, new ConcurrentHashMap<ProcessStepHandlerType, ConcurrentMap<Object, List<Handler>>>());
    }
    ConcurrentMap<ProcessStepHandlerType, ConcurrentMap<Object, List<Handler>>> dkMap = dynamicHandlerCache.get(dk);
    if (!dkMap.containsKey(ht)) {
      dkMap.putIfAbsent(ht, new ConcurrentHashMap<Object, List<Handler>>());
    }
    ConcurrentMap<Object, List<Handler>> htMap = dkMap.get(ht);
    if (!htMap.containsKey(dynamicIdentification)) {
      htMap.putIfAbsent(dynamicIdentification, result);
    }
  }
  
  
  private void clearCache() {
    lock.writeLock().lock();
    try {
      dynamicHandlerCache.clear();
    } finally {
      lock.writeLock().unlock();
    }
  }


  private Object createDynamicIdentification(XynaOrderServerExtension xose) {
    if (factories.size() <= 0) {
      return NO_DYNAMIC_HANDLER_IDENTIFICATION;
    } else if (factories.size() == 1) {
      return factories.values().iterator().next().extractCachingParameter(xose);
    } else {
      Set<Object> allIdentifiers = new HashSet<Object>();
      for (DynamicStepHandlerFactory decider : factories.values()) {
        allIdentifiers.add(decider.extractCachingParameter(xose));
      }
      return allIdentifiers;
    }
  }


  public void removeHandler(DestinationKey destinationKey, DestinationValue dv, ProcessStepHandlerType ht, Handler handler) {

    lock.writeLock().lock();

    try {
      Map<ProcessStepHandlerType, List<Handler>> map = handlers.get(destinationKey);

      if (map != null) {

        List<Handler> relevantHandlers = map.get(ht);

        if (relevantHandlers != null) {
          Handler removeIt = null;
          for (Handler h : relevantHandlers) {
            if (h == handler) {
              removeIt = h;
            }
          }
          relevantHandlers.remove(removeIt);
        }

      }
      clearCache();
    } finally {
      lock.writeLock().unlock();
    }

    clearFractalWorkflowInstancePools(dv, dv.resolveRevision(destinationKey));

  }

  
  private void clearFractalWorkflowInstancePools(DestinationValue dv, Long revision) {
    if (!(getParentSection() instanceof XynaFractalWorkflowEngine)) {
      throw new RuntimeException(getDefaultName() + " has to be deployed as a function group of the "
                      + XynaFractalWorkflowEngine.DEFAULT_NAME);
    }
    XynaFractalWorkflowEngine xwfe = (XynaFractalWorkflowEngine) getParentSection();
    xwfe.getProcessManager().clearInstancePool(dv, revision);
  }



  public void addFactory(String name, DynamicStepHandlerFactory factory) {
    lock.writeLock().lock();
    try {
      factories.put(name, factory);
      clearCache();
    } finally {
      lock.writeLock().unlock();
    }
  }


  public void removeFactory(String name) {
    lock.writeLock().lock();
    try {
      factories.remove(name);
      clearCache();
    } finally {
      lock.writeLock().unlock();
    }
  }
  

}
