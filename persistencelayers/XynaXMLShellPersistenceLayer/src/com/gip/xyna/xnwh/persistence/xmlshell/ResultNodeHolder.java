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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class ResultNodeHolder extends ResultNode {

  private static Logger logger = CentralFactoryLogging.getLogger(ResultNodeHolder.class);
  
  private ResultNode content;
  private boolean negated;
  private Set<String> allSet;
  
  public ResultNodeHolder(OPERATOR op) {
    super(op);
    this.negated = false;
  }
  
  public ResultNodeHolder(OPERATOR op, boolean negated) {
    super(op);
    this.negated = negated; 
    this.allSet = Collections.synchronizedSortedSet(new TreeSet<String>(XynaXMLShellPersistenceLayer.reverseOrder)); 
  }
  
  public void setContent(ResultNode content) {
    this.content = content;
  }
  
  public Set<String> process() throws PersistenceLayerException {
    Set<String> contentResult = content.process();    
    if (negated) {
      allSet.removeAll(contentResult);
      //logger.debug("Negation complete: " + allSet);
      result = allSet;
    } else {
      //logger.debug("positive holder, returning contentResult: " + contentResult);
      result = contentResult;
    }
    return super.process();
  }
  
  public void traverseAndReset(Set<String> allSet) {
    if (this.negated) {
      this.allSet.clear();
      this.allSet.addAll(allSet);
    }
    content.traverseAndReset(allSet);
    super.traverseAndReset(allSet);    
  }
  
  public ResultNodeHolder generateWorkingCopy(Set<String> allSet) {
    ResultNodeHolder clone = new ResultNodeHolder(this.getOperator());
    if (this.next() != null)
      clone.setNext(this.next().generateWorkingCopy(allSet));
    if (this.content != null)
      clone.setContent(content.generateWorkingCopy(allSet));
    if (this.negated) {
      clone.negated = true;
      clone.allSet = allSet;
    }
    return clone;
  }
  
  public void gatherSetNodes(List<ResultNode> setNodes) {
    content.gatherSetNodes(setNodes);
    super.gatherSetNodes(setNodes);
  }
}
