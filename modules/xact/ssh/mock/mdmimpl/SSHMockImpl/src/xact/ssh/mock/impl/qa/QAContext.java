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
package xact.ssh.mock.impl.qa;

import java.util.EnumMap;

import xact.ssh.mock.impl.QAParser.LineType;

public class QAContext extends QAWrapper {

  private final String expectContext;
  private final String defineContext;
  private final String removeContext;
  
  public QAContext(QA qa, EnumMap<LineType,String> parameter) {
    super(qa);
    this.expectContext = parameter.get(LineType.ContextExpect);
    this.defineContext = parameter.get(LineType.ContextDefine);
    this.removeContext = parameter.get(LineType.ContextRemove);
  }
  
  
  @Override
  public boolean matches(CurrentQAData data) {
    if( expectContext != null ) {
      if( ! expectContext.equals( data.getMockData().getCurrentContext() ) ) {
        return false;
      }
    }
    if( removeContext != null ) {
      if( ! removeContext.equals( data.getMockData().getCurrentContext() ) ) {
        return false;
      }
    }
    return qa.matches(data);
  }

  @Override
  public void handle(CurrentQAData data) {
    qa.handle(data);
    
    if( removeContext != null ) {
      data.getMockData().removeCurrentContext(); 
    }
    if( defineContext != null ) {
      data.getMockData().setCurrentContext(defineContext); 
    }
    
  }

  public static QA decorate(QA qa, EnumMap<LineType, String> parameter) {
    if( parameter.containsKey(LineType.ContextExpect) || 
        parameter.containsKey(LineType.ContextDefine) || 
        parameter.containsKey(LineType.ContextRemove) ) {
      return new QAContext(qa, parameter);
    } else {
      return qa;
    }
  }
  
}
