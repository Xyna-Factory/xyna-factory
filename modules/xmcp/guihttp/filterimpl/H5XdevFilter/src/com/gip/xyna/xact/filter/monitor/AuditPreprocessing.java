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

package com.gip.xyna.xact.filter.monitor;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.MissingImportsRestorer;
import com.gip.xyna.xact.filter.monitor.auditpreprocessing.MissingImportsRestorer.MissingImportRestorationResult;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;



public class AuditPreprocessing {
  
  public static final String PROPERTY_LAZY_LOADING_LIMIT = "xyna.processmonitor.lazyloading.limit";
  public static final XynaPropertyInt LAZY_LOADING_LIMIT = new XynaPropertyInt(PROPERTY_LAZY_LOADING_LIMIT, 1000)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Maximale Anzahl an Schleifen-Iterationen und Elementen in Listen, die bei Lazy Loading in Audits gesendet werden. -1 bedeutet kein Limit - Warnung: kann zu Fabrikabst�rzen wegen Out-of-Memory f�hren!")
      .setDefaultDocumentation(DocumentationLanguage.EN, "Maximum number of loop iterations and elements in lists to be transferred for lazy loading in audits. -1 means no limit - Warning: can lead to factory crashes due to out-of-memory!");

  private static final Logger logger = CentralFactoryLogging.getLogger(AuditPreprocessing.class);

  private static final String TAG_PARAMETER = "Parameter";
  private static final String TAG_DATA = "Data";
  private static final String TAG_VALUE = "Value";
  private static final String TAG_IMPORT = "Import";
  private static final String ATT_FOREACH_INDICES = "ForeachIndices";
  private static final String ATT_RETRY_COUNTER = "RetryCounter";


  private AuditPreprocessing() {}


  public static String filterAudit(String xml) throws ParserConfigurationException, SAXException, TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
    SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    SAXParser parser = parserFactory.newSAXParser();
    XMLReader xr = new LimitFilter(parser.getXMLReader());
    Source src = new SAXSource(xr, new InputSource(new StringReader(xml)));

    ByteArrayOutputStream filteredXmlStream = new ByteArrayOutputStream(); 
    javax.xml.transform.Result res = new StreamResult(filteredXmlStream);
    TransformerFactory.newInstance().newTransformer().transform(src, res);

    return filteredXmlStream.toString();
  }

  
  
  public static MissingImportRestorationResult restoreMissingImports(String filteredXml) throws Exception {
    
    MissingImportsRestorer restorer = new MissingImportsRestorer();
    MissingImportRestorationResult result = restorer.restoreMissingImports(filteredXml);
    
    return result;
  }
  
  
  

  private static class LimitFilter extends XMLFilterImpl {
    private boolean skipIteration;
    private boolean isInsideImport;
    private Deque<Integer> listLengths = new ArrayDeque<>();


    public LimitFilter(XMLReader xmlReader) {
      super(xmlReader);
    }


    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
      if (isInsideImport) {
        super.startElement(uri, localName, qName, atts);
        return;
      }

      if (TAG_IMPORT.equals(qName)) {
        isInsideImport = true;
        super.startElement(uri, localName, qName, atts);
      } else if (TAG_PARAMETER.equals(qName)) {
        // skip parameter tags with foreach indices over the iteration limit
        if (isOverForeachLimit(atts.getValue(ATT_FOREACH_INDICES), LAZY_LOADING_LIMIT.get()) ||
            isOverRetryLimit(atts.getValue(ATT_RETRY_COUNTER), LAZY_LOADING_LIMIT.get())) {
          skipIteration = true;
        } else {
          super.startElement(uri, localName, qName, atts);
          skipIteration = false;
        }
      } else if (TAG_DATA.equals(qName)) {
        listLengths.push(0);
        if (!skipIteration && !isOverListLimit(listLengths, LAZY_LOADING_LIMIT.get())) {
          super.startElement(uri, localName, qName, atts);
        }
      } else if (TAG_VALUE.equals(qName)) {
        listLengths.push(listLengths.pop() + 1);
        if (!skipIteration && !isOverListLimit(listLengths, LAZY_LOADING_LIMIT.get())) {
          super.startElement(uri, localName, qName, atts);
        }
      } else if (!skipIteration && !isOverListLimit(listLengths, LAZY_LOADING_LIMIT.get())) {
        super.startElement(uri, localName, qName, atts);
      }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (TAG_IMPORT.equals(qName)) {
        isInsideImport = false;
        super.endElement(uri, localName, qName);
        return;
      }

      if (isInsideImport) {
        super.endElement(uri, localName, qName);
        return;
      }

      if (!skipIteration && TAG_DATA.equals(qName)) {
        listLengths.pop();
      }

      if (!skipIteration && !isOverListLimit(listLengths, LAZY_LOADING_LIMIT.get())) {
        super.endElement(uri, localName, qName);
      }

      if (TAG_PARAMETER.equals(qName)) {
        skipIteration = false;
      }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
      if (!skipIteration && !isOverListLimit(listLengths, LAZY_LOADING_LIMIT.get())) {
        super.characters(ch, start, length);
      }
    }

    private boolean isOverForeachLimit(String foreachIndices, int limit) {
      if (foreachIndices == null || foreachIndices.isEmpty() || limit < 0) {
        return false;
      }

      String[] indicesArray = foreachIndices.split(",");
      int limitPerLevel = indicesArray.length > 0 ? Math.max(1, (int) Math.pow(limit, 1d / indicesArray.length)) : limit;

      try {
        for (String foreachIndexStr : indicesArray) {
          int foreachIndex = Integer.parseInt(foreachIndexStr);
          if (foreachIndex + 1 > limitPerLevel) {
            return true;
          }
        }
      } catch (Exception e) {
        Utils.logError("Could not parse foreach indices during audit pre-filtering -> assuming it's above the limit", e);
        return true;
      }

      return false;
    }
    
    private boolean isOverRetryLimit(String retryCounter, int limit) {
      if (retryCounter == null || retryCounter.isEmpty() || limit < 0) {
        return false;
      }

      try {
        return (Integer.parseInt(retryCounter)+1 > Math.max(1, limit));
      } catch (Exception e) {
        Utils.logError("Could not parse retry counter during audit pre-filtering -> assuming it's above the limit", e);
        return true;
      }
    }

    private boolean isOverListLimit(Deque<Integer> listLengths, int limit) {
      if (listLengths == null || listLengths.isEmpty() || limit < 0) {
        return false;
      }

      for (int listLength : listLengths) {
        if (listLength > Math.max(1, limit)) {
          return true;
        }
      }

      return false;
    }
  }
  }
