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
import java.util.List;

import xact.http.impl.JSONTokenizer.JSONToken;
import xact.http.impl.JSONTokenizer.JSONTokenType;



public class JSONParser {

  public enum JSONValueType {
    STRING, NUMBER, OBJECT, ARRAY, BOOLEAN, NULL;
  }


  private static final boolean validate = true;

  private final String json;


  public JSONParser(String json) {
    this.json = json;
  }


  private static class ParsingPosition {

    int pos;
  }


  private JSONValue parse(List<JSONToken> tokens, ParsingPosition pp) {
    switch (tokens.get(pp.pos).type) {
      case curleyBraceOpen :
        return parseObject(tokens, pp);
      case squareBraceOpen :
        return parseArray(tokens, pp);
      case jsonfalse :
        pp.pos++;
        return JSONValue.bool(false);
      case jsontrue :
        pp.pos++;
        return JSONValue.bool(true);
      case jsonnull :
        pp.pos++;
        return JSONValue.nullVal();
      case number :
        return JSONValue.number(getNumberValueAsString(tokens.get(pp.pos++)));
      case text :
        return JSONValue.string(parseString(tokens, pp));
      default :
        throw new InvalidJSONException(tokens.get(pp.pos).start, "Invalid start of JSON value");
    }
  }


  private JSONValue parseArray(List<JSONToken> tokens, ParsingPosition pp) {
    List<JSONValue> els = new ArrayList<>();
    pp.pos++; //bracket open
    if (validate) {
      validateNotEnd(tokens, pp.pos, "Array not closed.");
    }
    while (tokens.get(pp.pos).type != JSONTokenType.squareBraceClose) {
      els.add(parse(tokens, pp));
      if (tokens.get(pp.pos).type == JSONTokenType.comma) {
        pp.pos++;
        if (validate) {
          validateNotEnd(tokens, pp.pos, "Array not closed");
        }
      } else if (tokens.get(pp.pos).type == JSONTokenType.squareBraceClose) {
        break;
      } else if (validate) {
        throw new InvalidJSONException(tokens.get(pp.pos).start, "Expected comma or array end");
      }
    }

    pp.pos++; //bracket close
    return JSONValue.arr(els);
  }


  private void validateNotEnd(List<JSONToken> tokens, int pos, String cause) {
    if (tokens.size() == pos) {
      throw new InvalidJSONException(tokens.size() == 0 ? 0 : tokens.get(pos - 1).end, "Unexpected end of JSON: " + cause);
    }
  }


  private JSONValue parseObject(List<JSONToken> tokens, ParsingPosition pp) {
    pp.pos++; //curley brace
    if (validate) {
      validateNotEnd(tokens, pp.pos, "Object not closed");
    }
    JSONValue obj = JSONValue.obj();

    while (tokens.get(pp.pos).type != JSONTokenType.curleyBraceClose) {
      parseKeyValuePair(tokens, pp, obj);
      if (tokens.get(pp.pos).type == JSONTokenType.comma) {
        pp.pos++;
        if (validate) {
          validateNotEnd(tokens, pp.pos, "Object not closed");
        }
      } else if (tokens.get(pp.pos).type == JSONTokenType.curleyBraceClose) {
        break;
      } else if (validate) {
        throw new InvalidJSONException(tokens.get(pp.pos).start, "Expected comma or object end");
      }

    }
    pp.pos++; //curley closing brace
    return obj;
  }


  private void parseKeyValuePair(List<JSONToken> tokens, ParsingPosition pp, JSONValue obj) {
    int keypos = pp.pos;
    String key = parseString(tokens, pp);
    if (validate) {
      validateNotEnd(tokens, pp.pos, "Missing colon");
    }
    parseColon(tokens, pp);
    if (validate) {
      validateNotEnd(tokens, pp.pos, "Missing json value");
    }
    JSONValue val = parse(tokens, pp);
    if (obj.vals.put(key, val) != null) {
      if (validate) {
        throw new InvalidJSONException(tokens.get(keypos).start, "Duplicate key in object");
      }
    }
    obj.orderedKeys.add(key);
  }


  private void parseColon(List<JSONToken> tokens, ParsingPosition pp) {
    if (tokens.get(pp.pos++).type != JSONTokenType.colon) {
      if (validate) {
        throw new InvalidJSONException(tokens.get(pp.pos - 1).start, "Missing colon");
      }
    }
  }


  private String parseString(List<JSONToken> tokens, ParsingPosition pp) {
    if (tokens.get(pp.pos++).type != JSONTokenType.text) {
      if (validate) {
        throw new InvalidJSONException(tokens.get(pp.pos - 1).start, "Missing string");
      }
    }
    return getValueAsString(tokens.get(pp.pos - 1));
  }


  private String getNumberValueAsString(JSONToken next) {
    return json.substring(next.start, next.end + 1);
  }


  private String getValueAsString(JSONToken next) {
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


  public JSONValue parse(List<JSONToken> tokens) {
    if (validate) {
      validateNotEnd(tokens, 0, "JSON empty");
    }
    ParsingPosition pp= new ParsingPosition();
    JSONValue val = parse(tokens, pp);
    if (validate && tokens.size() > pp.pos) {
      throw new InvalidJSONException(tokens.get(pp.pos).start, "JSON continues after parser finished");
    }
    return val;
  }


}
