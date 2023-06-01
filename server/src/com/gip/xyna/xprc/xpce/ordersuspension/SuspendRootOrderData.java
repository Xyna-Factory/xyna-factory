/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 * Konfiguration der Root-Order-Suspendierung:
 * <ul>
 * <li>eine oder mehrere RootOrderIds</li>
 * <li>SuspensionTimedOutAction: Behandlung bei Timeout:
 *    <ul>
 *    <li>None: keine gesonderte Behandlung</li>
 *    <li>Interrupt: alle ausführenden Thread erhalten ein Interrupt</li>
 *    </ul></li>
 * <li>SuspensionFailedAction: Behandlung im Fehlerfall "Suspendierung nicht fertig":
 *    <ul>
 *    <li>UndoSuspensions: Bisherige Suspendierungen werden rückgängig gemacht</li>
 *    <li>StopSuspending: Es werden keine weiteren Suspendierungen durchgeführt, bestehende Suspendierungen werden nicht rückgängig gemacht</li>
 *    <li>KeepSuspending: Es wird weiter suspendiert, bestehende Suspendierungen werden nicht rückgängig gemacht</li>
 *    </ul></li>
 * <li>Timeout und SuspensionCause</li>
 * </ul>
 * Als Rückgabe werden folgende Daten eingetragen
 * <ul>
 * <li>getSuspensionResult() {Suspended, Timeout, Failed}</li>
 * <li>getResumeTargets() alle bekannten ResumeTargets (durch diese Suspendierung bewirkt)</li>
 * <li>getSuspensionNotFinished() RootOrderIds, bei denen die Suspendierung nicht abgeschlossen ist</li>
 * </ul>
 * Im Fehlerfall SuspensionResult.Failed sind noch folgende Daten abfragbar:
 * <ul>
 * <li>getFailedResumes() Fehlgeschlagene Resumes bei SuspensionResult.Failed und SuspensionFailedAction.UndoSuspensions</li>
 * <li>getNotSuspended() RootOrderIds, bei denen Suspendierung nicht begonnen wurde bei SuspensionResult.Failed</li>
 * </ul>
 */
public class SuspendRootOrderData {

  private Set<Long> rootOrderIds;
  private SuspensionCause suspensionCause;
  private Long timeoutInMillis; 
  private SuspensionSucceededAction suspensionSucceededAction;
  private SuspensionTimedOutAction suspensionTimedOutAction;
  private SuspensionFailedAction suspensionFailedAction;
  private List<ResumeTarget> resumeTargets;
  private SuspensionResult suspensionResult;
  private Map<Long, String> failedSuspensions;
  private Set<Long> suspensionNotFinished;
  private List<Triple<RootOrderSuspension, String, PersistenceLayerException>> failedResumes;
  private boolean failFast;
  private List<Pair<Long,Long>> miRedirections;
  
  public SuspendRootOrderData() {
    this.rootOrderIds = new HashSet<Long>();
    this.suspensionSucceededAction = SuspensionSucceededAction.None;
    this.suspensionTimedOutAction = SuspensionTimedOutAction.None;
    this.suspensionFailedAction = SuspensionFailedAction.KeepSuspending;
    this.resumeTargets = new ArrayList<ResumeTarget>();
    this.failedSuspensions = Collections.emptyMap();
    this.suspensionNotFinished = new HashSet<Long>();
    this.miRedirections = new ArrayList<Pair<Long,Long>>();
  }
  
  public SuspendRootOrderData(Set<Long> rootOrderIds) {
    this();
    this.rootOrderIds.addAll(rootOrderIds);
  }
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SuspendRootOrderData(");
    if( rootOrderIds.size() == 1 ) {
      sb.append(rootOrderIds.iterator().next());
    } else {
      sb.append(rootOrderIds.size()).append(" root orders");
    }
    sb.append(", ");
    sb.append(suspensionSucceededAction).append(",");
    sb.append(suspensionTimedOutAction).append(",");
    sb.append(suspensionFailedAction).append(",");
    sb.append(timeoutInMillis);
    sb.append(")");
    return sb.toString();
  }
  
  public enum SuspensionTimedOutAction {
    None,
    Interrupt,
    Stop;
  }
  public enum SuspensionSucceededAction {
    None,
    KeepUnresumeable;
  }
  
  public enum SuspensionFailedAction {
    /**
     * Bisherige Suspendierungen rückgängig machen
     */
    UndoSuspensions,
    /**
     * Nicht weiter suspendieren, bestehende Suspendierungen nicht rückgängig machen
     */
    StopSuspending,
    /**
     * Weiter suspendieren, bestehende Suspendierungen nicht rückgängig machen
     */
    KeepSuspending;
  }
  
  public enum SuspensionResult {
    Suspended, Timeout, Failed;
  }
  

  
  public SuspendRootOrderData addRootOrderId(Long rootOrderId) {
    if( rootOrderIds == null ) {
      rootOrderIds = new HashSet<Long>();
    }
    rootOrderIds.add(rootOrderId);
    return this;
  }
  
