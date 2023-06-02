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
package com.gip.xyna.utils.exceptions;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;

/**
 * Exception ohne konkrete Angabe der Message sondern nur des Codes. Die tatsächliche Fehler
 * Nachricht wird über den {@link ExceptionHandler} erzeugt. {@link #getMessage} ist überschrieben und
 * gibt die aus dem Code erzeugte Fehlernachricht zurück.
 */
public class XynaException extends Exception {

   private static final long serialVersionUID = 6647990825242390888L;
   private String[] args = null;
   private String code;
   private String message;
   
   /**
    * Erzeugt eine XynaException mit dem übergebenen Code
    * 
    * @param code
    */
   public XynaException(String code) {
     args = new String[0];
     this.code = code;
   }
   
   public XynaException(String code, Throwable cause) {
     this(code);
     initCause(cause);
   }
   
   protected void refreshArgs() {
   }

   /**
    * Erzeugt eine XynaException mit dem übergebenen Code und einem Argument
    * 
    * @param code
    * @param arg
    */
   public XynaException(String code, String arg) {
      this.code = code;
      args = new String[] { arg };
   }
   
   public XynaException(String code, String arg, Throwable cause) {
     this(code, arg);
     initCause(cause);
   }
   
   /**
   * 
   * @param codeAndArgs
   */
   public XynaException(String[] codeAndArgs) {
     if (codeAndArgs != null && codeAndArgs.length > 0) {
       this.code = codeAndArgs[0];
     }
     if (codeAndArgs != null && codeAndArgs.length > 1) {
       args = new String[codeAndArgs.length - 1];
       System.arraycopy(codeAndArgs, 1, args, 0, codeAndArgs.length - 1);
     }
   }
   
   public XynaException(String[] codeAndArgs, Throwable cause) {
     this(codeAndArgs);
     initCause(cause);
   }
   
   /**
    * erzeugt eine XynaException mit dem übergebenen Code und mehreren Argumenten
    * 
    * @param code
    * @param args
    */
   public XynaException(String code, String[] args) {
      this.code = code;
      this.args = args;
   }
   
   public XynaException(String code, String[] args, Throwable cause) {
     this(code, args);
     initCause(cause);
   }

   public String getCode() {
      return code;
   }

   public String[] getArgs() {
      return args;
   }   
   
   /**
   * initialisiert den Cause, und gibt die XynaException zurueck. kann nur einmal aufgerufen werden!
   * @see Throwable#initCause
   * @param t
   * @return
   */
   public XynaException initCause(Throwable t) {
     if (t == null) {
       //getCause gibt trotzdem noch null zurück. der einzige unterschied zum "normalen super.initCause(null)" ist, dass man initCause(null) mehrfach aufrufen kann.
       //vorteil: initCause(null) tut einfach nichts. deshalb kann der konstruktor XynaException(args, cause) immer aufgerufen werden, auch wenn der cause
       //ggfs null ist.
       return this;
     }
     return (XynaException) super.initCause(t);
  }


  /**
   * verwandelt die mehreren übergebenen exceptions in eine einzige exception, so dass im stacktrace klar wird, dass
   * mehrere exceptions vorlagen, die nicht voneinander abhängig gewesen waren.
   * <p>
   * kann nur einmal aufgerufen werden.
   */
  public XynaException initCauses(Throwable[] t) {
    if (t.length > 1) {
      return initCause(new ThrowableCollection(t));
    } else if (t.length == 1) {
       return initCause(t[0]);
     } else {
       return this;
     }
   }
   
   private static class ThrowableCollection extends Throwable {

    private static final long serialVersionUID = 4728635184668690057L;
    private Throwable[] causes = null;
    private String []causesSimpleClassNames = null;
    private String []messages = null;

    public ThrowableCollection(Throwable[] t) {
      List<StackTraceElement> stackTraces = new ArrayList<StackTraceElement>();
      int cnt = 1;
      for (Throwable cause : t) {
        addStackTraceElements(stackTraces, cause, 0, cnt, t.length);
        cnt++;
      }
      stackTraces.add(new StackTraceElement("----- This is the end of the multiple throwable", " ", t.length + " elements", -1));
      super.setStackTrace(stackTraces.toArray(new StackTraceElement[0]));
      if(t != null) {
        causesSimpleClassNames = new String[t.length];
        messages = new String[t.length];
        for(int i = 0; i < t.length; i++) {
          causesSimpleClassNames[i] = t[i].getClass().getSimpleName();
          messages[i] = t[i].getMessage();
        }        
      }
    }


