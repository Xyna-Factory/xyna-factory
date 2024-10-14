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
package com.gip.xyna.utils.exceptions.exceptioncode;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;



public class ExceptionCodeManagement {


  private static final java.util.regex.Pattern REGEXP_FOR_EXCEPTIONCODE_PATTERNS = java.util.regex.Pattern
                  .compile("(.*?)\\[\\[\\]\\](.*?)");

  private Map<String, CodeGroup> codeGroups;


  public ExceptionCodeManagement() {
    codeGroups = new HashMap<String, CodeGroup>();
  }
  
  protected void init(Map<String, CodeGroup> codeGroups) {
    this.codeGroups = codeGroups;
  }


  public String createExceptionCode(String codeGroupName) throws CodeGroupUnknownException, NoCodeAvailableException {
    if (codeGroupName == null) {
      throw new IllegalArgumentException("codeGroupName must not be null");
    }
    synchronized (codeGroups) {
      CodeGroup codeGroup = codeGroups.get(codeGroupName);
      if (codeGroup == null) {
        throw new CodeGroupUnknownException(codeGroupName);
      }
      Pattern pattern = codeGroup.getPattern(codeGroup.indexOfCurrentlyUsedPattern);
      if (pattern.currentIndex >= pattern.endIndex) {
        if (codeGroup.indexOfCurrentlyUsedPattern >= codeGroup.patterns.size() - 1) {
          throw new NoCodeAvailableException(codeGroupName);
        }
        codeGroup.indexOfCurrentlyUsedPattern++;
        pattern = codeGroup.getPattern(codeGroup.indexOfCurrentlyUsedPattern);
      }
      return pattern.createExceptionCode();
    }
  }


  public void createCodeGroup(String codeGroupName) throws DuplicateCodeGroupException {
    synchronized (codeGroups) {
      if (codeGroups.containsKey(codeGroupName)) {
        throw new DuplicateCodeGroupException(codeGroupName);
      }
      codeGroups.put(codeGroupName, new CodeGroup(codeGroupName));
    }
  }


  public void removeCodeGroup(String codeGroupName) throws CodeGroupUnknownException {
    synchronized (codeGroups) {
      if (null == codeGroups.remove(codeGroupName)) {
        throw new CodeGroupUnknownException(codeGroupName);
      }
    }
  }
  
  public void removeCodePattern(String codeGroupName, int patternIndex) throws CodeGroupUnknownException {
    synchronized (codeGroups) {
      CodeGroup codeGroup = codeGroups.get(codeGroupName);
      if (null == codeGroup) {
        throw new CodeGroupUnknownException(codeGroupName);
      }
      if (codeGroup.patterns != null && codeGroup.patterns.size() > patternIndex) {
        codeGroup.patterns.remove(patternIndex);
      } else {
        //TODO eigene fehlermeldung?
        if (codeGroup.patterns == null || codeGroup.patterns.size() == 0) {
          throw new IndexOutOfBoundsException("code group " + codeGroupName + " has no patterns.");
        } else {
          throw new IndexOutOfBoundsException("code group " + codeGroupName + " has only " + codeGroup.patterns.size() + " patterns.");
        }
      }
    }
  }

  /**
   * Beispiel: Pattern = "abc[[]]def", start=55, length=1000; padding=5
   * => generierte Codes sehen aus: abc00055def bis abc01054def
   * @param codeGroupName
   * @param pattern
   * @param start
   * @param length
   * @param padding formatierte länge von zahlen.
   * @throws CodeGroupUnknownException falls die angegeben code gruppe nicht existiert
   * @throws InvalidPatternException falls syntax des pattern nicht geparst werden kann
   * @throws OverlappingCodePatternException falls die angegebenen start+length parameter mit einer existierenden code gruppe kollidieren
   */
  public void addExceptionCodePattern(String codeGroupName, String pattern, int start, int length, int padding)
                  throws CodeGroupUnknownException, InvalidPatternException, OverlappingCodePatternException {
    if (start < 0 || length < 1 || padding < 1 || padding < ("" + (start + length - 1)).length()) {
      throw new IllegalArgumentException("invalid values for start, length, padding");
    }
    synchronized (codeGroups) {
      CodeGroup codeGroup = codeGroups.get(codeGroupName);
      if (codeGroup == null) {
        throw new CodeGroupUnknownException(codeGroupName);
      }
      Pattern p = checkOverlap(codeGroupName, pattern, start, length, padding);
      codeGroup.patterns.add(p);
    }
  }
  
