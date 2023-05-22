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

package com.gip.xyna.xsor.common;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import com.gip.xyna.debug.Debugger;
import com.gip.xyna.xsor.indices.management.IndexSearchResult;
import com.gip.xyna.xsor.indices.tools.IntValueWrapper;
import com.gip.xyna.xsor.indices.tools.SingleIntValueWrapper;


public class XSORUtil {

  private static final Debugger debugger = Debugger.getInstance();
  // TODO performance (while creating 80000 objects in quick succession this was profiled @ #12)
  //      would java.util.zip.Adler32 be reliable enough? (4 times faster in quick main-method tests)
  private static final int PARALLELITY = 16;
  private static final Checksum[] checksum = new CRC32[PARALLELITY];
  static {
    for (int i = 0; i<16; i++) {
      checksum[i] = new CRC32();
    }
  }
  private static AtomicIntegerArray aia = new AtomicIntegerArray(PARALLELITY);
  
  public static void sleep(int sleepTimeMillisseconds) {
    try {
      Thread.sleep(sleepTimeMillisseconds);
    } catch (InterruptedException e) {
      // TODO logging
    }
  }


  public static int calculateChecksum(byte[] value, int fromIndex, int length) {
    int pos = (length ^ fromIndex) % PARALLELITY;
    while (!aia.compareAndSet(pos, 0, 1)) {
      pos = (pos + 1) % PARALLELITY;
    }
    try {
      //   Checksum checksum = new CRC32();
      checksum[pos].reset();
      checksum[pos].update(value, fromIndex, length);
      return (int) checksum[pos].getValue();
    } catch (Exception e) {
      debugger.error("Error calculating checksum.", e);
      return 0;
    } finally {
      aia.set(pos, 0);
    }
  }
  
  
  static public int getInt(int startIndex, byte[] message) {
    return ((message[startIndex] & 0x000000FF) << 24) + ((message[startIndex + 1] & 0x000000FF) << 16) + ((message[startIndex + 2] & 0x000000FF) << 8) + (message[startIndex + 3] & 0x000000FF);
  }

  public static long getLong(int startIndex, byte[] message) {
    return 
    ((message[startIndex] & 0x00000000000000FFl) << 56)     
    + ((message[startIndex + 1] & 0x00000000000000FFl) << 48) 
    + ((message[startIndex + 2] & 0x00000000000000FFl) << 40) 
    + ((message[startIndex + 3] & 0x00000000000000FFl) << 32)
    + ((message[startIndex+4] & 0x00000000000000FFl) << 24) 
    + ((message[startIndex + 5] & 0x00000000000000FFl) << 16) 
    + ((message[startIndex + 6] & 0x00000000000000FFl) << 8) 
    + (message[startIndex + 7] & 0x00000000000000FFl);

  }


  public static String prettyPrint(byte[] ba, int startIdx, int recordSize) {
    StringBuffer sb = new StringBuffer();

    int size = Math.min(startIdx + recordSize, ba.length);
    for (int i = startIdx; i < size; i++) {
      byte b = ba[i];
      if (b > 32 && b < 128) {
        sb.append((char) b);
      } else {
        sb.append("(" + String.format("%02X", (b & 0x000000FF)) + ")");
      }
    }
    return sb.toString();
  }


  public static String prettyPrint(byte... ba) {
    return prettyPrint(ba, 0, 150);
  }
  
  static public void setInt(int intValue, byte[] array, int index) {
    array[index+0] = (byte) (intValue >>> 24);
    array[index+1] = (byte) (intValue >>> 16);
    array[index+2] = (byte) (intValue >>> 8);
    array[index+3] = (byte) (intValue >>> 0);
  }
  
   static public void setLong(long longValue, byte[] array, int index) {
    array[index+0] = (byte) (longValue >>> 56);
    array[index+1] = (byte) (longValue >>> 48);
    array[index+2] = (byte) (longValue >>> 40);
    array[index+3] = (byte) (longValue >>> 32);
    array[index+4] = (byte) (longValue >>> 24);
    array[index+5] = (byte) (longValue >>> 16);
    array[index+6] = (byte) (longValue >>> 8);
    array[index+7] = (byte) longValue;
  }
   
   public static byte[] lreZeroEncode(byte[] data, int offset, int length) {
     //Daten mit Lauflaengencodierung fuer Nullen senden.
     //Der Fall mehr als 65535 Nullen am Stueck wird nicht beruecksichtigt
     ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream(length);
     int state=(data[offset]==0)?0:1;
     int from=offset;
     for(int position=offset;position<offset+length;position++){      
       if (state==0 && data[position]!=0){//0->1
         byteArrayOutputStream.write(0);
         
         byteArrayOutputStream.write((byte)((position-from)%256));//2 Byte f�r Lauflaenge
         byteArrayOutputStream.write((byte)((position-from)/256));
         from=position;
         state=1;
       } else if (state==1 && data[position]==0){//1->0
         byteArrayOutputStream.write(data,from,position-from);
         from=position;
         state=0;
       }
     }
     if (data[from]!=0){
       byteArrayOutputStream.write(data,from,offset+length-from);
     } else {
       byteArrayOutputStream.write(0);
       byteArrayOutputStream.write((byte)((offset+length-from)%256));
       byteArrayOutputStream.write((byte)((offset+length-from)/256));
     }
     byte[] lreEncoded= byteArrayOutputStream.toByteArray();
     return lreEncoded;
   }
   
