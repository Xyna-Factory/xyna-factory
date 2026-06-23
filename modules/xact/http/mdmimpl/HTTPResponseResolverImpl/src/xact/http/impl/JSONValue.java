/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package xact.http.impl;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.utils.ByteUtils;



public class JSONValue {

  public enum Type {
    STRING, BOOLEAN, NUMBER, NULL, OBJECT, ARRAY;
  }


  public final Type type;
  public final String stringVal;
  public final boolean boolVal;
  public final String numberVal;
  public final Map<String, JSONValue> vals;
  public final List<String> orderedKeys;
  public final JSONValue[] list;


  private JSONValue(Type type, String stringVal, boolean boolVal, String numberVal, List<JSONValue> list) {
    this.type = type;
    this.stringVal = stringVal;
    this.boolVal = boolVal;
    this.numberVal = numberVal;
    this.vals = type == Type.OBJECT ? new HashMap<>() : null;
    this.orderedKeys = type == Type.OBJECT ? new ArrayList<>() : null;
    this.list = list == null ? null : list.toArray(new JSONValue[0]);
  }


  public static JSONValue string(String val) {
    return new JSONValue(Type.STRING, val, false, null, null);
  }


  public static JSONValue bool(boolean val) {
    return new JSONValue(Type.BOOLEAN, null, val, null, null);
  }


  public static JSONValue nullVal() {
    return new JSONValue(Type.NULL, null, false, null, null);
  }


  public static JSONValue obj() {
    return new JSONValue(Type.OBJECT, null, false, null, null);
  }


  public static JSONValue number(String numberVal) {
    return new JSONValue(Type.NUMBER, null, false, numberVal, null);
  }


  public static JSONValue arr(List<JSONValue> els) {
    return new JSONValue(Type.ARRAY, null, false, null, els);
  }


  public String toString() {
    StringBuilder sb = new StringBuilder();
    render(sb, "", false);
    return sb.toString();
  }


  void render(StringBuilder sb, String indentation, boolean skipFirstIndentation) {
    if (!skipFirstIndentation) {
      sb.append(indentation);
    }
    switch (type) {
      case ARRAY :
        sb.append("[").append("\n");
        for (int i = 0; i < list.length; i++) {
          JSONValue v = list[i];
          v.render(sb, indentation + "  ", false);
          if (i < list.length - 1) {
            sb.append(",");
          }
          sb.append("\n");
        }
        sb.append(indentation).append("]");
        break;
      case BOOLEAN :
        sb.append(boolVal);
        break;
      case NULL :
        sb.append("null");
        break;
      case NUMBER :
        sb.append(numberVal);
        break;
      case STRING :
        sb.append("\"").append(encodeString(stringVal)).append("\"");
        break;
      case OBJECT :
        sb.append("{\n");
        for (int i = 0; i < orderedKeys.size(); i++) {
          String key = orderedKeys.get(i);
          sb.append(indentation).append("  \"").append(key).append("\" : ");
          vals.get(key).render(sb, indentation + "  ", true);
          if (i < orderedKeys.size() - 1) {
            sb.append(",");
          }
          sb.append("\n");
        }
        sb.append(indentation).append("}");
        break;
    }
  }


  private static String encodeString(String s) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      switch (c) {
        case '\\' :
          sb.append("\\\\");
          break;
        case '"' :
          sb.append("\\\"");
          break;
        case '\b' :
          sb.append("\\b");
          break;
        case '\f' :
          sb.append("\\f");
          break;
        case '\n' :
          sb.append("\\n");
          break;
        case '\r' :
          sb.append("\\r");
          break;
        case '\t' :
          sb.append("\\t");
          break;
        default :
          if ((int) c < 32) {
            sb.append("\\u00");
            sb.append(ByteUtils.toHexString((byte) c, false));
          } else {
            /*
             * andere unicode charaktere zu escapen ist optional: http://www.ietf.org/rfc/rfc4627.txt
            
            char = unescaped /
                escape (
                    %x22 /          ; "    quotation mark  U+0022
                    %x5C /          ; \    reverse solidus U+005C
                    %x2F /          ; /    solidus         U+002F
                    %x62 /          ; b    backspace       U+0008
                    %x66 /          ; f    form feed       U+000C
                    %x6E /          ; n    line feed       U+000A
                    %x72 /          ; r    carriage return U+000D
                    %x74 /          ; t    tab             U+0009
                    %x75 4HEXDIG )  ; uXXXX                U+XXXX
            
            escape = %x5C              ; \
            
            quotation-mark = %x22      ; "
            
            unescaped = %x20-21 / %x23-5B / %x5D-10FFFF
             */
            sb.append(c);
          }
          break;
      }
    }
    return sb.toString();
  }
}