  /**
   * gibt pattern zurück, falls kein overlap gefunden wurde
   * @throws OverlappingCodePatternException
   * @throws InvalidPatternException 
   */
  private Pattern checkOverlap(String codeGroupName, String pattern, int start, int length, int padding) throws OverlappingCodePatternException, InvalidPatternException {    
    Pattern p = new Pattern(pattern, start, length, padding);
    for (Map.Entry<String, CodeGroup> entry : codeGroups.entrySet()) {
      for (Pattern oldPattern : entry.getValue().patterns) {
        if (oldPattern.padding == p.padding && oldPattern.prefix.equals(p.prefix) && oldPattern.suffix.equals(p.suffix)) {
          //codes sehen ähnlich aus: wie siehts mit den bereichen aus?
          //1. ist startIndex von p innerhalb des intervalls von oldPattern?
          if (p.startIndex >= oldPattern.startIndex && p.startIndex <= oldPattern.endIndex) {
            throw new OverlappingCodePatternException(codeGroupName, entry.getKey());
          }
          //2. ist endIndex von o innerhalb des intervalls von oldPattern?
          if (p.endIndex >= oldPattern.startIndex && p.endIndex <= oldPattern.endIndex) {
            throw new OverlappingCodePatternException(codeGroupName, entry.getKey());
          }
          //3. jetzt kann höchstens noch passieren, dass das oldPattern vollständig im neuen enthalten ist
          if (oldPattern.startIndex >= p.startIndex && oldPattern.startIndex <= p.endIndex) {
            throw new OverlappingCodePatternException(codeGroupName, entry.getKey());
          }
        }
      }
    }
    return p;
  }


  public CodeGroup[] getCodeGroups() {
    synchronized (codeGroups) {
      return codeGroups.values().toArray(new CodeGroup[0]);
    }
  }


  public static class CodeGroup {

    private String codeGroupName;
    private int indexOfCurrentlyUsedPattern;
    private List<Pattern> patterns;


    public CodeGroup(String codeGroupName) {
      this.codeGroupName = codeGroupName;
      patterns = new ArrayList<Pattern>();
    }

    public CodeGroup(String codeGroupName, int indexOfCurrentlyUsedPattern) {
      this.codeGroupName = codeGroupName;
      this.indexOfCurrentlyUsedPattern = indexOfCurrentlyUsedPattern;
      patterns = new ArrayList<Pattern>();
    }
    
    
    public Pattern getPattern(int index) {
      return patterns.get(index);
    }


    
    public String getCodeGroupName() {
      return codeGroupName;
    }


    
    public void setCodeGroupName(String codeGroupName) {
      this.codeGroupName = codeGroupName;
    }


    
    public int getIndexOfCurrentlyUsedPattern() {
      return indexOfCurrentlyUsedPattern;
    }


    
    public void setIndexOfCurrentlyUsedPattern(int indexOfCurrentlyUsedPattern) {
      this.indexOfCurrentlyUsedPattern = indexOfCurrentlyUsedPattern;
    }


    
    public List<Pattern> getPatterns() {
      return patterns;
    }


    
    public void setPatterns(List<Pattern> patterns) {
      this.patterns = patterns;
    }
    
    
  }

  public static class Pattern {

    private String prefix;
    private String suffix;
    private int padding;
    private int startIndex;
    private int endIndex;
    private int currentIndex;


    public Pattern(String pattern, int start, int length, int padding) throws InvalidPatternException {
      Matcher matcher = REGEXP_FOR_EXCEPTIONCODE_PATTERNS.matcher(pattern);
      if (matcher.matches()) {
        prefix = matcher.group(1);
        suffix = matcher.group(2);
        startIndex = start;
        endIndex = startIndex + length - 1;
        currentIndex = startIndex - 1;
        this.padding = padding;
      } else {
        throw new InvalidPatternException(pattern, REGEXP_FOR_EXCEPTIONCODE_PATTERNS.pattern());
      }
    }
    
    public Pattern(String prefix, String suffix, int padding, int startIndex, int endIndex, int currentIndex) {
      this.prefix = prefix;
      this.suffix = suffix;
      this.padding = padding;
      this.startIndex = startIndex;
      this.endIndex = endIndex;
      this.currentIndex = currentIndex;
    }


    public String createExceptionCode() {
      currentIndex++;
      return prefix + getPadded(currentIndex, padding) + suffix;
    }


    
    public String getPrefix() {
      return prefix;
    }


    
    public void setPrefix(String prefix) {
      this.prefix = prefix;
    }


    
    public String getSuffix() {
      return suffix;
    }


    
    public void setSuffix(String suffix) {
      this.suffix = suffix;
    }


    
    public int getPadding() {
      return padding;
    }


    
    public void setPadding(int padding) {
      this.padding = padding;
    }


    
    public int getStartIndex() {
      return startIndex;
    }


    
    public void setStartIndex(int startIndex) {
      this.startIndex = startIndex;
    }


    
    public int getEndIndex() {
      return endIndex;
    }


    
    public void setEndIndex(int endIndex) {
      this.endIndex = endIndex;
    }


    
    public int getCurrentIndex() {
      return currentIndex;
    }


    
    public void setCurrentIndex(int currentIndex) {
      this.currentIndex = currentIndex;
    }
    
      }


  private static String getPadded(int n, int padding) {
    String ret = "" + n;
    int l = ret.length();
    for (int i = 0; i < padding - l; i++) {
      ret = "0" + ret;
    }
    return ret;
  }

}
