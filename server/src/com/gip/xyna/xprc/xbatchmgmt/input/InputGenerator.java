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
package com.gip.xyna.xprc.xbatchmgmt.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.exceptions.XPRC_InputGeneratorInitializationException;


public abstract class InputGenerator {
  
  protected volatile String lastInputId; //letzte vergebene Id
  protected List<String> reusableInputIds; //Inputs die erneut vergeben werden k�nnen, weil zugeh�riger Slave nicht gestartet wurde
  protected Long revision; //Revision, in der der BatchProcess l�uft
  protected int maximumInputs;
  
  public InputGenerator(int maximumInputs, Long revision) {
    this.reusableInputIds = new ArrayList<String>();
    this.revision = revision;
    this.maximumInputs = maximumInputs;
  }
  
  
  public abstract boolean hasNext() throws XynaException;
  
  public abstract Pair<String, GeneralXynaObject> next() throws XynaException;
  
  public abstract void changeRevision(Long revision) throws XPRC_InputGeneratorInitializationException;

  public abstract int getRemainingInputs();

  public String getLastInputId () {
    return lastInputId;
  }
  
  public void setLastInputId (String lastInputId) {
    this.lastInputId = lastInputId;
  }
  
  
  public void copyReusableInputIds(List<String> reusableInputIds) {
    if (reusableInputIds != null) {
      this.reusableInputIds = new ArrayList<String>(reusableInputIds);
      Iterator<String> inputIdIter = this.reusableInputIds.iterator();
      while (inputIdIter.hasNext()) {
        String current = inputIdIter.next();
        if (current == null ||
            current.trim().length() <= 0) {
          inputIdIter.remove();
        }
      }
    }
  }
  
  public int getMaximumInputs() {
    return maximumInputs;
  }

  public void setAlreadyStarted(int started) {
  }
}
