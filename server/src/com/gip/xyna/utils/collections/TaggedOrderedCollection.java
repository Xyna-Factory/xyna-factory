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
package com.gip.xyna.utils.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


/**
 * Collection, die <code>Comparable</code>-Eintr�ge geordnet speichert und �ber einen Tag verwalten kann.
 * Diese Eintr�ge k�nnen der Reihe nach �ber einen Iterator abgefragt werden. Dieser Iterator
 * hat die Eigenschaft, nicht nur mit remove() den jeweils letzten Eintrag l�schen zu k�nnen, 
 * sondern auch �ber tag(String) mit einem Tag versehen zu k�nnen. 
 * Jedem Eintrag kann nur ein Tag zugeordnet werden, eine Neuzuordnung eines Tags l�scht die 
 * vorherige Zuordnung. Der Default-Tag (auch vergebbar) ist null.
 * �ber die Methoden hide(String) und show(String) kann �ber die TaggedOrderedCollection gesteuert werden,
 * ob der n�chste Iterator Eintr�ge zu einem bestimmten Tag verstecken soll (hide) oder ob dies r�ckg�ngig
 * gemacht werden soll (show).
 * Der Iterator hat w�hrend der Iteration ebenfalls die M�glichkeit �ber hide(String), alle folgenden
 * Eintr�ge eines Tags zu �berspringen.
 *
 */
public class TaggedOrderedCollection<E extends Comparable<E>> extends AbstractCollection<E> {
  
    public interface TaggedElementsListCreator<E extends Comparable<E>> {
      public List<E> createList();
    }
    private static class DefaultListCreator<E extends Comparable<E>> implements TaggedElementsListCreator<E> {
      
      public List<E> createList() {
        return new LinkedList<E>();
      }
      
    };
    protected OrderedLinkedList<E> untaggedData;
    protected HashMap<String, List<E>> taggedData;
    protected HashSet<String> hiddenTags;
    private TaggedElementsListCreator<E> taggedElementsListCreator;

    
    public TaggedOrderedCollection() {
      this((Collection<E>)null);
    }
    
    public TaggedOrderedCollection(Collection<? extends E> c) {
      this(new DefaultListCreator<E>(), c);
    }
    
    public TaggedOrderedCollection(TaggedElementsListCreator<E> taggedElementsListCreator, Collection<? extends E> c) {
      this.taggedElementsListCreator = taggedElementsListCreator;
      if (c == null) {
        untaggedData = new OrderedLinkedList<E>();
      } else {
        untaggedData = new OrderedLinkedList<E>(c);
      }
      taggedData = new HashMap<String, List<E>>();
      hiddenTags = new HashSet<String>();
    }
    
    
    public boolean add(E o) {
      return untaggedData.add(o);
    };
    
    @Override
    public boolean addAll(Collection<? extends E> c) {
      return untaggedData.addAll(c);
    }
    
    @Override
    public boolean remove(Object o) {
      if( untaggedData.remove(o) ) {
        return true;
      }
      for( List<E> tagged : taggedData.values() ) {
        if( tagged.remove(o) ) {
          return true;
        }
      }
      return false; 
    }
    
    @Override
    public boolean removeAll(Collection<?> c) {
      boolean changed = untaggedData.removeAll(c);
      for( List<E> tagged : taggedData.values() ) {
        changed &= tagged.removeAll(c);
      }
      return changed;
    }
    
    @Override
    public Iterator iterator() {
      return new Iterator();
    }

    @Override
    public int size() {
      int size = untaggedData.size();
      for( List<E> ll : taggedData.values() ) {
        size += ll.size();
      }
      return size;
    }
    
    /**
     * Erweiterung des PeekIterator um Sortierbarkeit
     * "Note: this class has a natural ordering that is inconsistent with equals."
     */
    protected static class ComparablePeekIterator<E extends Comparable<E> > extends PeekIterator<E> implements Comparable<ComparablePeekIterator<E>> {

      private String tag;

      public ComparablePeekIterator(String tag, java.util.Iterator<E> iter) {
        super(iter);
        this.tag = tag;
      }
      public ComparablePeekIterator(String tag, ListIterator<E> iter) {
        super(iter);
        this.tag = tag;
      }

      @Override
      public String toString() {
        if( hasNext() ) {
          return "ComparablePeekIterator("+tag+","+peek()+")";
        } else {
          return "ComparablePeekIterator("+tag+")";
        }
      }

      
      public int compareTo(ComparablePeekIterator<E> o) {
        if( hasNext() ) {
          if( o.hasNext() ) {
            return peek().compareTo( o.peek() );
          } else {
            return -1;
          }
        } else {
          return +1;
        }
      }
      
      @Override
      final public boolean equals(Object o) {
        return super.equals(o); //Iteratoren k�nnen nie gleich sein -> nur Object-Identity als gleich ansehen
      }
      
