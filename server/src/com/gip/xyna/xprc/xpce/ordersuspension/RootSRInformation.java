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

package com.gip.xyna.xprc.xpce.ordersuspension;

import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.StrongWeakReference;
import com.gip.xyna.xprc.xpce.ordersuspension.SuspendResumeAlgorithm.ResumeResult;
import com.gip.xyna.xprc.xpce.ordersuspension.interfaces.SuspendResumeAdapter;


/**
 *
 */
public class RootSRInformation<O> extends SRInformation {
  private static Logger logger = CentralFactoryLogging.getLogger(RootSRInformation.class);
  
  private StrongWeakReference<O> orderReference;
  private RootOrderSuspension rootOrderSuspension;
  private boolean orderHasRedirection; 
 
  public RootSRInformation(Long orderId, SRState state) {
    super(orderId, state);
  }
  
  public void convertOrderReferenceToWeak() {
    orderReference.convertToWeak();
  }
  
  public boolean convertOrderReferenceToStrong() {
    return orderReference.convertToStrong();
  }
  
  public O getOrder() {
    return orderReference == null ? null : orderReference.get();
  }
  
  public boolean setOrder(O order) {
    O curr = orderReference == null ? null : orderReference.get();
    if( curr == null ) {
      orderReference = new StrongWeakReference<O>(order);
      return true;
    } else if( curr != order ) { //!= ist richtig, Objekt-Identit�t n�tig
      if( logger.isDebugEnabled() ) {
        logger.debug("RootSRInformation has reference to old order "+curr+", new order is "+order);
      }
      orderReference.replace(order);
      return true;
    } else {
      return false;
    }
  }
    
  public RootOrderSuspension getRootOrderSuspension() {
    return rootOrderSuspension;
  }
  
  public void setRootOrderSuspension(RootOrderSuspension rootOrderSuspension) {
    this.rootOrderSuspension = rootOrderSuspension;
  }

  public boolean isMINecessary() {
    if( rootOrderSuspension == null ) return false;
    if( rootOrderSuspension.getManualInteractionData() == null ) return false;
    return rootOrderSuspension.getManualInteractionData().isMINecessary();
  }

  /**
   * @param targets
   * @return
   */
  public Pair<ResumeResult,String> checkResume(List<ResumeTarget> targets, SuspendResumeAdapter<?,O> srAdapter) {
    if( rootOrderSuspension != null ) {
      if( rootOrderSuspension.addResumeTargets(targets) ) {
        //RootOrder wird derzeit suspendiert, daher dort nur ResumeTargets vermerkt. 
        //Suspendierung soll nicht behindert werden, daher hier fertig.
        return Pair.of( ResumeResult.Resumed, "currently suspending");
      }
      if( rootOrderSuspension.isMINecessary() ) {
        /*
         * Auftrag hatte Fehler bei Thread.interrupt() oder Thread.stop(). Daher muss es eine Redirection-MI
         * geben, die vom Benutzer bearbeitet werden kann. Bei Bearbeitung wird dann das Resume ausgef�hrt. 
         */  
        return Pair.of(ResumeResult.Unresumeable, SuspendResumeManagement.UNRESUMABLE_MI_REDIRECTION);
      }
    }
    if( orderHasRedirection ) {
      //Auftrag hat eine MI-Redirection, daher darf er nicht resumt werden.
      //Durch die Redirection werden alle Lanes resumt, daher �bergebene ResumeTargets nicht speichern. 
      return Pair.of(ResumeResult.Unresumeable, SuspendResumeManagement.UNRESUMABLE_MI_REDIRECTION);
    }
    return Pair.of(null,null);
  }

  public void setOrderHasRedirection(boolean orderHasRedirection) {
    this.orderHasRedirection = orderHasRedirection;
  }

  public static RootOrderSuspension getRootOrderSuspension(SRInformation srInformation) {
    if( srInformation instanceof RootSRInformation ) {
      RootSRInformation<?> rootSRInformation = (RootSRInformation<?>)srInformation;
      return rootSRInformation.getRootOrderSuspension();
    }
    return null;
  }

}