  public SuspendRootOrderData timeout(long timout, TimeUnit unit) {
    timeoutInMillis = TimeUnit.MILLISECONDS.convert(timout, unit);
    return this;
  }

  public SuspendRootOrderData suspensionCause(SuspensionCause suspensionCause) {
    this.suspensionCause = suspensionCause;
    return this;
  }
  
  public SuspendRootOrderData suspensionSuccededAction(SuspensionSucceededAction suspensionSucceededAction) {
    this.suspensionSucceededAction = suspensionSucceededAction;
    return this;
  }

  public SuspendRootOrderData suspensionTimedOutAction(SuspensionTimedOutAction suspensionTimedOutAction) {
    this.suspensionTimedOutAction = suspensionTimedOutAction;
    return this;
  }

  public SuspendRootOrderData suspensionFailedAction(SuspensionFailedAction suspensionFailedAction) {
    this.suspensionFailedAction = suspensionFailedAction;
    return this;
  }
  
  public SuspendRootOrderData failFast(boolean failFast) {
    this.failFast = failFast;
    return this;
  }

  /**
   * @param resumeTargets
   * @return
   * package-private
   */
  SuspendRootOrderData resumeTargets(List<ResumeTarget> resumeTargets) {
    this.resumeTargets.addAll(resumeTargets);
    return this;
  }
  /**
   * @param suspendingOrders
   * package-private
   */
  SuspendRootOrderData addResumeTargets(List<RootOrderSuspension> rootOrderSuspensions) {
    for( RootOrderSuspension ros : rootOrderSuspensions ) {
      if( ros.isMINecessary() ) {
        this.miRedirections.add( Pair.of(ros.getRootOrderId(), ros.getManualInteractionData().getMIOrderId()) );
      } else {
        this.resumeTargets.addAll( ros.getResumeTargets() );
      }
    }
    return this;
  }
  SuspendRootOrderData addResumeTargets(RootOrderSuspension rootOrderSuspension) {
    this.resumeTargets.addAll( rootOrderSuspension.getResumeTargets() );
    return this;
  }
  
  /**
   * @param suspensionResult
   * package-private
   */
  SuspendRootOrderData suspensionResult(SuspensionResult suspensionResult) {
    this.suspensionResult = suspensionResult;
    return this;
  }
  /**
   * @param second
   * package-private
   */
  void failedSuspensions(Map<Long, String> failedSuspensions) {
    this.failedSuspensions = failedSuspensions;
  }
  /**
   * @param rootOrderId
   * package-private
   */
  void addSuspensionNotFinished(Long rootOrderId) {
    this.suspensionNotFinished.add(rootOrderId);
  }

  /**
   * @param failedResumes
   * package-private
   */
  void failedResumes(List<Triple<RootOrderSuspension, String, PersistenceLayerException>> failedResumes) {
    this.failedResumes = failedResumes;
  }

  public long getTimeout() {
    long timeout = 0;
    if( timeoutInMillis == null ) {
      timeout = XynaProperty.TIMEOUT_SUSPENSION.getMillis();
    } else {
      timeout = timeoutInMillis;
    }
    switch( suspensionTimedOutAction ) {
      case None:
        return timeout;
      case Interrupt:
        return timeout/2;//Timeout halbieren, da zweimal gewartet wird
      case Stop:
        return timeout/3;//Timeout dritteln, da dreimal gewartet wird
      default:
        return timeout;
    }
  }

  public Set<Long> getRootOrderIds() {
    return rootOrderIds;
  }

  public SuspensionCause getSuspensionCause() {
    return suspensionCause;
  }
  
  public SuspensionSucceededAction getSuspensionSucceededAction() {
    return suspensionSucceededAction;
  }
  
  public SuspensionTimedOutAction getSuspensionTimedOutAction() {
    return suspensionTimedOutAction;
  }
  
  public SuspensionFailedAction getSuspensionFailedAction() {
    return suspensionFailedAction;
  }
  
  
  public SuspensionResult getSuspensionResult() {
    return suspensionResult;
  }

  public List<ResumeTarget> getResumeTargets() {
    return resumeTargets;
  }

  public List<Long> getFailedRootOrderIds() {
    List<Long> failedRootOrderIds = new ArrayList<Long>();
    if( failedResumes != null ) {
      for( Triple<RootOrderSuspension, String, PersistenceLayerException> t : failedResumes ) {
        failedRootOrderIds.add( t.getFirst().getRootOrderId() );
      }
    }
    return failedRootOrderIds;
  }

  public List<Triple<RootOrderSuspension, String, PersistenceLayerException>> getFailedResumes() {
    return failedResumes;
  }
 
  public Map<Long, String> getFailedSuspensions() {
    return failedSuspensions;
  }
  
  public Set<Long> getSuspensionNotFinished() {
    return suspensionNotFinished;
  }

  public List<Pair<Long, Long>> getMIRedirections() {
    return miRedirections;
  }
  
  public boolean isFailFast() {
    return failFast;
  }

}
