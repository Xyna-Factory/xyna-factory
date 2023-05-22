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
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


public class CsvUtils {

  public static final String CSV_SEPARATOR = ",";
  public static final String CSV_MASKER = "\"";
  public static final String CSV_LINESEPARATOR = "\n";
  
  
  public static <T> String toCSV( List<T> list, String separator, String masker ) {
    return toCSVImpl(list, separator, masker, null);
  }


  private static <T> String toCSVImpl(List<T> list, String separator, String masker, String lineSeparator) {
    if( list == null ) {
      return null;
    }
    if( list.size() == 0 ) {
      return "";
    }
    //Stringl�nge absch�tzen
    int estimate = list.size() * String.valueOf( list.get(0) ).length();
    StringBuilder sb = new StringBuilder( estimate );
    
    //nun anh�ngen
    String sep = "";
    for( T t : list ) {
      sb.append(sep);
      sep = separator;
      if( t == null ) {
        continue;
      }
      String value = String.valueOf( t );
      boolean containsSep = value.indexOf(sep) != -1;
      boolean containsLineBreaks = lineSeparator != null && value.indexOf(lineSeparator) > -1;
      boolean containsMasker = value.indexOf(masker) != -1;

      if( containsMasker ) {
        value = value.replace(masker, masker+masker);
      }
      if (containsSep || containsLineBreaks || containsMasker) {
        sb.append(masker).append(value).append(masker);
      } else {
        sb.append(value);
      }
    }
    return sb.toString();
  }


  public static String toCSV( List<String> list ) {
    return toCSV(list, CSV_SEPARATOR, CSV_MASKER);
  }


  public static String toCSVMultiLine(List<List<String>> list) {
    return toCSVMultiLine(list, CSV_SEPARATOR, CSV_MASKER, CSV_LINESEPARATOR);
  }


  public static String toCSVMultiLine(List<List<String>> list, String separator, String masker, String lineSeparator) {
    StringBuilder result = new StringBuilder();
    int count = 0;
    for (List<String> nextLine : list) {
      result.append(toCSVImpl(nextLine, separator, masker, lineSeparator));
      count++;
      if (count < list.size()) {
        result.append(lineSeparator);
      }
    }
    return result.toString();
  }


  public static int numOfConsecutiveOccurrences(String of, String in, int at) {
    if( of == null || of.length() == 0 ) return -1;
    if( in == null || in.length() == 0 ) return -1;
    if( at < 0 || at > in.length() ) return -1;
    int cnt = 0;
    int pos = at;
    while( in.indexOf(of, pos) == pos ) {
      pos += of.length();
      ++cnt;
    }
    return cnt;
  }
  

  
  public static int startsWithNConsecutiveOccurrences(String of, String in) {
    if( of == null || of.length() == 0 ) return -1;
    if( in == null || in.length() == 0 ) return -1;
    int cnt = 0;
    String curr = in;
    while( curr.startsWith(of) ) {
      curr = curr.substring(of.length());
      ++cnt;
    }
    return cnt;
  }
  
  public static int countOccurrences(String of, String in) {
    if( of == null || of.length() == 0 ) return -1;
    if( in == null || in.length() == 0 ) return 0;
    int cnt = 0;
    int pos = 0;
    while( pos != -1 ) {
      pos = in.indexOf(of, pos);
      if( pos == -1 ) {
        return cnt;
      } else {
        pos += of.length();
        ++cnt;
      }
    }
    return cnt;
  }


  public static class CSVDocument {

    List<CSVIterator> lines;


    public CSVDocument(String csvDocument, String separator, String masker, String emptyFieldString) {
      this(csvDocument, separator, masker, "\n", emptyFieldString);
    }


