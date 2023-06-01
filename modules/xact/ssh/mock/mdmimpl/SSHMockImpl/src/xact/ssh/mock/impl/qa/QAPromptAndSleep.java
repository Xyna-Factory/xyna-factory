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

import java.util.EnumMap;
import java.util.Random;

import xact.ssh.mock.SleepBehavior;
import xact.ssh.mock.impl.QAParser.LineType;

public class QAPromptAndSleep extends QAWrapper {

  
  private static Random random = new Random();
  
  private final String prompt;
  private final int constantSleep;
  private final int randomSleep;
  
  
  public QAPromptAndSleep(QA qa, EnumMap<LineType, String> parameter) {
    super(qa);
    
    if( parameter.get(LineType.Sleep) != null ) {
      String[] parts = parameter.get(LineType.Sleep).split("\\+\\-");
      if( parts.length == 1 ) {
        this.constantSleep = Integer.parseInt(parts[0]);
        this.randomSleep = 0;
      } else {
        this.constantSleep = Integer.parseInt(parts[0]);
        this.randomSleep = Integer.parseInt(parts[1]);
      }
    } else {
      this.constantSleep = 0;
      this.randomSleep = 0;
    }
    
    if( parameter.containsKey(LineType.PromptPattern ) ) {
      this.prompt = parameter.get(LineType.PromptPattern);
    } else {
      this.prompt = parameter.get(LineType.Prompt);
    }
  }

  @Override
  public String getPrompt(CurrentQAData data) {
    if( prompt == null ) {
      return data.getMockData().getCurrentPrompt();
    }
    data.getMockData().setPrompt(prompt);
    return prompt;
  }


  @Override
  public void handle(CurrentQAData data) {
    SleepBehavior sb = data.getMockData().getMockedDevice().getSleepBehavior();
    long sleep;
    if( sb == null ) {
      sleep = constantSleep;
      if( randomSleep > 0 ) {
        sleep += random.nextInt(randomSleep+1);
      }
    } else {
      long add  = sb.getAddMilliseconds() == null ? 0   : sb.getAddMilliseconds().longValue();
      double dc = sb.getScale()           == null ? 1.0 : sb.getScale().doubleValue();
      sleep = (long)(add + constantSleep*dc);
      if( randomSleep > 0 ) {
        double dr = sb.getScaleRandomness() == null ? 1.0 : sb.getScaleRandomness().doubleValue();
        sleep += random.nextInt((int)(randomSleep*dr+1));
      }
    }
    
    if( sleep > 0 ) {
      try {
        Thread.sleep(sleep);
      } catch (InterruptedException e) {
        //dann halt kürzer warten
      }
    }
    qa.handle(data);
  }



  
  
  public static QA decorate(QA qa, EnumMap<LineType, String> parameter) {
    if( parameter.containsKey(LineType.Sleep) || 
        parameter.containsKey(LineType.Prompt) ||
        parameter.containsKey(LineType.PromptPattern )  ) {
      return new QAPromptAndSleep(qa, parameter);
    } else {
      return qa;
    }
  }
  
}
