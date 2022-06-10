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
import java.util.List;
import java.util.Stack;

import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.XFLLexem;
import com.gip.xyna.xprc.xfractwfe.formula.XFLLexer.TokenType;

public class XFLOperatorPrecedenceAdjuster {
  
  public static List<XFLLexem> handleOperatorPrecedence(List<XFLLexem> lexems) {
    Stack<List<XFLLexem>> scopeStack = new Stack<List<XFLLexem>>();
    scopeStack.push(new ArrayList<XFLLexem>());
    for (XFLLexem lexem : lexems) {
      boolean startScope = false;
      boolean appendCurrent = false;
      boolean  endScope = false;
      switch (lexem.getType()) {
        case BRACE :
        case LIST_ACCESS :
          if (isScopeStart(lexem)) {
            appendCurrent = true;
            startScope = true;
          } else {
            appendCurrent = true;
            endScope = true;
          }
          break;
        case ARGUMENT_SEPERATOR :
          startScope = true;
          appendCurrent = true;
          endScope = true;
          break;
        default :
          appendCurrent = true;
          break;
      }
      if (endScope) {
        List<XFLLexem> currentScope = handleCurrentOperatorPrecedenceScopeIterativly(scopeStack.pop());
        scopeStack.peek().addAll(currentScope);
      }
      if (appendCurrent) {
        scopeStack.peek().add(lexem);
      }
      if (startScope) {
        scopeStack.push(new ArrayList<XFLLexem>());
      }
    }
    if (scopeStack.size() > 1) {
      throw new RuntimeException("Invalid Expression: Missing Brace to close SubExpression.");
    }
    return handleCurrentOperatorPrecedenceScopeIterativly(scopeStack.pop());
  }
  
  
  private static List<XFLLexem> handleCurrentOperatorPrecedenceScopeIterativly(List<XFLLexem> currentScope) {
    List<XFLLexem> newScope = new ArrayList<XFLLexem>(currentScope);
    while (newScope != currentScope) {
      currentScope = newScope;
      newScope = handleCurrentOperatorPrecedenceScope(currentScope);  
    }
    return newScope;
  }
  
  
  private static boolean isScopeStart(XFLLexem lexem) {
    char brace = lexem.getToken().charAt(0);
    switch (brace) {
      case '(' :
      case '[' :
        return true;
      default :
        return false;
    }
  }
    
  
  private static List<XFLLexem> handleCurrentOperatorPrecedenceScope(List<XFLLexem> currentScope) {
    List<XFLLexem> rootOperators = new ArrayList<XFLLexem>();
    int scopeIndex = 0;
    for (XFLLexem lexem : currentScope) {
      switch (lexem.getType()) {
        case BRACE :
        case LIST_ACCESS :
          if (isScopeStart(lexem)) {
            scopeIndex++;
          } else {
            scopeIndex--;
          }
          break;
        case OPERATOR :
        case ASSIGNMENT :
          if (scopeIndex == 0) {
            rootOperators.add(lexem);
          }
        default :
          break;
      }
    }
    if (rootOperators.size() <= 1) {
      return currentScope;
    } else {
      boolean samePrecedence = true;
      int precedence = -1;
      for (XFLLexem lexem : rootOperators) {
        if (precedence == -1) {
          precedence = getOperatorPrecedence(lexem);
        } else if (precedence != getOperatorPrecedence(lexem)) {
          int newPrecedence = getOperatorPrecedence(lexem);
          if (precedence > newPrecedence) {
            precedence = newPrecedence;
          }
          samePrecedence = false;
        }
      }
      if (samePrecedence) {
        return currentScope;
      } else {
        XFLLexem previousOperator = null;
        XFLLexem foundOperator = null;
        XFLLexem nextOperator = null;
        for (XFLLexem lexem : rootOperators) {
          if (getOperatorPrecedence(lexem) == precedence) {
            if (foundOperator == null) {
              foundOperator = lexem;
            } else {
              nextOperator = lexem;
              break;
            }
          } else {
            if (foundOperator == null) {
              previousOperator = lexem;
            } else {
              nextOperator = lexem;
              break;
            }
          }
        }
        List<XFLLexem> rewrite = new ArrayList<XFLLexem>();
        int start = 0;
        int end = currentScope.size();
        if (previousOperator != null) {
          start = currentScope.indexOf(previousOperator) + 1;
        }
        if (nextOperator != null) {
          end = currentScope.indexOf(nextOperator);
        }
        rewrite.addAll(currentScope.subList(0, start));
        rewrite.add(new XFLLexem("(", TokenType.BRACE));
        rewrite.addAll(currentScope.subList(start, end));
        rewrite.add(new XFLLexem(")", TokenType.BRACE));
        rewrite.addAll(currentScope.subList(end, currentScope.size()));
        return rewrite;
      }
    }
  }


  /*
   * 0  postfix  expr++ expr--
   * 1  unary   ++expr --expr +expr -expr ~ !
   * 2  multiplicative  * / %
   * 3  additive  + -
   * 4  shift   << >> >>>
   * 5  relational  < > <= >= instanceof
   * 6  equality  == !=
   * 7  bitwise AND   &
   * 8  bitwise exclusive OR  ^
   * 9  bitwise inclusive OR  |
   * 10 logical AND   &&
   * 11 logical OR  ||
   * 12 ternary   ? :
   * 13 assignment  = += -= *= /= %= &= ^= |= <<= >>= >>>=
   *
   * [Source: http://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html ]
   */
  private static int getOperatorPrecedence(XFLLexem token) {
    switch (token.getType()) {
      case OPERATOR :
        char first = token.getToken().charAt(0);
        switch (first) {
          case '*' :
          case '/' :
            return 2;
          case '+' :
          case '-' :
            return 3;
          case '<' :
          case '>' :
            return 5;
          case '!' :
            if (first == '!' && 
              token.getToken().length() == 1) {
              return 1;
            }
            //fallthrough
          case '=' :
            if (token.getToken().length() == 2 &&
                token.getToken().charAt(1) == '=') {
              return 6;
            } else {
              throw new RuntimeException("Can not determine OperatorPrecedence for: " + token);
            }
          case '&' :
            if (token.getToken().length() == 2 &&
                token.getToken().charAt(1) == '&') {
              return 10;
            } else {
              throw new RuntimeException("Can not determine OperatorPrecedence for: " + token);
            }
          case '|' :
            if (token.getToken().length() == 2 &&
                token.getToken().charAt(1) == '|') {
              return 11;
            } else {
              throw new RuntimeException("Can not determine OperatorPrecedence for: " + token);
            }
          default :
            throw new RuntimeException("Can not determine OperatorPrecedence for: " + token);
        }
      case ASSIGNMENT :
        return 13;
      default :
        throw new RuntimeException("Can not determine OperatorPrecedence for: " + token);
    }
  }
  
  
}