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



import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.utils.exceptions.utils.codegen.InvalidParameterNameException;



/**
 * parse() aufrufen, ansonsten sind die membervariablen nicht gefüllt.
 */
public class ExceptionStorageParser_1_1 extends ExceptionStorageParserBase {

  private static Logger logger = Logger.getLogger(ExceptionStorageParser_1_1.class.getName());
  
  private static final String XSD_FILENAME = "MessageStorage1.1.xsd";

  private static final Pattern filenamePattern = Pattern.compile("\\$\\{([^\\}]*)\\}");


  public ExceptionStorageParser_1_1(Document doc) {
    super(doc);
  }
     
  /**
   * löst strings der form ... ${key} ... ${key2} ... auf indem key und key2 mit system-properties
   * ersetzt werden (falls vorhanden).
   * @param fileName
   * @return
   */
  private static String resolveFileNameVariables(String fileName) {
    Matcher m = filenamePattern.matcher(fileName);
    StringBuffer sb = new StringBuffer();
    if (m.find()) {
      Map<String, String> env = System.getenv();
      Properties props = System.getProperties();
      do {
        String key = m.group(1);
        if (logger.isTraceEnabled()) {
          logger.trace("found variable " + key + " in fileName");
        }
        if (env.containsKey(key)) {
          m.appendReplacement(sb, Matcher.quoteReplacement(env.get(key)));
        } else if (props.containsKey(key)) {
          m.appendReplacement(sb, Matcher.quoteReplacement(props.get(key).toString()));
        }
      } while (m.find()); 
      m.appendTail(sb);
      if (logger.isTraceEnabled()) {
        logger.trace("substituted fileName is " + sb.toString());
      }
      return sb.toString();
    }
    return fileName;
  }


