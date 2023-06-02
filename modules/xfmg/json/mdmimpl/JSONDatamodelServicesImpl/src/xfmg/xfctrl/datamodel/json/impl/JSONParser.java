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
package xfmg.xfctrl.datamodel.json.impl;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.utils.ByteUtils;

import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONToken;
import xfmg.xfctrl.datamodel.json.impl.JSONTokenizer.JSONTokenType;



public class JSONParser {


  public static class JSONObject {

    public Map<String, JSONValue> objects = new HashMap<String, JSONValue>();


    /** 
     * führende indentation wird nicht geschrieben
     *        {
     *          key : val,
     *          key2 : val2,
     *          key3 : [
     *            a,
     *            b
     *          ],
     *          key4 : {
     *            a : b
     *          }
     *        }
     */
    public String toJSON(String indentation) {
      StringBuilder sb = new StringBuilder();
      sb.append("{");
      if (objects.size() > 0) {
        sb.append("\n");
        String subindentation = indentation + "  ";
        int cnt = 0;
        List<Entry<String, JSONValue>> l = new ArrayList<Map.Entry<String,JSONValue>>(objects.entrySet());
        Collections.sort(l, new Comparator<Entry<String, JSONValue>>() {

          public int compare(Entry<String, JSONValue> o1, Entry<String, JSONValue> o2) {
            return o1.getKey().compareTo(o2.getKey());
          }
          
        });
        for (Entry<String, JSONValue> e : l) {
          sb.append(subindentation);
          sb.append("\"");
          sb.append(encodeString(e.getKey()));
          sb.append("\"");
          sb.append(" : ");
          sb.append(e.getValue().toJSON(subindentation));
          if (objects.size() > ++cnt) {
            sb.append(",");
          }
          sb.append("\n");
        }
        sb.append(indentation);
      }
      sb.append("}");
      return sb.toString();
    }

  }

  public enum JSONValueType {
    STRING, NUMBER, OBJECT, ARRAY, BOOLEAN, NULL;
  }

  public static class JSONValue {

    public JSONValueType type;
    public String stringOrNumberValue;
    public JSONObject objectValue;
    public List<JSONValue> arrayValue;
    public boolean booleanValue;


    /**
     * führende indentation wird nicht geschrieben
     *             [
     *               val1,
     *               val2
     *             ] 
     */
    public String toJSON(String indentation) {
      StringBuilder sb = new StringBuilder();
      switch (type) {
        case ARRAY :
          sb.append("[");
          if (arrayValue.size() > 0) {
            sb.append("\n");
            String subindentation = indentation + "  ";
            int cnt = 0;
            for (JSONValue v : arrayValue) {
              sb.append(subindentation);
              sb.append(v.toJSON(subindentation));
              if (arrayValue.size() > ++cnt) {
                sb.append(",");
              }
              sb.append("\n");
            }
            sb.append(indentation);
          }
          sb.append("]");
          break;
        case BOOLEAN :
          sb.append(booleanValue);
          break;
        case NULL :
          sb.append("null");
          break;
        case NUMBER :
          sb.append(stringOrNumberValue);
          break;
        case STRING :
          sb.append("\"");
          sb.append(encodeString(stringOrNumberValue));
          sb.append("\"");
          break;
        case OBJECT :
          sb.append(objectValue.toJSON(indentation + "  "));
          break;

      }
      return sb.toString();
    }

  }


  private static final boolean validate = true;

  private final String json;


  public JSONParser(String json) {
    this.json = json;
  }


  /**
   * 
   * @param tokens
   * @param pos position von {
   * @param job
   * @return position von }
   */
  public int fillObject(List<JSONToken> tokens, int pos, JSONObject job) {
    if (validate) {
      if (tokens.get(pos).type != JSONTokenType.curleyBraceOpen) {
        throw new InvalidJSONException(tokens.get(pos).start, "Expected the start of a new object ('{').");
      }
    }
    while (true) {
      //lese nächstes key-value paar
      pos++;
      if (pos >= tokens.size()) {
        throw new InvalidJSONException(tokens.get(pos - 1).end, "Missing '}'.");
      }
      JSONToken next = tokens.get(pos);
      int commaCnt = 0;
      while (next.type == JSONTokenType.comma) {
        pos++;
        commaCnt++;
        if (pos >= tokens.size()) {
          throw new InvalidJSONException(tokens.get(pos - 1).end, "Missing '}'.");
        }
        next = tokens.get(pos);
      }
      if (validate) {
        if (commaCnt > 1) {
          throw new InvalidJSONException(next.start - 1, "Too may commas.");
        }
      }
      if (next.type == JSONTokenType.curleyBraceClose) {
        return pos;
      }
      if (validate) {
        //fehlte hier ein komma?
        switch (commaCnt) {
          case 0 :
            //dann muss vornedran { stehen
            if (tokens.get(pos - 1).type != JSONTokenType.curleyBraceOpen) {
              throw new InvalidJSONException(next.start, "Missing comma.");
            }
            break;
          case 1 :
            //dann darf vornedran kein { stehen
            if (tokens.get(pos - 2).type == JSONTokenType.curleyBraceOpen) {
              throw new InvalidJSONException(tokens.get(pos - 1).start, "Unexpected comma.");
            }
            break;
        }
      }
      if (next.type != JSONTokenType.text) {
        throw new InvalidJSONException(next.start, "Missing key.");
      }

      String key = getValueAsString(next);
      pos++; //colon      
      if (pos >= tokens.size()) {
        throw new InvalidJSONException(next.start, "Missing ':'.");
      }
      if (validate) {
        if (tokens.get(pos).type != JSONTokenType.colon) {
          throw new InvalidJSONException(tokens.get(pos).start, "Missing ':'.");
        }
      }
      pos++; //value
      if (pos >= tokens.size()) {
        throw new InvalidJSONException(next.start, "Missing value.");
      }
      JSONValue value = new JSONValue();
      job.objects.put(key, value);
      pos = fillValue(tokens, pos, value);
    }
  }


