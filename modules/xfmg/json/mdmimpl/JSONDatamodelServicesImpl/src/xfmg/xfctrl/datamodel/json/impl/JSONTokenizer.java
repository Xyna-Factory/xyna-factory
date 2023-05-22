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
package xfmg.xfctrl.datamodel.json.impl;



import java.util.ArrayList;
import java.util.List;



//dokumentation siehe hier: http://www.json.org/
public class JSONTokenizer {

  private static final boolean validate = true;

  public enum JSONTokenType {
    text, curleyBraceOpen, curleyBraceClose, squareBraceOpen, squareBraceClose, colon, number, comma, jsontrue, jsonfalse, jsonnull;
  }

  public static class JSONToken {

    public final int start;
    public final int end;
    public final JSONTokenType type;


    public JSONToken(JSONTokenType type, int start, int end) {
      this.type = type;
      this.start = start;
      this.end = end;
    }


    public String toString() {
      return type + " " + start + (start != end ? " " + end : "");
    }
  }


  public List<JSONToken> tokenize(String json) {
    List<JSONToken> l = new ArrayList<JSONToken>();
    int len = json.length();
    int pos = -1;
    int start;
    outer : while (++pos < len) {
      char n = json.charAt(pos);
      switch (n) {
        case '\"' :
          start = pos;
          while (++pos < len) {
            n = json.charAt(pos);
            switch (n) {
              case '\\' :
                //escaping
                n = json.charAt(++pos);
                switch (n) {
                  case '"' :
                  case '\\' :
                  case 'n' :
                  case 'r' :
                  case 't' :
                  case '/' :
                  case 'b' :
                  case 'f' :
                    break;
                  case 'u' : //unicode
                    if (validate) {
                      //hexadezimale zahl
                      for (int i = 1; i < 5; i++) {
                        n = json.charAt(pos + i);
                        if (n >= '0' && n <= '9') {
                          //ok
                        } else if (n >= 'A' && n <= 'F') {
                          //ok
                        } else if (n >= 'a' && n <= 'f') {
                          //ok
                        } else {
                          throw new InvalidJSONException(pos, "Expected unicode code point (4 hexadecimal digits)");
                        }
                      }
                    }
                    pos += 3;
                    break;
                  default :
                    if (validate) {
                      throw new InvalidJSONException(pos,
                                                     "Backslash may not be used in strings except to escape a fixed set of characters.");
                    }
                }
                break;
              case '\"' :
                l.add(new JSONToken(JSONTokenType.text, start + 1, pos - 1));
                continue outer;
              default : //next
            }
          }
          break;
        case '{' :
          l.add(new JSONToken(JSONTokenType.curleyBraceOpen, pos, pos));
          break;
        case '}' :
          l.add(new JSONToken(JSONTokenType.curleyBraceClose, pos, pos));
          break;
        case '[' :
          l.add(new JSONToken(JSONTokenType.squareBraceOpen, pos, pos));
          break;
        case ']' :
          l.add(new JSONToken(JSONTokenType.squareBraceClose, pos, pos));
          break;
        case ',' :
          l.add(new JSONToken(JSONTokenType.comma, pos, pos));
          break;
        case ':' :
          l.add(new JSONToken(JSONTokenType.colon, pos, pos));
          break;
        case '0' :
        case '1' :
        case '2' :
        case '3' :
        case '4' :
        case '5' :
        case '6' :
        case '7' :
        case '8' :
        case '9' :
        case '-' :
          start = pos;
          while (pos < len) {
            n = json.charAt(pos);
            switch (n) {
              case ' ' :
              case ',' :
              case '}' :
              case ']' :
              case '\n' :
              case '\r' :
              case '\t' :
              case '\b' :
                JSONToken number = new JSONToken(JSONTokenType.number, start, pos - 1);
                l.add(number);
                pos--; //letztes zeichen muss als neues token geparst werden
                if (validate) {
                  validateNumber(json, number);
                }
                continue outer;
              default : //next
            }
            pos++;
          }
          JSONToken number = new JSONToken(JSONTokenType.number, start, pos - 1);
          l.add(number);
          if (validate) {
            validateNumber(json, number);
          }
          break;
        case 't' :
          l.add(new JSONToken(JSONTokenType.jsontrue, pos, pos + 3));
          if (validate) {
            if (!json.substring(pos + 1, pos + 4).equals("rue")) {
              throw new InvalidJSONException(pos, "Unexpected characters");
            }
          }
          pos += 3;
          break;
        case 'f' :
          l.add(new JSONToken(JSONTokenType.jsonfalse, pos, pos + 4));
          if (validate) {
            if (!json.substring(pos + 1, pos + 5).equals("alse")) {
              throw new InvalidJSONException(pos, "Unexpected characters");
            }
          }
          pos += 4;
          break;
        case 'n' :
          l.add(new JSONToken(JSONTokenType.jsonnull, pos, pos + 3));
          if (validate) {
            if (!json.substring(pos + 1, pos + 4).equals("ull")) {
              throw new InvalidJSONException(pos, "Unexpected characters");
            }
          }
          pos += 3;
          break;
        default :
          //ignore. zeilenumbr�che, whitespaces, etc
          if (validate) {
            if (!Character.isWhitespace(n)) {
              throw new InvalidJSONException(pos, "Unexpected character");
            }
          }
      }
    }
    return l;
  }


  private void validateNumber(String json, JSONToken number) {
    String n = json.substring(number.start, number.end + 1);
    try {
      Double d = Double.valueOf(n);
    } catch (NumberFormatException e) {
      throw new InvalidJSONException(number.start, "Text could not be parsed as a number.");
    }
  }

}
