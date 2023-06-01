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
package xact.ssh.mock.impl.qa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import com.gip.xyna.utils.collections.Pair;

import xact.ssh.mock.impl.MockData;

public class CurrentQAData {

  private final String question;
  private final String trimmedQuestion;
  private final MockData md;
  private Matcher qm;
  private List<String> questionParameter;
  private List<Pair<String, String>> mappings;
  
  public CurrentQAData(String question, MockData md) {
    this.question = question;
    this.trimmedQuestion = question.trim();
    this.md = md;
  }

  @Override
  public String toString() {
    return "CurrentQAData("+trimmedQuestion+", questionParameter="+ questionParameter+", mappings="+mappings+")";
  }

  
  public String getTrimmedQuestion() {
    return trimmedQuestion;
  }
  
  public String getQuestion() {
    return question;
  }
  
  public List<String> getQuestionParameter() {
    if( questionParameter == null ) {
      if( qm == null ) {
        questionParameter = Collections.emptyList();
      } else {
        questionParameter = new ArrayList<String>();
        for( int i=1; i<=qm.groupCount(); ++i ) {
          questionParameter.add( qm.group(i) );
        }
      }
    }
    return questionParameter;
  }
  
  

  public MockData getMockData() {
    return md;
  }

  public void setQuestionMatcher(Matcher m) {
    this.qm = m;
  }

  public void addMapping(String key, String value) {
    if( mappings == null ) {
      mappings = new ArrayList<Pair<String,String>>();
    }
    mappings.add( Pair.of(key, value ));
  }
  
  public List<Pair<String,String>> getMappings() {
    return mappings;
  }

}
