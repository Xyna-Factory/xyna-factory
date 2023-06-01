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

package com.gip.xyna.xprc.xpce.statustracking;



import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassProvider;
import com.gip.xyna.xfmg.xfctrl.classloading.UndeploymentHandler;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceCompensationStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceStatus;
import com.gip.xyna.xprc.xprcods.orderarchive.OrderInstanceSuspensionStatus;



public class StatusChangeProvider extends FunctionGroup {

  public static final String DEFAULT_NAME = "StatusChangeProvider";
  private static final Logger logger = CentralFactoryLogging.getLogger(StatusChangeProvider.class);

  /*
   * Use an array to be able to rapidly iterate of the entries without iterator creation
   */
  private ReentrantReadWriteLock lock;
  private ReadLock readLock;
  private WriteLock writeLock;

  private HashMap<DestinationKey, IStatusChangeListener[]> listeners;

  private HashMap<IStatusChangeListener, UndeploymentHandler> undeploymentHandlers;


  public StatusChangeProvider() throws XynaException {
    super();
  }


  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }


  @Override
  protected void init() throws XynaException {
    lock = new ReentrantReadWriteLock();
    readLock = lock.readLock();
    writeLock = lock.writeLock();
    listeners = new HashMap<DestinationKey, IStatusChangeListener[]>();
    undeploymentHandlers = new HashMap<IStatusChangeListener, UndeploymentHandler>();
  }


  @Override
  protected void shutdown() throws XynaException {
  }


  private void addStatusChangeListenerInternally(ClassProvider c, final IStatusChangeListener newListener) {

    if (newListener == null) {
      throw new IllegalArgumentException("Status change listener may not be null!");
    }

    writeLock.lock();
    try {

      destkeys: for (DestinationKey dk : newListener.getWatchedDestinationKeys()) {

        IStatusChangeListener[] oldListeners = listeners.get(dk);

        if (oldListeners == null) {
          oldListeners = new IStatusChangeListener[0];
        } else {
          for (IStatusChangeListener listener : oldListeners) {
            if (listener == newListener) {
              if (logger.isInfoEnabled()) {
                logger.info("Tried to register " + IStatusChangeListener.class.getSimpleName()
                                + " that was already registered, doing nothing for this "
                                + DestinationKey.class.getSimpleName());
              }
              continue destkeys;
            }
          }
        }

        IStatusChangeListener[] newListeners = new IStatusChangeListener[oldListeners.length + 1];
        System.arraycopy(oldListeners, 0, newListeners, 0, oldListeners.length);
        newListeners[oldListeners.length] = newListener;

        listeners.put(dk, newListeners);

      }

      if (c != null) {
        // make sure that the listener is removed once the class is undeployed
        UndeploymentHandler handler = new UndeploymentHandler() {

          public void onUndeployment() {
            removeStatusChangeListener(newListener);
          }

        };
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getAutomaticUnDeploymentHandlerManager().addUnDeploymentHandler(c, handler);
        undeploymentHandlers.put(newListener, handler);
      }

    } finally {
      writeLock.unlock();
    }
  }


  public void addStatusChangeListener(final IStatusChangeListener newListener) {
    addStatusChangeListenerInternally(null, newListener);
  }


  public void addStatusChangeListener(final ClassProvider c, final IStatusChangeListener newListener) {
    if (c == null) {
      throw new IllegalArgumentException("ClassProvider may not be null.");
    }
    addStatusChangeListenerInternally(c, newListener);
  }


  public void removeStatusChangeListener(final IStatusChangeListener toBeRemoved) {

    writeLock.lock();
    try {

      for (DestinationKey dk : toBeRemoved.getWatchedDestinationKeys()) {

        IStatusChangeListener[] oldListeners = listeners.get(dk);
        if (oldListeners == null || oldListeners.length == 0) {
          if (logger.isDebugEnabled()) {
            logger.debug("Could not remove " + toBeRemoved + ", no listeners registered for DestinationKey " + dk
                            + " (order type: " + dk.getOrderType() + ")");
          }
          continue;
        }

        int index = -1;
        for (int i = 0; i < oldListeners.length; i++) {
          if (oldListeners[i] == toBeRemoved) {
            index = i;
            break;
          }
        }

        if (index < 0) {
          if (logger.isDebugEnabled()) {
            logger.debug("Could not remove " + toBeRemoved
                            + ", it had not been registered before or was already removed");
          }
          continue;
        }
        
        if (oldListeners.length == 1) {
          listeners.remove(dk);
        } else {

          IStatusChangeListener[] newListeners = new IStatusChangeListener[oldListeners.length - 1];
          if (index > 0) {
            System.arraycopy(oldListeners, 0, newListeners, 0, index);
          }
          if (index < newListeners.length) {
            System.arraycopy(oldListeners, index + 1, newListeners, index, newListeners.length - index);
          }

          listeners.put(dk, newListeners);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("Successfully removed StatusChangeListener " + toBeRemoved);
        }

      }

      UndeploymentHandler oldUndeploymentHandler = undeploymentHandlers.remove(toBeRemoved);
      if (oldUndeploymentHandler != null) {
        XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl()
                        .getAutomaticUnDeploymentHandlerManager().removeUnDeploymentHandler(oldUndeploymentHandler);
      }

    } finally {
      writeLock.unlock();
    }

  }


  /**
   * Special treatment of Manual Interaction: Parent workflows are notified as well, but with a different status
   * ('Manual Interaction in Subworkflow')
   * @param xo
   * @param oldState If this is set, the method call is interpreted as a termination of the manual interaction state
   *          and this old state is propagated
   */
  public void notifyListenersMI(XynaOrderServerExtension xo, OrderInstanceStatus oldState) {

    if (oldState == null) {
      if (logger.isTraceEnabled()) {
        logger.trace("Notifying listener to id " + xo.getId() + " of status transition to '"
                        + OrderInstanceSuspensionStatus.MANUAL_INTERACTION + "'");
      }
    } else {
      if (logger.isTraceEnabled()) {
        logger.trace("Notifying listener to id " + xo.getId() + " of status transition to '" + oldState + "'");
      }
    }

    if (oldState != null) {
      notifyListeners(xo, oldState);
      return;
    }

    final long sourceId = xo.getId();

    notifyListeners(xo, OrderInstanceSuspensionStatus.MANUAL_INTERACTION);
    xo = xo.getParentOrder();

    while (xo != null) {
      notifyListenersMIIndirect(xo, sourceId);
      xo = xo.getParentOrder();
    }

  }


  public void notifyListenersMIIndirect(XynaOrderServerExtension xo, long sourceId) {
    if (logger.isTraceEnabled()) {
      logger.trace("Notifying listener to id " + xo.getId() + " of indirect status transition to '"
                      + OrderInstanceSuspensionStatus.MANUAL_INTERACTION_IN_SUBWF + "'.");
    }
    notifyListeners(xo, OrderInstanceSuspensionStatus.MANUAL_INTERACTION_IN_SUBWF.getName(), sourceId);
  }


  /**
   * Notifies all listeners that are registered for the DestinationKey that is provided by the XynaOrder.
   */
  private void notifyListeners(XynaOrderServerExtension xo, String newState, Long sourceId) {

    if (logger.isTraceEnabled()) {
      logger.trace("calling statuschangelisteners for order " + xo.toString() + " for new state '" + newState + "'");
    }

    if (!xo.getInformStateTransitionListeners()) {
      return;
    }

    IStatusChangeListener[] copy;
    readLock.lock();
    try {
      //TODO performance: die sicherheitskopie könnte man auch nur einmal machen und sich dann in der xynaorder merken.
     
      //vielleicht ist das sogar ein bug, und man MUSS das machen. ansonsten werden nicht alle zustandsübergänge
      //eines auftrags innerhalb eines listeners ausgeführt
      //achtung: transactionmgmt verlässt sich auf das bisherige feature!!

      IStatusChangeListener[] relevantListeners = listeners.get(xo.getDestinationKey());

      if (relevantListeners == null || relevantListeners.length == 0) {
        xo.setInformStateTransitionListeners(false); //nicht nochmal versuchen
        return;
      }

      int l = relevantListeners.length;
      //ungelockt auf kopie arbeiten, falls statusChanged länger dauert
      copy = new IStatusChangeListener[l];
      System.arraycopy(relevantListeners, 0, copy, 0, l);

    } finally {
      readLock.unlock();
    }

    Long orderId = Long.valueOf(xo.getId());

    for (IStatusChangeListener listener : copy) {
      try {
        listener.statusChanged(orderId, newState, sourceId);
      } catch (Throwable t) {
        Department.handleThrowable(t);
        // there is nothing more we can do, reasonable exception handling has to be done inside
        logger.error("Error while notifying status change listeners", t);
      }
    }

  }


  public void notifyListeners(XynaOrderServerExtension xo, OrderInstanceStatus newState) {
    notifyListeners(xo, newState.getName(), null);
  }
  public void notifyListeners(XynaOrderServerExtension xo, OrderInstanceSuspensionStatus newState) {
    notifyListeners(xo, newState.getName(), null);
  }
  public void notifyListeners(XynaOrderServerExtension xo, OrderInstanceCompensationStatus newState) {
    notifyListeners(xo, newState.getName(), null);
  }

}
