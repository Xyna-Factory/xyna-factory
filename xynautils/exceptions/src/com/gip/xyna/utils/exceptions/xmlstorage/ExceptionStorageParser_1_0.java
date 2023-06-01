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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.utils.exceptions.utils.codegen.InvalidParameterNameException;


public class ExceptionStorageParser_1_0 extends ExceptionStorageParserBase {

  private static final String XSD_FILENAME = "MessageStorage.xsd";

  private interface XML_1_0 {
    public static final String NS_STORAGE = "http://www.gip.com/xyna/1.5/utils/message/storage/1.0";
    public static final String ELEMENT_IMPORT = "Import";
    public static final String ATTRIBUTE_IMPORT_FILE = "File";
    public static final String ELEMENT_MESSAGE = "Message";
    public static final String ATTRIBUTE_MESSAGE_CODE = "Code";
    public static final String ATTRIUBTE_MESSAGE_PARAMCNT = "NoOfParameters";
    public static final String ELEMENT_MESSAGETEXT = "MessageText";
    public static final String ATTRIBUTE_MESSAGETEXT_LANGUAGE = "Language";
    public static final String ELEMENT_MESSAGESTORE = "MessageStore";
    public static final String ATTRIBUTE_MESSAGESTORE_NAME = "Name";
    //private static final String ATTRIBUTE_MESSAGESTORE_VERSION = "Version";
    //private static final String ATTRIBUTE_MESSAGESTORE_TYPE = "Type";
    public static final String ATTRIBUTE_MESSAGESTORE_DEFAULTLANG = "DefaultLanguage";
    public static final String ELEMENT_VARNAME = "VarName";
    public static final String ELEMENT_PARAMETER = "Parameter";
    public static final String ATTRIBUTE_PARAMETER_NUMBER = "Number";
    public static final String ELEMENT_JAVAGEN_PARANAME = "Name";
    public static final String ELEMENT_JAVAGEN_PARATYPE = "FQJavaType";
    public static final String ELEMENT_FQCLASSNAME = "FQClassName";
    public static final String ELEMENT_JAVJAGEN = "JavaGen";
    public static final String ELEMENT_DESCRIPTION = "Description";
  }

  
  
  public ExceptionStorageParser_1_0(Document doc) {
    super(doc);
  }


  public void validateAgainstXSD() throws InvalidXMLException, XSDNotFoundException {
    Element root = doc.getDocumentElement();
    if (root == null || !root.getNodeName().equals(XML_1_0.ELEMENT_MESSAGESTORE)
                    || !root.getNamespaceURI().equals(XML_1_0.NS_STORAGE)) {
      //throw new RootElementNotFoundException(XML_1_0.ELEMENT_MESSAGESTORE, XML_1_0.NS_STORAGE, doc.getDocumentURI());
    }

    super.validateAgainstXSD();    
  }

  @Override
  protected String getXSDFileName() {
    return XSD_FILENAME;
  }


  @Override
  protected ExceptionStorageInstance parseInternal(boolean resolveImports, int depth) throws InvalidXMLException, XSDNotFoundException
                  {

    ExceptionStorageInstance_1_0 esi = new ExceptionStorageInstance_1_0();
    Element root = doc.getDocumentElement();
    
    String lang = root.getAttribute(XML_1_0.ATTRIBUTE_MESSAGESTORE_DEFAULTLANG);
    esi.setDefaultLanguage(lang);    
    
    List<Element> importElements = XMLUtils.getChildElementsByName(root, XML_1_0.ELEMENT_IMPORT);
    for (Element importElement : importElements) {
      String fileName = importElement.getAttribute(XML_1_0.ATTRIBUTE_IMPORT_FILE);
      esi.addIncludedFile(fileName);
      if (resolveImports) {   
        ExceptionStorageParser parser = null;
        try {
           parser = ExceptionStorageParserFactory.getParser(fileName);
        } catch (Exception e) {
          throw new InvalidXMLException(doc.getDocumentURI(), e);
        }
        ExceptionStorageInstance importEsi = parser.parse(resolveImports, depth+1);
        importEsi.setXmlFile(fileName);
        esi.addInclude(importEsi);
      }
    }
    
    Element javagen = XMLUtils.getChildElementByName(root, XML_1_0.ELEMENT_JAVJAGEN);
    if (javagen != null) {
      Element fqClassNameElement = XMLUtils.getChildElementByName(javagen, XML_1_0.ELEMENT_FQCLASSNAME);
      String fqClassName = XMLUtils.getTextContent(fqClassNameElement);
      esi.setFQClassName(fqClassName);
    }
    
    List<Element> messageElements = XMLUtils.getChildElementsByName(root, XML_1_0.ELEMENT_MESSAGE);
    for (Element messageElement : messageElements) {
      String code = messageElement.getAttribute(XML_1_0.ATTRIBUTE_MESSAGE_CODE);
      List<Element> textMessageElements = XMLUtils.getChildElementsByName(messageElement, XML_1_0.ELEMENT_MESSAGETEXT);
      Map<String, String> messageMap = new HashMap<String, String>();
      for (Element textMessageElement : textMessageElements) {
        String langName = textMessageElement.getAttribute(XML_1_0.ATTRIBUTE_MESSAGETEXT_LANGUAGE);
        String text = XMLUtils.getTextContent(textMessageElement);
        messageMap.put(langName, text);
      }      
      ExceptionEntry_1_0 entry = new ExceptionEntry_1_0(messageMap, code);   
      esi.addEntry(entry);
      
      javagen = XMLUtils.getChildElementByName(messageElement, XML_1_0.ELEMENT_JAVJAGEN);
      if (javagen != null) {
        Element varNameElement = XMLUtils.getChildElementByName(javagen, XML_1_0.ELEMENT_VARNAME);
        String varName = XMLUtils.getTextContent(varNameElement);
        entry.setVariableName(varName);        
      }
      
      
      //parameter
      List<Element> parameterElements = XMLUtils.getChildElementsByName(messageElement, XML_1_0.ELEMENT_PARAMETER);
      int cnt = 0;
      for (Element parameterElement : parameterElements) {
        String varName = "varName_" + (cnt++); 
        String javaType = String.class.getName(); 
        Element javaGenElement = XMLUtils.getChildElementByName(parameterElement, XML_1_0.ELEMENT_JAVJAGEN);
        if (javaGenElement != null) {
          Element fqJavaTypeElement = XMLUtils.getChildElementByName(javaGenElement, XML_1_0.ELEMENT_JAVAGEN_PARATYPE);
          Element varNameElement = XMLUtils.getChildElementByName(javaGenElement, XML_1_0.ELEMENT_JAVAGEN_PARANAME);
          if (fqJavaTypeElement != null) {
            javaType = XMLUtils.getTextContent(fqJavaTypeElement);
          }
          if (varNameElement != null) {
            varName = XMLUtils.getTextContent(varNameElement);
          }
        }
        String label = null; 
        Element descriptionElement = XMLUtils.getChildElementByName(parameterElement, XML_1_0.ELEMENT_DESCRIPTION);
        if (descriptionElement != null) {
          label = XMLUtils.getTextContent(descriptionElement);
        }
        ExceptionParameter parameter;
        try {
          parameter = new ExceptionParameter(varName, false);
        } catch (InvalidParameterNameException e) {
          try {
            parameter = new ExceptionParameter("_" + varName, false);
          } catch (InvalidParameterNameException e1) {
            throw new RuntimeException(e1);
          }
        }
        parameter.setJavaType(javaType);
        parameter.setLabel(label);
        entry.addExceptionParameter(parameter);
      }
    }
    
    return esi;
  }

}
