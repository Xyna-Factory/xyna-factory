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
package com.gip.xyna.xprc.xfractwfe.base;



import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.XynaOrderServerExtension;



public abstract class DefaultSubworkflowCall<T extends Scope> extends FractalProcessStep<T> implements SubworkflowCall {

  private static final long serialVersionUID = 1L;
  protected boolean firstExecution = true;
  protected XynaOrderServerExtension subworkflow;


  public DefaultSubworkflowCall(int i) {
    super(i);
  }


  @Override
  public XynaOrderServerExtension getChildOrder() {
    try {
      lazyCreateSubWf();
    } catch (XynaException e) {
    }
    return subworkflow;
  }


  protected void reinitialize() {
    super.reinitialize();
    subworkflow = null;
    firstExecution = true;
  }


  protected abstract void lazyCreateSubWf() throws XynaException;


  @Override
  public void compensateInternally() throws XynaException {
    if (subworkflow != null) {
      com.gip.xyna.XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().compensateOrderSynchronously(subworkflow);
    }
  }


  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //readobject methode wegen bug 20384
    //passiert genau dann, wenn durch klassenänderung ein ehemals teilausgeführter nicht-subworkflow-step beim deserialisieren durch einen subworkflow-step ersetzt wird
    //dann ist firstExecution == false und subworkflow == null. wenn man diesen fix nicht einbaut, würde beim resume der subworkflow kein planning durchführen.
    //trifft zwar auch bereits beendete detached subworkflow-aufrufe, aber das macht nichts. da ist dann halt das flag merkwürdig gesetzt...
    s.defaultReadObject();
    if (subworkflow == null && !firstExecution) {
      firstExecution = true;
    }
  }
  
  public boolean removeXynaOrderReference(long orderId) {
    if (subworkflow != null && subworkflow.getId() == orderId) {
      subworkflow = null;
      return true;
    }
    return false;
  }
  
  //passiert in der compensation irgendwas im subworkflow?
  //true, falls bei der compensation in den subworkflow hinabgestiegen werden muss
  //false, falls compensation überschrieben ist, oder der subworkflow (rekursiv) selbst keine compensation-schritte enthält
  public abstract boolean compensationRecursive();
}
