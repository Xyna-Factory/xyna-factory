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
package com.gip.xyna.utils.collections;



import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;



/**
 * Liste die intern aus zwei Partitionen besteht. Elemente 0 bis n gehören zu Sorte E, die danach 
 * folgenden zu Sorte F.
 * Die erste Partition ist beschränkt. Falls zuviele Elemente in der Liste sind, werden die hinteren Elemente
 * zu Sorte F transformiert.
 * Falls die erste Partition schrumpft, werden Elemente aus der zweiten Partition wieder zurücktransformiert 
 * und in Partition E geladen.
 * 
 * Es wird nur garantiert, dass nie mehr Elemente von Sorte E in der Liste sind als über die Beschränkung
 * angegeben, es können aber weniger sein.
 * 
 * UseCase: Teure, dicke Objekte in Partition E, billige Identifier-Objekte in Partition F.
 * 
 * ACHTUNG: Damit contains, containsAll, remove, removeAll oder retainAll funktionieren können, 
 * muss E eine equals-Methode haben, die die Transformation E -&gt; F -&gt; E überlebt. 
 * D.h. die nicht überschriebene Default-Equals-Implementierung über den Identity-Hashcode 
 * funktioniert höchstwahrscheinlich nicht! 
 * 
 * @param <E> Objekte der ersten Partition
 * @param <F> Objekte der zweiten Partition
 */
public class PartitionedList<E, F> extends AbstractList<E> {


  public interface Transformator<E, F> {

    public E transformBack(F id);

    /**
     * Muss derart transformiert werden, dass man das ursprüngliche Objekt vergessen werden kann
     */
    public F transformTo(E object);

    /**
     * E-object geht nicht verloren, das transformierte Objekt wird nur vorrübergehend benötigt.
     */
    public F transformTemporarily(E object);
  }


  /**
   * falls unterhalb von lowerBoundSize, werden elemente nachgeladen, bis (upperBoundSize+lowerBoundSize)/2 elemente
   * erreicht werden. falls oberhalb von upperBoundSize, werden elemente transformiert, bis
   * (upperBoundSize+lowerBoundSize)/2 elemente erreicht werden.
   */
  private final int lowerBoundSize;
  private final int upperBoundSize;
  private final List<E> innerListTypeA; //beschränkt
  private final ReadWriteLock l = new ReentrantReadWriteLock();
  private final List<F> innerListTypeB; //TODO alternative implementierung könnte dieses objekt auch persistieren.
  private final Transformator<E, F> transformator;


  public PartitionedList(Transformator<E, F> transformator, int maxNumberOfElementsOfTypeA) {
    innerListTypeA = new LinkedList<E>();
    innerListTypeB = new LinkedList<F>();
    this.transformator = transformator;
    lowerBoundSize = (int) Math.round(maxNumberOfElementsOfTypeA * 0.65); //65% unter maxwert => nachladen auf ~82% befüllung
    upperBoundSize = maxNumberOfElementsOfTypeA;
  }
  
  public void addFirst(E o) {
    add(0, o);  //vorne einfügen, nicht in der der Nähe der Transformations-Grenze
  }

  @Override
  public void add(int index, E element) {
    l.writeLock().lock();
    try {
      if (index <= innerListTypeA.size()) {
        innerListTypeA.add(index, element);
        checkUpperBound();
      } else {
        addTransformed(getIndexTransformed(index), element);
      }
    } finally {
      l.writeLock().unlock();
    }
  }


  /**
   * nachladen, falls nötig
   */
  private void checkLowerBound() {
    if (innerListTypeA.size() <= lowerBoundSize) {
      if (innerListTypeB.size() > 0) {
        ListIterator<F> iterator = innerListTypeB.listIterator();
        while (iterator.hasNext() && innerListTypeA.size() < getTargetSizeAfterUnderFlow()) {
          F f = iterator.next();
          iterator.remove();
          E element = transformator.transformBack(f);
          innerListTypeA.add(element);
        }
      }
    }
  }


