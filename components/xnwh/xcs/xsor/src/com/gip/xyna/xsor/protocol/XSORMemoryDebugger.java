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
package com.gip.xyna.xsor.protocol;



import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;



public class XSORMemoryDebugger {

  private final XSORMemory xsorMemory;


  public XSORMemoryDebugger(XSORMemory xsorMemory) {
    this.xsorMemory = xsorMemory;
  }


  public void listAllObjects(BufferedWriter w) {
    Set<Object> pks = xsorMemory.getAllKeys();
    try {
      w.write("size of xsormemory=");
      w.write(String.valueOf(pks.size()));
      w.newLine();
      for (Object pk : pks) {
        listObjectDontClose(w, pk);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  private static void writeHex(Appendable a, byte[] b) throws IOException {
    for (int i = 0; i < b.length; i++) {
      if (i > 0) {
        a.append(':');
      }
      int low = 0x0f & b[i];
      int high = 0x0f & (b[i] >> 4);
      a.append(Character.forDigit(high, 16));
      a.append(Character.forDigit(low, 16));
    }
  }


  private void listObjectDontClose(BufferedWriter w, Object pk) {
    try {
      int objectIndex = xsorMemory.getObjectIndex(pk);
      if (objectIndex < 0) {
        w.write("object not found");
        w.newLine();
        return;
      }
      writeObjectStateFormatted(w, pk, objectIndex);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private void writeObjectStateFormatted(BufferedWriter w, Object pk, int objectIndex) {
    try {
      char state = xsorMemory.getState(objectIndex);
      long modTime = xsorMemory.getModificationTime(objectIndex);
      long releaseTime = xsorMemory.getReleaseTime(objectIndex);
      int checkSum = xsorMemory.getCheckSum(objectIndex);
      int lockTID = xsorMemory.getLockState(objectIndex);
      w.write("pk=");
      if (pk.getClass().isArray() && pk.getClass().getComponentType() == byte.class) {
        writeHex(w, (byte[]) pk);
      } else {
        w.write(pk.toString());
      }
      w.write(", state=");
      w.write(state);
      w.write(", modTime=");
      w.write(String.valueOf(modTime));
      w.write(", releaseTime=");
      w.write(String.valueOf(releaseTime));
      w.write(", checkSum=");
      w.write(String.valueOf(checkSum));
      w.write(", lock=");
      w.write(String.valueOf(lockTID));
      w.newLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void listObject(BufferedWriter w, byte[] pk) {
    listObjectDontClose(w, xsorMemory.example.byteArrayToPk(pk));
  }


  public void writeQueueState(BufferedWriter w) {
    try {
      w.write("size of outgoing queue=");
      w.write(String.valueOf(xsorMemory.outgoingQueue.size()));
      w.newLine();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  public void writeQueueContent(BufferedWriter w) {
    
  }
  
  
  public void writeFreeListState(BufferedWriter w) {
    try {
      synchronized (xsorMemory.freeList) {
        int size = 0;
        int inconsistencies = 0;
        int currentIdx = xsorMemory.freeListTail.get();
        int[] allPkObjectIndices = getAllValues();
        w.write("the following objects were in an inconsistent state:");
        while (currentIdx != xsorMemory.freeListHead.get()) {
          boolean writeObjectState = false;
          if (xsorMemory.state[currentIdx] != 'N') {
            w.write("IllegalState: ");
            writeObjectState = true;
          }
          if (Arrays.binarySearch(allPkObjectIndices, currentIdx) >= 0) {
            w.write("contained in primary index: ");
            writeObjectState = true;
          }
          if (writeObjectState) {
            inconsistencies++;
            XSORPayload payload = xsorMemory.copyFromXSOR(currentIdx);
            writeObjectStateFormatted(w, payload.getPrimaryKey(), currentIdx);
          }
          currentIdx = xsorMemory.freeList[currentIdx];
          size++;
        }
        w.newLine();
        w.write(String.valueOf(inconsistencies));
        w.write(" inconsistencies found");
        w.newLine();
        w.write("freeListSize=");
        w.write(String.valueOf(size));
        w.newLine();
        w.write("expected primary index size=");
        w.write(String.valueOf(xsorMemory.size - size));
        w.write(" (+ current writeCopies)");
        w.newLine();
        w.write("primary index size=");
        w.write(String.valueOf(allPkObjectIndices.length));
        w.newLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public int[] getAllValues() {
    int[] values = xsorMemory.pkIndex.values();
    Arrays.sort(values);
    return values;
  }

  public Set<Object> getAllKeys() {
    return xsorMemory.getAllKeys();
  }

}
