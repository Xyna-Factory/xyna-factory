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

import com.gip.xyna.xprc.xpce.ordersuspension.suspensioncauses.SuspensionCause;


/**
 * ProcessSuspendedException transportiert die bei der letzten Ausf�hrung aufgetreten  
 * Suspendierungen in Form einer SuspensionCause.
 */
public class ProcessSuspendedException extends RuntimeException {
  private static final long serialVersionUID = 1L;
    
  private final SuspensionCause suspensionCause;
  
  public ProcessSuspendedException(SuspensionCause supensionCause) {
    this.suspensionCause = supensionCause;
  }
    
  public SuspensionCause getSuspensionCause() {
    return suspensionCause;
  }
  
  @Override
  public String toString() {
    return "ProcessSuspendedException("+"suspensionCause="+suspensionCause+")";
  }

  /**
   * gibt aus performancegr�nden immer this zur�ck, macht aber nichts
   * <p>
   * diese exception ist kein echter fehler sondern nur die einfachste art, den thread zu beenden, ohne in jeder methode
   * auf dem weg eine entsprechende behandlung einzubauen. diese art von exceptions ben�tigen deshalb gar keinen
   * stacktrace, weil dieser nie ausgelesen wird.
   * <p>
   * siehe zb hier: http://www.javaspecialists.eu/archive/Issue129.html
   */
  public Throwable fillInStackTrace() {
    return this;
  }
  
}
