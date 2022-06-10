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
package com.gip.xyna.utils.exceptions.xmlstorage;



import java.io.InputStream;

import org.w3c.dom.Document;

import com.gip.xyna.utils.exceptions.utils.FileUtils;
import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.utils.exceptions.utils.FileUtils.ResourceNotFoundException;



public abstract class ExceptionStorageParserBase implements ExceptionStorageParser {

  protected Document doc;
  protected ExceptionStorageInstance instance;
  private boolean validated = false;


  public ExceptionStorageParserBase(Document doc) {
    this.doc = doc;
  }


  protected boolean isValidated() {
    return validated;
  }


  protected void setValidated(boolean validated) {
    this.validated = validated;
  }


  protected abstract ExceptionStorageInstance parseInternal(boolean resolveImports, int depth)
                  throws InvalidXMLException, XSDNotFoundException;


  public ExceptionStorageInstance parse(boolean resolveImports, int depth) throws InvalidXMLException,
                  XSDNotFoundException {
    if (instance != null) {
      return instance;
    }
    if (depth > 20) {
      throw new RuntimeException("circular dependency between xmls detected");
    }
    if (!isValidated()) {
      validateAgainstXSD();
    }
    instance = parseInternal(resolveImports, depth);
    doc = null;
    return instance;
  }


  protected abstract String getXSDFileName();


  private InputStream getInputStreamToXSD() throws ResourceNotFoundException  {
    return FileUtils.getInputStreamToResource(getXSDFileName());
  }


  public void validateAgainstXSD() throws InvalidXMLException, XSDNotFoundException {
    InputStream is = null;
    try {
      is = getInputStreamToXSD();
    } catch (ResourceNotFoundException e) {
      throw new XSDNotFoundException(getXSDFileName(), e);
    }
    XMLUtils.validateAgainstXSD(doc, is, getXSDFileName());
    validated = true;
  }

}
