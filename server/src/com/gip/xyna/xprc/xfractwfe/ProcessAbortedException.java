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



import com.gip.xyna.xprc.xsched.orderabortion.AbortionCause;


// this ist mostly a copy of ProcessSuspendedException
// FIXME This is not a part of the Workflow Engine! It is used all over the processing. refactoring is not so easy
//       when taking into account backward compatibility since the exception may be part of serialized objects
public class ProcessAbortedException extends RuntimeException {

  private static final long serialVersionUID = -4756037101655324923L;
  
  final private boolean abortParentOrder = true;
  private Long originOrderId;

  /**
   * @deprecated wegen serialisierungs-abw�rtskompatibilit�t noch drin
   */
  @Deprecated
  final private boolean needToFreeCapacities = true;
  
  /**
   * @deprecated wegen serialisierungs-abw�rtskompatibilit�t noch drin
   */
  @Deprecated
  final private boolean needToFreeVetos = true;
  private AbortionCause abortionCause;


  public ProcessAbortedException(AbortionCause abortionCause) {
    this(null, abortionCause);
  }


  public ProcessAbortedException(Long originOrderId) {
    this(originOrderId, AbortionCause.UNKNOWN);
  }


  public ProcessAbortedException(Long originOrderId, AbortionCause abortionCause) {
    super("Order " + originOrderId + " has been aborted.");
    this.originOrderId = originOrderId;
    this.abortionCause = abortionCause;
  }


  public ProcessAbortedException(ProcessAbortedException cause) {
    this(cause.getOriginOrderId(), cause.getAbortionCause(), cause);
  }


  private ProcessAbortedException(Long originOrderId, AbortionCause abortionCause, ProcessAbortedException cause) {
    super("Order " + originOrderId + " has been aborted.", cause);
    this.originOrderId = originOrderId;
    this.abortionCause = abortionCause;
  }


  public boolean needsToAbortParentOrderEvenIfNotAborted() {
    return abortParentOrder;
  }


  public Long getOriginOrderId() {
    return originOrderId;
  }


  public void setOriginId(Long originOrderId) {
    this.originOrderId = originOrderId;
  }


  public AbortionCause getAbortionCause() {
    return abortionCause;
  }


  public void setSuspensionCause(AbortionCause abortionCause) {
    this.abortionCause = abortionCause;
  }

  
}
