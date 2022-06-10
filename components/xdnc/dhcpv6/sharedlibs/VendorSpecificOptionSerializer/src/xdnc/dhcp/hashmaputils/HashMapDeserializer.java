package xdnc.dhcp.hashmaputils;
import java.io.Serializable;
import java.util.*;


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

public class HashMapDeserializer {

  private char[] c=null;
  private int position=0;

  public HashMap deserializeHm(String s){    
    c=s.toCharArray();
    position=0;    
    HashMap ret=readHashMap();
    return ret;
  }

  private HashMap readHashMap() {
    incPosition();// '{'
    HashMap<String,Object> hm=new HashMap<String,Object>();
    while(getCharAtPosition()!='}'){
      String key=readKey();
      incPosition();
      Object value=readValue();      
      hm.put(key,value);
      if(getCharAtPosition()==','){
         incPosition();
      }
    }
    incPosition();// '}'
    return hm;
  }

  private char getCharAtPosition() {
    if (position>=c.length){
      throw new IllegalArgumentException("syntax error parsing '"+new String(c)+"'");
    }
    return c[position];
  }

  private void incPosition() {
    position=position+1;
  }

  private Object readValue() {
    
    if (getCharAtPosition()=='{'){
      return readHashMap();
    } else {
      StringBuffer sb=new StringBuffer();
      while(getCharAtPosition()!=',' && getCharAtPosition()!='}'){
        char ch=nextEscapedChar();
        sb.append(ch);
      }
      return sb.toString();
    }
  }

  private char nextEscapedChar() {
    char ch=getCharAtPosition();
    incPosition();
    if (ch=='\\'){//escape character
      ch=getCharAtPosition();
      incPosition();
      switch(ch){
        case 't': return '\t';
        case 'n': return '\n';
        case 'r': return '\r';
        default: return ch;        
      }
    } else {
      return ch;
    }
  }

  private String readKey() {
    StringBuffer sb=new StringBuffer();
    while(getCharAtPosition()!='='){
      sb.append(nextEscapedChar());
    }
    return sb.toString();
  }

}

