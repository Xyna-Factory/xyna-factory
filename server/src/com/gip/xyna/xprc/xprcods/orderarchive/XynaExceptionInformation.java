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

package com.gip.xyna.xprc.xprcods.orderarchive;



import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaRuntimeException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class XynaExceptionInformation implements Serializable {

  private static final long serialVersionUID = -4386659196250925705L;

  private final StackTraceElement[] stacktraceLines;
  private final String code;
  private final String message;
  private final boolean causedByRuntimeException;
  private final boolean causedByError;
  private final String xml;

  
  public XynaExceptionInformation(String message, StackTraceElement[] stacktraceLines, String code, boolean causedByRuntimeException, boolean causedByError, String xml) {
    this.message = message;
    this.stacktraceLines = stacktraceLines;
    this.code = code;
    this.causedByRuntimeException = causedByRuntimeException;
    this.causedByError = causedByError;
    if (xml == null) {
      this.xml = getEmptyXML(GenerationBase.CORE_EXCEPTION); //FIXME
    } else {
      this.xml = xml;
    }
  }
  
  public XynaExceptionInformation(Throwable e, long version, GeneralXynaObject.XMLReferenceCache cache) {
    message = e.getClass().getName() + ": " + e.getMessage();

    ArrayList<StackTraceElement> stacktraceElementsAsList = new ArrayList<StackTraceElement>();
    addStackTraceElements(stacktraceElementsAsList, e, 0);
    stacktraceLines = stacktraceElementsAsList.toArray(new StackTraceElement[0]);

    String tempXml = null;
    // get the simple strings
    if (e instanceof XynaException) {
      code = ((XynaException) e).getCode();
      if (e instanceof GeneralXynaObject) {
        tempXml = ((GeneralXynaObject) e).toXml(null, false, version, cache);
      }
    } else {
      code = null;
    }
    if (tempXml == null) {
      tempXml = getEmptyXML(e);
    }

    // find out if the exception was caused by an uncaught runtime exception (uncaught in the sense that is was not
    // handled within a process or service
    causedByRuntimeException = (e.getCause() != null && (e.getCause() instanceof XynaRuntimeException))
                    || (e instanceof RuntimeException);
    if (!causedByRuntimeException) {
      causedByError = e instanceof Error;
    } else {
      causedByError = false;
    }

    xml = tempXml;
  }

  public XynaExceptionInformation(Throwable e) {
    this(e, -1, null);
  }

  public static String getEmptyXML(Throwable t) {    
    String fqException = null;
    if (t instanceof XynaException) {
      fqException = GenerationBase.CORE_XYNAEXCEPTION;
    } else {
      fqException = GenerationBase.CORE_EXCEPTION;
    }
    return getEmptyXML(fqException);
  }
  
  private static String getEmptyXML(String fqException) {
    StringBuilder sb = new StringBuilder();
    String typepath = GenerationBase.getPackageNameFromFQName(fqException);
    String typename = GenerationBase.getSimpleNameFromFQName(fqException);
    sb.append("<").append(GenerationBase.EL.EXCEPTION).append(" ").append(GenerationBase.ATT.REFERENCENAME)
                    .append("=\"").append(typename).append("\" ").append(GenerationBase.ATT.REFERENCEPATH)
                    .append("=\"").append(typepath).append("\"").append(" />");
    return sb.toString();
  }

  public String getCode() {
    return code;
  }


  public String getMessage() {
    return message;
  }


  public StackTraceElement[] getStacktrace() {
    return stacktraceLines;
  }


  public boolean isCausedByRuntimeException() {
    return causedByRuntimeException;
  }


  public boolean isCausedByError() {
    return causedByError;
  }


  public String getXml() {
    return xml;
  }


  private static void addStackTraceElements(List<StackTraceElement> stackTraces, Throwable t, int depth) {

    if (depth > 0) {
      stackTraces.add(new StackTraceElement("----- Caused by " + t.getClass().getName() + " " + t.getMessage(), " ", "depth: " + depth, -1));
    }

    StackTraceElement[] causeStackTrace = new StackTraceElement[0];
    if (t.getCause() != null) {
      causeStackTrace = t.getCause().getStackTrace();
    }
    StackTraceElement[] stackTrace = t.getStackTrace();

    int m = stackTrace.length - 1, n = causeStackTrace.length - 1;
    while (m >= 0 && n >= 0 && stackTrace[m].equals(causeStackTrace[n])) {
      m--;
      n--;
    }
    int framesInCommon = stackTrace.length - 1 - m;

    for (int i = 0; i <= m; i++) {
      stackTraces.add(stackTrace[i]);
    }
    if (framesInCommon > 0) {
      stackTraces.add(new StackTraceElement("   ..", " ", framesInCommon + " more", -1));
    }
    if (t.getCause() != null) {
      addStackTraceElements(stackTraces, t.getCause(), depth + 1);
    }
  }

  
  @Override
  public String toString() {
    //wird von den automatisierten XTF-Tests verwendet
    StringBuilder sb = new StringBuilder("XynaExceptionInformation(");
    sb.append(message);
    if( stacktraceLines != null ) {
      for( StackTraceElement ste : stacktraceLines ) {
        if( ste.getClassName() != null && ste.getClassName().startsWith( "----- Caused by " ) ) {
          sb.append(",").append(ste.getClassName());
        }
      }
    }
    sb.append(")");
    return sb.toString();
  }
  
}
