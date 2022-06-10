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
package com.gip.xyna.utils.collections.trees;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


/**
 *
 */
public class SimpleTreeSet<T extends SimpleTreeSet.TreeElement<T>> extends AbstractSet<T> {

  public interface TreeElement<E extends TreeElement<E>> {
    
    /**
     * Ist possibleChild ein Kind von this?  
     * (Ein TreeElement darf sich als eigenen Child bezeichnen: this.hasChild(this) darf true liefern)
     * @param possibleChild
     * @return
     */
    boolean hasChild( E possibleChild );
    
  }
  
  private TreeNode<T> root;
  private int size;
  private int modCounter;
  private List<T> helperList;
  private int helperListModCount;
  
  public SimpleTreeSet() {
    root = TreeNode.<T>root();
  }
  
  public SimpleTreeSet(Collection<T> c) {
    root = TreeNode.<T>root();
    for( T element :c ) {
      ++size;
      add(element);
    }
  }
  
  @Override
  public boolean add( T element ) {
    TreeNode<T> parent = root.getParent(element);
    boolean added = parent.addChild(element);
    if( added ) {
      ++size;
      ++modCounter;
    }
    return added;
  }
  
  @Override
  public boolean remove(Object o) {
    @SuppressWarnings("unchecked")
    T element = (T)o;
    TreeNode<T> parent = root.getParent(element);
    boolean removed = parent.removeChild(element);
    if( removed ) {
      --size;
      ++modCounter;
    }
    return removed;
  }
  
  
  @Override
  public int size() {
    return size;
  }

  @Override
  public Iterator<T> iterator() {
    return toUnmodifiableList().iterator();
  }
  
  public List<T> toUnmodifiableList() {
    if( helperList == null || helperListModCount != modCounter ) {
      List<T> list = new ArrayList<T>();
      root.addToList(list, true);
      helperList = Collections.unmodifiableList(list);
      helperListModCount = modCounter;
    }
    return helperList;
  }

  @Override
  public void clear() {
    modCounter = 0;
    root = TreeNode.<T>root();
    helperList = null;
    helperListModCount = 0;
  }
    
  public void sort() {
    root.sortRecursively(new TreeNodeComparator<T>());
    ++modCounter;
  }
  public void sort(Comparator<T> comparator) {
    root.sortRecursively(new TreeNodeComparator<T>(comparator));
    ++modCounter;
  }
  
  
  public T getParent(T element) {
    TreeNode<T> parent = root.getParent(element);
    if( parent != null && parent != root ) {
      return parent.element;
    }
    return null;
  }
  
  public List<T> getChildren(T element, boolean recursively) {
    List<T> list = new ArrayList<T>();
    TreeNode<T> parent = root.getParent(element);
    TreeNode<T> node = parent.getChild(element);
    if( node != null ) {
      node.addToList(list, recursively);
    } else {
      //keinen passenden Node zu element gefunden. Evtl. liegen im parent noch passende 
      for( TreeNode<T> n : parent.getChildrenOf(element) ) {
        list.add(n.element);
        if( recursively ) {
          n.addToList(list,recursively);
        }
      }
    }
    return list;
  }
  

  
  
  private static class TreeNode<T extends SimpleTreeSet.TreeElement<T>> {

    private T element;
    private List<TreeNode<T>> children;
    private TreeNode<T> parent;
    
