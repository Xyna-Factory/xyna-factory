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
package com.gip.xyna.xfmg.xfctrl.datamodel.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.xml.XmlIterator;
import com.gip.xyna.utils.xml.XmlIterator.ChildElementsByName;
import com.gip.xyna.xfmg.xfctrl.datamodel.tr069.DataModelParseContext;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/**
 *
 */
public class TRDocument extends TRDataModelDefinition {
  
  private Document doc;
  private Element document;
  private Map<String,TRImport> imports;
  private Map<String,TRModel> models;
  private Map<String,TRComponent> components;
  private Map<String,String> componentSynonyms;
  private Map<String,String> modelSynonyms;
  
  public TRDocument(File file) throws XPRC_XmlParsingException, Ex_FileAccessException {
    super(file);
    String xml =  FileUtils.readFileAsString(file);
    this.doc = XMLUtils.parseString(xml, true);
    this.document = doc.getDocumentElement();
    this.imports = new HashMap<String,TRImport>();
    this.models = new HashMap<String,TRModel>();
    this.components = new HashMap<String,TRComponent>();
    this.componentSynonyms = new HashMap<String,String>();
    this.modelSynonyms = new HashMap<String,String>();
  }
  

  @Override
  public String toString() {
    return "TRDocument("+name+")";
  }

  public Element getDocumentElement() {
    return document;
  }

  public Set<String> getImportReferences() {
    return imports.keySet();
  }
  
  public Collection<TRImport> getImports() {
    return imports.values();
  }
 
  public void addComponent(TRComponent component) {
    components.put( component.getName(), component);
  }

  public void addModel(TRModel model) {
    models.put( model.getFqName(), model);
  }

  public Collection<TRModel> getModels() {
    return models.values();
  }

  public void addImport(TRImport trImport) {
    imports.put(trImport.getReference(), trImport);
  }
  
  public TRModel getModel(String fqName) {
    return models.get(fqName);
  }
  
  public TRComponent getComponent(String name) {
    return components.get(name);
  }

  public TRModel migrateModel(String baseFqName, String fqName) {
    TRModel model = models.remove(baseFqName);
    if( model == null ) {
      return null; //nicht migrierbares
    }
    model.migrateFqName(fqName);
    models.put( model.getFqName(), model);
    return model;
  }

  public Collection<TRComponent> getComponents() {
    return components.values();
  }

  public String addComponentSynonym(String ref, String name) {
    if( ref != null && ref.length() != 0 ) {
      componentSynonyms.put(name, ref);
      return ref;
    }
    return name;
  }

  public Map<String, String> getComponentSynonyms() {
    return componentSynonyms;
  }

  public TRComponent getComponentByNameOrSynonym(String name) {
    TRComponent component = components.get(name);
    if( component == null ) {
      String syn = componentSynonyms.get(name);
      component = components.get(syn);
      
    }
    return component;
  }

  public String addModelSynonym(String ref, String name) {
    if( ref != null && ref.length() != 0 ) {
      modelSynonyms.put(name, ref);
      return ref;
    }
    return name;
  }

