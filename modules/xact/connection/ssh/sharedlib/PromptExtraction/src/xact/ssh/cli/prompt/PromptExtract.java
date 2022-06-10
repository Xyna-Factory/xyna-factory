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
package xact.ssh.cli.prompt;


public class PromptExtract {

  private final ExtractionRule identification;
  private final String extraction;
  

  PromptExtract(ExtractionRule identification, String extraction) {
   this.identification = identification;
   this.extraction = extraction;
  }
  
  
  /**
   * the extracted prompt 
   */
  public String getExtract() {
    return extraction;
  }
  
  
  /**
   * the rule used for extraction or {@link ExtractionRule.UNMATCHED_PROMPT} if no rule was able to extract
   */
  public ExtractionRule getMatchingRule() {
    return identification;
  }
  
}
