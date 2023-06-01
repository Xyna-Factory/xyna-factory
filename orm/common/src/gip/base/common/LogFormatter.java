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
package gip.base.common;


import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;


/** 
 * Dient der Formatierung von Logging-Messages.
 */
public class LogFormatter extends Formatter {
  
  private String _timeFormat;      // Format des Zeitstempels 
  private boolean _methodLogging;  // Soll Klasse & Methode mit ausgegebenen werden? (Performance!)
  private String _methodName;
  private String _fileName;
  private int _lineNumber;
  private boolean _logLevelInfo;
  
  
  /**
   * Default Konstruktor 
   * Enabled alle OBLog-Infos.
   */
  public LogFormatter() {
    this("yyyy-MM-dd HH:mm:ss", true, true);  //$NON-NLS-1$
  }

  
  /**
   * @param timeFormat Java-uebliches Zeitformat, oder aber 'off' um es auszuschalten.
   * @param enableLogLevelInfo info
   * @param enableMethodLogging method 
   */
  public LogFormatter(String timeFormat, boolean enableLogLevelInfo, boolean enableMethodLogging) {
    _logLevelInfo=enableLogLevelInfo;
    _timeFormat = timeFormat;
    _methodLogging = enableMethodLogging;
  }
  
  
  /**
   * Formatiert eine OBLog-Message.
   * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
   */
  public String format(LogRecord record) {
    StringBuffer sb = new StringBuffer(100);
    
    if (!_timeFormat.equalsIgnoreCase("off")) { //$NON-NLS-1$
      sb.append(getTimestamp(record.getMillis(), _timeFormat)).append(" ");  //$NON-NLS-1$
    }

    if (_logLevelInfo==true) {
      sb.append(rPad(record.getLevel().getName(), 7) ).append(" "); //$NON-NLS-1$ // WARNING hat 7 Zeichen, deswegen auf 7 Zeichen expandieren.
    }
    
    if (_methodLogging) {
      inferCaller(); 
      sb.append("[").append(_fileName); //$NON-NLS-1$
      sb.append(":").append(rPad(""+_lineNumber, 4)); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append("] ").append(_methodName).append("()"); //$NON-NLS-1$ //$NON-NLS-2$
      sb.append(" : "); //$NON-NLS-1$
    }
    
    sb.append(record.getMessage()).append("\n"); //$NON-NLS-1$
    return sb.toString();
  }

  
  /**
   * Diese Methode stellt den Klassenname, Methodenname, Filename und Zeilennummer zur Verfuegung.
   * Aus LogRecord kopiert und modifiziert.
   */
  private void inferCaller() {
    // Get the stack trace.
    StackTraceElement stack[] = (new Throwable()).getStackTrace();
    // First, search back to a method in the Logger class.
    int ix = 0;
    while (ix < stack.length) {
      StackTraceElement frame = stack[ix];
      String cname = frame.getClassName();
      if (cname.equals("gip.base.common.OBLog") || //$NON-NLS-1$
          cname.equals("java.util.logging.Logger")) {  //$NON-NLS-1$
        break;
      }
      ix++;
    }
    // Now search for the first frame before the "Logger" class.
    while (ix < stack.length) {
      StackTraceElement frame = stack[ix];
      String cname = frame.getClassName();
      if (!cname.equals("gip.base.common.OBLog") &&  //$NON-NLS-1$
          !cname.equals("java.util.logging.Logger")) { //$NON-NLS-1$
        // We've found the relevant frame, set the members:
        setSourceMethodName(frame.getMethodName());
        setSourceFileName(frame.getFileName());
        setSourceFileLineNumber(frame.getLineNumber());
        return;
      }
      ix++;
    }
    // We haven't found a suitable frame, so just punt. This is
    // OK as we are only commited to making a "best effort" here.
  }

  
  /**
   * @param lineNumber Zeilennummer
   */
  private void setSourceFileLineNumber(int lineNumber) {
    _lineNumber=lineNumber;
    
  }


  /**
   * @param fileName Name der Datei
   */
  private void setSourceFileName(String fileName) {
    _fileName=fileName;
  }


  /**
   * @param methodName
   */
  private void setSourceMethodName(String methodName) {
    _methodName=methodName;
  }



  /**
   * Formatiert den Millisekunden Zeitstempel im angegebenen Format
   * @param millis Zeitstempel
   * @param format Format
   * @return formatierter Zeitstempel
   */
  protected String getTimestamp(long millis, String format) {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    return sdf.format(new Date(millis), new StringBuffer(), new FieldPosition(0)).toString();
  }

  
  
  
  /**
   * 
   * @return Returns the _methodLogging.
   */
  public boolean isMethodLogging() {
    return _methodLogging;
  }

  
  /**
   * @param enableMethodLogging The _methodLogging to set.
   */
  public void setMethodLogging(boolean enableMethodLogging) {
    _methodLogging = enableMethodLogging;
  }
  
  
  /**
   * @return Returns the _timeFormat.
   */
  public String getTimeFormat() {
    return _timeFormat;
  }

  
  /**
   * @param timeFormat The _timeFormat to set for OBLog-Output. Set it to 'off' to disable.
   */
  public void setTimeFormat(String timeFormat) {
    _timeFormat = timeFormat;
  }
  
  
  /**
   * Expandiert einen String auf der rechten Seite mit Leerzeichen bis zur Groesse size.
   * @param str der Zu paddende String
   * @param size Gesamtlaenge
   * @return
   */
  private StringBuffer rPad(String str, int size) {
    if (str.length()>=size) {
      return new StringBuffer(str);
    } 
    else {
      StringBuffer sb=new StringBuffer(size);
      sb.append(str);
      for (int i=str.length(); i<size; i++) {
        sb.append(" "); //$NON-NLS-1$
      }
      return sb;
    }
  }

  
  /**
   * Expandiert einen String auf der linken Seite mit Leerzeichen bis zur Groesse size.
   * @param str der zu paddende String
   * @param size Gesamtlaenge
   * @return Expandierter StringBuffer
   */
  public StringBuffer lPad(String str, int size) {
    if (str.length()>=size) {
      return new StringBuffer(str);
    } 
    else {
      StringBuffer sb=new StringBuffer(size);
      for (int i=str.length(); i<size; i++) {
        sb.append(" "); //$NON-NLS-1$
      }
      sb.append(str);
      return sb;
    }
  }

}