  public TRModel getModelByNameOrSynonym(String fqName) {
    TRModel model = models.get(fqName);
    if( model == null ) {
      String syn = modelSynonyms.get(fqName);
      model = models.get(syn);
    }
    return model;

  }

  
  public void parseImports() {
    for( Element importEl : XmlIterator.childElementsByName(getDocumentElement(), "import")) {
      String file = importEl.getAttribute("file");
      TRImport imp = new TRImport(file);
      for( Element dataType : XmlIterator.childElementsByName(importEl, "dataType") ) {
        imp.addDataType( dataType.getAttribute("name") );
      }
      for( Element model : XmlIterator.childElementsByName(importEl, "model") ) {
        String ref = model.getAttribute("ref");
        String name = model.getAttribute("name");
        imp.addModel( addModelSynonym(ref,name) );
      }
      for( Element component : XmlIterator.childElementsByName(importEl, "component") ) {
        String ref = component.getAttribute("ref");
        String name = component.getAttribute("name");
        imp.addComponent( addComponentSynonym(ref,name) ); //siehe parseComponents(..)
      }
      addImport( imp );
    }
  }
  
  
  public void parse(DataModelParseContext context) {
    //Imports aufl�sen
    for( TRImport imp : getImports() ) {
      TRDataModelDefinition referenced = context.getDocument(imp.getReference());
      if( referenced == null ) {
        referenced = context.searchSurrogateDocument(imp.getReference());
      }
      if( referenced == null ) {
        context.warn( "Could not import from missing document "+imp.getReference() );
        continue;
      }
      Set<String> components = new HashSet<String>(imp.getComponents());
      for( String fqName : imp.getModels() ) {
        TRModel base = referenced.getModelByNameOrSynonym(fqName);
        if( base == null ) {
          context.warn("Could not import model "+fqName+" from referenced document "+referenced.getFileName());
          return;
        }
        TRModel newModel = new TRModel(this, base );
        addModel(newModel);
        components.addAll(newModel.getComponentNames());
      }
      for( String name : components ) {
        addComponent( new TRComponent(this, referenced.getComponent(name) ) );
      }
    }
    
    parseComponents(XmlIterator.childElementsByName(document, "component"), context);
    
    for( Element model : XmlIterator.childElementsByName(document, "model") ) {
      parseModel(model, context);
    }
  }
  
  
  private void parseModel(Element model, DataModelParseContext context) {
    String name = model.getAttribute("name");
    context.trace( "Parsing model "+name+" in document "+getName() );
    TRModel trModel = null;
    String baseFqName = model.getAttribute("base");
    if( isEmpty(baseFqName) ) {
      trModel = new TRModel(this,name);
    } else {
      trModel = migrateModel(baseFqName, name);
      if( trModel == null ) {
        context.warn("Could not migrate Model "+baseFqName+": not found");
        return;
      }
    } 
    
    //Alle Objekte parsen
    for( Element object : XmlIterator.childElementsByName(model, "object") ) {
      parseObject(trModel, object, context);
    }    
    
    //Objekte ineinanderstecken
    trModel.treeify(context);
    
    for( Element component : XmlIterator.childElementsByName(model, "component") ) {
      TRObject path = trModel.getObject(component.getAttribute("path"));
      String componentName = component.getAttribute("ref");
      TRComponent trComponent = getComponentByNameOrSynonym(componentName);
      if( trComponent != null ) {
        if( !trModel.getComponentNames().contains(trComponent.getName() ) ) {
          trModel.addComponent(trComponent);
          for( TRObject root : trComponent.getRootObjects() ) {
            path.addChild(root, trComponent);
            trModel.getChildObjects().add(new TRObject(trComponent, trComponent.getObject(trComponent.getName() + "."), path.getFqName() + trComponent.getName() + "."));
          }
        }
      } else {
        context.warn("Component "+componentName+" missing in documnet "+getName() );
      }
    }
    
    addModel(trModel);
  }
  
  
  private void parseComponents(ChildElementsByName components, DataModelParseContext context) {
    //Components k�nnen nicht direkt geparst werden, da Referenzen darin nicht sauber angeben sind
    //Beispiel: Auszug aus tr-106-1-1-0.xml:
    //<import file="tr-106-1-0.xml" spec="urn:broadband-forum-org:tr-106-1-0">
    //  <component name="_ManagementServer" ref="ManagementServer"/>
    //</import>
    //<component name="ManagementServerDiffs">
    //  <object base="ManagementServer." access="readOnly" minEntries="1" maxEntries="1">
    //  ...
    //    </object>
    //</component>
    //<!-- Full components -->
    //<component name="ManagementServer">
    //  <component ref="_ManagementServer"/>
    //  <component ref="ManagementServerDiffs"/>
    //</component>
    //<model name="Device:1.1" base="Device:1.0">
    //  <component path="Device." ref="ManagementServerDiffs"/>
    //</model>
    //
    //Logischer w�re folgendes:
    //<import file="tr-106-1-0.xml" spec="urn:broadband-forum-org:tr-106-1-0">
    //  <component name="ManagementServer" />
    //</import>
    //<component base="ManagementServer">
    //  <object base="ManagementServer." access="readOnly" minEntries="1" maxEntries="1">
    //  ...
    //    </object>
    //</component>
    //<model name="Device:1.1" base="Device:1.0">
    //</model>
    //
    //Nun wird versucht, trotz obigem das untere auszuf�hren
    
    
    List<Pair<String, Element>> componentsToParse = new ArrayList<Pair<String, Element>>();
    
    for( Element component : components ) {
      String name = component.getAttribute("name");
      for( Element child : XmlIterator.childElements(component) ) {
        String childName = child.getNodeName();
        if( "component".equals(childName) ) {
          addComponentSynonym(name, child.getAttribute("ref") );
        } else {
          componentsToParse.add( Pair.of(name, component) );
          break;
        }
      }
    }
    
    for( Pair<String, Element> comp : componentsToParse ) {
      String name = comp.getFirst();
      TRComponent base = getComponentByNameOrSynonym(name);
      parseComponent(comp.getSecond(), base, context);
    }
  }

