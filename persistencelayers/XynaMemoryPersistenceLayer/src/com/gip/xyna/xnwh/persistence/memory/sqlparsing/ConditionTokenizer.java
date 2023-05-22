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
package com.gip.xyna.xnwh.persistence.memory.sqlparsing;



import java.util.ArrayList;
import java.util.List;



public class ConditionTokenizer {

  //keine whitespaces
  public enum TokenType {
    OR, AND, NOT, IN, LIKE, IS, NULL, WORD, BRACE_OPEN, BRACE_CLOSE, GT, GTE, LT, LTE, EQ, NEQ, /* string in anf�hrungszeichen */ESCAPED, /* ? */PARAMETER, COMMA;

    private String lowercase;


    private TokenType() {
      lowercase = this.name().toLowerCase();
    }

  }

  public static class SQLTokenizerException extends Exception {

    public SQLTokenizerException(String msg) {
      super(msg);
    }

  }

  public static class ConditionToken {

    public final TokenType type;
    public final int startIndex;
    public final int endIndex;


    public ConditionToken(TokenType type, int startIndex, int endIndex) {
      this.type = type;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
    }


    public String getAsString(String sqlString) {
      return sqlString.substring(startIndex, endIndex + 1);
    }
  }


  public List<ConditionToken> tokenize(String condition) throws SQLTokenizerException {
    List<ConditionToken> tokens = new ArrayList<>();
    condition = condition.toLowerCase();
    int len = condition.length();
    int idx = 0;
    while (idx < len) {
      char c = condition.charAt(idx);
      switch (c) {
        case 'o' :
          idx = parseWordOr(idx, condition, tokens, TokenType.OR);
          break;
        case 'a' :
          idx = parseWordOr(idx, condition, tokens, TokenType.AND);
          break;
        case 'l' :
          idx = parseWordOr(idx, condition, tokens, TokenType.LIKE);
          break;
        case 'i' :
          idx = parseWordOr(idx, condition, tokens, TokenType.IS, TokenType.IN);
          break;
        case 'n' :
          idx = parseWordOr(idx, condition, tokens, TokenType.NOT, TokenType.NULL);
          break;
        case '(' :
          tokens.add(new ConditionToken(TokenType.BRACE_OPEN, idx, idx));
          idx++;
          break;
        case ')' :
          tokens.add(new ConditionToken(TokenType.BRACE_CLOSE, idx, idx));
          idx++;
          break;
        case ' ' :
        case '\t' :
        case '\n' :
          //whitespaces ignorieren
          idx++;
          break;
        case '<' :
          if (++idx < len) {
            c = condition.charAt(idx);
            switch (c) {
              case '>' :
                tokens.add(new ConditionToken(TokenType.NEQ, idx - 1, idx));
                idx++;
                break;
              case '=' :
                tokens.add(new ConditionToken(TokenType.LTE, idx - 1, idx));
                idx++;
                break;
              default :
                tokens.add(new ConditionToken(TokenType.LT, idx - 1, idx - 1));
                break;
            }
          } else {
            tokens.add(new ConditionToken(TokenType.LT, idx - 1, idx - 1));
          }
          break;
        case '>' :
          if (++idx < len) {
            c = condition.charAt(idx);
            switch (c) {
              case '=' :
                tokens.add(new ConditionToken(TokenType.GTE, idx - 1, idx));
                idx++;
                break;
              default :
                tokens.add(new ConditionToken(TokenType.GT, idx - 1, idx - 1));
                break;
            }
          } else {
            tokens.add(new ConditionToken(TokenType.GT, idx - 1, idx - 1));
          }
          break;
        case '=' :
          tokens.add(new ConditionToken(TokenType.EQ, idx, idx));
          idx++;
          break;
        case ',' :
          tokens.add(new ConditionToken(TokenType.COMMA, idx, idx));
          idx++;
          break;
        case '?' :
          tokens.add(new ConditionToken(TokenType.PARAMETER, idx, idx));
          idx++;
          break;
        case '!' :
          if (++idx < len) {
            c = condition.charAt(idx);
            switch (c) {
              case '=' :
                tokens.add(new ConditionToken(TokenType.NEQ, idx - 1, idx));
                idx++;
                break;
              default :
                throw new SQLTokenizerException("dangling ! at position " + (idx - 1));
            }
          } else {
            throw new SQLTokenizerException("dangling ! at position " + (idx - 1));
          }
          break;
        case '"' :
          int lastIdxOfEscapedString = parseEscapedString('"', idx, condition);
          tokens.add(new ConditionToken(TokenType.ESCAPED, idx + 1, lastIdxOfEscapedString - 1));
          idx = lastIdxOfEscapedString + 1;
          break;
        case '\'' :
          lastIdxOfEscapedString = parseEscapedString('\'', idx, condition);
          tokens.add(new ConditionToken(TokenType.ESCAPED, idx + 1, lastIdxOfEscapedString - 1));
          idx = lastIdxOfEscapedString + 1;
          break;
        default :
          int lastIdxOfWord = nextWord(condition, idx);
          tokens.add(new ConditionToken(TokenType.WORD, idx, lastIdxOfWord));
          idx = lastIdxOfWord + 1;
          break;
      }
    }
    return tokens;
  }


  //parse von startIdx bis zum n�chsten nicht escapten vorkommnis von escapechar
  private int parseEscapedString(char escapeChar, int startIdx, String condition) throws SQLTokenizerException {
    int idx = startIdx;
    int len = condition.length();
    boolean escaped = false;
    while (idx < len) {
      char c = condition.charAt(++idx);
      if (c == '\\' && !escaped) {
        escaped = true;
        continue;
      } else if (c == escapeChar && !escaped) {
        return idx;
      }
      escaped = false;
    }
    throw new SQLTokenizerException("unfinished escape sequence starting at position " + startIdx);
  }


  private int parseWordOr(int idx, String condition, List<ConditionToken> tokens, TokenType... types) {
    int lastIdxOfWord = nextWord(condition, idx);
    for (TokenType t : types) {
      if (substringIs(condition, idx, lastIdxOfWord, t.lowercase)) {
        tokens.add(new ConditionToken(t, idx, lastIdxOfWord));
        return lastIdxOfWord + 1;
      }
    }
    tokens.add(new ConditionToken(TokenType.WORD, idx, lastIdxOfWord));
    return lastIdxOfWord + 1;
  }


  private boolean substringIs(String condition, int idx, int lastIdxOfWord, String string) {
    if (lastIdxOfWord - idx + 1 != string.length()) {
      return false;
    }
    for (int i = 0; i < string.length(); i++) {
      if (condition.charAt(idx + i) != string.charAt(i)) {
        return false;
      }
    }
    return true;
  }


  //parse bis zum n�chsten zeichen, welches das wort beendet, d.h. es sind nur buchstaben und zahlen und underscore erlaubt
  private int nextWord(String condition, int idx) {
    int len = condition.length();
    while (idx < len - 1) {
      char c = condition.charAt(++idx);
      int diffA = c - 'a';
      if (diffA >= 0 && diffA < 26) {
        continue;
      }
      int diffN = c - '0';
      if (diffN >= 0 && diffN < 10) {
        continue;
      }
      if (c == '_') {
        continue;
      }
      return idx - 1;
    }
    return idx;
  }


}
