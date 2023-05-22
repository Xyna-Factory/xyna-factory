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
package com.gip.xyna.utils.db.utils;

public class RepeatedExceptionCheck {

  private int repeationCount = 0;
  private Throwable lastThrowable;
  private boolean checkStack;
  
  public RepeatedExceptionCheck() {
    this.checkStack = false;
  }
  public RepeatedExceptionCheck(boolean checkStack) {
    this.checkStack = checkStack;
  }

  
  public synchronized int checkRepeationCount(Throwable t) {
    boolean repeated = exceptionsEquals( t, lastThrowable, checkStack );
    if( repeated ) {
      ++repeationCount;
    } else {
      lastThrowable = t;
      repeationCount = 0;
    }
    return repeationCount;
  }
  
  public synchronized boolean checkRepeated(Throwable t) {
    return checkRepeationCount(t) > 0;
  }

  public synchronized int getRepeationCount() {
    return repeationCount;
  }

  public Throwable getLastThrowable() {
    return lastThrowable;
  }
  
  public synchronized void clear() {
    repeationCount = 0;
    lastThrowable = null;
  }

  @Override
  public synchronized String toString() {
    if( repeationCount == 0 ) {
      if( lastThrowable == null ) {
        return "No exception occured at the last check";
      } else {
        return "Last exception with message +\""+lastThrowable.getMessage()+"\"";
      }
    } else {
      return "Last exception repeated for the "+repeationCountString()+" time, message is \""+lastThrowable.getMessage()+"\""; 
    }
  }
  
  private String repeationCountString() {
    switch( repeationCount ) {
      case 0: return "zero";
      case 1: return "1st";
      case 2: return "2nd";
      case 3: return "3rd";
      default: return String.valueOf(repeationCount)+"th";
    }
  }
  
  
  public static boolean exceptionsEquals(Throwable t1, Throwable t2, boolean checkStack) {
    //Throwable direkt vergleichen, null == null erlaubt
    if( t1 == null && t2 == null ) {
      return true;
    } else if( t1 == null || t2 == null ) {
      return false;
    }
    //Messages direkt vergleichen, null == null erlaubt
    if( ! stringsEquals( t1.getMessage(), t2.getMessage() ) ) {
      return false; //Messages ungleich
    }
    //nun Stacktrace vergleichen
    if( checkStack && ! stackTraceEquals(t1.getStackTrace(),t2.getStackTrace() ) ) {
      return false;
    }
    //Exceptions sind gleich, daher Causes vergleichen
    return exceptionsEquals( t1.getCause(), t2.getCause(), checkStack );
  }
  
  public static boolean stringsEquals( String s1, String s2 ) {
    if( s1 == null && s2 == null ) {
      return true;
    } else if( s1 == null || s2 == null ) {
      return false;
    } else {
      return s1.equals(s2);
    }
  }

  public static boolean stackTraceEquals( StackTraceElement[] ste1, StackTraceElement[] ste2 ) {
    if( ste1 == null && ste2 == null ) {
      return true;
    } else if( ste1 != null && ste2 == null ) {
      return false; //StackTraces ungleich
    } else if( ste1.length != ste2.length ) {
      return false; //StackTraces ungleich
    } else {
      //zeilenweises �berpr�fen des StackTraces
      for( int i=0; i<ste1.length; ++i ) {
        if( ! ste1[i].equals(ste2[i]) ) {
          return false; //StackTraces ungleich
        }
      }
    }
    return true;
  }
  
}
