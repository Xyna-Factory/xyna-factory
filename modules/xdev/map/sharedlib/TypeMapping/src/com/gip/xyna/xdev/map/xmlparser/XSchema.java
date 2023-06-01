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
package com.gip.xyna.xdev.map.xmlparser;



import java.util.Vector;

import org.apache.xerces.impl.xs.util.StringListImpl;
import org.apache.xerces.xs.XSComplexTypeDefinition;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSImplementation;
import org.apache.xerces.xs.XSLoader;
import org.apache.xerces.xs.XSModel;
import org.apache.xerces.xs.XSTypeDefinition;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;




public class XSchema {

  private XSModel schema;


  private static XSLoader getXSLoader() throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException {

    // Get DOM Implementation using DOM Registry
    //TODO sun klassen ersetzen
    System.setProperty(DOMImplementationRegistry.PROPERTY,
                       "com.sun.org.apache.xerces.internal.dom.DOMXSImplementationSourceImpl");
    DOMImplementationRegistry registry;
    try {
      registry = DOMImplementationRegistry.newInstance();
    } catch (ClassCastException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }

    XSImplementation impl = (XSImplementation) registry.getDOMImplementation("XS-Loader");

    return impl.createXSLoader(null);
  }


  private XSchema(final String[] xsds) throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    Vector<String> v = new Vector<String>();
    for (String xsd : xsds) {
      v.add(xsd);
//      v.add(new File(xsd).toURI().toString());
    }

    XSLoader loader = getXSLoader();
    schema = loader.loadURIList(new StringListImpl(v));
  }


  public static XSchema parse(String[] parts) throws ClassCastException, ClassNotFoundException, InstantiationException, IllegalAccessException {
    return new XSchema(parts);
  }


  public XSComplexType getElement(String namespace, String localName) {
    XSElementDeclaration el = schema.getElementDeclaration(localName, namespace);
    if (el == null) {
      throw new RuntimeException("rootelement with name " + namespace + ":" + localName + " not found in schema.");
    } else {
      XSTypeDefinition type = el.getTypeDefinition();
      if (type instanceof XSComplexTypeDefinition) {
        return new XSComplexType((XSComplexTypeDefinition) el.getTypeDefinition());
      } else {
        throw new RuntimeException("rootelement must be of complex type");
      }
    }
  }

}
