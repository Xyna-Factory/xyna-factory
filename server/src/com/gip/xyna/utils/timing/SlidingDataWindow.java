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
package com.gip.xyna.utils.timing;



import java.io.IOException;
import java.io.Serializable;



public class SlidingDataWindow implements Serializable {

  private static final long serialVersionUID = 1L;

  private transient Object lock;

  private final double[] data; //schubladen und ihr wert
  private double size; //summe über die werte der schubladen

  private final long width; //breite einer schublade
  private int startIndex; //index im array, der gerade dem start entspricht (0 bis data.length-1)
  private long startPosition; //absolute position, die dem linken rand der ersten schublade (also bei startIndex) entspricht


  /**
   * man hat n schubladen, jede schublade hat eine virtuelle breite.
   * nun kann man objekte mit absoluten werten einfügen, und diese werden immer derart eingefügt, dass
   * der bisher höchste eingefügte wert in der letzten schublade liegt. die anderen liegen relativ dazu in 
   * ihren schubladen. werte, die mehr als n*width kleiner sind als der aktuell größte wert, werden nicht 
   * eingetragen.
   * 
   * werte, die zu niedrig sind, werden automatisch entfernt.
   * 
   */
  public SlidingDataWindow(int n, long width) {
    data = new double[n];
    lock = new Object();
    startIndex = 0;
    size = Double.MIN_VALUE;
    this.width = width;
  }


  /**
   * fügt wert zu schublade hinzu
   * @return false, falls die position ausserhalb der gültigen range liegt 
   */
  public boolean put(long positionAbsolute, double value) {
    synchronized (lock) {
      int idx = getIndex(positionAbsolute, true);
      if (idx < 0) {
        return false;
      }
      size += value - data[idx];
      data[idx] = value;
      return true;
    }
  }


  /**
   * fügt wert relativ zu min/max position evtl verteilt auf mehrere schubladen hinzu.
   * @param minPosition included in range 
   * @param maxPosition included in range
   */
  public void addInRange(long minPosition, long maxPosition, double value) {
    if (minPosition > maxPosition) {
      throw new IllegalArgumentException("maxPosition must be greater than or equal to minPosition.");
    }
    synchronized (lock) {
      if (size == Double.MIN_VALUE) {
        getIndex(minPosition, true); //muss nach maxIdx nochmal ermittelt werden. wenn man es nicht ausführt, wird aber evtl nicht weit genug geslided.
      }

      int maxIdx = getIndex(maxPosition, true);
      if (maxIdx < 0) {
        //liegt vollständig ausserhalb des aktuellen datawindows
        return;
      }
      long overheadRight = (maxPosition + 1 - startPosition) % width;
      if (overheadRight == 0) {        
        overheadRight = width;
      }
      int minIdx = getIndex(minPosition, true);
      long overheadLeft;
      if (minIdx < 0) {
        //also muss der value reduziert werden, und minIdx auf die erste schublade gesetzt werden
        minIdx = startIndex;
        value -= value * (startPosition - minPosition) / (maxPosition - minPosition + 1);
        minPosition = startPosition;
        overheadLeft = width;
      } else if (maxIdx == minIdx) {
        data[maxIdx] += value;
        size += value;
        return;
      } else {
        overheadLeft = width - ((minPosition - startPosition) % width);
      }

      //value auf mehrere schubladen verteilen
      double valuePerPoint = 1.0 * value / (maxPosition - minPosition + 1);
      data[minIdx] += overheadLeft * valuePerPoint;
      data[maxIdx] += overheadRight * valuePerPoint;

      int l = data.length;
      if (maxIdx < minIdx) {
        //ringbuffer berücksichtigen -> später modula l nehmen
        maxIdx += l;
      }
      double valuePerBucket = width * valuePerPoint;
      for (int idx = minIdx + 1; idx < maxIdx; idx++) {
        data[idx % l] += valuePerBucket;
      }

      size += value;
    }
  }


