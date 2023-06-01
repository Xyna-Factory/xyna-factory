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
package xact.ssh.cli.prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;


public class PromptExtractor {
  
  private static Logger logger = Logger.getLogger(PromptExtractor.class);

  private List<ExtractionRule> rules;
  // Guards against concurrentModification if adds would be performed during extractions
  private final ReadWriteLock rulesLock = new ReentrantReadWriteLock();
  
  public PromptExtractor() {
    this(new ArrayList<ExtractionRule>());
  }
  
  
  public PromptExtractor(List<ExtractionRule> rules) {
    this.rules = rules;
  }
  
  
  public void addRule(ExtractionRule rule) {
    rulesLock.writeLock().lock();
    try {
      rules.add(rule);
    } finally {
      rulesLock.writeLock().unlock();
    }
  }
  
  
  /**
   * returns whether a promptExtraction rule would have been able to extract a prompt or not
   */
  public boolean containsPrompt(String response) {
    return extractPrompt(response).getMatchingRule() != ExtractionRule.UNMATCHED_PROMPT;
  }
  
  
  /**
   * extracts the prompt from a given input
   */
  public PromptExtract extractPrompt(String response) {


    // 
    // ausgabe der ascii codes der response
    // zur identifikation von steuerzeichen vor prompts
    if (logger.isTraceEnabled()) {
      logger.trace("### prompt char debug (char ==> dec):");
      logger.trace(response + ":");
      for (int i = 0; i < response.length(); i++) {
        logger.trace(response.charAt(i) + " ==> " + (int) response.charAt(i));
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug("extractPrompt response=" + response);
    }
    rulesLock.readLock().lock();
    try {
      for (ExtractionRule rule : rules) {
        String extract = rule.extract(response);
        if (extract != null) {
          if (logger.isDebugEnabled()) {
            logger.debug("return extract=" + extract);
          }
          return new PromptExtract(rule, extract);
        }
      }
      logger.debug("return ExtractionRule.UNMATCHED_PROMPT");
      return new PromptExtract(ExtractionRule.UNMATCHED_PROMPT, null);
    } finally {
      rulesLock.readLock().unlock();
    }
  }
  
  
}
