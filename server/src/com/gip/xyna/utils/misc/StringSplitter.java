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
package com.gip.xyna.utils.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.Pair;



/**
 * StringSplitter zerlegt Strings anhand des im Konstruktor übergebenen Patterns. 
 * Im Unterschied zu String.split(..) können auch die Trenner mit die zurückgegebene 
 * Liste eingetragen werden.
 * Beispiel: new StringSplitter("\\.").split(".a.bc.d") liefert Liste [., a, ., bc, ., d]
 */
public class StringSplitter {

  private Pattern pattern;

  public StringSplitter(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }

  public boolean isSeparator(String string) {
    return pattern.matcher(string).matches();
  }
    
  public List<String> split(String string, boolean withSeparator) {
    List<String> list = new ArrayList<String>();
    Matcher m = pattern.matcher(string);
    
    int index = 0;
    while(m.find()) {
      if( index != m.start() ) {
        String match = string.subSequence(index, m.start()).toString();
        list.add(match);
      }
      if( withSeparator ) {
        String sep = string.subSequence(m.start(), m.end()).toString();
        list.add(sep);
      }
      index = m.end();
    }
    if( index != string.length() ) {
      list.add(string.subSequence(index, string.length()).toString());
    }
    return list;
  }

  public <S,M> List<Pair<S,M>> splitAndApply(String string, SplitApply<S,M> apply ) {
    List<Pair<S,M>> list = new ArrayList<Pair<S,M>>();
    Matcher matcher = pattern.matcher(string);
    
    int index = 0;
    while(matcher.find()) {
      S s = null;
      if( index != matcher.start() ) {
        String sep = string.subSequence(index, matcher.start()).toString();
        s = apply.applySeparator(sep);
      }
      M m = apply.applyMatch(matcher);
      index = matcher.end();
      list.add( Pair.of(s,m) );
    }
    if( index != string.length() ) {
      String sep = string.subSequence(index, string.length()).toString();
      S s = apply.applySeparator(sep);
      list.add( Pair.of(s,(M)null) );
    }
    return list;
  }

  public interface SplitApply<S,M> {

    M applyMatch(Matcher matcher);

    S applySeparator(String sep);
    
  }
  
}