   public static byte[] lreZeroDecode(byte[] buffer, int offset, int length) {
     ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream(65535);
     for(int i=offset; i<offset+length;i++){
       if (buffer[i]!=0){
         byteArrayOutputStream.write(buffer[i]);
       } else {
         int cntZero=(buffer[i+1] & 0xFF)+((buffer[i+2] & 0xFF)<<8);
         for (int j=0;j<cntZero;j++){
           byteArrayOutputStream.write(0);
         } 
         i=i+2;
       }
     }
     byte[] ret=byteArrayOutputStream.toByteArray();
     return ret;
   }
   
   

   public static int[] unsafeIntegerCollectionToArray(Collection<Integer> values) {
     int[] intValues = new int[values.size()];
     Iterator<Integer> it = values.iterator();
     for (int i = 0; i < values.size(); i++) {
       intValues[i] = it.next().intValue();
     }
     return intValues;
   }
   

  // implementation stolen from AbstractCollection.toArray()
  // faster than a defensive copy, but slower than an unsafe iteration 
  public static int[] safeIntegerCollectionToArray(Collection<Integer> values) {
    int[] r = new int[values.size()];
    Iterator<Integer> it = values.iterator();
    for (int i = 0; i < r.length; i++) {
      if (!it.hasNext()) {// fewer elements than expected
        return Arrays.copyOf(r, i);
      }
      r[i] = it.next();
    }
    return it.hasNext() ? finishToArray(r, it) : r;
  }


  private static int[] finishToArray(int[] r, Iterator<Integer> it) {
    int i = r.length;
    while (it.hasNext()) {
      int cap = r.length;
      if (i == cap) {
        int newCap = ((cap / 2) + 1) * 3;
        if (newCap <= cap) { // integer overflow
          newCap = Integer.MAX_VALUE;
        }
        r = Arrays.copyOf(r, newCap);
      }
      r[i++] = it.next().intValue();
    }
    // trim if overallocated
    return (i == r.length) ? r : Arrays.copyOf(r, i);
  }
  
  
  // faster than conversion to List & Collections.shuffle(List) with an about equal distribution
  public static int[] shuffleIntArray(int[] array) {
    if (array.length == 1) {
      return array;
    }
    Random rnd = new Random();
    // array might be a direct reference from MultiIntValueWrapper.values() ...we don't want to shuffle those
    int[] protectiveCopy = Arrays.copyOf(array, array.length);
    for (int i = 0; i < protectiveCopy.length; i++) {
      int position = i + rnd.nextInt(protectiveCopy.length - i);
      int temp = protectiveCopy[i];
      protectiveCopy[i] = protectiveCopy[position];
      protectiveCopy[position] = temp;
    }
    return protectiveCopy;
  }
  
  
  public static IndexSearchResult intValueWrapperValuesToIndexSearchResult(Collection<IntValueWrapper> values, int length) {
    int[] result = new int[length];
    Iterator<IntValueWrapper> iterator = values.iterator();
    int currentLength = 0;
    while (iterator.hasNext()) {
      int[] valueArray = iterator.next().getValues();
      if (result.length < currentLength + valueArray.length) {
        System.arraycopy(valueArray, 0, result, currentLength, result.length - currentLength);
        return new IndexSearchResult(result, false, false); // more than maxResults
      } else {
        System.arraycopy(valueArray, 0, result, currentLength, valueArray.length);
        currentLength += valueArray.length;
      }
    }
    if (currentLength < result.length) {
      return new IndexSearchResult(Arrays.copyOf(result, currentLength), false, true);
    } else {
      return new IndexSearchResult(result, false, true);
    }
  }
  
  
  public static IndexSearchResult intValueWrapperValuesToIndexSearchResult(Collection<IntValueWrapper> values) {
    int[] result = new int[(int) Math.min(values.size() * 3, Integer.MAX_VALUE)]; // good size?
    Iterator<IntValueWrapper> iterator = values.iterator();
    int resultLength = 0;
    while (iterator.hasNext()) {
      int[] valueArray = iterator.next().getValues();
      if (result.length < resultLength + valueArray.length) {
        int[] increasedResult = Arrays.copyOf(result, (((result.length / 2) + 1 ) *3) + valueArray.length);
        result = increasedResult;
      }
      System.arraycopy(valueArray, 0, result, resultLength, valueArray.length);
      resultLength += valueArray.length;
    }
    if (resultLength < result.length) {
      return new IndexSearchResult(Arrays.copyOf(result, resultLength), false, true);
    } else {
      return new IndexSearchResult(result, false, true);
    }
  }
  
  
  public static int[] intValueWrapperValuesToIntArray(Collection<IntValueWrapper> values) {
    int[] result = new int[(int) Math.min(values.size() * 3, Integer.MAX_VALUE)]; // good size?
    Iterator<IntValueWrapper> iterator = values.iterator();
    int resultLength = 0;
    while (iterator.hasNext()) {
      int[] valueArray = iterator.next().getValues();
      if (result.length < resultLength + valueArray.length) {
        int[] increasedResult = Arrays.copyOf(result, (((result.length / 2) + 1 ) *3) + valueArray.length);
        result = increasedResult;
      }
      System.arraycopy(valueArray, 0, result, resultLength, valueArray.length);
      resultLength += valueArray.length;
    }
    if (resultLength < result.length) {
      return Arrays.copyOf(result, resultLength);
    } else {
      return result;
    }
  }
  
  
}
