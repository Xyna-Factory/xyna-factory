/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.types;

public enum State {
  
  REQUESTED( "requested"), //Auftrag ist eingegangen
  SUCCEEDED( "succeeded"), //Auftrag ist erfolgreich bearbeitet
  FAILED(    "failed"),    //Auftrag ist fehlgeschlagen
  IGNORED(   "ignored"),   //Auftrag wurde aus fachlichen Gründen ignoriert
  REJECTED(  "rejected"),  //Auftrag wurde aufgrund Überlastung abgewiesen 
  ROLLBACKED("rollbacked"),//Auftrag wurde rollbacked (vor dem Commit ist ein nachfolgender
                           //        Auftrag gescheitert) 
  UNEXPECTED("unexpected");//Auftrag hat zwar den gewünschten Endzustand hinterlassen, aber auf einer 
                           //        unerwarteten Datenbasis gearbeitet (bspw. Delete von 0 Zeilen) 

  private String name;

  private State( String name ) {
    this.name = name;
  }
  
  
  public static State fromSnmpIndex(String index) {
    return values()[ Integer.parseInt( index ) -1 ];
  }
  
  @Override
  public String toString() {
    return name;
  }
  
}