  protected ExceptionStorageInstance parseInternal(boolean resolveIncludes, int depth) throws InvalidXMLException,
                  XSDNotFoundException {
    ExceptionStorageInstance_1_1 esi = new ExceptionStorageInstance_1_1();
    esi.setXmlFile(doc.getDocumentURI());
    Element root = doc.getDocumentElement();

    String lang = root.getAttribute(XML_1_1.ATTRIBUTE_DEFAULTLANGUAGE);
    esi.setDefaultLanguage(lang);

    //includes = weitere xmls die die exception-definition ausmachen. werden generiert
    List<Element> includeElements = XMLUtils.getChildElementsByName(root, XML_1_1.ELEMENT_INCLUDE);
    for (Element includeElement : includeElements) {
      String fileName = resolveFileNameVariables(includeElement.getAttribute(XML_1_1.ATTRIBUTE_IMPORT_INCLUDE_FILE));
      if (!new File(fileName).exists() && !fileName.startsWith("/")) {
        fileName = new File(new File(doc.getDocumentURI()).getParent(), fileName).getAbsolutePath();
      }
      esi.addIncludedFile(fileName);
      if (resolveIncludes) {
        ExceptionStorageParser esp = null;
        try {
          esp = ExceptionStorageParserFactory.getParser(fileName);
        } catch (Exception e) {
          throw new InvalidXMLException(doc.getDocumentURI(), e);
        }
        ExceptionStorageInstance includeEsi = esp.parse(true, depth + 1);
        includeEsi.setXmlFile(fileName);
        esi.addInclude(includeEsi);
      }
    }
    
    //imports = referenzierte xmls, die nur zur auflösung von abhängigkeiten benutzt werden.
    List<Element> importElements = XMLUtils.getChildElementsByName(root, XML_1_1.ELEMENT_IMPORT);
    for (Element importElement : importElements) {
      String fileName = resolveFileNameVariables(importElement.getAttribute(XML_1_1.ATTRIBUTE_IMPORT_INCLUDE_FILE));
      if (!new File(fileName).exists() && !fileName.startsWith("/")) {
        fileName = new File(new File(doc.getDocumentURI()).getParent(), fileName).getAbsolutePath();
      }
    //  esi.addImportedFile(fileName);
      ExceptionStorageParser esp = null;
      try {
        esp = ExceptionStorageParserFactory.getParser(fileName);
      } catch (Exception e) {
        throw new InvalidXMLException(doc.getDocumentURI(), e);
      }
      ExceptionStorageInstance impEsi = esp.parse(true, depth + 1);
      impEsi.setXmlFile(fileName);
      esi.addImport(impEsi);
    }

    List<Element> exceptionElements = XMLUtils.getChildElementsByName(root, XML_1_1.ELEMENT_EXCEPTION);
    for (Element exceptionElement : exceptionElements) {
      List<Element> messageElements = XMLUtils.getChildElementsByName(exceptionElement, XML_1_1.ELEMENT_MESSAGE);
      Map<String, String> messages = new HashMap<String, String>();
      for (Element messageElement : messageElements) {
        String language = messageElement.getAttribute(XML_1_1.ATTRIBUTE_LANGUAGE);
        String message = XMLUtils.getTextContent(messageElement);
        messages.put(language, message);
      }
      String name = exceptionElement.getAttribute(XML_1_1.ATTRIBUTE_EXCEPTIONNAME);
      String path = exceptionElement.getAttribute(XML_1_1.ATTRIBUTE_EXCEPTIONPATH);
      String code = exceptionElement.getAttribute(XML_1_1.ATTRIBUTE_CODE);
      ExceptionEntry_1_1 entry = new ExceptionEntry_1_1(messages, name, path, code);
      String _abstract = exceptionElement.getAttribute(XML_1_1.ATTRIBUTE_ISABSTRACT);
      String baseExceptionName = exceptionElement.getAttribute(XML_1_1.ATTRIBUTE_BASEEXCEPTIONNAME);
      String baseExceptionPath = exceptionElement.getAttribute(XML_1_1.ATTRIBUTE_BASEEXCEPTIONPATH);
      String label = exceptionElement.getAttribute(XML_1_1.ATTRIBUTE_LABEL);
      if (_abstract != null && _abstract.length() > 0) {
        entry.setAbstract(_abstract.equalsIgnoreCase("true"));
      }
      if (baseExceptionName != null && baseExceptionName.length() > 0) {
        entry.setBaseExceptionName(baseExceptionName);
      }
      if (baseExceptionPath != null && baseExceptionPath.length() > 0) {
        entry.setBaseExceptionPath(baseExceptionPath);
      }
      if (label != null && label.length() > 0) {
        entry.setLabel(label);
      }
      List<Element> parameterElements = XMLUtils.getChildElements(exceptionElement);
      for (Element parameterElement : parameterElements) {
        if (parameterElement.getNodeName().equals(XML_1_1.ELEMENT_DATA)
            || parameterElement.getNodeName().equals(XML_1_1.ELEMENT_EXCEPTION_PARA))
          entry.addExceptionParameter(parseParameter(parameterElement, code));
      }
      esi.addEntry(entry);
    }
    return esi;
  }