    public CSVDocument(String csvDocument, String separator, String masker, String lineSeparator,
                       String emptyFieldString) {

      if (masker.length() != 1) {
        // the splitDocumentToLines method is not capable of correctly skipping the next characters
        // if it hits a multi-
        throw new IllegalArgumentException("Masker length " + masker.length() + " of masker \"" + masker
            + "\" is not supported. Has to be a single character.");
      }

      lines = new ArrayList<CsvUtils.CSVIterator>();
      Iterable<String> lineSplitter = splitDocumentToLines(csvDocument, masker, lineSeparator);
      for (String nextLine : lineSplitter) {
        lines.add(new CSVIterator(nextLine, separator, String.valueOf(masker), emptyFieldString));
      }

    }


    public List<CSVIterator> getLines() {
      return lines;
    }


    static List<String> splitDocumentToLines(String csvDocument, String masker, String lineBreak) {

      List<String> result = new ArrayList<String>();
      char[] characters = csvDocument.toCharArray();

      StringBuilder nextLineBuilder = new StringBuilder();
      boolean masked = false;
      int escapeCounter = 0;

      char[] maskerChar = masker.toCharArray();
      char[] lineBreakChar = lineBreak.toCharArray();
      for (int i=0; i<characters.length; i++) {

        char nextChar = characters[i];
        boolean nextIsMaskerSequence = charArrayStartsWithAtIndex(characters, maskerChar, i);

        if (masked) {

          /*
           * Wenn jetzt kein Anf�hrungszeichen kommt
           *    1.1 Wenn vorher ungerade Anzahl kam => �nderung Masker-Flag
           *    1.2 Wenn vorher gerade Anzahl kam => Keine �nderung Masker-Flag
           */
          if (!nextIsMaskerSequence) {
            // ordinary character case
            if (escapeCounter % 2 == 1) {
              masked = false;
            }
            escapeCounter = 0;
            // now take special care of the case that a masked area is followed by a linebreak
            if (!masked && charArrayStartsWithAtIndex(characters, lineBreakChar, i)) {
              result.add(nextLineBuilder.toString());
              nextLineBuilder = new StringBuilder(nextLineBuilder.length());
              // skip the rest of the line break character sequence
              // count till x-1 because the following "continue" increments i by one as well
              i += lineBreakChar.length - 1;
              continue;
            }
          } else {
            // masker case
            escapeCounter++;
            for (int k = 0; k < maskerChar.length; k++) {
              nextLineBuilder.append(characters[i+k]);
            }
            i += maskerChar.length - 1;
            continue;
          }

        } else {

          if (charArrayStartsWithAtIndex(characters, lineBreakChar, i)) {
            masked = false;
            result.add(nextLineBuilder.toString());
            nextLineBuilder = new StringBuilder(nextLineBuilder.length());
            escapeCounter = 0;
            // skip the rest of the line break character sequence
            // count till x-1 because the following "continue" increments i by one as well
            i += lineBreakChar.length - 1;
            continue;
          } else if (nextIsMaskerSequence) {
            masked = true;
            for (int k = 0; k < maskerChar.length; k++) {
              nextLineBuilder.append(characters[i + k]);
            }
            i += maskerChar.length - 1;
            continue;
          }

        }

        nextLineBuilder.append(nextChar);

      }

      if (nextLineBuilder.length() > 0) {
        result.add(nextLineBuilder.toString());
      }

      return result;
    }


    static boolean charArrayStartsWithAtIndex(char[] content, char[] searchedArray, int beginIndex) {

      int maxContentIndex = beginIndex + searchedArray.length;
      if (maxContentIndex > content.length) {
        return false;
      }

      for (int i = 0; i < searchedArray.length; i++) {
        if (content[i + beginIndex] != searchedArray[i]) {
          return false;
        }
      }
      return true;

    }

  }

  
  public static class CSVIterator implements Iterator<String> {

    private String separator;
    private String masker;
    private String subCsv; //( <separator><content> ) *
    private int sepLength;
    private int maskLength;
    private String next;
    private String resultIfEmptyString = null;

    private boolean hasNextCalled = false;