  /**
   * ermittelt den aktuellen index und sorgt für konsistenz, falls das sliding window weiter slidet.
   * negativ, falls ausserhalb des gültigen bereichs
   */
  private int getIndex(long positionAbsolute, boolean slideWindowToPositionIfPossible) {
    if (size == Double.MIN_VALUE) {
      if (slideWindowToPositionIfPossible) {
        size = 0;
        return newStart(positionAbsolute);
      } else {
        return -1;
      }
    }
    if (positionAbsolute < startPosition) {
      return -1;
    }
    int indexAccordingToCurrentStartPosition = (int) ((positionAbsolute - startPosition) / width);
    if (indexAccordingToCurrentStartPosition < data.length) {
      //kein slide notwendig
      return (indexAccordingToCurrentStartPosition + startIndex) % data.length;
    }
    //=> index >= data.length
    if (slideWindowToPositionIfPossible) {
      //slide durchführen
      int indexSlide = indexAccordingToCurrentStartPosition - data.length + 1;
      if (indexSlide >= data.length) {
        reset();
        size = 0;
        return newStart(positionAbsolute);
      }
      for (int i = 0; i < indexSlide; i++) {
        size -= data[startIndex];
        data[startIndex] = 0;
        startIndex = (startIndex + 1) % data.length;
      }
      startPosition += width * indexSlide;
      return (startIndex - 1 + data.length) % data.length;
    }
    return -1;
  }


  private int newStart(long positionAbsolute) {
    startIndex = 0;
    //in der mitte anfangen
    int idx = data.length / 2;
    startPosition = positionAbsolute - idx * width;
    return idx;
  }


  /**
   * atomares äquivalent zu 
   * <pre>
   * put(positionAbsolute, get(positionAbsolute)+1);
   * </pre> 
   * @return false, falls die position ausserhalb der gültigen range liegt
   */
  public boolean increment(long positionAbsolute) {
    synchronized (lock) {
      int idx = getIndex(positionAbsolute, true);
      if (idx < 0) {
        return false;
      }
      data[idx]++;
      size++;
      return true;
    }
  }


  /**
   * atomares äquivalent zu 
   * <pre>
   * put(positionAbsolute, get(positionAbsolute)+value);
   * </pre> 
   * @return false, falls die position ausserhalb der gültigen range liegt
   */
  public boolean add(long positionAbsolute, double value) {
    synchronized (lock) {
      int idx = getIndex(positionAbsolute, true);
      if (idx < 0) {
        return false;
      }
      data[idx] += value;
      size += value;
      return true;
    }
  }


  /**
   * @return inhalt der schublade an der angegebenen position  oder {@link Double#MIN_VALUE} falls position
   * ausserhalb der gültigen range liegt
   */
  public double get(long positionAbsolute) {
    synchronized (lock) {
      int idx = getIndex(positionAbsolute, false);
      if (idx < 0) {
        return Double.MIN_VALUE;
      }
      return data[idx];
    }
  }


  /**
   * summe über die schubladen inhalte, ohne die daten zu ändern
   */
  public double size() {
    synchronized (lock) {
      return size == Double.MIN_VALUE ? 0 : size;
    }
  }


  /**
   * summe über die schubladen inhalte, wobei zu der übergebenen position geslided wird
   */
  public double size(long absolutePositionToSlideTo) {
    synchronized (lock) {
      getIndex(absolutePositionToSlideTo, true);
      return size == Double.MIN_VALUE ? 0 : size;
    }
  }


  public void reset() {
    synchronized (lock) {
      for (int i = 0; i < data.length; i++) {
        data[i] = 0;
      }
      size = Double.MIN_VALUE;
    }
  }


  /**
   * 
   * @return min und max von window
   */
  public long[] interval() {
    synchronized (lock) {
      return new long[] {startPosition, startPosition + data.length * width - 1};
    }
  }


  private void readObject(java.io.ObjectInputStream s) throws IOException, ClassNotFoundException {
    s.defaultReadObject();
    lock = new Object();
  }

}
