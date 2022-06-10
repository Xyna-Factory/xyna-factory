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
package com.gip.xyna.utils.misc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import com.gip.xyna.utils.collections.Pair;


/**
 * CompensatingStack ist ein einfaches Hilfsmittel, um mehrere Operationen nacheinander 
 * ausführen zu können und nach einem Fehler alle bereits ausgeführten Operationen kompensieren
 * zu können. 
 * <br>
 * Beispiel
 * <pre>
   public static void main(String[] args) throws SQLException, RemoteException {
     DoThreeOps dto = new DoThreeOps();
     try {
       dto.operationOne(); //kann SQLException werfen
       dto.operationTwo(); //kann RemoteException werfen
       dto.operationThree(); 
       dto.success();
     } finally {
       dto.compensate();
     }
   }
   private static class DoThreeOps extends CompensatingStack {
     public void operationOne(String[] args) throws SQLException {
       OperationOne o1 = new OperationOne();
       addStep(o1);
       o1.execute();
     }
     ....
   }
   private static class OperationOne implements CompensatableStep {
     public void compensate() throws SQLException {
       ...
     }
     public void execute() throws SQLException {
       ...
     }
  }
 </pre>
 * Die Klasse DoThreeOps müsste nicht implementiert werden, der CompensatingStack ist auch direkt verwendbar.
 * Wichtig ist allerdings, dass die einzelnen Operationen das Interface {@link CompensatableStep} erfüllen.
 */
public class CompensatingStack {
  
 
  /**
   * Damit ein Schritt kompensierbar ist, muss die compensate()-Methode implementiert werden. Diese
   * darf beliebige Exceptions werfen, die dann währende der Kompensation gesammelt werden
   *
   */
  public interface CompensatableStep {
    public void compensate() throws Exception;
  }
  
  private Stack<CompensatableStep> compensatingStack;
  private boolean compensationNeeded = true;
  private List<Pair<String,Exception>> compensationExceptions;
  
  public CompensatingStack() {
    compensatingStack = new Stack<CompensatableStep>();
  }
  
  /**
   * Eintragen des CompensatableStep, damit Kompensation ausgeführt werden kann
   * @param step
   */
  public void addStep(CompensatableStep step) {
    compensatingStack.add(step);
  }
  
  /**
   * Markierung, dass alle Operationen erfolgreich waren, demzufolge keine Kompensation nötig ist
   */
  public void success() {
    compensationNeeded = false;
    compensatingStack.clear();
  }
  
  /**
   * Führt die Kompensation aus. Alle dabei auftretenden Exceptions werden gesammelt
   */
  public void compensate() {
    if( ! compensationNeeded ) {
      return; //nichts zu tun
    }
    while( ! compensatingStack.isEmpty() ) {
      CompensatableStep step = compensatingStack.pop();
      try {
        step.compensate();
      } catch( Exception e ) {
        handleExceptionInCompensate(step.getClass().getSimpleName(), e);
      }
    }
  }

  /**
   * Hinzufügen einer weiteren Exception, die beim Kompensieren aufgetreten ist
   * @param comment
   * @param e
   */
  public void handleExceptionInCompensate(String comment, Exception e) {
    if( compensationExceptions == null ) {
      compensationExceptions = new ArrayList<Pair<String,Exception>>();
    }
    compensationExceptions.add(Pair.of(comment, e));
  }
  
  /**
   * @return Sind Exceptions während der Kompensation aufgetreten?
   */
  public boolean hasCompensationExceptions() {
    return compensationExceptions != null && ! compensationExceptions.isEmpty();
  }
  
  /**
   * Ausgabe aller während der Kompensation aufgetretenen Exceptions
   * @return
   */
  public List<Pair<String,Exception>> getCompensationExceptions() {
    return compensationExceptions != null ? compensationExceptions : Collections.<Pair<String,Exception>>emptyList();
  }

}
