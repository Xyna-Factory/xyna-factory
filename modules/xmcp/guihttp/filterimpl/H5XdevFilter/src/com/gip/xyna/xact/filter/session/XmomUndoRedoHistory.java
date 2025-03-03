/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
package com.gip.xyna.xact.filter.session;



import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;



public class XmomUndoRedoHistory {

  public static final XynaPropertyInt UNDO_LIMIT = new XynaPropertyInt("xyna.processmodeller.undo.limit", 50)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Die maximale Anzahl an Einträgen der Undo-Historie.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The maximum number of entries in the undo history.");
  public static final XynaPropertyInt REDO_LIMIT = new XynaPropertyInt("xyna.processmodeller.redo.limit", 50)
      .setDefaultDocumentation(DocumentationLanguage.DE, "Die maximale Anzahl an Einträgen der Redo-Historie.")
      .setDefaultDocumentation(DocumentationLanguage.EN, "The maximum number of entries in the redo history.");


  private HashMap<FQName, LinkedList<XMOMHistoryItem>> xmomUndoHistory;
  private HashMap<FQName, LinkedList<XMOMHistoryItem>> xmomRedoHistory;

  public XmomUndoRedoHistory() {
    xmomUndoHistory = new HashMap<>();
    xmomRedoHistory = new HashMap<>();
  }

  public void save(FQName fqName) {
    LinkedList<XMOMHistoryItem> undoHistoryItems = xmomUndoHistory.get(fqName);
    if (undoHistoryItems != null) {
      undoHistoryItems.forEach(item -> setSavedAndModified(item));
    }

    LinkedList<XMOMHistoryItem> redoHistoryItems = xmomRedoHistory.get(fqName);
    if (redoHistoryItems != null) {
      redoHistoryItems.forEach(item -> setSavedAndModified(item));
    }
  }


  public void createXmomUndoHistoryItem(FQName fqName, GenerationBaseObject currentGbo, String currentXml) {
    if (!xmomUndoHistory.containsKey(fqName)) {
      xmomUndoHistory.put(fqName, new LinkedList<>());
    }

    if (xmomUndoHistory.get(fqName).size() > UNDO_LIMIT.get()) {
      while (xmomUndoHistory.get(fqName).size() > UNDO_LIMIT.get()) {
        xmomUndoHistory.get(fqName).removeFirst();
      }
    }

    XMOMHistoryItem history = new XMOMHistoryItem(fqName, currentXml, currentGbo.getSaveState(), currentGbo.hasBeenModified());
    xmomUndoHistory.get(fqName).add(history);
  }


  public void createXmomRedoHistoryItem(FQName fqName, GenerationBaseObject currentGbo, String currentXml) {
    if (!xmomRedoHistory.containsKey(fqName)) {
      xmomRedoHistory.put(fqName, new LinkedList<>());
    }

    if (xmomRedoHistory.get(fqName).size() > REDO_LIMIT.get()) {
      while (xmomRedoHistory.get(fqName).size() > REDO_LIMIT.get()) {
        xmomRedoHistory.get(fqName).removeFirst();
      }
    }

    XMOMHistoryItem historyItem = new XMOMHistoryItem(fqName, currentXml, currentGbo.getSaveState(), currentGbo.hasBeenModified());
    xmomRedoHistory.get(fqName).add(historyItem);
  }


  public void close(FQName fqName) {
    xmomRedoHistory.remove(fqName);
    xmomUndoHistory.remove(fqName);
  }


  public void reset(FQName fqName) {
    xmomUndoHistory.put(fqName, new LinkedList<>());
    xmomRedoHistory.put(fqName, new LinkedList<>());
  }


  public void resetRedo(FQName fqName) {
    xmomRedoHistory.put(fqName, new LinkedList<>());
  }


  public XMOMHistoryItem undo(FQName fqName) {
    return xmomUndoHistory.get(fqName).removeLast();
  }
  
  public XMOMHistoryItem redo(FQName fqName) {
    return xmomRedoHistory.get(fqName).removeLast();
  }


  public void replace(FQName oldFqn, FQName newFqn, GenerationBaseObject newGbo) {
    LinkedList<XMOMHistoryItem> redoHistory = xmomRedoHistory.remove(oldFqn);
    if (redoHistory != null) {
      xmomHistoryFqnChanged(redoHistory, newGbo);
      xmomRedoHistory.put(newFqn, redoHistory);
    }

    LinkedList<XMOMHistoryItem> undoHistory = xmomUndoHistory.remove(oldFqn);
    if (undoHistory != null) {
      xmomHistoryFqnChanged(undoHistory, newGbo);
      xmomUndoHistory.put(newFqn, undoHistory);
    }
  }

  
  public boolean canUndo(FQName fqName) {
    return xmomUndoHistory.containsKey(fqName) && !xmomUndoHistory.get(fqName).isEmpty();
  }
  
  public boolean canRedo(FQName fqName) {
    return xmomRedoHistory.containsKey(fqName) && !xmomRedoHistory.get(fqName).isEmpty();
  }

  private void setSavedAndModified(XMOMHistoryItem item) {
    if (item == null) {
      return;
    }
    item.setModified(true);
    item.setSaveState(true);
  }


  private void xmomHistoryFqnChanged(LinkedList<XMOMHistoryItem> history, final GenerationBaseObject newGbo) {
    if (history == null) {
      return;
    }

    for (XMOMHistoryItem item : history) {
      try {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(item.getXml())));
        XPath xpath = XPathFactory.newInstance().newXPath();
        NodeList nodes = (NodeList) xpath.evaluate("/Service", doc, XPathConstants.NODESET);
        if (nodes == null || nodes.getLength() != 1) {
          continue;
        }
        Node label = nodes.item(0).getAttributes().getNamedItem("Label");
        Node typeName = nodes.item(0).getAttributes().getNamedItem("TypeName");
        Node typePath = nodes.item(0).getAttributes().getNamedItem("TypePath");
        label.setNodeValue(newGbo.getGenerationBase().getLabel());
        typeName.setNodeValue(newGbo.getGenerationBase().getOriginalSimpleName());
        typePath.setNodeValue(newGbo.getGenerationBase().getOriginalPath());

        StringWriter writer = new StringWriter();
        Transformer xformer = TransformerFactory.newInstance().newTransformer();
        xformer.transform(new DOMSource(doc), new StreamResult(writer));

        item.setXml(writer.getBuffer().toString());
        setSavedAndModified(item);
      } catch (Exception e) {
        com.gip.xyna.xact.filter.util.Utils.logError(e);
      }
    }
  }
}