  private int getTargetSizeAfterUnderFlow() {
    return getTargetSizeAfterOverFlow();
  }


  /**
   * falls oberhalb von upperBoundSize, werden elemente persistiert
   */
  private void checkUpperBound() {
    if (innerListTypeA.size() >= upperBoundSize) {
      ListIterator<E> iter = innerListTypeA.listIterator(getTargetSizeAfterOverFlow());
      List<E> toTransform = new ArrayList<E>();
      while (iter.hasNext()) {
        E next = iter.next();
        toTransform.add(next);
        iter.remove();
      }
      addTransformedAll(toTransform);
    }
  }


  private int getTargetSizeAfterOverFlow() {
    return (lowerBoundSize + upperBoundSize) / 2;
  }


  private void addTransformed(int transformedIndex, E element) {
    F f = transformator.transformTo(element);
    innerListTypeB.add(transformedIndex, f);
  }


  private void addTransformedAll(List<E> toTransform) {
    int cnt = 0;
    for (E e : toTransform) {
      addTransformed(cnt++, e);
    }
  }


  @Override
  public E remove(int index) {
    l.writeLock().lock();
    try {
      if (index < innerListTypeA.size()) {
        E el = innerListTypeA.remove(index);
        checkLowerBound();
        return el;
      } else {
        return removeTransformed(getIndexTransformed(index));
      }
    } finally {
      l.writeLock().unlock();
    }
  }


  private E removeTransformed(int indexTransformed) {
    F f = innerListTypeB.remove(indexTransformed);
    return transformator.transformBack(f);
  }


  @Override
  public E set(int index, E element) {
    l.writeLock().lock();
    try {
      if (index < innerListTypeA.size()) {
        return innerListTypeA.set(index, element);
      } else {
        return setTransformed(getIndexTransformed(index), element);
      }
    } finally {
      l.writeLock().unlock();
    }
  }


  private E setTransformed(int indexTransformed, E element) {
    //FIXME kann man irgendwie einen persist-zugriff sparen?
    E old = transformator.transformBack(transformator.transformTemporarily(element));
    F f = transformator.transformTo(element);
    innerListTypeB.set(indexTransformed, f);
    return old;
  }


  @Override
  public E get(int index) {
    l.readLock().lock();
    try {
      if (innerListTypeA.size() > index) {
        return innerListTypeA.get(index);
      } else {
        return getTransformed(index);
      }
    } finally {
      l.readLock().unlock();
    }
  }


  private E getTransformed(int index) {
    F id = innerListTypeB.get(getIndexTransformed(index));
    return transformator.transformBack(id);
  }


  private int getSizeTransformed() {
    return innerListTypeB.size();
  }


  private int getIndexTransformed(int index) {
    return index - innerListTypeA.size();
  }


  @Override
  public int size() {
    return innerListTypeA.size() + getSizeTransformed();
  }


  private class Iter implements ListIterator<E> {

    //TODO comodification tests
    private ListIterator<E> innerIter;
    private ListIterator<F> idIter;


    public Iter(int index) {
      if (index < innerListTypeA.size()) {
        innerIter = innerListTypeA.listIterator(index);
      } else {
        idIter = innerListTypeB.listIterator(getIndexTransformed(index));
      }
    }


    public void add(E e) {
      if (innerIter != null) {
        innerIter.add(e);
        checkUpperBoundInIter();
      } else {
        F f = transformator.transformTo(e);
        idIter.add(f);
      }
    }


    private void checkUpperBoundInIter() {
      if (innerListTypeA.size() >= upperBoundSize) {
        //der cursor des iterators ist innerhalb der innerlist
        int indexBefore = innerIter.nextIndex();
        int targetSize = getTargetSizeAfterOverFlow();

        innerIter = innerListTypeA.listIterator(innerListTypeA.size());
        for (int i = 0; i < innerListTypeA.size() - targetSize; i++) {
          //previous elemente entfernen und an id-liste anhängen
          E e = innerIter.previous();
          innerIter.remove();
          F f = transformator.transformTo(e);
          innerListTypeB.add(0, f);
        }

        //zum ehemals aktuellen index springen.
        if (indexBefore > innerListTypeA.size()) {
          innerIter = null;
          idIter = innerListTypeB.listIterator(getIndexTransformed(indexBefore));
        } else {
          innerIter = innerListTypeA.listIterator(indexBefore);
        }
      }
    }


