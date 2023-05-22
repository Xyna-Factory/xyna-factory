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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.Pair;


/**
 * StringReplacer kann mehrere Textersetzungen vornehemen. 
 * Die einzelnen Ersetzungsschritte werden dabei vorher �ber den StringReplacerBuilder konfiguriert.
 * StringReplacer ist threadsafe und kann daher gut als Konstante abgelegt werden.
 */
public class StringReplacer {

  List<Pair<Pattern,String>> replacements;
  
  private StringReplacer(List<Pair<Pattern,String>> replacements) {
    this.replacements = new ArrayList<Pair<Pattern,String>>(replacements);
  }
  
  public String replace(String string) {
    if( string == null ) {
      return null;
    }
    String out = string;
    for( Pair<Pattern,String> pair : replacements ) {
      out = pair.getFirst().matcher(out).replaceAll(pair.getSecond());
    }
    return out;
  }

  
  
  public static class StringReplacerBuilder {

    List<Pair<Pattern,String>> replacements = new ArrayList<Pair<Pattern,String>>();
    
    /**
     * Ersetzt einzelnes Zeichen
     * @param c
     * @param replacement
     * @return
     */
    public StringReplacerBuilder replace(char c, String replacement) {
      replacements.add( Pair.of( Pattern.compile( String.valueOf(c), Pattern.LITERAL), 
                                 Matcher.quoteReplacement(replacement) ) );
      return this;
    }
    
    /**
     * Ersetzt exakten String
     * @param string
     * @param replacement
     * @return
     */
    public StringReplacerBuilder replace(String string, String replacement) {
      replacements.add( Pair.of( Pattern.compile( string, Pattern.LITERAL), 
                                 Matcher.quoteReplacement(replacement) ) );
      return this;
    }
    
    /**
     * Ersetzt Pattern
     * @param pattern
     * @param replacement
     * @return
     */
    public StringReplacerBuilder replacePattern(String pattern, String replacement) {
      replacements.add( Pair.of( Pattern.compile(pattern), 
                                 Matcher.quoteReplacement(replacement) ) );
      return this;
    }
    
    /**
     * Ersetzt Pattern
     * @param pattern
     * @param replacement
     * @return
     */
    public StringReplacerBuilder replacePattern(Pattern pattern, String replacement) {
      replacements.add( Pair.of( pattern, 
                                 Matcher.quoteReplacement(replacement) ) );
      return this;
    }
    
    /**
     * Ersetzt Pattern durch weiteres Pattern, um beispielsweise Zugriff auf die gematchten Teile zu haben
     * @param pattern
     * @param replacement
     * @return
     */
    public StringReplacerBuilder replacePatternWithPattern(String pattern, String replacement) {
      replacements.add( Pair.of( Pattern.compile(pattern), replacement ) );
      return this;
    }
    

    public StringReplacer build() {
      return new StringReplacer(replacements);
    }
    
  }

  public static StringReplacerBuilder replace(char c, String string) {
    StringReplacerBuilder srb = new StringReplacerBuilder();
    return srb.replace(c,string);
  }


}