      @Override
      final public int hashCode() {
        return super.hashCode(); //Iteratoren k�nnen nie gleich sein -> nur Object-Identity als gleich ansehen
      }
     
   }

    
    /**
     * Spezieller Iterator f�r die TaggedOrderedCollection, der nicht nur �ber remove Eintr�ge l�schen kann, 
     * sondern auch mit tag(String) Eintr�ge taggen kann und die weitere Iteration �ber hide(String) 
     * beeinflussen kann. 
     *
     */
    public class Iterator implements java.util.Iterator<E> {

      private ListIterator<E> untaggedIter;
      private HashMap<String,ComparablePeekIterator<E>> taggedIter;
      private ComparablePeekIterator<E> currentIter;
      private E current;
      private OrderedQueue<ComparablePeekIterator<E>> iters;
      
      private Iterator() {
        iters = new OrderedQueue<ComparablePeekIterator<E>>();
        if( ! hiddenTags.contains( null ) ) {
          if( ! untaggedData.isOrdered() ) {
            untaggedData.order();
          }
          untaggedIter = untaggedData.listIterator(); 
          iters.add( new ComparablePeekIterator<E>(null, untaggedIter) );
        }
        taggedIter = new HashMap<String,ComparablePeekIterator<E>>();
        for( Map.Entry<String, List<E>> entry : taggedData.entrySet() ) {
          if( ! hiddenTags.contains( entry.getKey() ) ) {
            ComparablePeekIterator<E> cpIter = new ComparablePeekIterator<E>( entry.getKey(), entry.getValue().listIterator() );
            taggedIter.put(entry.getKey(), cpIter );
            iters.add( cpIter );
          }
        }
      }
      
      public String toString() {
        return "TOC-Iterator("+iters+")";
      }
      
      public boolean hasNext() {
        if( iters.isEmpty() ) {
          return false;
        }
        return iters.peek().hasNext();
      }

      public E next() {
        currentIter = iters.peek();
        current = currentIter.next();
        iters.refresh();
        return current;
      }

      public void remove() {
        currentIter.remove();
      }

      /**
       * Taggen des zuletzt geholten Eintrags mit dem Tag tag.
       * @param tag
       */
      public void tag(String tag) {
        currentIter.remove();
        ComparablePeekIterator<E> tagIter = taggedIter.get(tag);
        if( tagIter == null ) {
          List<E> tagList = taggedData.get(tag);
          if( tagList == null ) {
            tagList = taggedElementsListCreator.createList();
            taggedData.put(tag,tagList);
          }
          tagList.add(current);
        } else {
          //Dadurch, dass der TagIter schon lange versteckt sein konnte, 
          //zeigt seine aktuelle Postion nicht unbedingt mehr zur richtigen Stelle, 
          //bei der nun eingetragen werden muss
          while( tagIter.hasNext() ) {
            if( tagIter.peek().compareTo(current) < 0 ) {
              tagIter.next();
            } else {
              break;
            }
          }
          ((ListIterator<E>)tagIter.iterator()).add(current);
        }
      }

      /**
       * Verstecken aller mit tag getaggten Eintr�ge in der weiteren Iteration
       * @param tag
       */
      public void hide(String tag) {
        ComparablePeekIterator<E> iter = taggedIter.get(tag);
        iters.remove(iter);
      }
      
    }

    /**
     * Liste aller nicht getaggten Eintr�ge
     * @return
     */
    public List<E> getUntagged() {
      return untaggedData.getData();
    }


    /**
     * Liste der mit Tag tag getaggten Eintr�ge
     * @param tag
     * @return
     */
    public List<E> getTagged(String tag) {
      List<E> list = taggedData.get(tag);
      if( list == null ) {
        return Collections.emptyList();
      } else {
        return Collections.unmodifiableList(list);
      }
    }


    /**
     * Verstecken der mit Tag tag getaggten Eintr�ge, so dass alle neuen Iteratoren diese Daten nicht
     * mehr anzeigen
     * @param tag
     */
    public void hide(String tag) {
      hiddenTags.add(tag);
    }


    /**
     * Entfernen des Tags tag aus der Liste der nicht anzuzeigenden Tags: Alle neuen Iteratoren k�nnen diese
     * Daten wieder anzeigen
     * @param tag
     */
    public void show(String tag) {
      hiddenTags.remove(tag);
    }

    /** 
     * Ausgabe aller sichtbaren Daten in sortierter Reihenfolge
     */
    @Override
    public String toString() {
      //return toString2();
      return super.toString();
    }

    /**
     * Ausgabe aller Daten aufgespalten nach einzelnen Tags
     */
    public String toString2() {
      String td = taggedData.toString();
      if( td.length() == 2 ) {
        return "{untagged="+untaggedData+"}";
      } else {
        return "{untagged="+untaggedData+", "+td.substring(1);
      }
    }

    /**
     * Entfernen leerer TagListen
     */
    public void removeEmptyTagLists() {
      java.util.Iterator<Entry<String, List<E>>> iter = taggedData.entrySet().iterator();
      while( iter.hasNext() ) {
        Entry<String, List<E>> entry = iter.next();
        if( entry.getValue().isEmpty() ) {
          iter.remove();
        }
      }
    }

  public Set<String> getTags() {
    return Collections.unmodifiableSet(taggedData.keySet());
  }
    
}
