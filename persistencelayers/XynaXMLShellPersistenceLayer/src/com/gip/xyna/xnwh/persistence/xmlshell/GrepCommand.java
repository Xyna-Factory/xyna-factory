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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser.EscapeParameters;


public class GrepCommand implements Runnable {

  private static enum OPERATION {EQUALS, LIKE, GREATER, SMALLER}; 
  
  private static Logger logger = CentralFactoryLogging.getLogger(GrepCommand.class);
  
  private final static String SHELL = "sh";
  private final static String SHELLPARAM = "-c";
  private final static String PRECOMMANDS = "find . -type f | xargs ";
  //private final static String GREP = "grep";
  private final static String FIXEDSTRING = "-F";
  private final static String RESTRICT_TO_FILES = "-l";
  //private final static String RESTRICT_TO_FILES_AND_RECURSE = "-lr";   -r is not support on Suns, which our VMs are 
  private final static String EXTENDED_REGULAR_EXPRESSION = "-E";
  
  private final static String EQUALS = " = ";
  private final static String LIKE = " LIKE ";
  private final static String GREATER = " > ";
  private final static String SMALLER = " < ";
  
  private Pattern pattern;
  private Matcher matcher;
  
  private String archiveDir;
  private List<String> grepPattern;
  private String grepCommand;
  private List<OPERATION> operation = null;
  
  private Set<String> output = Collections.synchronizedSortedSet(new TreeSet<String>(XynaXMLShellPersistenceLayer.reverseOrder)); 
  private Set<String> error = Collections.synchronizedSortedSet(new TreeSet<String>(XynaXMLShellPersistenceLayer.reverseOrder)); 
  private CountDownLatch latch;
  
  private static Runtime runtime;
  
  static {
    runtime = Runtime.getRuntime();
  }
  
  /*
   * where looks like this: 'ColumnName Operator ?'
   * ColumnName -> [if operator =] <ColumnName>?</ColumnName>
   *               [if operator LIKE] <ColumnName>?</ColumnName> -> parse ? (replace % in ? with .*)
   *               [if operator <|>] <ColumnName>?</ColumnName> -> parse ? (replace ? with a regular expression describing all greater or smaller numbers)
   */
  public GrepCommand(String where) throws SQLException {
    grepPattern = new ArrayList<String>();
    operation = new ArrayList<OPERATION>();
    parse(where);
  }
  
  public GrepCommand(List<String> wheres) throws SQLException {
    grepPattern = new ArrayList<String>();
    operation = new ArrayList<OPERATION>();
    for (String string : wheres) {
      parse(string);
    }
    
  }
  
  private void parse(String where) throws SQLException {
    pattern = Pattern.compile(EQUALS);
    matcher = pattern.matcher(where);
    if (matcher.find()) {
      this.operation.add(OPERATION.EQUALS);
    } else {
      pattern = Pattern.compile(LIKE);
      matcher = pattern.matcher(where);
      if (matcher.find()) {
        this.operation.add(OPERATION.LIKE);
      } else {
        pattern = Pattern.compile(SMALLER);
        matcher = pattern.matcher(where);
        if (matcher.find()) {
          this.operation.add(OPERATION.SMALLER);
        } else {
          pattern = Pattern.compile(GREATER);
          matcher = pattern.matcher(where);
          if (matcher.find()) {
            this.operation.add(OPERATION.GREATER);
          } else {
            throw new SQLException("unknown operation found");
          }
        }
      }     
    }
            
    String columnName = where.substring(0, matcher.start());
    StringBuilder builder = new StringBuilder();
    builder.append("<");
    builder.append(columnName);
    builder.append(">?</");
    builder.append(columnName);
    builder.append(">");
    grepPattern.add(builder.toString());
  }
  