  private ExceptionParameter parseParameter(Element parameterElement, String code) {
    String varName = parameterElement.getAttribute(XML_1_1.ATTRIBUTE_VARNAME);
    String referenceName = parameterElement.getAttribute(XML_1_1.ATTRIBUTE_REFERENCENAME);
    String referencePath = parameterElement.getAttribute(XML_1_1.ATTRIBUTE_REFERENCEPATH);
    String label = parameterElement.getAttribute(XML_1_1.ATTRIBUTE_LABEL);
    String isListAttribute = parameterElement.getAttribute(XML_1_1.ATTRIBUTE_ISLIST);
    boolean isList = isListAttribute != null && isListAttribute.equalsIgnoreCase("true");
    boolean isReference = referenceName != null && referenceName.length() > 0;
    ExceptionParameter ep;
    try {
      ep = new ExceptionParameter(varName, isReference);
    } catch (InvalidParameterNameException e) {
      try {
        ep = new ExceptionParameter("_" + varName, isReference);
      } catch (InvalidParameterNameException e1) {
        throw new RuntimeException(e1);
      }
    }
    ep.setIsList(isList);
    if (isReference) {
      ep.setTypeName(referenceName);
      ep.setTypePath(referencePath);
    } else {
      List<Element> metaElements = XMLUtils.getChildElementsByName(parameterElement, XML_1_1.ELEMENT_META);
      if (metaElements.size() != 1) {
        throw new RuntimeException("Invalid xml. Element \"" + XML_1_1.ELEMENT_META
                        + "\" expected under parameter " + varName + " of exception " + code);
      }
      List<Element> typeElements = XMLUtils.getChildElementsByName(metaElements.get(0), XML_1_1.ELEMENT_TYPE);
      if (typeElements.size() != 1) {
        throw new RuntimeException("Invalid xml. Element \"" + XML_1_1.ELEMENT_TYPE
                        + "\" expected under parameter " + varName + " of exception " + code);
      }
      ep.setJavaType(XMLUtils.getTextContent(typeElements.get(0)));

      // TODO PMOD-259
//      Element documentationElement = XMLUtils.getChildElementByName(metaElements.get(0), XML_1_1.ELEMENT_DOCUMENTATION);
//      ep.setDocumentation(XMLUtils.getTextContent(documentationElement));
    }
    if (label != null && label.length() > 0) {
      ep.setLabel(label);
    }
    ep.setType(parameterElement.getNodeName());
    return ep;
  }


  private interface XML_1_1 {

    public static final String ATTRIBUTE_ISLIST = "IsList";
    public static final String ATTRIBUTE_IMPORT_INCLUDE_FILE = "File";
    public static final String ELEMENT_TYPE = "Type";
    public static final String ELEMENT_META = "Meta";
    public static final String ATTRIBUTE_REFERENCEPATH = "ReferencePath";
    public static final String ATTRIBUTE_REFERENCENAME = "ReferenceName";
    public static final String ATTRIBUTE_VARNAME = "VariableName";
    public static final String ELEMENT_DATA = "Data";
    public static final String ELEMENT_EXCEPTION_PARA = "Exception";
    public static final String ATTRIBUTE_LABEL = "Label";
    public static final String ATTRIBUTE_BASEEXCEPTIONPATH = "BaseTypePath";
    public static final String ATTRIBUTE_BASEEXCEPTIONNAME = "BaseTypeName";
    public static final String ATTRIBUTE_ISABSTRACT = "IsAbstract";
    public static final String ATTRIBUTE_LANGUAGE = "Language";
    public static final String ATTRIBUTE_CODE = "Code";
    public static final String ATTRIBUTE_EXCEPTIONPATH = "TypePath";
    public static final String ATTRIBUTE_EXCEPTIONNAME = "TypeName";
    public static final String ELEMENT_MESSAGE = "MessageText";
    public static final String ELEMENT_EXCEPTION = "ExceptionType";
    public static final String ELEMENT_EXCEPTIONSTORE = "ExceptionStore";
    public static final String NS_STORAGE = "http://www.gip.com/xyna/3.0/utils/message/storage/1.1";
    public static final String ELEMENT_IMPORT = "Import";
    public static final String ELEMENT_INCLUDE = "Include";
    public static final String ATTRIBUTE_DEFAULTLANGUAGE = "DefaultLanguage";
    public static final String ELEMENT_DOCUMENTATION = "Documentation";
  }


  public void validateAgainstXSD() throws InvalidXMLException, XSDNotFoundException {
    Element root = doc.getDocumentElement();
    if (root == null || !root.getNodeName().equals(XML_1_1.ELEMENT_EXCEPTIONSTORE)
                    || !root.getNamespaceURI().equals(XML_1_1.NS_STORAGE)) {
     throw new RootElementNotFoundException(doc.getDocumentURI(), XML_1_1.ELEMENT_EXCEPTIONSTORE, XML_1_1.NS_STORAGE);
    }

    super.validateAgainstXSD();
  }


  @Override
  protected String getXSDFileName() {
    return XSD_FILENAME;
  }


}