    public CSVIterator(String csv) {
      this(csv, CSV_SEPARATOR, CSV_MASKER);
    }


    public CSVIterator(String csv, String separator, String masker) {
      this.separator = separator;
      this.masker = masker;
      this.subCsv = csv != null ? separator + csv : null; //separator vorne anf�gen, dann kann man immer bis zum n�chsten separator suchen und die behandlung am ende der zeile ist einfacher
      this.sepLength = separator.length();
      this.maskLength = masker.length();
    }

    public CSVIterator(String csv, String separator, String masker, String resultIfEmptyString) {
      this(csv, separator, masker);
      this.resultIfEmptyString = resultIfEmptyString;
    }


    public boolean hasNext() {


      if( subCsv == null || subCsv.length() == 0) {
        return false;
      }

      subCsv = subCsv.substring(sepLength);      
      hasNextCalled = true;
      
      if (subCsv.length() == 0 || subCsv.startsWith(separator)) {
        next = resultIfEmptyString;
        return true;
      }
   
      int nextSepPos = 0;
      while( true ) {
        nextSepPos = subCsv.indexOf(separator, nextSepPos );
        if( nextSepPos == -1 ) {
          if( subCsv.length() == 0 ) {
            next = resultIfEmptyString;
          } else {
            next = unmask( subCsv ); //kompletten Rest nehmen
          }
          subCsv = null;
          return true;
        }
        int maskerCnt = CsvUtils.countOccurrences( masker, subCsv.substring(0, nextSepPos ) );
        if( maskerCnt % 2 == 1 ) {
          //ungerade Anzahl von Maskern, daher ist gefundener separator kein richtiger
          nextSepPos += sepLength;
          continue;
        } else {
          next = unmask( subCsv.substring(0,nextSepPos) );
          subCsv = subCsv.substring(nextSepPos);
          return true;
        }
      }
    }


    String unmask(String masked) {
      if (masked.indexOf(separator) != -1) {
        //String muss maskiert sein
        String trimmed = masked.trim();
        if( trimmed.startsWith(masker) && trimmed.endsWith(masker) ) {
          return trimmed.substring(maskLength,trimmed.length()-maskLength).replace(masker+masker, masker);
        } else {
          if (!(masked.startsWith(masker) && masked.endsWith(masker))) {
            return masked.replace(masker+masker, masker);
          } else {
            //Problem
            throw new IllegalArgumentException("String '" + masked
                + "' is missing leading or trailing masker '" + masker + "'");
          }
        }
      } else {
        // String kann maskiert sein
        String trimmed = masked.trim();
        if( trimmed.startsWith(masker) && trimmed.endsWith(masker) ) {
          return trimmed.substring(maskLength,trimmed.length()-maskLength).replace(masker+masker, masker);
        } else {
          return masked.replace(masker+masker, masker);
        }
      }
    }


    public String next() {
      if (hasNextCalled) {
        hasNextCalled = false;
        return next;
      } else {
        if (hasNext()) {
          return next;
        } else {
          throw new NoSuchElementException("No next entry available");
        }
      }
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }

  
  public static class CSVIterable implements Iterable<String> {

    private CSVIterator iterator;

    public CSVIterable(String csv, String separator, String masker) {
      this.iterator = new CSVIterator(csv, separator, masker);
    }

    public CSVIterable(String csv) {
      this.iterator = new CSVIterator(csv);
    }

    public Iterator<String> iterator() {
      return iterator;
    }
    
  }
  
  public static Iterable<String> iterate(String csv) {
    //aus abw�rtskompatibilit�tsgr�nden leere strings als null behandeln
    return iterate(csv, false); 
  }
  
  public static Iterable<String> iterate(String csv, boolean treatEmptyStringAsOneCell) {
    if (treatEmptyStringAsOneCell) {
      return new CSVIterable(csv);
    } else {
      return new CSVIterable(csv != null && csv.length() == 0 ? null : csv);
    }
  }

  
}
