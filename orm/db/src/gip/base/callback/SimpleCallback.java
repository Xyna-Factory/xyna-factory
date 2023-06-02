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


import java.rmi.Remote;
import java.rmi.RemoteException;



public interface SimpleCallback extends Remote {
  
  /**
   * @throws RemoteException
   */
  public void testOut() throws RemoteException;

  public boolean decideBoolean (DecisionContainer decDto) throws RemoteException;
  
  public String decideMultipleChoice(DecisionContainer decDto) throws RemoteException;
  
  public void printWarning(DecisionContainer dec) throws RemoteException;
  
  public String askForValue(DecisionContainer dec) throws RemoteException;
  
  public void notification(DecisionContainer dec) throws RemoteException;
  
  public void setDecision(OBDecision decision) throws RemoteException;
  
  public OBDecision getDecision() throws RemoteException;
  
  public void openCCBReportForm(String title, long key, int mode, int menuType, String bericht, String ss, String hsList) throws RemoteException;
}