  private int fillValue(List<JSONToken> tokens, int pos, JSONValue value) {
    JSONToken next = tokens.get(pos);
    switch (next.type) {
      case curleyBraceOpen :
        JSONObject o = new JSONObject();
        pos = fillObject(tokens, pos, o);
        value.objectValue = o;
        value.type = JSONValueType.OBJECT;
        break;
      case jsonfalse :
        value.booleanValue = false;
        value.type = JSONValueType.BOOLEAN;
        break;
      case jsontrue :
        value.booleanValue = true;
        value.type = JSONValueType.BOOLEAN;
        break;
      case jsonnull :
        value.type = JSONValueType.NULL;
        break;
      case text :
        value.type = JSONValueType.STRING;
        value.stringOrNumberValue = getValueAsString(next);
        break;
      case squareBraceOpen :
        value.type = JSONValueType.ARRAY;
        List<JSONValue> arr = new ArrayList<JSONValue>();
        pos = fillArray(tokens, pos, arr);
        value.arrayValue = arr;
        break;
      case number :
        value.type = JSONValueType.NUMBER;
        value.stringOrNumberValue = getNumberValueAsString(next);
        break;
      default :
        throw new InvalidJSONException(next.start, "Expected the start of a JSON value.");
    }
    return pos;
  }


  /**
   * 
   * @param tokens
   * @param pos position von [
   * @param arr
   * @return position von ]
   */
  public int fillArray(List<JSONToken> tokens, int pos, List<JSONValue> arr) {
    if (tokens.get(pos).type != JSONTokenType.squareBraceOpen) {
      throw new InvalidJSONException(tokens.get(pos).start, "Expected the start of a JSON list '['.");
    }
    while (true) {
      pos++;
      if (pos >= tokens.size()) {
        throw new InvalidJSONException(tokens.get(pos - 1).end, "Missing ']'.");
      }

      //lese nächstes listenelement
      JSONToken next = tokens.get(pos);
      int commaCnt = 0;
      while (next.type == JSONTokenType.comma) {
        pos++;
        if (pos >= tokens.size()) {
          throw new InvalidJSONException(tokens.get(pos - 1).end, "Missing ']'.");
        }
        next = tokens.get(pos);
        commaCnt++;
      }
      if (validate) {
        if (commaCnt > 1) {
          throw new InvalidJSONException(next.start - 1, "Too may commas.");
        }
      }
      if (next.type == JSONTokenType.squareBraceClose) {
        return pos;
      }
      if (validate) {
        //fehlte hier ein komma?
        switch (commaCnt) {
          case 0 :
            //dann muss vornedran [ stehen
            if (tokens.get(pos - 1).type != JSONTokenType.squareBraceOpen) {
              throw new InvalidJSONException(next.start, "Missing comma.");
            }
            break;
          case 1 :
            //dann darf vornedran kein [ stehen
            if (tokens.get(pos - 2).type == JSONTokenType.squareBraceOpen) {
              throw new InvalidJSONException(tokens.get(pos - 1).start, "Unexpected comma.");
            }
            break;
        }
      }
      //value
      JSONValue value = new JSONValue();
      arr.add(value);
      pos = fillValue(tokens, pos, value);
    }
  }


  private String getNumberValueAsString(JSONToken next) {
    return json.substring(next.start, next.end + 1);
  }


  public String getValueAsString(JSONToken next) {
    StringBuilder sb = new StringBuilder(next.end + 1 - next.start);
    int pos = next.start - 1;
    while (true) {
      if (++pos > next.end) {
        break;
      }
      char c = json.charAt(pos);
      switch (c) {
        case '\\' :
          pos++;
          c = json.charAt(pos);
          switch (c) {
            case '\\' :
            case '/' :
            case '"' :
              sb.append(c);
              break;
            case 'b' :
              sb.append('\b');
              break;
            case 'f' :
              sb.append('\f');
              break;
            case 'n' :
              sb.append('\n');
              break;
            case 'r' :
              sb.append('\r');
              break;
            case 't' :
              sb.append('\t');
              break;
            case 'u' :
              String n = json.substring(pos + 1, pos + 5);
              pos += 4;
              sb.append((char) Integer.parseInt(n, 16));
              break;
            default :
              sb.append('\\');
              sb.append(c);
              break;
          }
          break;
        default :
          sb.append(c);
          break;
      }
    }
    return sb.toString();
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