    public TreeNode(T element) {
      this.element = element;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T extends SimpleTreeSet.TreeElement<T>> TreeNode<T> root() {
      TreeElement te = new TreeElement(){
        public boolean hasChild(TreeElement possibleChild) {
          return true; //RootNode nimmt alle als Kind auf
        }};
      TreeNode root = new TreeNode(te);
      return (TreeNode<T>)root;
    }
    
    @Override
    public String toString() {
      return "TreeNode("+element+")";
    }
    
    public TreeNode<T> getParent(T element) {
      if( children == null ) {
        return this; //keine Kinder -> selbst Parent
      }
      for( TreeNode<T> child : children ) {
        if( child.element.equals(element) ) {
          return this; //selbst parent von child
        }
        if( child.element.hasChild(element) ) {
          return child.getParent(element);
        }
      }
      return this; //kein passendes Kind  -> selbst Parent
    }
    
    /**
     * Liefert direktes Kind
     * @param element
     * @return
     */
    public TreeNode<T> getChild(T element) {
      if( children == null ) {
        return null; //keine Kinder
      }
      for( TreeNode<T> child : children ) {
        if( child.element.equals(element) ) {
          return child; //selbst parent von child
        }
      } 
      return null; //nichts gefunden
    }
    
    /**
     * Liefert alle direkten Kinder, die unter element einsortiert wären, wenn es das passende Kind dazu gäbe
     * @param element
     * @return
     */
    public List<TreeNode<T>> getChildrenOf(T element) {
      if( children == null ) {
        return Collections.emptyList(); //keine Kinder
      }
      List<TreeNode<T>> list = null;
      for( TreeNode<T> child : children ) {
        if( element.hasChild(child.element) ) {
          if( list == null ) {
            list = new ArrayList<TreeNode<T>>();
          }
          list.add(child);
        }
      }
      if( list == null ) {
        return Collections.emptyList(); //keine passenden Kinder
      } else {
        return list;
      }
    }
    
    public boolean addChild(T element) {
      if( children == null ) {
        children = new ArrayList<TreeNode<T>>();
      }
      
      if( getChild(element) != null ) {
        return false; //element schon enthalten!
      }
      
      TreeNode<T> node = new TreeNode<T>( element);
      node.parent = this;
      
      //evtl. sind nun Kinder von this Kinder von node
      Iterator<TreeNode<T>> iter = children.iterator();
      while( iter.hasNext() ) {
        TreeNode<T> child = iter.next();
        if( node.element.hasChild( child.element ) ) {
          if( node.children == null ) {
            node.children = new ArrayList<TreeNode<T>>();
          }
          node.children.add(child);
        
          child.parent = node;
          iter.remove();
        }
      }
      
      children.add(node);
      return true;
    }
    
    public boolean removeChild(T element) {
      if( children == null ) {
        return false; //kann element nicht enthalten
      }
      
      TreeNode<T> removed = getChild(element);
      if( removed == null ) {
        return false; //element existiert nicht
      }
      children.remove(removed);
      
      //Kinder von removed dürfen nicht verloren gehen
      if( removed.children != null ) {
        children.addAll(removed.children);
      }
      
      return true;
    }
    
    public void addToList(List<T> list, boolean recursively) {
      if( children == null ) {
        return;
      }
      for( TreeNode<T> child : children ) {
        list.add(child.element);
        if( recursively ) {
          child.addToList(list, true);
        }
      }
    }

    public void sortRecursively(TreeNodeComparator<T> comparator) {
      if( children == null ) {
        return;
      }
      Collections.sort(children, comparator);
      for( TreeNode<T> child : children ) {
        child.sortRecursively(comparator);
      }
    }
    
   @SuppressWarnings("unused")
   public TreeNode<T> getParent() {
     return parent;
   }

  }

  
  private static class TreeNodeComparator<T extends SimpleTreeSet.TreeElement<T>> implements Comparator<TreeNode<T>> {

    private Comparator<T> comparator;

    public TreeNodeComparator() {
    }
    
    public TreeNodeComparator(Comparator<T> comparator) {
      this.comparator = comparator;
    }

    @SuppressWarnings("unchecked")
    public int compare(TreeNode<T> tn1, TreeNode<T> tn2) {
      if( comparator != null ) {
        return comparator.compare(tn1.element, tn2.element);
      } else {
        return ((Comparable<T>)tn1.element).compareTo(tn2.element);
      }
    }
    
  }

  
}
