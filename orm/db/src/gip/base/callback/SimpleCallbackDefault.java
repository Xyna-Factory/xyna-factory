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
package gip.base.callback;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;

/**
 * SimpleCallbackRMI
 */
public class SimpleCallbackDefault implements SimpleCallback {
  
  private transient static Logger logger = Logger.getLogger(SimpleCallbackDefault.class);
  

  private OBDecision _decision;
  
  
  /**
   * @throws RemoteException
   */
  public SimpleCallbackDefault() throws RemoteException {
    this(new OBDecisionDefault());
  }

  /**
   * @param decision 
   * @throws RemoteException
   */
  public SimpleCallbackDefault(OBDecision decision) throws RemoteException {
    super();
    this._decision = decision;
  }


  /**
   * 
   */
  public void testOut() throws RemoteException {
    //logger.debug("callback successfull:");
  }
  

  public boolean decideBoolean (DecisionContainer dec) {
    return dec.getDefaultAnswerBoolean();
  }

  public String decideMultipleChoice(DecisionContainer dec) {
    return dec.getDefaultAnswerString();
  }

  public void printWarning(DecisionContainer dec) {
//    decision.setMessageText(dec.getMessageText());
//    decision.printWarning();
    logger.debug(dec.getMessageText());
  }
  
  public String askForValue(DecisionContainer dec) {
    return "";//$NON-NLS-1$
  }
  
  public void notification(DecisionContainer dec) {
    return;
  }
  
  public void setDecision(OBDecision decision) {
    this._decision = new OBDecisionDefault();
  }

  
  public OBDecision getDecision() {
    return _decision;
  }
  
  /**
   * Oeffnet den CCBReport Dialog
   */
  public void openCCBReportForm(String title, long key, int mode, int menuType, String bericht, String ss, String hsList) {
    /* ntbd */
  }
  
  
}


