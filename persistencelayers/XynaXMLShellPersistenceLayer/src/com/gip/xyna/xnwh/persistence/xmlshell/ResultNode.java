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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public class ResultNode {
  
  private static Logger logger = CentralFactoryLogging.getLogger(ResultNode.class);
  
  public static enum OPERATOR{AND, OR}
  
  protected Set<String> result;
  private ResultNode next;
  private OPERATOR operator;
  
  public ResultNode() {
    result = null;
  }
  
  public ResultNode(OPERATOR op) {
    super();
    this.operator = op;
  }
  
  public ResultNode next() {
    return next;
  }
  
  public void setNext(ResultNode next) {
    this.next = next;
  }
  
  public void setOperator(OPERATOR op) {
    this.operator = op;
  }
  
  public OPERATOR getOperator() {
    return this.operator;
  }
  
  public void setResult(Set<String> result) {
    this.result = result;
  }
  
  public Set<String> process() throws PersistenceLayerException {
    if (next() == null) { // we are a single leaf
      return result;
    }
    
    if (next().operator != null) { //we process our chain
      Set<String> chainResult = next.process();
      switch (next().operator) {
        case AND :
          logger.debug("Executing AND-Operation");
          if (chainResult == null || chainResult.size() == 0) {
            logger.debug("incoming result is null || 0, returning empty result");
            result.clear();
            return result;
          }          
          result.retainAll(chainResult);
          break;
        case OR :
          logger.debug("Executing OR-Operation");
          if (chainResult == null || chainResult.size() == 0) {
            //logger.debug("incoming result is null || 0, returning: " + result);
            return result;
          }          
          result.addAll(chainResult);
          break;
      }
      //logger.debug("Operation completed, returning: " + result);
      return result;
    }
    throw new XNWH_GeneralPersistenceLayerException("Found a ResultNode with next Element but without operator ");
  }
  
  public void traverseAndReset(Set<String> allSet) {
    this.result = null;
    if (next != null) {
      next.traverseAndReset(allSet);
    }
  }
  
  public ResultNode generateWorkingCopy(Set<String> allSet) {
    ResultNode clone = new ResultNode();
    clone.operator = this.operator;
    if (this.next != null)
      clone.next = this.next().generateWorkingCopy(allSet);
    return clone;
  }
  
  public void gatherSetNodes(List<ResultNode> setNodes) {
    if (!(this instanceof ResultNodeHolder)) {
      setNodes.add(this);
    }
    if (next != null) {
      next.gatherSetNodes(setNodes);
    }
  }
}
