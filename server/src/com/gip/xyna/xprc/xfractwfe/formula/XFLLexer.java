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
package com.gip.xyna.xprc.xfractwfe.formula;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class XFLLexer {
  
  public enum TokenType {
    LITERAL("[\"][\\s\\S]*[\"]"),
    VARIABLE("%[0-9]+%"),
    ACCESS_PART("\\.\\w+"),
    FUNCTION("#?[\\w]+"),
    BRACE("[)(]"),
    LIST_ACCESS("\\]|\\["),
    NEGATION("[!]"), // the only supported unary operator
    OPERATOR("[-+*/<>]=?|\\|\\||&&|[=!]="), // all binary operators
    ASSIGNMENT("[:~]=?|="),
    ARGUMENT_SEPERATOR(","),
    UNKNOWN(".*"); // NEEDS TO REMAIN THE LAST ENUM ELEMENT
    
    // for this pattern to work inner patterns might not be allowed to open matching groups
    private static Pattern TYPIFICATION_PATTERN;
    
    private final String pattern;
    
    
    private TokenType(String pattern) {
      this.pattern = pattern;
    }
    
    private static Pattern getTypificationPattern() {
      if (TYPIFICATION_PATTERN == null) {
        StringBuilder patternBuilder = new StringBuilder("(");
        TokenType[] values = values();
        for (int i = 0; i < values.length; i++) {
          patternBuilder.append('(').append((values[i]).pattern).append(')');
          if (i + 1 < values.length) {
            patternBuilder.append('|');
          }
        }
        patternBuilder.append(")");
        TYPIFICATION_PATTERN = Pattern.compile(patternBuilder.toString(),Pattern.MULTILINE);
      }
      return TYPIFICATION_PATTERN;
    }
    
    static TokenType detect(XFLToken token, boolean allowUnknown) {
      Matcher matcher = getTypificationPattern().matcher(token.getToken());
      TokenType[] values = values();
      if (matcher.matches()) {
        for (int i = 2; i <= values.length + 1; i++) {
          if (matcher.group(i) != null &&
              matcher.group(i).length() > 0) {
            TokenType type = values[i-2];
            if (!allowUnknown && 
                type == UNKNOWN) {
              throw new RuntimeException("Token failed to be lexed: '" + token.token + "' at position " + token.startIndex + " - " + token.endIndex);
            }
            return type;
          }
        }
      }
      throw new RuntimeException("Token failed to be lexed: '" + token.token + "' at position " + token.startIndex + " - " + token.endIndex);
    }
    
  }
  
  
  public static class XFLLexem {
    private XFLToken token;
    private TokenType type;
    
    XFLLexem(XFLToken token, TokenType type) {
      this.token = token;
      this.type = type;
    }
    
    XFLLexem(String token, TokenType type) {
      this(XFLToken.create(token), type);
    }
    
    public String getToken() {
      return token.getToken();
    }
    
    public TokenType getType() {
      return type;
    }
    
    @Override
    public String toString() {
      return type.toString() + " '" + token.getToken() + "'" ;
    }
    
  }
  
  
  public static class XFLToken {
    private final String token;
    private final int startIndex;
    private final int endIndex;
    
    private XFLToken(String token, int startIndex, int endIndex) {
      this.token = token;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }
    
    public String getToken() {
      return token;
    }
    
    public int getStartIndex() {
      return startIndex;
    }
    
    public int getEndIndex() {
      return endIndex;
    }
    
    public static XFLToken asSubstring(String largeString, int startIndex, int endIndex) {
      return new XFLToken(largeString.substring(startIndex, endIndex), startIndex, endIndex);
    }
    
    public static XFLToken create(String token) {
      return new XFLToken(token, -1, -1);
    }
    
    @Override
    public String toString() {
      return token;
    }
  }
  
  
  public static List<XFLToken> tokenize(String input) {
    List<XFLToken> tokens = new ArrayList<XFLToken>();
    int startIndex = 0;
    int index = 0;
    while (index < input.length()) {
      char current = input.charAt(index);
      switch (current) {
        case ' ' : // whitespaces
        case '\t' :
          startIndex++;
          index++;
          break;
        case '[' : // single sign tokens
        case ']' :
        case '(' :
        case ')' :
        case '+' :
        case '-' :
        case '*' :
        case '/' :
        case ',' :
          index++;
          tokens.add(XFLToken.asSubstring(input, startIndex, index));
          startIndex = index;
          break;
        case '!' : // single sign or possible trailing '='
        case '<' :
        case '>' :
        case '~' :
        case '=' :
        case ':' :
          if (index + 1 < input.length() &&
              input.charAt(index + 1) == '=') {
            index++;
          }
          index++;
          tokens.add(XFLToken.asSubstring(input, startIndex, index));
          startIndex = index;
          break;
        case '&' : // double sign operators
        case '|' :
          if (input.charAt(index + 1) == current) {
            index++;
          } else {
            throw new RuntimeException("TOKENS: " + tokens + "  current: " + current);
          }
          index++;
          tokens.add(XFLToken.asSubstring(input, startIndex, index));
          startIndex = index;
          break;
        case '%' :
          isNumeral: while (true) {
            index++;
            if (index < input.length()) {
              current = input.charAt(index);
              switch (current) {
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                  break; // continue isNumeral
                default :
                  break isNumeral;
              }
            } else {
              break isNumeral;
            }
          }
          if (current == '%') {
            index++;
            tokens.add(XFLToken.asSubstring(input, startIndex, index));
          } else {
            throw new RuntimeException("TOKENS: " + tokens + "  current: " + current);
          }
          startIndex = index;
          break;
        case '.' :
          isAccessPart: while (true) {
            index++;
            if (index < input.length()) {
              current = input.charAt(index);
              switch (current) {
                case ' ' :
                case '\t' :
                case '.' :
                case '[' :
                case ']' :
                case '(' :
                case ')' :
                case '+' :
                case '-' :
                case '*' :
                case '/' :
                case ',' :
                case '!' :
                case '<' :
                case '>' :
                case '~' :
                case '=' :
                case ':' :
                case '|' :
                case '&' :
                case '#' :
                  break isAccessPart;
              }
            } else {
              break isAccessPart;
            }
          }
          tokens.add(XFLToken.asSubstring(input, startIndex, index));
          startIndex = index;
          break;
        case '"' :
          isLiteral: while (true) {
            index++;
            if (index < input.length()) {
              current = input.charAt(index);
              if (current == '\\') {
                if (input.charAt(index + 1) == '"' ||
                    input.charAt(index + 1) == '#' ||
                    input.charAt(index + 1) == '\\') {
                  index++;
                }
              } else if (current == '"') {
                break isLiteral;
              }
            } else {
              break isLiteral;
            }
          }
          if (index < input.length()) {
            index++;
          }
          tokens.add(XFLToken.asSubstring(input, startIndex, index));
          startIndex = index;
          break;
        default :
          isFunction: while (true) {
            index++;
            if (index < input.length()) {
              current = input.charAt(index);
              switch (current) {
                case '.' :
                case '[' :
                case ']' :
                case '(' :
                case ')' :
                case '+' :
                case '-' :
                case '*' :
                case '/' :
                case ',' :
                case '!' :
                case '<' :
                case '>' :
                case '~' :
                case '=' :
                case ':' :
                case '|' :
                case '&' :
                  break isFunction;
              }
            } else {
              break isFunction;
            }
          }
          tokens.add(XFLToken.asSubstring(input, startIndex, index));
          startIndex = index;
          break;
      }
      
    }
    return tokens;
  }
  
  
  public static List<XFLLexem> lex(String input) {
    return lex(input, false);
  }
  
  public static List<XFLLexem> lex(String input, boolean allowUnknown) {
    return lex(tokenize(input), allowUnknown);
  }
  
  
  static List<XFLLexem> lex(List<XFLToken> tokens, boolean allowUnknown) {
    List<XFLLexem> lexems = new ArrayList<XFLLexem>();
    for (XFLToken token : tokens) {
      lexems.add(new XFLLexem(token, TokenType.detect(token, allowUnknown)));
    }
    return lexems;
  }
  
  
  
  public static String lexemStreamToString(List<XFLLexem> lexems) {
    StringBuilder sb = new StringBuilder();
    for (XFLLexem lexem : lexems) {
      sb.append(lexem.getToken());
    }
    return sb.toString();
  }
  
  /*
  public static void main(String[] args) throws IOException {
    //String input = "%1%.formula~=concat(\"(%0%.testWrapper[\\\"0\\\"].testDataOne.dataOne==\\\"\", replaceall(replaceall(%0%.dataOne, \"\\\\\\\\\", \"\\\\\\\\\\\\\\\\\"), \"\\\"\", \"\\\\\\\\\\\"\"), \"\\\")\")";
    //String input = "%1%.formula~=concat(\"(%0%.testWrapper[\\\"0\\\"].testDataOne.dataOne==\\\"\", replaceall(replaceall(%0%.dataOne, \"baum\", \"wald\"), \"baum\", \"wald\")), \"\\\"\")";
    String input = "%1%.formula~=concat(\"(%0%.deviceId.id==\\\"\", replaceall(replaceall(%0%.deviceId.id, \"\\\\\\\\\", \"\\\\\\\\\\\\\\\\\"), \"\\\"\", \"\\\\\\\\\\\"\"), \"\\\")\")";
    XFLLexer lex = new XFLLexer();
    System.out.println(lex.lex(input));
  }
  */
  
}
