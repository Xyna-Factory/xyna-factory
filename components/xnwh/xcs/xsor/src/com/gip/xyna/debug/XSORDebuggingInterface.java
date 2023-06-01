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
package com.gip.xyna.debug;



import java.io.BufferedWriter;


public interface XSORDebuggingInterface {

  public void listAllObjects(BufferedWriter w);


  public void listObject(BufferedWriter w, byte[] pk);


  public void listQueueState(BufferedWriter w);
  
  
  public void listFreeListState(BufferedWriter w);
  
  public void checkPrimaryKeyIndexIntegrity(BufferedWriter w, String[] indexIdentifiers);
  
}
