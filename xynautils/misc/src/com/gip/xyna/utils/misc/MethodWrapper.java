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

import com.gip.xyna._1_5.xsd.faults._1.XynaFault_ctype;
import com.gip.xyna.utils.exceptions.ExceptionHandler;

import java.rmi.RemoteException;

import org.apache.log4j.Logger;


/**
 * basis wrapper. logt methodenbeginn, -ende und fehler. kapselt exceptionhandling.
 * gedacht f�r webservices, so dass man sich auf die implementierung der methode
 * konzentrieren kann und in jeder methode sich wiederholende fehlerbehandlung etc
 * sparen kann.
 * service-/projektspezifische sich wiederholende dinge sollten in einer von
 * methodwrapper abgeleiteten klasse geschehen (vgl. MethodWithSql)<p>
 * f�r beispiele kann man sich webservices aus dem factorymanagement anschauen.
 */
public abstract class MethodWrapper<T, U> {

  private String methodName;
  private Logger logger;

  public MethodWrapper(String methodName, Logger logger) {
    this.methodName = methodName;
    this.logger = logger;
  }

  public abstract U doStuff(T req) throws Exception;

  /**
   * geschieht direkt zu beginn von execute.
   */
  public void preface() {
    logger.debug("entered " + methodName + ".");
  }

  /**
   * geschieht innerhalb des try catch blocks
   * @param req
   * @return
   * @throws Exception
   */
  public U tryBlock(T req) throws Exception {
    return doStuff(req);
  }

  /**
   * wird ausgef�hrt, falls ein fehler in tryBlock passiert
   * @param e
   * @return
   * @throws XynaFault_ctype
   * @throws RemoteException
   */
  public U onError(Exception e) throws XynaFault_ctype, RemoteException {
    logger.error("Error in " + methodName + ":", e);    
    ExceptionHandler.handleException(e);
    return null; //hier kommt man nicht vorbei
  }

  /**
   * wird als finally ausgef�hrt
   */
  public void finallyBlock(U ret) {
    logger.debug("exiting " + methodName + ".");
  }

  /**
   * f�hrt die methode doStuff innerhalb eines try catch blocks aus.
   * <code>
   * preface();<br>
    U ret = null;<br>
    try {<br>
      ret = tryBlock(req);<br>
    } catch (Exception e) {<br>
      ret = onError(e);<br>
    } finally {<br>
      finallyBlock();<br>
    }<br>
    return ret;
   * </code>
   * @param req
   * @return
   * @throws XynaFault_ctype
   * @throws RemoteException
   */
  public U execute(T req) throws XynaFault_ctype, RemoteException {
    preface();
    U ret = null;
    try {
      ret = tryBlock(req);
    } catch (Exception e) {
      ret = onError(e);
    } finally {
      finallyBlock(ret);
    }
    return ret;
  }

  public String getMethodName() {
    return methodName;
  }

  public Logger getLogger() {
    return logger;
  }
}
