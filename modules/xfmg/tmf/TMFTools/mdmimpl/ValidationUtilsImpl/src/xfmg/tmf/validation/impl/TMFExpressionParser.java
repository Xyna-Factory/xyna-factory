/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package xfmg.tmf.validation.impl;



import java.util.ArrayList;
import java.util.List;

import xfmg.tmf.validation.impl.functioninterfaces.TMFDirectFunction;
import xfmg.tmf.validation.impl.functioninterfaces.TMFFunction;
import xfmg.tmf.validation.impl.functioninterfaces.TMFInfixFunction;



public class TMFExpressionParser {

  final List<TMFDirectFunction> functions;
  final List<TMFFunction> infixFunctions;


  public TMFExpressionParser(List<TMFDirectFunction> functions, List<TMFFunction> infixFunctions) {
    super();
    this.functions = functions;
    this.infixFunctions = infixFunctions;
  }


  /*
   * 3 parts:
   * 1. parse hard coded
   *   a) (...)
   *   b) Constants
   *      "..."
   *      numbers
   *      true/false
   *   c) $<number>
   * 2. parse direct functions, i.e. FUNCTIONNAME(...)
   * 3. parse infix functions, i.e. + ..., or - ...
   *    can occur multiple times afterwards
   */
  public SyntaxTreeNode parse(final String expr, int idx, boolean parseInfix) {
    final int startIdx = idx;
    final int lastIdx = expr.length() - 1;
    char c = expr.charAt(idx);
    while (Character.isWhitespace(c)) {
      if (idx == lastIdx) {
        throw new RuntimeException("Unexpected end of expression: " + expr);
      }
      c = expr.charAt(++idx);
    }
    SyntaxTreeNode n = null;
    switch (c) {
      case '(' :
        n = parse(expr, idx + 1, true);
        idx = n.lastIdx;
        if (lastIdx == idx) {
          return new SyntaxTreeNode(null, null, expr.substring(startIdx, idx + 1), idx);
        }
        c = expr.charAt(++idx);
        if (c != ')') {
          throw new RuntimeException("Missing closing parenthesis at index " + idx + ": " + expr);
        }
        //parenthesis function is sugar, not important, only for debugging
        //important is update of lastIdx of n
        n = new SyntaxTreeNode(new Parenthesis(), new SyntaxTreeNode[] {n}, null, idx);
        break;
      case '\"' :
        n = parseString(expr, idx + 1);
        idx = n.lastIdx;
        break;
      case '$' :
        n = parseInteger(expr, idx + 1);
        if (n.value.startsWith("-")) {
          throw new RuntimeException("Negative variable index not allowed at idx " + idx + ": " + expr);
        }
        n = new SyntaxTreeNode(null, null, "$" + n.value, n.lastIdx);
        idx = n.lastIdx;
        break;
      case '-' :
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
        n = parseNumber(expr, idx);
        idx = n.lastIdx;
        break;
      case 't' :
      case 'T' :
        if (lastIdx - idx >= 3 && expr.substring(idx + 1, idx + 4).equalsIgnoreCase("rue")) {
          n = new SyntaxTreeNode(null, null, "true", idx + 3);
          idx = n.lastIdx;
        }
        break;
      case 'f' :
      case 'F' :
        if (lastIdx - idx >= 4 && expr.substring(idx + 1, idx + 5).equalsIgnoreCase("alse")) {
          n = new SyntaxTreeNode(null, null, "false", idx + 4);
          idx = n.lastIdx;
        }
        break;
      default :
        n = null;
        break;
    }
    if (n == null) {
      int remaining = lastIdx - idx + 1; //including current idx
      for (TMFDirectFunction f : functions) {
        String name = f.getName();
        int len = name.length();
        //2 parentheses expected at least
        if (remaining >= len + 2 && expr.substring(idx, idx + len).equals(name)) {
          //parse args
          idx += len;
          c = expr.charAt(idx);
          if (c != '(') {
            throw new RuntimeException("Expected opening parenthesis at index " + idx + ": " + expr);
          }
          c = expr.charAt(++idx);
          List<SyntaxTreeNode> args = new ArrayList<>();
          while (c != ')') {
            SyntaxTreeNode lastArg = parse(expr, idx, true);
            args.add(lastArg);
            //skip comma (and whitespaces) if there are any
            idx = lastArg.lastIdx;
            if (lastIdx == idx) {
              throw new RuntimeException("Expression (starting at index " + startIdx
                  + ") not ending correctly. Missing at least one closing parenthesis: " + expr);
            }
            c = expr.charAt(++idx);
            while (Character.isWhitespace(c)) {
              if (lastIdx == idx) {
                throw new RuntimeException("Expression (starting at index " + startIdx
                    + ") not ending correctly. Missing at least one closing parenthesis: " + expr);
              }
              c = expr.charAt(++idx);
            }
            if (c != ',' && c != ')') {
              throw new RuntimeException("Expected closing parenthesis or comma at index " + idx + ": " + expr);
            }
            if (c == ',') {
              if (lastIdx == idx) {
                throw new RuntimeException("Expression not ending correctly. Missing at least one closing parenthesis: " + expr);
              }
              c = expr.charAt(++idx);
              while (Character.isWhitespace(c)) {
                if (idx == lastIdx) {
                  throw new RuntimeException("Expression not ending correctly. Missing at least one closing parenthesis: " + expr);
                }
                c = expr.charAt(++idx);
              }
            }
          }
          n = new SyntaxTreeNode(f, args.toArray(new SyntaxTreeNode[args.size()]), null, idx);
          break; //function found, don't check other functions
        }
      }
    }
    if (n == null) {
      throw new RuntimeException("Unexpected character " + c + " at index " + idx + ": " + expr);
    }

    if (!parseInfix) {
      return n;
    }

    /*
     * check infix expressions
     * there can be multiple, then "operator precedence" has to be evaluated.
     * => first collect all list of infix functions, then group args by precedence.
     */
    List<SyntaxTreeNode> foundInfixFunctions = new ArrayList<>();
    List<SyntaxTreeNode> infixFunctionArgs = new ArrayList<>();
    infixFunctionArgs.add(n);
    while (true) {
      if (idx == lastIdx) {
        return orderInfix(foundInfixFunctions, infixFunctionArgs);
      }
      c = expr.charAt(++idx);
      while (Character.isWhitespace(c)) {
        if (idx == lastIdx) {
          return orderInfix(foundInfixFunctions, infixFunctionArgs);
        }
        c = expr.charAt(++idx);
      }
      switch (c) {
        case ',' : //fall through
        case ')' :
          return orderInfix(foundInfixFunctions, infixFunctionArgs);
        default : //ntbd
      }

      int remaining = lastIdx - idx + 1;
      boolean identifiedInfix = false;
      for (TMFFunction f : infixFunctions) {
        String name = f.getName();
        int len = name.length();
        //+1 because of arg minimum
        if (remaining >= len + 1 && expr.substring(idx, idx + len).equals(name)) {
          foundInfixFunctions.add(new SyntaxTreeNode(f, null, null, idx));
          identifiedInfix = true;
          //don't parse infix functions because we want all of them in a flat list here to being able to order them correctly
          n = parse(expr, idx + len, false);
          break;
        }
      }
      if (!identifiedInfix) {
        throw new RuntimeException("Unexpected character " + c + " at index " + idx + ": " + expr);
      }
      infixFunctionArgs.add(n);
      idx = n.lastIdx;
    }

  }


