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

import org.apache.log4j.Logger;


/**
 * Liefert eine Entscheidung, die voreingestellt ist.
 */
@SuppressWarnings("serial")
public class OBDecisionDefault extends OBDecision implements java.io.Serializable {

  private transient static Logger logger = Logger.getLogger(OBDecisionDefault.class);
  

  /**
   * Das ist der Konstruktor, der benutzt werden sollte. 
   * Die Werte sind dann in der entsprechenden Klasse zu setzen.
   */
  public OBDecisionDefault() {
    // ntbd
  }


  /**
   * @see gip.base.callback.OBDecision#decideBoolean()
   */
  public boolean decideBoolean() {
    return getDefaultAnswer();
  }


  /**
   * @see gip.base.callback.OBDecision#decideMultipleChoice()
   */
  public String decideMultipleChoice() {
    if (getDefaultAnswerString() == null || getDefaultAnswerString().length() == 0) {
      if (getMultipleChoices() != null && getMultipleChoices().length > 0) {
        return getMultipleChoices()[0];
      }
      else {
        return ""; // Alternative: Exception werfen, da Dialog falsch konfiguriert //$NON-NLS-1$
      }
    }
    return getDefaultAnswerString();
  }


  /**
   * @see gip.base.callback.OBDecision#printWarning()
   */
  public void printWarning() {
    logger.warn(getMessageText());
  }


  /**
   * @see gip.base.callback.OBDecision#askForValue()
   */
  public String askForValue() {
    return getDefaultAnswerString();
  }
  
  /**
   * @see gip.base.callback.OBDecision#notification()
   */
  public void notification() {
    return;
  }
  
}


