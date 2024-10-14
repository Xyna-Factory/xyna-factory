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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.validation;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class ErrorHandlerImpl implements ErrorHandler {

  private List<SAXParseException> warning;
  private List<SAXParseException> error;
  private List<SAXParseException> fatalError;
  private boolean errorsThisRun;
  
  ErrorHandlerImpl() {
    warning = new ArrayList<SAXParseException>();
    error = new ArrayList<SAXParseException>();
    fatalError = new ArrayList<SAXParseException>();
  }
  
  public void error(SAXParseException e) throws SAXException {
    errorsThisRun = true;
    error.add(e);
  }


  public void fatalError(SAXParseException e) throws SAXException {
    errorsThisRun = true;
    fatalError.add(e);
  }


  public void warning(SAXParseException e) throws SAXException {
    warning.add(e);
  }
  
  void nextRun() {
    errorsThisRun = false;
  }
  
  boolean accepted() {
    return !errorsThisRun;
  }
  
  
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Warnings:\n");
    for (SAXParseException e : warning) {
      sb.append(e.getMessage())
        .append("\n");
    }
    sb.append("Errors:\n");
    for (SAXParseException e : error) {
      sb.append(e.getMessage())
      .append("\n");
    }
    sb.append("FatalErrors:\n");
    for (SAXParseException e : fatalError) {
      sb.append(e.getMessage())
      .append("\n");
    }
    return sb.toString();
  }

  void throwMostSevereError() throws SAXParseException {
    if (fatalError.size() > 0) {
      throw fatalError.get(0);
    }
    if (error.size() > 0) {
      throw error.get(0);
    }
  }
  

}