    private static void addStackTraceElements(List<StackTraceElement> stackTraces, Throwable t, int depth, int throwableCnt, int throwableSize) {
      if (depth == 0) {
        stackTraces.add(new StackTraceElement("----- This is the " + throwableCnt, " stacktrace of " + throwableSize+ " ", t.getClass().getName() + " " + t.getMessage(), -1));
      } else {
        stackTraces.add(new StackTraceElement("----- Caused by " + t.getClass().getName() + " " + t.getMessage(), " ", "depth=" + depth, -1));
      }

      StackTraceElement[] causeStackTrace = new StackTraceElement[0];
      if (t.getCause() != null) {
        causeStackTrace = t.getCause().getStackTrace();
      }
      StackTraceElement[] stackTrace = t.getStackTrace();
      
      int m = stackTrace.length-1, n = causeStackTrace.length-1;      
      while (m >= 0 && n >=0 && stackTrace[m].equals(causeStackTrace[n])) {
          m--; n--;
      }
      int framesInCommon = stackTrace.length - 1 - m;
      
      for (int i = 0; i<=m; i++) {
        stackTraces.add(stackTrace[i]);
      }
      if (framesInCommon > 0) {
        stackTraces.add(new StackTraceElement("   ..", " ", framesInCommon + " more", -1));
      }
      if (t.getCause() != null) {
        addStackTraceElements(stackTraces, t.getCause(), depth + 1, throwableCnt, throwableSize);
      }
    }
    
    public String getMessage() {
      StringBuffer msg = new StringBuffer();
      msg.append("Multiple Causes:\n");
      for (int i = 0; i < causesSimpleClassNames.length; i++) { 
        msg.append(i+1).append(". ");
        msg.append("[").append(causesSimpleClassNames[i]).append("] ");
        msg.append(messages[i]);
        msg.append("\n");
      }
      return msg.toString();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {

      in.defaultReadObject();
      if(causesSimpleClassNames == null && causes != null) {
        causesSimpleClassNames = new String[causes.length];
        messages = new String[causes.length];
        for(int i = 0; i < causes.length; i++) {
          causesSimpleClassNames[i] = causes[i].getClass().getSimpleName();
          messages[i] = causes[i].getMessage();
        }     
      }
    }
    
  }
   
  public XynaFault_ctype toXynaFault() {
    return ExceptionHandler.toXynaFault(this, getRevision());
  }

  public XynaFault_ctype toXynaFault(String lang) {
    return ExceptionHandler.toXynaFault(this, lang, getRevision());
  }
  
  private void writeObject(java.io.ObjectOutputStream s) throws IOException {
    message = getMessage();
    s.defaultWriteObject();
  }

  public String getMessage() {
    refreshArgs();
    //erst versuchen mit aktueller default-sprache die fehlermeldung zu bauen. falls nicht möglich => rollback auf gecachte fehlermeldung.
    try {
      message = ExceptionHandler.getErrorMessage(code, args, getRevision());
    } catch (UnknownExceptionCodeException e) {
      //TODO konfigurierbar: cache/kein cache. wie ist das verhalten bei rmi? der client hat andere gecachte-codes?
      if (message != null) {
        return message;
      }
      try {
        //nicht einfach e.getMessage() aufrufen, weil das zu endlos-rekursion führen kann
        message = ExceptionHandler.getErrorMessage(e.getCode(), e.getArgs(), e.getRevision());
      } catch (UnknownExceptionCodeException f) {
        message = "Internal Error: Code " + e.getCode() + " not found. Caused by: Code " + code + " not found";
      }
    }
    return message;
  }

  public Long getRevision() {
    return -1L;
  }
  
  
  private static ConcurrentMap<String, Field> fieldMap = new ConcurrentHashMap<>();
  
  public static Field getField(String target_fieldname) {
    Field foundField = null;
    foundField = fieldMap.get(target_fieldname);
    if (foundField != null) {
      return foundField;
    }
    try {
      foundField = XynaException.class.getDeclaredField(target_fieldname);
    } catch (NoSuchFieldException e) {
    }
    if (foundField == null) {
      throw new IllegalArgumentException("Parameter '" + target_fieldname + "' not found");
    } else {
      foundField.setAccessible(true);
      fieldMap.put(target_fieldname, foundField);
      return foundField;
    }
  }
  
  
  //TODO besserer umgang mit dynamischer lokalisierung (für verschiedenartige fehlermeldungen im gleichen system)
  
}