  /*
   * operator (=infixFunction) precedence ordering
   * A op1 B op2 C op3 D
   * -> A op1 X op3 D
   */
  private SyntaxTreeNode orderInfix(List<SyntaxTreeNode> infixFunctions, List<SyntaxTreeNode> infixFunctionArgs) {
    if (infixFunctions.size() + 1 != infixFunctionArgs.size()) {
      throw new RuntimeException("Bug: " + infixFunctions.size() + " , " + infixFunctionArgs.size());
    }
    if (infixFunctions.isEmpty()) {
      return infixFunctionArgs.get(0);
    }
    int maxPrecedence = infixFunctions.stream().mapToInt(f -> ((TMFInfixFunction) f.f).operatorPrecedence()).max().getAsInt();
    List<SyntaxTreeNode> remainingFunctions = new ArrayList<>();
    List<SyntaxTreeNode> remainingArgs = new ArrayList<>();
    for (int i = 0; i < infixFunctions.size(); i++) {
      //handle maxprecedence and store rest for next recursion
      SyntaxTreeNode n = infixFunctions.get(i);
      TMFInfixFunction f = (TMFInfixFunction) n.f;
      if (maxPrecedence == f.operatorPrecedence()) {
        SyntaxTreeNode merged = new SyntaxTreeNode(f, new SyntaxTreeNode[] {infixFunctionArgs.get(i), infixFunctionArgs.get(i + 1)}, null,
                                                   infixFunctionArgs.get(i + 1).lastIdx);
        infixFunctionArgs.set(i + 1, merged);
      } else {
        remainingFunctions.add(n);
        remainingArgs.add(infixFunctionArgs.get(i));
      }
    }
    remainingArgs.add(infixFunctionArgs.get(infixFunctionArgs.size() - 1));
    return orderInfix(remainingFunctions, remainingArgs);
  }