  /**
   * z.b.: find . -type f | xargs grep -F -l "&lt;name&gt;cl&lt;/name&gt;"
   */
  public void fillParameter(List<Object> params, String archiveDir, CountDownLatch latch) throws SQLException {
    this.latch = latch;
    this.archiveDir = archiveDir;
    
    if (params.size() != grepPattern.size()) {      
      throw new SQLException("wrong number of params");
    }

    StringBuilder builder = new StringBuilder();
    builder.append(PRECOMMANDS);
    builder.append(XynaXMLShellPersistenceLayer.getPathToGrep());
    if (params.size() > 1) {
      builder.append(" ");
      builder.append(EXTENDED_REGULAR_EXPRESSION);
    } else {
      switch (this.operation.get(0)) {
        case EQUALS :
          builder.append(" ");
          builder.append(FIXEDSTRING);
          break;
        case SMALLER :
        case GREATER :
          builder.append(" ");
          builder.append(EXTENDED_REGULAR_EXPRESSION);
          break;
        default :
          break;
      }        
    }
    builder.append(" ");
    builder.append(RESTRICT_TO_FILES);
    builder.append(" \"");
    
    for (int i = 0; i<params.size(); i++) {
      switch (this.operation.get(0)) {
        case EQUALS :
          builder.append(grepPattern.get(i).replace("?", SelectionParser.escapeParams(params.get(i).toString(), false, new EscapeForXMLShell())));
          break;
        case LIKE :
          builder.append(grepPattern.get(i).replace("?", SelectionParser.escapeParams(params.get(i).toString(), true, new EscapeForXMLShell())));
          break;
        case SMALLER :
          builder.append(grepPattern.get(i).replace("?", parseSmaller(params.get(i))));
          break;
        case GREATER :
          builder.append(grepPattern.get(i).replace("?", parseGreater(params.get(i))));
          break;
        default :
          break;
      }
      if (i+1 < params.size()) {
        builder.append("|");
      }
    }
    
    builder.append("\" ");
    this.grepCommand = builder.toString();
  }
  
  
  // this should look something like this for param = 123  
  // 12[0-2] | 1[0-1][0-9] | [0-9]{1,2}
  // for param = 76543
  // 7654[0-2] | 765[0-3][0-9] | 76[0-4][0-9]{2} | 7[0-5][0-9]{3} | [1-6][0,9]{4} | [0-9]{1,4} 
  // for params NMPR
  // NMP[0-(R-1)] | NM[0-(P-1)][0-9]{1} | N[0-(M-1)][0-9]{2} | [1-(N-1)][0-9]{3} | [0-9]{1,3}
  // falls eine stelle == 0 ist, entfällt die alternative, die (Stelle-1) rechnet
  // beispiel: 1000
  // [0-9]{1,3}
  private static String parseSmaller(Object param) throws SQLException {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    // we could care what number this actually is...for now...we don't^^
    String number = param.toString();
    //parse to ensure it's a number
    boolean first = true;
    if (number.startsWith("-")) {
      //TODO wieso das?
      number = number.substring(1);
    } else {
     /*TODO support für negative zahlen
       first = false;
      builder.append("-[0-9]*");*/
    }
    number = String.valueOf(Long.parseLong(number)); //führende nullen entfernen
    
    Pattern numberPat = Pattern.compile("^[0-9]+$");
    Matcher numberMat = numberPat.matcher(number);
    if (!numberMat.find()) {
      throw new SQLException("no number supplied as paramater for <");
    }
    
    int l = number.length();
    for (int i = 0; i<l; i++) {
      if (number.endsWith("0")) {
        number = number.substring(0, number.length()-1);
        continue;
      }
      
      if (first) {
        first = false;
      } else {
        builder.append("|");
      }
      
      int end = Integer.parseInt("" + number.charAt(number.length()-1));
      number = number.substring(0, number.length()-1);
      builder.append(number);
      if (end == 1) {
        if (i< l-1) {
          builder.append("0");
        }
      } else {
        builder.append("[");
        if (i == l-1) {
          builder.append("1");
        } else {
          builder.append("0");
        }
        builder.append("-").append(end - 1).append("]");
      }
      if (i > 0) {
        builder.append("[0-9]{").append(i).append("}");
      }
    }
    if (l > 1) {
      if (!first) {
        builder.append("|");
      }
      builder.append("[0-9]{1,").append(l-1).append("}");
    }

    builder.append(")");
    return builder.toString();
  }
 /* 
  public static void main(String[] args) throws SQLException {
    Object[] numbers = new Object[]{"0", 1512, "1000", "003", "004042", "5", "-54"};
    for (Object o : numbers) {
      System.out.println(o + " -> smaller " + parseSmaller(o));
      System.out.println(o + " -> greater " + parseGreater(o));
    }
  }*/
  