  private void parseComponent(Element component, TRComponent base, DataModelParseContext context) {
    String name = component.getAttribute("name");
    context.trace( "Parsing component "+name+" in document "+getName() );
    TRComponent trComponent = null;
    if( base != null ) {
      trComponent = base;
    } else {
      trComponent = new TRComponent(this, name);
    }
    
    //Alle Objekte parsen
    for( Element object : XmlIterator.childElementsByName(component, "object") ) {
      parseObject(trComponent, object, context);
    }
    
    //Objekte ineinanderstecken
    trComponent.treeify(context);
    addComponent(trComponent);
  }



  private void parseObject(TRObjectContainer container, Element object, DataModelParseContext context) {
    String name = object.getAttribute("name");
    if( ! isEmpty(name) ) {
      context.trace("  Parsing object "+name+" in "+container.getPrettyName(false) );
      TRObject trObject = new TRObject(container,name);
      Element description = XmlIterator.getFirstChildElement(object, "description");
      if( description != null ) {
        trObject.setDescription( description.getTextContent() );
      }
      for( Element parameter : XmlIterator.childElementsByName(object, "parameter") ) {
        parseParameter(container, trObject, parameter, context);
      }
      container.addObject(trObject);
      return;
    }
    String base = object.getAttribute("base");
    if( ! isEmpty(base) ) {
      context.trace("  Referencing object "+base+" in "+container.getPrettyName(false) );
      TRObject trObject = container.getObject(base);
      if( trObject == null ) {
        context.warn("Referenced object "+base+" not found in "+container.getPrettyName(true) );
        return;
      }
      Element description = XmlIterator.getFirstChildElement(object, "description");
      if( description != null ) {
        trObject.setFileNameOfLastChange(container.getFileName());
        trObject.setDescription( description.getTextContent() );
      }
      for( Element parameter : XmlIterator.childElementsByName(object, "parameter") ) {
        parseParameter(container, trObject, parameter, context);
      }
    }
  }



  private void parseParameter(TRObjectContainer container, TRObject object, Element parameter, DataModelParseContext context) {
    String name = parameter.getAttribute("name");
    if( ! isEmpty(name) ) {
      context.trace( "    Parsing parameter "+name+" in object "+object.getName() );
      TRParameter trParameter = new TRParameter(object,name, container.getFileName());
      for( Node c : XmlIterator.children(parameter) ) {
        if( c.getNodeName().equals("description") ) {
          trParameter.setDescription( c.getTextContent() );
        }
        if( c.getNodeName().equals("syntax") ) {
          parseSyntax(trParameter, c );
        }
      }
      object.addParameter(trParameter);
    }
    String base = parameter.getAttribute("base");
    if( ! isEmpty(base) ) {
      context.trace( "    Referencing parameter "+base+" in object "+object.getName() );
      TRParameter trParameter = object.getParameter(base);
      if( trParameter == null ) {
        throw new IllegalStateException("Referenced parameter "+base+" not found in object "+object.getName() );
      }
      trParameter.setFileNameOfLastChange(container.getFileName());
      for( Node c : XmlIterator.children(parameter) ) {
        if( c.getNodeName().equals("description") ) {
          Element desc = (Element)c;
          String text = desc.getTextContent();
          String action = desc.getAttribute("action");
          if( "replace".equals(action) ) {
            trParameter.setDescription( text );
          } else if( "append".equals(action) ) {
            trParameter.setDescription( trParameter.getDescription() +"\n"+ text );
          } else {
            context.warn("Unexpected action "+action+" in parameter "+trParameter.getName()+" in "+container.getPrettyName(true) );
          }
        }
        if( c.getNodeName().equals("syntax") ) {
          parseSyntax(trParameter, c );
        }
      }
    }
  }

  private void parseSyntax(TRParameter trParameter, Node syntax) {
    for( Node sc : XmlIterator.children(syntax) ) {
      if( sc instanceof Element ) {
        if( sc.getNodeName().equals("default") ) {
          continue;
        }
        trParameter.setType(sc.getNodeName());//FIXME
      }
    }
  }
  
  private static boolean isEmpty(String string) {
    return string == null || string.length() == 0;
  }
  
}
