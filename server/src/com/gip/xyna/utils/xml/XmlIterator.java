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
package com.gip.xyna.utils.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * Iteratoren, mit denen �ber org.w3c.dom.Node iteriert werden kann
 */
public class XmlIterator {
  
  
  
  /**
   * Kinder mit angegebenem Namen
   * @param parent
   * @param name
   * @return
   */
  public static ChildElementsByName childElementsByName(Element parent, String name) {
    return new ChildElementsByName(parent, name);
  }
  
  /**
   * alle Kinder, die Elemente sind
   * @param parent
   * @return
   */
  public static ElementChildren childElements(Element parent) {
    return new ElementChildren(parent);
  }
  
  /**
   * alle Kinder (Element, Attribut, ...)
   * @param parent
   * @return
   */
  public static Children children(Node parent) {
    return new Children(parent);
  }
  
  
  /**
   * alle Kinder und Kindeskinder mit angegebenem Namen
   * @param parent
   * @param name
   * @return
   */
  public static ElementsByTagName elementsByTagName(Element parent, String name) {
    return new ElementsByTagName(parent,name);
  }
  
  
  
  
  public static class ChildElementsByName implements Iterable<Element> {
    private NodeList nl;
    private String name;
    
    public ChildElementsByName(Element parent, String name) {
      this.nl = parent.getChildNodes();
      this.name = name;
    }

    public Iterator<Element> iterator() {
      return new NodeListFilterIterator<Element>(nl, new NameFilter(name)); 
    }
    
  }
  
  public static Element getFirstChildElement(Element parent, String name) {
    for( Element child : new ChildElementsByName(parent,name) ) {
      return child;
    }
    return null;
  }

  public static class ElementChildren implements Iterable<Element> {
    private NodeList nl;
    
    public ElementChildren(Element parent) {
      nl = parent.getChildNodes();
    }

    public Iterator<Element> iterator() {
      return new NodeListFilterIterator<Element>(nl, new ElementFilter()); 
    }
    
  }
 
  
  public static class Children implements Iterable<Node> {
    private NodeList nl;
    
    public Children(Node parent) {
      nl = parent.getChildNodes();
    }

    public Iterator<Node> iterator() {
      return new NodeListIterator<Node>(nl); 
    }
    
  }
  

  public static class ElementsByTagName implements Iterable<Element> {
    private NodeList nl;
    
    public ElementsByTagName(Element parent, String name) {
      nl = parent.getElementsByTagName(name);
    }

    public Iterator<Element> iterator() {
      return new NodeListIterator<Element>(nl); 
    }
    
  }
  
  
  public static class NodeListIterator<T> implements Iterator<T> {

    private NodeList nodeList;
    private int size;
    private int pos;

    public NodeListIterator(NodeList nodeList) {
      this.nodeList = nodeList;
      this.size = nodeList.getLength();
      this.pos = 0;
    }

    public boolean hasNext() {
      return pos < size;
    }

    @SuppressWarnings("unchecked")
    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      Node n = nodeList.item(pos);
      ++pos;
      return (T)n;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }
  
  
  public static interface Filter {
    public boolean accept(Node n);
  }
  
  public static class NameFilter implements Filter {
    private String name;

    public NameFilter(String name) {
      this.name = name;
    }

    public boolean accept(Node n) {
      return n.getNodeName().equals(name);
    }
  }
  
  public static class ElementFilter implements Filter {
    public boolean accept(Node n) {
      return n instanceof Element;
    }
  }
  
 
  
  public static class NodeListFilterIterator<T> implements Iterator<T> {

    private NodeList nodeList;
    private int size;
    private int pos;
    private T next;
    private Filter filter;
    
    public NodeListFilterIterator(NodeList nodeList, Filter filter) {
      this.nodeList = nodeList;
      this.size = nodeList.getLength();
      this.filter = filter;
      this.pos = 0;
      this.next = getNext();
    }

    @SuppressWarnings("unchecked")
    private T getNext() {
      while( pos < size ) {
        Node n = nodeList.item(pos);
        ++pos;
        if( filter.accept(n) ) {
          return (T)n;
        }
      }
      return null;
    }

    public boolean hasNext() {
      return next != null;
    }

    public T next() {
      if (!hasNext()) {
        throw new NoSuchElementException();
      }
      T n = next;
      next = getNext();
      return n;
    }

    public void remove() {
      throw new UnsupportedOperationException();
    }
    
  }

  
  
}
