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
package gip.base.callback;

import gip.base.common.OBAttribute;
import gip.base.common.OBDTOInterface;
import gip.base.common.OBException;

import java.io.Serializable;
import java.util.HashMap;

/**
 * DecisionContainer ist ein Datenkontainer f�r Callbacks vom Server an den Client. 
 * Clientseitig wird dann dieser Datenkontainer grafisch aufbereitet.
 */
@SuppressWarnings("serial")
public class DecisionContainer implements OBDTOInterface, Serializable {


  public final static String TERMINATE_TOKEN="$CLIENT_SERVERTRACKER_MUST_DIE$"; //$NON-NLS-1$

  public final static int BOOLEAN_DECISION=10;
  public final static int MULTIPLE_DECISION=20;
  public final static int WARNING_DECISION=30;
  public final static int VALUE_DECISION=40;
  public final static int NOTIFICATION=50;
  public final static int CCBREPORTFORM=100;

  
  private String _messageText;
  private String[] _multipleChoices;

  private String _answerString;
  private boolean _answerBoolean;

  private String _defaultAnswerString;
  private boolean _defaultAnswerBoolean;
  
  private String _trueText;
  private String _falseText;
  
  private String _defaultAnswerStringGUI;
  private Boolean _defaultAnswerGUI;
  
  private int _decMode=Integer.MIN_VALUE;

  
  /**
   * Default-Konstruktor. 
   * Dieser belegt auch schon die 'Question' mit 'NOT INITILIZED' vor.
   */
  public DecisionContainer() {
    _messageText="NOT INITILIZED"; //$NON-NLS-1$
    _multipleChoices = new String[] {"OK", "CANCEL"};//$NON-NLS-1$//$NON-NLS-2$
    _trueText = "Ja";//$NON-NLS-1$
    _falseText = "Nein";//$NON-NLS-1$
    _defaultAnswerStringGUI = "";//$NON-NLS-1$
  }
  
  
  /**
   * @see gip.base.common.OBDTOInterface#getCapabilityId()
   */
  public long getCapabilityId() {
    return OBAttribute.NULL;
  }


  /**
   * @see gip.base.common.OBDTOInterface#setCapabilityId(long)
   */
  public void setCapabilityId(long _capabilityId) throws OBException {
    throw new OBException(OBException.OBErrorNumber.capabilityUnknown);
  }


  /**
   * @return Messagetext
   */
  public String getMessageText() {
    return _messageText;
  }


  /**
   * @param text
   */
  public void setMessageText(String text) {
    _messageText = text;
  }


  /**
   * @return AntwortString
   */
  public String getAnswerString() {
    return _answerString;
  }


  /**
   * @param answer 
   */
  public void setAnswerString(String answer) {
    this._answerString = answer;
  }


  /**
   * @return Returns the _defaultAnswer.
   */
  public String getDefaultAnswerString() {
    return _defaultAnswerString;
  }


  /**
   * @param defaultAnswer The _defaultAnswer to set.
   */
  public void setDefaultAnswerString(String defaultAnswer) {
    this._defaultAnswerString = defaultAnswer;
  }


  /**
   * @return Returns the _posibleAnswers.
   */
  public String[] getMultipleChoices() {
    return _multipleChoices;
  }


  /**
   * @param mc The _posibleAnswers to set.
   */
  public void setMultipleChoices(String[] mc) {
    _multipleChoices = mc;
  }

  
  /**
   * @return Returns the _answerBoolean.
   */
  public boolean getAnswerBoolean() {
    return _answerBoolean;
  }

  
  /**
   * @param boolean1 The _answerBoolean to set.
   */
  public void setAnswerBoolean(boolean boolean1) {
    _answerBoolean = boolean1;
  }

  
  /**
   * @return Returns the _defaultAnswerBoolean.
   */
  public boolean getDefaultAnswerBoolean() {
    return _defaultAnswerBoolean;
  }

  
  /**
   * @param answerBoolean The _defaultAnswerBoolean to set.
   */
  public void setDefaultAnswerBoolean(boolean answerBoolean) {
    _defaultAnswerBoolean = answerBoolean;
  }


  /**
   * @return Returns the _falseText.
   */
  public String getFalseText() {
    return _falseText;
  }
  
  
  /**
   * @param text The _falseText to set.
   */
  public void setFalseText(String text) {
    _falseText = text;
  }
  
  
  /**
   * @return Returns the _trueText.
   */
  public String getTrueText() {
    return _trueText;
  }
  
  
  /**
   * @param text The _trueText to set.
   */
  public void setTrueText(String text) {
    _trueText = text;
  }


  /**
   * @return Returns the _mode.
   */
  public int getDecisionMode() {
    return _decMode;
  }

  
  /**
   * @param mode The _mode to set.
   */
  public void setDecisionMode(int mode) {
    this._decMode = mode;
  }

  /** 
   * Setzt eine andere DefaultAnswer die von 
   * abgeleiteten Klassen die eine GUI bedienen
   * ausgewertet werden kann.
   * @param da
   */
  public void setDefaultAnswerGUI(String da) {
    _defaultAnswerStringGUI=da;
  }

  
  /**
   * Setzt eine andere DefaultAnswer die von 
   * abgeleiteten Klassen die eine GUI bedienen
   * ausgewertet werden kann.
   * @param da
   */
  public void setDefaultAnswerGUI(boolean da) {
    _defaultAnswerGUI= new Boolean(da);
  }
  
  /**
   * Liefert die expliziet gesetzte GUI-DefaultAnswer,
   * falls dieser nicht gesetzt ist, die prim�ren DefaultAnswer.
   * @return Default fuer GUI
   */
  public boolean getDefaultAnswerGUI() {
    if (_defaultAnswerGUI != null) {
      return _defaultAnswerGUI.booleanValue();
    } 
    else {
      return getDefaultAnswerBoolean();
    }
  }


  /**
   * Liefert die expliziet gesetzte GUI-DefaultAnswer,
   * falls dieser nicht gesetzt ist, die prim�ren DefaultAnswer.
   * @return Default fuer GUI
   */
  public String getDefaultAnswerStringGUI() {
    if (_defaultAnswerStringGUI != null && _defaultAnswerStringGUI.length() > 0) {
      return _defaultAnswerStringGUI;
    } 
    else {
      return getDefaultAnswerString();
    }
  }

  public HashMap<String, Object> convertToHashMap(String key) {
    return new HashMap<String, Object>();
  }

}