  /*
   * number = integer ( "." positiveinteger)? ( "e" integer)?
   */
  private SyntaxTreeNode parseNumber(String expr, final int startIdx) {
    SyntaxTreeNode n = parseInteger(expr, startIdx);
    int lastIdx = expr.length() - 1;
    if (lastIdx == n.lastIdx) {
      return n;
    }
    int idx = n.lastIdx + 1;
    int dotidx = -1;
    char c = expr.charAt(idx);
    for (int i = 0; i < 2; i++) {
      switch (c) {
        case '.' :
          if (dotidx != -1) {
            return n;
          }
          SyntaxTreeNode dec = parseInteger(expr, idx + 1);
          if (dec.value.startsWith("-")) {
            return n;
          }
          n = new SyntaxTreeNode(null, null, n.value + "." + dec.value, dec.lastIdx);
          dotidx = idx;
          idx = dec.lastIdx;
          break;
        case 'e' :
          SyntaxTreeNode exp = parseInteger(expr, idx + 1);
          return new SyntaxTreeNode(null, null, n.value + "e" + exp.value, exp.lastIdx);
        default :
          return n;
      }
      if (lastIdx == idx) {
        return new SyntaxTreeNode(null, null, expr.substring(startIdx, idx + 1), idx);
      }
      c = expr.charAt(++idx);
    }
    return null;
  }


  private SyntaxTreeNode parseInteger(String expr, final int startIdx) {
    int lastIdx = expr.length() - 1;
    int idx = startIdx;
    char c = expr.charAt(idx);
    if (c == '-') {
      if (lastIdx == idx) {
        return new SyntaxTreeNode(null, null, expr.substring(startIdx, idx + 1), idx);
      }
      c = expr.charAt(++idx);
    }
    while (true) {
      switch (c) {
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
          //next
          break;
        default :
          return new SyntaxTreeNode(null, null, expr.substring(startIdx, idx), idx - 1);
      }
      if (lastIdx == idx) {
        return new SyntaxTreeNode(null, null, expr.substring(startIdx, idx + 1), idx);
      }
      c = expr.charAt(++idx);
    }
  }


  private SyntaxTreeNode parseString(String expr, final int startIdx) {
    int lastIdx = expr.length() - 1;
    int idx = startIdx;
    char c = expr.charAt(idx);
    char[] data = new char[expr.length() - startIdx];
    int cnt = 0;
    while (true) {
      switch (c) {
        case '"' :
          return new SyntaxTreeNode(null, null, String.valueOf(data, 0, cnt), idx);
        case '\\' :
          if (idx == lastIdx) {
            throw new RuntimeException("String started at idx " + idx + ", but never ends: " + expr);
          }
          char next = expr.charAt(++idx);
          switch (next) {
            case '\\' :
            case '"' :
              break;
            default :
              data[cnt++] = c;
              break;
          }
          data[cnt++] = next;
          break;
        default :
          data[cnt++] = c;
          break;
      }
      if (idx == lastIdx) {
        throw new RuntimeException("String started at idx " + idx + ", but never ends: " + expr);
      }
      c = expr.charAt(++idx);
    }
  }
}
