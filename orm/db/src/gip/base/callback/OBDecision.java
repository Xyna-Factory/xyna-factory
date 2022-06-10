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

/** 
 * Zum Treffen von Entscheidungen aus unterschiedlichen Quellen.
 * Z.B. anhand eines Dialogs oder einer Voreinstellung.
 */
@SuppressWarnings("serial")
abstract public class OBDecision extends Object implements java.io.Serializable {

  private String _msgText = "";//$NON-NLS-1$
  private String _textTrue = "ja";//$NON-NLS-1$
  private String _textFalse = "nein";//$NON-NLS-1$
  private boolean _defaultAnswer;
  private String _defaultAnswerString;
  private String[] _multipleChoices;
  private boolean _defaultAnswerGUI;
  private String _defaultAnswerStringGUI;
  private boolean _defaultAnswerGUI_ISSET=false;
  private boolean _defaultAnswerStringGUI_ISSET=false;

  /**
   * Liefert true oder false als Entscheidung zurück
   * @return Boolean-Entscheidung
   */
  abstract public boolean decideBoolean();

  /**
   * 
   * @return Entscheidung
   */
  public boolean decide() {
    return decideBoolean(); 
  }

  /**
   * Liefert die Entscheidung als String zurück, 
   * nützlich bei mehreren Wahlmöglichkeiten.
   * @return Entscheidung
   */
  abstract public String decideMultipleChoice();

  public String xdecide() {
    return decideMultipleChoice();
  }
  
  /**
   * Liefert nur eine Warnung, die i.d.R. bestätigt werden muss,
   * die abgeleiteten Klassen müssen damit entsprechen umgehen. 
   */
  abstract public void printWarning();  
  

  /**
   * Fragt nach einen Wert der eingegeben werden kann (GUI).
   * Bei nichtgrafischen abgeleiteten Klassen wird hier 
   * die Default-Answer zurückgeliefert.
   * @return Wert
   */
  abstract public String askForValue();

  
  /**
   * Informiert den Benutzer
   */
  abstract public void notification();
  
  /**
   * Setzt den Text der Frage. 
   * Desweiteren wird das Objekt zurückgesetzt.
   * Dies muss hier geschen, 
   * da immer ein und das selbe Objekt neu konfiguriert wird
   * und zur Abfrage verwendet wird.
   * @param msgText
   */
  public void setMessageText(String msgText) {
    // init
    _textTrue = "ja";//$NON-NLS-1$
    _textFalse = "nein";//$NON-NLS-1$
    _defaultAnswer=false;
    _defaultAnswerString=null;
    _multipleChoices=null;
    _defaultAnswerGUI=false;
    _defaultAnswerStringGUI="";//$NON-NLS-1$
    _defaultAnswerGUI_ISSET=false;
    _defaultAnswerStringGUI_ISSET=false;

    // MsgText setzen
    _msgText = msgText;
  }

  /**
   * @param msgText Text der Message
   */
  public void setText(String msgText) {
    setMessageText(msgText);
  }
  
  /**
   * @param text Text fuer true
   */
  public void setTrueText(String text) {
    _textTrue = text;
  }

  /**
   * @param text Text fuer True
   */
  public void setTextTrue(String text) {
    setTrueText(text);
  }
  
  /**
   * @param text Text fuer false
   */
  public void setFalseText(String text) {
    _textFalse = text;
  }

  /**
   * @param text Text fuer false
   */
  public void setTextFalse(String text) {
    setFalseText(text);
  }

  /**
   * Setzt eine Menge von Entscheidungsmöglichkeiten.
   * @param mc Mehrere Strings
   */
  public void setMultipleChoices(String[] mc) {
    _multipleChoices = mc;
    _defaultAnswerString = mc[0];
  }

  /**
   * @param mc Mehrere Strings
   */
  public void setTextArray(String[] mc) {
    setMultipleChoices(mc);
  }

  /**
   * Setzt die primäre DefaultAnswer die von 
   * abgeleiteten Klassen ausgewertet wird.
   * @param da Default
   */
  public void setDefaultAnswer(boolean da) {
    _defaultAnswer = da;
  }
  

  /**
   * Setzt die primäre DefaultAnswer die von 
   * abgeleiteten Klassen ausgewertet wird.
   * @param da Default
   */
  public void setDefaultAnswer(String da) {
    _defaultAnswerString = da;
  }
  

  /** 
   * Setzt eine andere DefaultAnswer die von 
   * abgeleiteten Klassen die eine GUI bedienen
   * ausgewertet werden kann.
   * @param da Default fuer GUI
   */
  public void setDefaultAnswerGUI(String da) {
    _defaultAnswerStringGUI=da;
    _defaultAnswerStringGUI_ISSET=true;
  }

  
  /**
   * Setzt eine andere DefaultAnswer die von 
   * abgeleiteten Klassen die eine GUI bedienen
   * ausgewertet werden kann.
   * @param da Default fuer GUI
   */
  public void setDefaultAnswerGUI(boolean da) {
    _defaultAnswerGUI=da;
    _defaultAnswerGUI_ISSET=true;
  }


  /**
   * @return MessageText
   */
  public String getMessageText() {
    return _msgText;
  }
  
  
  /**
   * @return Text fuer True
   */
  protected String getTrueText() {
    return _textTrue;
  }


  /**
   * @return Text fuer false
   */
  protected String getFalseText() {
    return _textFalse;
  }

  
  /**
   * Liefert die möglichen Entscheidungen.
   * @return Multiple-Choice-Texte
   */
  protected String[] getMultipleChoices() {
    return _multipleChoices;
  }


  /**
   * @return Default-Antwort (true/false)
   */
  protected boolean getDefaultAnswer() {
    return _defaultAnswer;
  }


  /**
   * @return Default-Antwort bei Multiple Choice
   */
  protected String getDefaultAnswerString() {
    return _defaultAnswerString;
  }


  /**
   * Liefert die expliziet gesetzte GUI-DefaultAnswer,
   * falls dieser nicht gesetzt ist, die primären DefaultAnswer.
   * @return GUI-Default bei boolean
   */
  protected boolean getDefaultAnswerGUI() {
    if (_defaultAnswerGUI_ISSET==true) {
      return _defaultAnswerGUI;
    } 
    else {
      return getDefaultAnswer();
    }
  }


  /**
   * Liefert die expliziet gesetzte GUI-DefaultAnswer,
   * falls dieser nicht gesetzt ist, die primären DefaultAnswer.
   * @return GUI-Default bei Multiple Choice
   */
  protected String getDefaultAnswerStringGUI() {
    if (_defaultAnswerStringGUI_ISSET==true) {
      return _defaultAnswerStringGUI;
    } 
    else {
      return getDefaultAnswerString();
    }
  }

}


