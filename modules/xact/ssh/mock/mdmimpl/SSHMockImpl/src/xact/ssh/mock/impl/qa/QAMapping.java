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
import java.util.EnumMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.collections.Triple;

import xact.ssh.mock.impl.QAParser.LineType;

public class QAMapping extends QAWrapper {

  private static final Logger logger = CentralFactoryLogging.getLogger(QAMapping.class);

  private final List<Triple<String,String,Integer>> getter;
  private final List<Triple<String,String,Integer>> putter;
  private final List<Triple<String, String, Integer>> special;
  private final List<Pair<String,String>> definer;
  private final List<Pair<String,String>> remover;
  private final List<Pair<String,String>> matcher;
  private final boolean mapAnswer;
  private final boolean mapPrompt;

  
  
  public QAMapping(QA qa, EnumMap<LineType, String> parameter) {
    super(qa);
    putter = parseKeyNum( parameter.get(LineType.MappingPut) );
    getter = parseKeyNum( parameter.get(LineType.MappingGet) );
    special = parseKeyNum( parameter.get(LineType.MappingSpecial) );
    
    definer = parseKeyValue( parameter.get(LineType.MappingDefine) );
    remover = parseKeyValue( parameter.get(LineType.MappingRemove) );
    matcher = parseKeyValue( parameter.get(LineType.MappingMatch) );
    
    mapAnswer = parameter.containsKey(LineType.AnswerPattern);
    mapPrompt = parameter.containsKey(LineType.PromptPattern);
  }

  @Override
  public boolean matches(CurrentQAData data) {
    if( matcher != null ) {
      for( Pair<String,String> match : matcher ) {
        String val = data.getMockData().getParam( match.getFirst() );
        if( val == null  ) {
          if( match.getSecond() != null ) {
            return false;
          }
        } else {
          if( ! val.equals(match.getSecond() ) ) {
            return false;
          }
        }
      }
    }
    return super.matches(data);
  }
  
  
  @Override
  public void handle(CurrentQAData data) {
    super.handle(data);
    if( definer != null ) {
      for( Pair<String,String> define : definer ) {
        data.getMockData().putParam( define.getFirst(), define.getSecond() );
      }
    }
    
    if( putter != null ) {
      List<String> params = data.getQuestionParameter();
      for( Triple<String,String,Integer> put : putter ) {
        String value = params.get(put.getThird());
        data.getMockData().putParam( put.getFirst(), value );
        data.addMapping( put.getSecond(), value );
      }
    }
    if( getter != null ) {
      for( Triple<String,String,Integer> get : getter ) {
        String value = data.getMockData().getParam( get.getFirst() );
        data.addMapping( get.getSecond(), value );
      }
    }
    if( remover != null ) {
      for( Pair<String,String> remove : remover ) {
        data.getMockData().removeParam( remove.getFirst());
      }
    }
  }
  
  @Override
  public String getPrompt(CurrentQAData data) {
    if( mapPrompt ) {
      String prompt = replaceMapping( data, super.getPrompt(data) );
      data.getMockData().setPrompt(prompt);
      return prompt;
    } else {
      return super.getPrompt(data);
    }
  }
  
  @Override
  public String getResponse(CurrentQAData data) {
    if( mapAnswer ) {
      return replaceMapping( data, super.getResponse(data) );
    } else {
      return super.getResponse(data);
    }
  }
  
  private String replaceMapping(CurrentQAData data, String string) {
    if( logger.isTraceEnabled() ) {
      logger.trace(" replaceMapping in #"+string+"#: "+ data.getMappings());
    }
    for( Pair<String,String> map : data.getMappings() ) {
      String replace = map.getSecond();
      if( replace == null ) {
        //jede Zeile, in der map.getFirst() steht, muss gelöscht werden
        StringBuilder newResponse = new StringBuilder();
        String sep = "";
        for( String line : string.split( "\n") ) {
          if( line.contains(map.getFirst()) ) {
            continue;
          }
          newResponse.append(sep).append(line);
          sep = "\n";
        }
        string = newResponse.toString();
      } else {
        string = string.replace(map.getFirst(), map.getSecond() );
      }
    }
    if( special != null ) {
      for( Triple<String, String, Integer> sp : special ) {
        String key = sp.getFirst();
        if( "backspace".equals(key) ) {
          string = replaceBackspace( string, sp.getSecond() );
        }
      }
      
    }
    
    return string;
  }


  private static String replaceBackspace(String string, String marker) {
    int idx = string.indexOf(marker);
    if( idx < 0 ) {
      return string;
    }
    String last = string;
    StringBuilder sb = new StringBuilder();
    while( idx >= 0 ) {
      sb.append( last.substring(0, idx) );
      last = last.substring(idx+marker.length());
      sb.setLength(sb.length()-1);
      idx = last.indexOf(marker);
    }
    sb.append(last);
    
   
    return sb.toString();
  }

  public static QA decorate(QA qa, EnumMap<LineType, String> parameter) {
    if( parameter.containsKey(LineType.AnswerPattern) ||
        parameter.containsKey(LineType.PromptPattern) ||
        parameter.containsKey(LineType.MappingDefine) ||
        parameter.containsKey(LineType.MappingGet)    ||
        parameter.containsKey(LineType.MappingMatch)  ||
        parameter.containsKey(LineType.MappingPut)    ||
        parameter.containsKey(LineType.MappingRemove)    ||
        parameter.containsKey(LineType.MappingSpecial)       ) {
      
      if( (parameter.containsKey(LineType.AnswerPattern) ||
           parameter.containsKey(LineType.PromptPattern) ) 
          && ! 
          ( parameter.containsKey(LineType.MappingGet ) || 
            parameter.containsKey(LineType.MappingPut ) ) 
          ) {
        throw new IllegalStateException("Answer/Prompt-Pattern without MappingGet or MappingPut");
      }
      return new QAMapping(qa, parameter);
    } else {
      return qa;
    }
  }
  
  private List<Triple<String,String,Integer>> parseKeyNum(String string) { //Format key1=%0%,key2=%1%
    if( string == null ) {
      return null;
    }
    List<Triple<String,String,Integer>> list= new ArrayList<Triple<String,String,Integer>>();

    String[] kvps = string.split("\\s*,\\s*");
    for( String kvp : kvps ) {
      try {
        String[] kv = kvp.split("\\s*=\\s*");
        String key = kv[0];
        String val = kv[1];
        String num = val.substring(1, val.length()-1);
        list.add(Triple.of(key, val, Integer.parseInt(num)) );
      } catch( Exception e ) {
        throw new IllegalArgumentException("Parsing \""+kvp+"\" failed",e);
      }
    }

    return list;
  }
  
  private List<Pair<String,String>> parseKeyValue(String string) { //Format key1=value1,key2,key3=,
    if( string == null ) {
      return null;
    }
    List<Pair<String,String>> list= new ArrayList<Pair<String,String>>();
    String[] kvps = string.split("\\s*,\\s*");
    for( String kvp : kvps ) {
      try {
        String[] kv = kvp.split("\\s*=\\s*");
        String key = kv[0];
        if( kv.length > 1 ) {
          list.add(Pair.of(key, kv[1]));
        } else {
          list.add(Pair.of(key, (String)null));
        }
      } catch( Exception e ) {
        throw new IllegalArgumentException("Parsing \""+kvp+"\" failed",e);
      }
    }
    return list;
  }

}
