package xdnc.dhcp.hashmaputils;
import java.io.Serializable;
import java.util.*;


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

public class HashMapSerializer{
  
  public static String serialize(Map m) {
    StringBuffer sb=new StringBuffer();
    sb.append("{");
    Object keyArray[]=m.keySet().toArray();
    Arrays.sort(keyArray);
    for (int i=0;i<keyArray.length;i++){
      sb.append(esc(keyArray[i].toString()));
      sb.append("=");
      Object o=m.get(keyArray[i]);
      if (o instanceof String){
        sb.append(esc(m.get(keyArray[i]).toString()));
      } else if (o instanceof Map){
        sb.append(new HashMapSerializer().serialize((Map)o));
      }
      if(i<keyArray.length-1){
        sb.append(",");
      }

    }
    sb.append("}");
    return sb.toString();
  }

  private static Object esc(String s) {
    StringBuffer ret=new StringBuffer();
    for(int i=0;i<s.length();i++){
      char c=s.charAt(i);
      switch (c){
        case '{':
        case '}':
        case ',':
        case '=':
        case '\\':          
          ret.append("\\"+c);
          break;
        case '\t': ret.append("\\t");break;
        case '\n': ret.append("\\n");break;
        case '\r': ret.append("\\r");break;
        default: ret.append(c);
      }
    }
    return ret;
  }
 
}