    public boolean hasNext() {
      if (innerIter != null) {
        if (innerIter.hasNext()) {
          return true;
        } else {
          return innerListTypeB.size() > 0;
        }
      }
      return idIter.hasNext();
    }


    public boolean hasPrevious() {
      if (idIter != null) {
        if (idIter.hasPrevious()) {
          return true;
        } else {
          return innerListTypeA.size() > 0;
        }
      }
      return innerIter.hasPrevious();
    }


    public E next() {
      if (innerIter != null) {
        if (innerIter.hasNext()) {
          return innerIter.next();
        } else {
          if (innerListTypeB.size() > 0) {
            innerIter = null;
            idIter = innerListTypeB.listIterator();
          } else {
            throw new NoSuchElementException();
          }
        }
      }
      F id = idIter.next();
      return transformator.transformBack(id);
    }


    public int nextIndex() {
      if (innerIter != null) {
        return innerIter.nextIndex();
      }
      return idIter.nextIndex() + innerListTypeA.size();
    }


    public E previous() {
      if (innerIter != null) {
        return innerIter.previous();
      }
      if (idIter.hasPrevious()) {
        F f = idIter.previous();
        return transformator.transformBack(f);
      } else {
        idIter = null;
        innerIter = innerListTypeA.listIterator(innerListTypeA.size());
        return innerIter.previous();
      }
    }


    public int previousIndex() {
      if (innerIter != null) {
        return innerIter.previousIndex();
      }
      return idIter.nextIndex() + innerListTypeA.size();
    }


    public void remove() {
      if (innerIter != null) {
        innerIter.remove();
        checkLowerBoundInIter();
      } else {
        idIter.remove();
      }
    }


    private void checkLowerBoundInIter() {
      if (innerListTypeA.size() <= lowerBoundSize) {
        if (innerListTypeB.size() > 0) {
          //ans ende des iterators gehen und dann elemente migrieren.
          
          //evtl sind in TypeB liste wenige elemente. maximal soviele migrieren, wie in listTypeB sind
          int numberOfElementsToMigrate = Math.min(getTargetSizeAfterUnderFlow() - innerListTypeA.size(), innerListTypeB.size());
          int startIndex = innerIter.nextIndex();
          innerIter = innerListTypeA.listIterator(innerListTypeA.size());
          for (int i = 0; i < numberOfElementsToMigrate; i++) {
            F f = innerListTypeB.remove(0);
            E e = transformator.transformBack(f);
            innerIter.add(e);
          }

          //zurück zum ursprünglichen index:
          innerIter = innerListTypeA.listIterator(startIndex);
        }
      }
    }


    public void set(E e) {
      if (innerIter != null) {
        innerIter.set(e);
      } else {
        F f = transformator.transformTo(e);
        idIter.set(f);
      }
    }

  }


  @Override
  public ListIterator<E> listIterator(int index) {
    return new Iter(index);
  }


  @Override
  public Iterator<E> iterator() {
    return new Iter(0);
  }
  
  
  public Iterator<E> getReadOnlyUntransformedIterator() {
    return new ReadOnlyUntransformedIterator();
  }
  
  
  private class ReadOnlyUntransformedIterator implements Iterator<E> {

    Iterator<E> innerIterator;
    
    private ReadOnlyUntransformedIterator() {
      innerIterator = innerListTypeA.iterator();
    }
    
    public boolean hasNext() {
      return innerIterator.hasNext();
    }

    public E next() {
      return innerIterator.next();
    }

    public void remove() {
    }
    
  }


}
