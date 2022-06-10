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
package com.gip.xyna.xnwh.persistence.xmom;


//could be server side representation for xmom-obj
public class DeleteParameter {
  
  private boolean includingHistory;
  private ForwardReferenceHandling forwardReferenceHandling;
  private BackwardReferenceHandling backwardReferenceHandling;
  
  public DeleteParameter(boolean includingHistory) {
    this(includingHistory, (String)null, (String)null);
  }
  
  public DeleteParameter(boolean includingHistory, String forwardReferenceHandling, String backwardReferenceHandling) {
    this(includingHistory, 
         ForwardReferenceHandling.getByStringRepresentation(forwardReferenceHandling), 
         BackwardReferenceHandling.getByStringRepresentation(backwardReferenceHandling));
  }
  
  public DeleteParameter(boolean includingHistory, ForwardReferenceHandling forwardReferenceHandling, BackwardReferenceHandling backwardReferenceHandling) {
    this.includingHistory = includingHistory;
    this.forwardReferenceHandling = forwardReferenceHandling;
    this.backwardReferenceHandling = backwardReferenceHandling;
  }
  
  public boolean doIncludeHistory() {
    return includingHistory;
  }
  
  public ForwardReferenceHandling getForwardReferenceHandling() {
    return forwardReferenceHandling;
  }
  
  public BackwardReferenceHandling getBackwardReferenceHandling() {
    return backwardReferenceHandling;
  }
  
  
 /**
  * - recursive: Alle abhängigen XMOM Storables rekursiv löschen. Dabei wird jeweils auch das Verhalten für Rückwärtsreferenzen beachtet.
  * - keep: Referenzierte Objekte werden nicht gelöscht sondern bleiben bestehen
  */
  public static enum ForwardReferenceHandling {
    RECURSIVE_DELETE("recursive"), KEEP("keep");
    
    private final String stringRepresentation;
    
    private ForwardReferenceHandling(String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }
    
    public static ForwardReferenceHandling getByStringRepresentation(String stringRepresentation) {
      if (stringRepresentation != null) {
        String loweredStringRepresentation = stringRepresentation.toLowerCase();
        for (ForwardReferenceHandling value : values()) {
          if (value.stringRepresentation.equals(loweredStringRepresentation)) {
            return value;
          }
        }
      }
      return ForwardReferenceHandling.KEEP;
    }
    
  }
  
  
  /**
   * - cascade: Alle XMOM Storables, die ein zu löschendes XMOM Storable referenzieren, werden ebenso gelöscht. Rekursiv
   * - error: Default. Fehler, falls eine solche Abhängigkeit festgestellt wird
   * - delete: Das Objekt wird entfernt. Andere XMOM Storables, die dieses Objekt referenzieren, referenzieren es jetzt nicht mehr, sind aber noch funktionstüchtig.
   * 
   * - ignore is only to be used internally, no handling for backward references will be performed
   */
  public static enum BackwardReferenceHandling {
    CASCADE("cascade"), ERROR("error"), DELETE("delete"), IGNORE("ignore");
    
    private final String stringRepresentation;
    
    private BackwardReferenceHandling(String stringRepresentation) {
      this.stringRepresentation = stringRepresentation;
    }
    
    public static BackwardReferenceHandling getByStringRepresentation(String stringRepresentation) {
      if (stringRepresentation != null) {
        String loweredStringRepresentation = stringRepresentation.toLowerCase();
        for (BackwardReferenceHandling value : values()) {
          if (value.stringRepresentation.equals(loweredStringRepresentation)) {
            return value;
          }
        }
      }
      return BackwardReferenceHandling.ERROR;
    }
    
  }

}