  // this should look something like this for param = 123
  // 12[4-9] | 1[3-9][0-9] | 2[0-9]{2,}| [1-9][0-9]{3,}
  // for param = 76543
  // 7654[4-9] | 765[5-9][0-9] | 76[6-9][0-9]{2,} | 7[7-9][0-9]{3,} | [8-9][0-9]{4,} | [1-9][0-9]{5,}
  // for params NNNN
  // NNN[(N+1)-9] | NN[(N+1)-9][0-9] | N[N+1-9][0-9]{2,} | [N+1-9][0-9]{3,} | [1-9][0-9]{4,}
  // 1939
  // 194[0-9] | 19[4-9][0-9]{1,} | 20[0-9]{2,} | [1-9][0-9]{3,} | [1-9][0-9]{4,}
  // wenn 9 ansteht dann nimm die ganze restliche Zahl +1 und häng nen weiteres [0-9] an
  // 9999 => 1000[0-9]|100[0-9]{1,}|10[0-9]{2,}|1[0-9]{3,}|[1-9][0-9]{4,}
  private static String parseGreater(Object param) throws SQLException {
    StringBuilder builder = new StringBuilder();
    builder.append("(");
    // we could care what number this actually is...for now...we don't^^
    String number = param.toString();
    //parse to ensure it's a number
    if (number.startsWith("-")) {
      number = number.substring(1);
    }
    number = String.valueOf(Long.parseLong(number)); //führende nullen entfernen
    Pattern numberPat = Pattern.compile("^[0-9]+$");
    Matcher numberMat = numberPat.matcher(number);
    if (!numberMat.find()) {
      throw new SQLException("no number supplied as paramater for <");
    }
    
    for (int i = 0; i < number.length(); i++) {
      if (i == 0) { //first conversion doesn't quite suit the conversion pattern
        if (number.endsWith("9")) {
          builder.append((Long.parseLong(number)+1)/10);
          builder.append("[0-9]");
        } else {
          builder.append(number.substring(0, number.length() -1));
          builder.append("[");
          builder.append(Integer.parseInt(number.substring(number.length() -1))+1);
          builder.append("-9]");
        }
        if (number.length() == 1) {
          builder.append("|");
          builder.append("[1-9][0-9]+");
        }
      } else {
        if (Integer.parseInt(number.substring((number.length()-1-i), (number.length()-i))) == 9) {
          builder.append((Long.parseLong(number.substring(0, number.length() -i))+1)/10);
          builder.append("[0-9]{");
          builder.append(i+1);
          builder.append(",}");
        } else {
          builder.append(number.substring(0, number.length() -1 -i));
          builder.append("[");
          builder.append(Integer.parseInt(number.substring(number.length() -1 -i, number.length() -1 -i +1))+1); // this should append the new last number reduced by 1
          builder.append("-9]");
          builder.append("[0-9]{");
          builder.append(i);
          builder.append(",}");
        }
        
        if (i == number.length()-1) {
          builder.append("|");
          builder.append("[1-9][0-9]{");
          builder.append(i+1);
          builder.append(",}");
        }
      }
      if (i < number.length()-1) {
        builder.append("|");
      } 
    }
    builder.append(")");
    return builder.toString();
  }
  
  
  public int getNumParams() {
    return grepPattern.size();
  }
  
  
  
  public void run() {
    try {     
      Process p = null;
      try {    
        if (logger.isDebugEnabled()) {
          logger.debug(new StringBuilder().append("building process with: ").append(SHELL).append(" ").append(SHELLPARAM).append(" ").append(grepCommand).toString());
          logger.debug(new StringBuilder().append("executing query in: ").append(archiveDir).toString());
        }
        p = runtime.exec(new String[]{SHELL, SHELLPARAM, grepCommand}, null, new File(archiveDir));        
      } catch (IOException e) {
        logger.error("Error setting grep working directory to " + archiveDir, e);
        error.add(e.getMessage());
        for (StackTraceElement ste : e.getStackTrace()) {
          error.add(ste.toString());
        }        
      }
      
      output.clear();
      error.clear();

      /*
       * The order in which these are read is important as long as we're not reading threaded
       * p.waitFor() won't ever return if the buffer runs full
       * the inputStream is more likely to run full because there could many orders in the orderArchive (happened with 12000 orders)
       * BEWARE: if there would ever be a reason for the grep to write several thousand lines into the error stream before finishing 
       *         the InputStream this thread will deadlock (System won't freeze cause of the timeout at the latch in the surrounding PreparedQuery)
       */
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(p.getInputStream()));
      String line = null;
      while ((line = bufferedReader.readLine()) != null)
      {
        //logger.debug("adding outputLine: " + line);
        output.add(line);
      }
      
      bufferedReader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
      line = null;
      while ((line = bufferedReader.readLine()) != null)
      { 
        error.add(line);
      }
      
      try {
        p.waitFor(); //we should not wait here, instead we should sleep and check a bool..if that has been set we destroy our process and run out
                     //but we would need to refactory the In- Outstreams back into seperate processes
      } catch (InterruptedException e) {
        logger.warn("Grep was interrupted while waiting", e);
        p.destroy();
        //error.add(e.getMessage());
        //for (StackTraceElement ste : e.getStackTrace()) {
        //  error.add(ste.toString());
        //} 
      }
      
 
    } catch (Throwable t) {
      error.add(t.getMessage());
      for (StackTraceElement ste : t.getStackTrace()) {
        error.add(ste.toString());
      } 
    } finally {
      latch.countDown();
    }
  }
  
  public Set<String> retrieveOutput() throws GrepException {
    if (error.size() > 0) {
      StringBuilder sb = new StringBuilder();
      for (String e : error) {
        sb.append(e);
        sb.append("\n");
      }
      throw new GrepException("Grep returned an error: \n" + sb.toString());
    }
    return output;
  }


  public static class EscapeForXMLShell implements EscapeParameters {

    public String escapeForLike(String toEscape) {
      return toEscape;
    }

    @Override
    public String getMultiCharacterWildcard() {
      return ".*";
    }

    @Override
    public String getSingleCharacterWildcard() {
      return ".";
    }
    
  }

}
