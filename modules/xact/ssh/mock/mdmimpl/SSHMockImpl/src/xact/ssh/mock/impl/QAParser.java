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
package xact.ssh.mock.impl;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;

import xact.ssh.mock.ParseBehaviorException;
import xact.ssh.mock.impl.qa.QA;
import xact.ssh.mock.impl.qa.QAContext;
import xact.ssh.mock.impl.qa.QAMapping;
import xact.ssh.mock.impl.qa.QAPromptAndSleep;
import xact.ssh.mock.impl.qa.SimpleQA;
import xact.ssh.mock.impl.qa.SimpleQAPattern;


public class QAParser {

  
  private static final Logger logger = CentralFactoryLogging.getLogger(QAParser.class);

  
  public enum LineType {
    QuestionMotd("QM", true),
    Question("Q", true),
    QuestionPattern("QP", true),
    
    Sleep("S"),
    
    Answer("A"),
    AnswerPattern("AP"),
    
    MappingDefine("MD"),
    MappingGet("MG"),
    MappingMatch("MM"),
    MappingPut("MP"),
    MappingRemove("MR"),
    MappingSpecial("MS"),
    
    Prompt("P"),
    PromptPattern("PP"),
    
    ContextDefine("CD"),
    ContextExpect("CE"),
    ContextRemove("CR"),
    
    Execution("X"),
    
    Continuation("C"),
    
    Comment("#");
   
    
    private String prefix;
    private boolean start;

    private LineType(String prefix) {
      this(prefix,false);
    }
    private LineType(String prefix, boolean start) {
      this.prefix = prefix;
      this.start = start;
    }

    public static LineType of(String value) {
      for(LineType lt : values() ) {
        if( lt.prefix.equals(value) ) {
          return lt;
        }
      }
      return null;
    }
    public boolean isStart() {
      return start;
    }
    public String getPrefix() {
      return prefix;
    }
  }
  
  
  private List<QA> qas;
  private List<QA> motds;
  
  public List<QA> getMotds() {
    return motds;
  }

  public List<QA> getQas() {
    return qas;
  }
  
  public void parse(String qa) throws ParseBehaviorException {
    qas = new ArrayList<QA>();
    motds = new ArrayList<QA>();
    
    StringBuilder question = null;
    StringBuilder answer = null;
    StringBuilder continuation = null;
    LineType lastStart = null;
    
    EnumMap<LineType,String> parameter = new  EnumMap<LineType,String>(LineType.class);
    
    for( String line : qa.split("\n") ) {
      
      Pair<LineType, String> pair = parseLine(line);
      if( logger.isTraceEnabled() ) {
        logger.trace("parseLine: "+pair.getFirst() + " -> " + pair.getSecond() );
      }
      
      if( pair.getFirst().isStart() ) {
        if( lastStart != null ) {
          addQA(lastStart, question, answer, parameter);
        }
        question = new StringBuilder();
        answer = new StringBuilder();
        parameter.clear();
        lastStart = pair.getFirst();
      }
      parameter.put( pair.getFirst(), pair.getSecond() );
      
      switch( pair.getFirst() ) {
        case QuestionMotd:
          break;
        case Question:
          question.append(pair.getSecond());
          continuation = question;
          break;
        case QuestionPattern:
          question.append(pair.getSecond());
          continuation = question;
          break;
        case Answer:
          answer.append(pair.getSecond());
          continuation = answer;
          break;
        case AnswerPattern:
          answer.append(pair.getSecond());
          continuation = answer;
          break;
        case Continuation:
          if( continuation != null ) {
            continuation.append("\r\n").append(pair.getSecond());
          }
          break;
        default:
       }
    }
    
    if( lastStart != null ) {
      addQA(lastStart, question, answer, parameter );
    }

  }

  public static String rtrimNewlines(String s) {
    int i = s.length()-1;
    while (i >= 0 && s.charAt(i) == '\n') {
      i--;
    }
    return s.substring(0,i+1);
}
  
  private void addQA(LineType lastStart, StringBuilder question, StringBuilder answer, 
      EnumMap<LineType,String> parameter) throws ParseBehaviorException {
    String trimmedAnswer = rtrimNewlines(answer.toString()); 
    
    try {
      switch( lastStart ) {
      case QuestionMotd:
        motds.add( buildQA( lastStart, "", trimmedAnswer, parameter) );
        break;
      case Question:
        qas.add( buildQA( lastStart, question.toString(), trimmedAnswer, parameter) );
        break;
      case QuestionPattern:
        qas.add( buildQA( lastStart, question.toString(), trimmedAnswer, parameter) );
        break;
      default:
      }
    } catch( Exception e ) {
      String reason = e.getClass().getSimpleName()+": "+e.getMessage();
      throw new ParseBehaviorException(question.toString(), reason, e);
    }
  }

  private QA buildQA(LineType type, String question, String answer, EnumMap<LineType, String> parameter) {
    QA qa = null;
    if( type == LineType.QuestionPattern ) {
      qa = new SimpleQAPattern(question.toString().trim(), answer.toString(), parameter );
    } else {
      qa = new SimpleQA(question.toString().trim(), answer.toString(), parameter );
    }
    qa = QAContext.decorate( qa, parameter );
    qa = QAPromptAndSleep.decorate( qa, parameter );
    qa = QAMapping.decorate( qa, parameter );
    return qa;
  }
  

  private Pair<LineType, String> parseLine(String line) {
    StringBuilder sb = new StringBuilder();
    String sep = "^(";
    for( LineType lt : LineType.values() ) {
      if( lt.getPrefix() != null ) {
        sb.append(sep).append(lt.getPrefix());
        sep = "|";
      }
    }
    sb.append("):(.*)$");
    
    
    Pattern p = Pattern.compile(sb.toString());
    Matcher m = p.matcher(line);
    if( m.matches() ) {
      return Pair.of(LineType.of(m.group(1)), m.group(2));
    }
    return Pair.of(LineType.Continuation,line);
  }

 
}
