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
package com.gip.xyna.xact.trigger;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LocalMailStore verwaltet empfangene Mails.
 * 
 * Mails werden nicht sofort aus der Inbox gelöscht, sollen aber ntÃ¼rlich nicht direkt einen 
 * zweiten Auftrag starten. Daher mÃ¼ssen die Mails, fÃ¼r die gerade ein Auftrag läuft, verwaltet werden.
 * Wenn der auftrag fertig ist, kann die Mail aus Inbox und aus LocalMailStore entfernt werden.
 * Auch fÃ¼r Retries werden hier die RetryCounter verwaltet.
 *
 */
public class LocalMailStore implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private ConcurrentHashMap<String,Boolean> receivedMails = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String,Integer> retriedMails = new ConcurrentHashMap<>();
  
  public boolean contains(String messageId) {
    return receivedMails.containsKey(messageId);
  }

  public void add(String messageId) {
    receivedMails.put( messageId, Boolean.TRUE);
  }

  public boolean retry(String messageId, int maxRetries) {
    Integer retry = retriedMails.get(messageId);
    int nextRetry =  retry == null ? 1 : retry.intValue()+1;
    if( nextRetry <= maxRetries ) {
      retriedMails.put(messageId, nextRetry);
      receivedMails.remove(messageId); //soll wieder gelesen werden können
      return true;
    }
    return false; //Retries Ã¼berschritten, daher nun Mail löschen -> Aufgabe des Filters
  }

  public void delete(String messageId) {
    retriedMails.remove(messageId);
    receivedMails.remove(messageId);
  }

  public void notProcessed(String messageId) {
    receivedMails.remove(messageId);// Mail soll wieder gelesen werden können
  }
  
}
