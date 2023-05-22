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
package com.gip.xyna.xmcp;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.w3c.dom.Element;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xmcp.exceptions.XMCP_RMIExceptionWrapper;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmlBuilder;


public class ErroneousOrderExecutionResponse extends OrderExecutionResponse {

  private static final Logger logger = CentralFactoryLogging.getLogger(ErroneousOrderExecutionResponse.class);
  private static final long serialVersionUID = -1890514175749065783L;
  
  private final SerializableExceptionInformation exception;
  private Long orderId;
  
  public ErroneousOrderExecutionResponse(SerializableExceptionInformation exception) {
    this.exception = exception;
  }
  
  
  public ErroneousOrderExecutionResponse(Throwable t, ResultController controller) {
    this(generateSerializableExceptionInformation(t, controller));    
  }
  
  
  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }
  
  
  public Long getOrderId() {
    return orderId;
  }
  
  @Override
  public boolean hasExecutedSuccesfully() {
    return false;
  }
  
  
  public SerializableExceptionInformation getExceptionInformation() {
    return exception;
  }

  public static SerializableExceptionInformation generateSerializableExceptionInformation(Throwable t, ResultController controller) {
    Throwable currentThrowable = t;
    SerializableExceptionInformation conversionRoot = null;
    SerializableExceptionInformation currentParent = null;
    while (currentThrowable != null) {
      Throwable cause = currentThrowable.getCause();      
      SerializableExceptionInformation current = new SerializableExceptionInformation(currentThrowable);
      
      if (currentThrowable instanceof XynaExceptionBase && controller.isSupported(currentThrowable.getClass(), WrappingType.XML)) {
        current.xml = ((XynaExceptionBase) currentThrowable).toXml(null, false, -1, null);
      }
      if (controller.isSupported(currentThrowable.getClass(), WrappingType.ORIGINAL)) {
        current.throwable = currentThrowable;
        setCauseToNull(currentThrowable);
      } 
      //else SIMPLE: ntbd
      
      if (conversionRoot == null) {
        conversionRoot = current;
      } else {
        currentParent.setCause(current);
      }
      currentParent = current;
      if (cause == null ||
          currentThrowable == cause) {
        break;
      } else {
        currentThrowable = cause;
      }
    }
    
    return conversionRoot;
  }
  
  
  private static Field causeField;
  private static Field detailFieldOfRemoteException;
  static {
    try {
      causeField = Throwable.class.getDeclaredField("cause");
      causeField.setAccessible(true);
      detailFieldOfRemoteException = RemoteException.class.getDeclaredField("detail");
      detailFieldOfRemoteException.setAccessible(true);
    } catch (SecurityException e) {
      logger.error("could not get cause field of " + Throwable.class.getName(), e);
    } catch (NoSuchFieldException e) {
      logger.error("could not get cause field of " + Throwable.class.getName(), e);
    }
  }
  
  
  private static void setCauseToNull(Throwable currentThrowable) {
    if (currentThrowable.getCause() == null) {
      return;
    }
    if (causeField == null) {
      logger.warn("could not null cause of throwable");
      return;
    }
    try {
      //TODO gibt es noch andere spezialexceptions, die getCause() �berschrieben haben oder getMessage?
      //     das f�hrt bei der deserialisierung dann evtl auch zu problemen...
      if (currentThrowable instanceof RemoteException) {
        detailFieldOfRemoteException.set(currentThrowable, null); //nicht auf currentthrowable setzen, das f�hrt zu stackoverflow
      }
      causeField.set(currentThrowable, currentThrowable);
    } catch (IllegalArgumentException e) {
      logger.warn("could not null cause of throwable", e);
    } catch (IllegalAccessException e) {
      logger.warn("could not null cause of throwable", e);
    }
  }


  public static class SerializableExceptionInformation implements Serializable {
    

    private static final long serialVersionUID = 6634549213481895941L;
    
    private SerializableExceptionInformation cause;
    private StackTraceElement[] stackTraceElements;
    private String message;
    private String className;
    private String xml;
    private Throwable throwable;
    private transient Throwable localThrowable;
    private String[] xynaExceptionArgs;
    
    public SerializableExceptionInformation(Throwable t) {
      stackTraceElements = t.getStackTrace();
      message = t.getMessage();
      className = t.getClass().getName();
      localThrowable = t;
      if (t instanceof XynaException) {
        xynaExceptionArgs = ((XynaException) t).getArgs();
      }
    }
    
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((cause == null) ? 0 : cause.hashCode());
      result = prime * result + ((className == null) ? 0 : className.hashCode());
      result = prime * result + ((message == null) ? 0 : message.hashCode());
      result = prime * result + Arrays.hashCode(stackTraceElements);
      return result;
    }


    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      SerializableExceptionInformation other = (SerializableExceptionInformation) obj;
      if (cause == null) {
        if (other.cause != null)
          return false;
      } else if (!cause.equals(other.cause))
        return false;
      if (className == null) {
        if (other.className != null)
          return false;
      } else if (!className.equals(other.className))
        return false;
      if (message == null) {
        if (other.message != null)
          return false;
      } else if (!message.equals(other.message))
        return false;
      if (!Arrays.equals(stackTraceElements, other.stackTraceElements))
        return false;
      return true;
    }


    public SerializableExceptionInformation() {
    }


    public SerializableExceptionInformation getCause() {
      return cause;
    }
    
    
    public void setCause(SerializableExceptionInformation cause) {
      this.cause = cause;
    }

    
    public StackTraceElement[] getStackTraceElements() {
      return stackTraceElements;
    }

    
    public void setStackTraceElements(StackTraceElement[] stackTraceElements) {
      this.stackTraceElements = stackTraceElements;
    }

    
    public String getMessage() {
      return message;
    }

    
    public void setMessage(String message) {
      this.message = message;
    }


    public String getClassName() {
      return className;
    }


    public void setClassName(String className) {
      this.className = className;
    }


    public String getXml() {
      return xml;
    }
    
    public static final String XML_ROOT = "Ex";
    
    public String toXml() {
      XmlBuilder xb = new XmlBuilder();
      toXml(xb, XML_ROOT);
      return xb.toString();
    }

    private static final String XML_CNAME = "CName";
    private static final String XML_MSG = "Msg";
    private static final String XML_XML = "XML";
    private static final String XML_STE = "STE";
    private static final String XML_STE_FILE = "F";
    private static final String XML_STE_CLASS = "C";
    private static final String XML_STE_METHOD = "M";
    private static final String XML_STE_LINE = "L";
    private static final String XML_ARG = "Arg";
    private static final String XML_CAUSE = "Cause";


    private void toXml(XmlBuilder xb, String name) {
      xb.startElementWithAttributes(name);
      xb.addAttribute(XML_CNAME, className);
      xb.endAttributes();
      xb.element(XML_MSG, message == null ? "" : escape(message));
      xb.element(XML_XML, xml == null ? "" : escape(xml));
      //stacktrace
      for (StackTraceElement e : stackTraceElements) {
        xb.startElementWithAttributes(XML_STE);
        xb.addAttribute(XML_STE_FILE, e.getFileName());
        xb.addAttribute(XML_STE_CLASS, e.getClassName());
        xb.addAttribute(XML_STE_METHOD, e.getMethodName());
        xb.addAttribute(XML_STE_LINE, "" + e.getLineNumber());
        xb.endAttributesAndElement();
      }
      //args
      if (xynaExceptionArgs != null) {
        for (String arg : xynaExceptionArgs) {
          xb.element(XML_ARG, arg == null ? "" : escape(arg));
        }
      }
      if (cause != null) {
        cause.toXml(xb, XML_CAUSE);
      }
      xb.endElement(name);
    }


    private String escape(String v) {
      return XMLUtils.replaceControlAndInvalidChars(XMLUtils.escapeXMLValue((v)));
    }


    public static SerializableExceptionInformation fromXml(Element exceptionElement) {
      SerializableExceptionInformation ret = new SerializableExceptionInformation();
      ret.className = exceptionElement.getAttribute(XML_CNAME);
      ret.xml = XMLUtils.getTextContent(XMLUtils.getChildElementByName(exceptionElement, XML_XML));
      ret.message = XMLUtils.getTextContent(XMLUtils.getChildElementByName(exceptionElement, XML_MSG));
      List<Element> stes = XMLUtils.getChildElementsByName(exceptionElement, XML_STE);
      List<StackTraceElement> stesList = new ArrayList<>(stes.size());
      for (Element ste : stes) {
        int l;
        try {
          l = Integer.valueOf(ste.getAttribute(XML_STE_LINE));
        } catch (NumberFormatException e) {
          l = -1;
        }
        stesList.add(new StackTraceElement(ste.getAttribute(XML_STE_CLASS), ste.getAttribute(XML_STE_METHOD),
                                           ste.getAttribute(XML_STE_FILE), l));
      }
      ret.stackTraceElements = stesList.toArray(new StackTraceElement[0]);
      List<Element> args = XMLUtils.getChildElementsByName(exceptionElement, XML_ARG);
      ret.xynaExceptionArgs = new String[args.size()];
      for (int i = 0; i < args.size(); i++) {
        ret.xynaExceptionArgs[i] = XMLUtils.getTextContent(args.get(i));
      }
      Element cause = XMLUtils.getChildElementByName(exceptionElement, XML_CAUSE);
      if (cause != null) {
        ret.cause = fromXml(cause);
      }
      return ret;
    }


    public Throwable getThrowable() {
      return throwable;
    }


    /**
     * Versucht, die urspr�ngliche Exception zu rekonstruieren. 
     * Dazu sollte diese als Throwable oder als XML vorliegen, ansonsten wird ein 
     * Surrogat XMCP_RMIExceptionWrapper gebaut.
     * @return
     */
    public Throwable recreateThrowable(long revision) {
      if( localThrowable != null ) {
        return localThrowable;
      }
      if( throwable != null ) {
        return throwable;
      }
      Throwable t = null;
      if( xml != null ) {
        GeneralXynaObject gxo;
        try {
          gxo = XynaObject.generalFromXml(xml, revision);
          t = (XynaExceptionBase)gxo;
        } catch (XPRC_XmlParsingException e) {
        } catch (XPRC_InvalidXMLForObjectCreationException e) {
        } catch (XPRC_MDMObjectCreationException e) {
        }
      } 
      if (t == null && xynaExceptionArgs != null) {
        //mit appclassloader versuchen wiederherzustellen
        try {
          Class<?> c = Class.forName(className);
          if (XynaException.class.isAssignableFrom(c)) {
            t = tryCreatePerReflection(c);
          }
        } catch (ClassNotFoundException e) {
          //ok wir wollen ja nur die vom appclassloader wieder herstellen
        }
      }
      if( t == null ) {
        t = new XMCP_RMIExceptionWrapper(className+": "+message);
      }
      t.setStackTrace(stackTraceElements);
      
      if( cause != null ) {
        t.initCause(cause.recreateThrowable(revision));
      }
      
      localThrowable = t;
      return localThrowable;
    }


    private Throwable tryCreatePerReflection(Class<?> c) {
      /*
       * typischerweise gibt es 2 oder 3 konstruktoren.
       * 1) leer
       * 2) mit parametern
       * 3) mit parametern und throwable
       * 
       * der mittlere entf�llt, wenn es keine parameter gibt
       */
      try {
     
        if (xynaExceptionArgs == null || xynaExceptionArgs.length == 0) {
          //leeren kontruktor verwenden
          return (Throwable) c.getConstructor().newInstance();
        } else {
          Constructor<?>[] constrs = c.getConstructors();
          Constructor<?> constr = null;
          for (Constructor<?> co : constrs) {
            if (co.getParameterTypes().length == 0) {
              continue;
            }
            if (co.getParameterTypes()[co.getParameterTypes().length - 1] == Throwable.class) {
              continue;
            }
            if (co.getParameterTypes().length != xynaExceptionArgs.length) {
              continue;
            }
            constr = co;
            break;
          }
          if (constr == null) {
            return null;
          }
          Object[] params = new Object[xynaExceptionArgs.length];
          for (int i = 0; i<params.length; i++) {
            params[i] = convert(xynaExceptionArgs[i], constr.getParameterTypes()[i]);
          }
          return (Throwable) constr.newInstance(params);
        }
      } catch (Exception e) {
        //ignore
        return null;
      }
    }


    private Object convert(String stringRepOfValue, Class<?> clazz) {
      if (stringRepOfValue == null) {
        return null;
      }
      if (clazz == String.class) {
        return stringRepOfValue;
      } else if (clazz == Integer.class || clazz == int.class) {
        return Integer.valueOf(stringRepOfValue);
      } else if (clazz == Double.class || clazz == double.class) {
        return Double.valueOf(stringRepOfValue);
      } else if (clazz == Float.class || clazz == float.class) {
        return Float.valueOf(stringRepOfValue);
      } else if (clazz == Long.class || clazz == long.class) {
        return Long.valueOf(stringRepOfValue);
      }
      return null;
    }

  }

}
