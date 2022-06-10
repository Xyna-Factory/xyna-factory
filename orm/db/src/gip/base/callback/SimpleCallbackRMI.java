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
package gip.base.callback;

import gip.base.common.OBConstants;

import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

/**
 * SimpleCallbackRMI
 */
@SuppressWarnings("serial")
public class SimpleCallbackRMI extends UnicastRemoteObject implements SimpleCallback {
  
  private transient static Logger logger = Logger.getLogger(SimpleCallbackRMI.class); 
  

  private OBDecision decision;
  
  /**
   * @throws RemoteException
   */
  public SimpleCallbackRMI() throws RemoteException {
    super();
  }
  
  /**
   * @param port 
   * @param range 
   * @throws RemoteException
   */
  public SimpleCallbackRMI(int port, int range) throws RemoteException {
    super(0, new FixedPortSocketFactory(port, range), new FixedPortSocketFactory(port, range));
  }
  
  
  /**
   * 
   */
  public void testOut() throws RemoteException {
    //logger.debug("callback successfull:");
  }
  

  public boolean decideBoolean (DecisionContainer dec) {
    decision.setMessageText(dec.getMessageText());
    decision.setTrueText(dec.getTrueText());
    decision.setFalseText(dec.getFalseText());
    decision.setDefaultAnswer(dec.getDefaultAnswerBoolean());
    decision.setDefaultAnswerGUI(dec.getDefaultAnswerGUI());
    return decision.decideBoolean();
  }
  
  public String decideMultipleChoice(DecisionContainer dec) {
    decision.setMessageText(dec.getMessageText());
    decision.setMultipleChoices(dec.getMultipleChoices());
    decision.setDefaultAnswer(dec.getDefaultAnswerString());
    decision.setDefaultAnswerGUI(dec.getDefaultAnswerStringGUI());
    return decision.decideMultipleChoice();
  }

  public void printWarning(DecisionContainer dec) {
    decision.setMessageText(dec.getMessageText());
    decision.printWarning();
  }
  
  public String askForValue(DecisionContainer dec) {
    decision.setMessageText(dec.getMessageText());
    return decision.askForValue();
  }
  
  public void notification(DecisionContainer dec) {
    decision.setMessageText(dec.getMessageText());
    decision.notification();
  }
  
  public void setDecision(OBDecision decision) {
    this.decision = decision;
  }

  public OBDecision getDecision() {
    return decision;
  }
  
  /**
   * Oeffnet den CCBReport Dialog
   */
  public void openCCBReportForm(String title, long key, int mode, int menuType, String bericht, String ss, String hsList) {
    try {
      Class<?> cls_CCBReportForm = Class.forName("gip.client.aida.CCBReportForm");//$NON-NLS-1$
      Method mth = cls_CCBReportForm.getMethod("openFormAsync", new Class[]{String.class, Long.TYPE, Integer.TYPE, Integer.TYPE, String.class, String.class, String.class, Boolean.TYPE});//$NON-NLS-1$
      mth.invoke(cls_CCBReportForm, new Object[]{title, new Long(key), new Integer(OBConstants.UPDATE), new Integer(0), bericht, ss, hsList, new Boolean(true)});
    }
    catch (Exception e) {
      logger.error("open ccbReportForm",e);//$NON-NLS-1$
    }
  }

}
