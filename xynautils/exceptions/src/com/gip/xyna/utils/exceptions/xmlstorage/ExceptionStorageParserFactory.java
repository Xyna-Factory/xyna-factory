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
package com.gip.xyna.utils.exceptions.xmlstorage;



import java.io.FileNotFoundException;

import org.w3c.dom.Document;

import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;



public class ExceptionStorageParserFactory {

  public static ExceptionStorageParser getParser(String xmlFile) throws InvalidXMLException, XSDNotFoundException,
                  FileNotFoundException {
    Document doc = XMLUtils.getDocumentFromFile(xmlFile);
    return getParser(doc);
  }


  public static ExceptionStorageParser getParser(Document doc) throws InvalidXMLException, XSDNotFoundException {
    ExceptionStorageParser parser = new ExceptionStorageParser_1_1(doc);
    try {
      parser.validateAgainstXSD();
    } catch (RootElementNotFoundException e) {
      parser = new ExceptionStorageParser_1_0(doc);
      try {
        parser.validateAgainstXSD();
      } catch (RootElementNotFoundException f) {
        throw f;
      }
    }
    return parser;
  }

}
