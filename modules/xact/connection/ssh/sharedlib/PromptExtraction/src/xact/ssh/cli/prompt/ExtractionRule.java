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
package xact.ssh.cli.prompt;


public interface ExtractionRule {
  
  /**
   * return null if the rule does not detect a fitting prompt, the next rule will be tried 
   */
  public String extract(String input);
  
  /**
   * extraction rule returned from PromptExtractor if no provided rule did manage to extract a prompt 
   */
  public static final ExtractionRule UNMATCHED_PROMPT = new ExtractionRule() {
    public String extract(String input) {
      return null;
    }
  };

}
