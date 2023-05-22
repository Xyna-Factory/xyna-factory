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
package com.gip.xyna.utils.misc;


/**
 * StringBuilder-Nachbau, der sich um eine Einr�ckung der generierten Textes k�mmert.
 * <br>
 * Die Einr�ckung wird dabei automatisch eingef�gt, sobald der n�chste 
 * {@link #append(Object)}- oder {@link #append(String)}-Aufruf nach einem 
 * {@link #linebreak()}-Aufruf erfolgt.<br>
 * Die Einr�ckung ist anfangs 0, sie kann dann �ber {@link #increment()}, {@link #increment(int)},
 * {@link #decrement()} und {@link #decrement(int)} angepasst werden.
 * Default-Einr�ckzeichen ist ein Leerzeichen und Default-Einr�cktiefe ist 2.
 *
 */
public class IndentableStringBuilder {
  StringBuilder sb = new StringBuilder();
  private int indent;
  private char indentChar;
  private boolean indentNext;
  private int indentStep;
  
  public IndentableStringBuilder() {
    indent = 0;
    indentChar = ' ';
    indentStep = 2;
    indentNext = false;
  }
 
  /**
   * Erh�hung der Einr�cktiefe um die Default-Einr�cktiefe
   * @return
   */
  public IndentableStringBuilder increment() {
    indent += indentStep;
    return this;
  }
  
  /**
   * Verringerung der Einr�cktiefe um die Default-Einr�cktiefe
   * @return
   */
  public IndentableStringBuilder decrement() {
    indent -= indentStep;
    return this;
  }
  
  public IndentableStringBuilder increment(int inc) {
    indent += inc;
    return this;
  }
  
  public IndentableStringBuilder decrement(int dec) {
    indent -= dec;
    return this;
  }
  
  public IndentableStringBuilder indent() {
    for( int i=0; i<indent; ++i ) {
      sb.append(indentChar);
    }
    return this;
  }
  
  /**
   * Erg�nzt einen Zeilenumbruch und setzt das Flag, so dass das n�chste Append einger�ckt wird.
   * @return
   */
  public IndentableStringBuilder linebreak() {
    sb.append('\n');
    indentNext = true;
    return this;
  }
  
  public IndentableStringBuilder append(String string ) {
    if( indentNext ) {
      indent();
      indentNext = false;
    }
    sb.append(string);
    return this;
  }
  
  public IndentableStringBuilder append(Object o ) {
    if( indentNext ) {
      indent();
      indentNext = false;
    }
    sb.append(String.valueOf(o));
    return this;
  }
  
  
  @Override
  public String toString() {
    return sb.toString();
  }

}
