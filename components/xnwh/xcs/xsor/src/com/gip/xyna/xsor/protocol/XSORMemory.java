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



import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.Lock;

import org.apache.log4j.Logger;

import com.gip.xyna.debug.Debugger;
import com.gip.xyna.xsor.common.Interconnectable;
import com.gip.xyna.xsor.common.ReplyCode;
import com.gip.xyna.xsor.common.Replyable;
import com.gip.xyna.xsor.common.XSORUtil;
import com.gip.xyna.xsor.indices.XSORPayloadPrimaryKeyIndex;
import com.gip.xyna.xsor.indices.management.IndexManagement;
import com.gip.xyna.xsor.interconnect.InterconnectSender;



public class XSORMemory implements Interconnectable {
  
  XSORPayload example;
  
  private static final Logger logger=Logger.getLogger(XSORMemory.class.getName());

  private static final String CHARSET_ISO_8859_15 = "ISO-8859-15";// 8bit Encoding
  byte[][] chunk = null;
  int[] freeList;
  int size;
  int recordSize;
  AtomicInteger freeListTail;
  AtomicInteger freeListHead;
  protected char[] state;
  /**
   * wert pro array-index: 0 = unlocked. Thread-ID = locked.
   */
  AtomicIntegerArray lock;
  XSORPayloadPrimaryKeyIndex pkIndex;
  long payloadModificationTime[];
  long releaseTime[];
  int[] checksum;
  int type;
  static AtomicInteger corrId;
  WaitManagement waitManagement;
  IndexManagement indexManagement;
  LinkedBlockingDequeWithLockAccess outgoingQueue;
  private int[] writeCopy; //schreib kopien auf denen gearbeitet wird
  int recordsPerChunk;
  String name;
  boolean winner;


  private Debugger debugger = Debugger.getInstance();

  private volatile CountDownLatch syncLatch;
  private volatile int syncCorrId;

  private volatile boolean queueModeMerging=false;
  
  private HashMap<BigInteger, byte[]> mergeStateS=new HashMap<BigInteger, byte[]>();
  private HashMap<BigInteger, byte[]> mergeStateI=new HashMap<BigInteger, byte[]>();

  
  

  boolean isWinner() {
    return winner;
  }


  public WaitManagement getWaitManagement() {
    return waitManagement;
  }


  static {
    corrId = new AtomicInteger(17121966);
  }


  /**
   * freien index zurückgeben
   */
  void freeListPut(int objectIndex) {
    synchronized (freeList) {
      freeList[freeListHead.get()] = objectIndex;
      freeListHead.set(objectIndex);
      state[objectIndex] = 'N';
    }
  }


  /**
   * neuen freien index holen
   */
  public int freeListGet() {
    synchronized (freeList) {
      if (freeListHead.get() == freeListTail.get()) {
        throw new RuntimeException("table too small");
      }
      int temp = freeListTail.get();
      freeListTail.set(freeList[temp]);
      writeCopy[temp] = temp;
      return temp;
    }
  }
  
  private XSORMemory(int maxCount, IndexManagement indexManagement, XSORPayload example) {
    this.example = example;
    size = maxCount;
    state = new char[size];
    Arrays.fill(state, 'N');// not exists
    lock = new AtomicIntegerArray(size);
    payloadModificationTime = new long[size];
    releaseTime = new long[size];
    checksum = new int[size];
    outgoingQueue = new LinkedBlockingDequeWithLockAccess();
    writeCopy = new int[size];
    waitManagement = new WaitManagement();

    this.indexManagement = indexManagement;
    pkIndex = indexManagement.getXSORPayloadPrimaryKeyIndex(example.getTableName());
    
    freeList = new int[size];
    for (int i = 0; i < size; i++) {
      freeList[i] = (i +1) % size;
    }
    freeListTail = new AtomicInteger(0);
    freeListHead = new AtomicInteger(size - 1); 
    xsorMemoryChecker=new XSORMemoryChecker(this);
    Thread t=new Thread(xsorMemoryChecker);
    t.setDaemon(true);
    t.start();

}
  
  public XSORMemory(int recordSize, int maxCount, int objectType, String name, boolean winner, XSORPayload example, IndexManagement indexManagement) {
    this(maxCount, indexManagement, example);

    this.name = name;
    type = objectType;
    this.winner = winner;
    this.recordSize = recordSize;

    //chunk init
    int testfactor = 10;
    this.recordsPerChunk = (Integer.MAX_VALUE / testfactor / 100 * 99) / recordSize;
    
    int chunkCnt = size / recordsPerChunk + 1;
    chunk = new byte[chunkCnt][];
    int remainingRecordCnt = size;
    for (int i = 0; i < chunkCnt; i++) {
      int recs = Math.max(0, Math.min(recordsPerChunk, remainingRecordCnt));
      remainingRecordCnt -= recs;
      chunk[i] = new byte[recs * recordSize];
    }
  }


  public XSORMemory(XSORMemory xsorMemory, IndexManagement indexManagement) {
    this(xsorMemory.size, indexManagement, xsorMemory.example);
    
    this.name = xsorMemory.name;
    type = xsorMemory.type;
    this.winner = xsorMemory.winner;
    this.recordSize = xsorMemory.recordSize;

    //chunk init
    this.recordsPerChunk = xsorMemory.recordsPerChunk;
    chunk = xsorMemory.chunk;
  }


  public XSORPayload getExample() {
    return example;
  }
  

  static private int getNextCorrId() {
    final int ret = corrId.incrementAndGet();// Atomizitaet von incrementAndGet() garantiert Eindeutigkeit
    if (ret == Integer.MAX_VALUE - 171266) {// potentiell gibt corrId auch Werte> Integer.MAX_VALUE-171266 zurueck. Auch
                                           // diese sind eindeutig
      corrId.set(17121966); //macht nur ein thread
    }

    return ret;
  }


  public byte[] getBytes(int objectIndex, int offset, int length) {
    byte[] data = getData(objectIndex);
    int offsetBytes = getOffsetBytes(objectIndex);
    return Arrays.copyOfRange(data, offsetBytes + offset, offsetBytes + offset + length);
  }


  public int getInt(int objectIndex, int offset) {
    byte[] data = getData(objectIndex);
    int offsetBytes = getOffsetBytes(objectIndex);
    int startIndex = offsetBytes + offset;
    return ((data[startIndex] & 0x000000FF) << 24) + ((data[startIndex + 1] & 0x000000FF) << 16) + ((data[startIndex + 2] & 0x000000FF) << 8) + (data[startIndex + 3] & 0x000000FF);
  }


  public long getLong(int objectIndex, int offset) {
    byte[] data = getData(objectIndex);
    int offsetBytes = getOffsetBytes(objectIndex);
    int startIndex = offsetBytes + offset;
    return ((data[startIndex] & 0x00000000000000FFl) << 56) + ((data[startIndex + 1] & 0x00000000000000FFl) << 48) + ((data[startIndex + 2] & 0x00000000000000FFl) << 40) + ((data[startIndex + 3] & 0x00000000000000FFl) << 32) + ((data[startIndex + 4] & 0x00000000000000FFl) << 24) + ((data[startIndex + 5] & 0x00000000000000FFl) << 16) + ((data[startIndex + 6] & 0x00000000000000FFl) << 8) + (data[startIndex + 7] & 0x00000000000000FFl);
  }


  public String getString(int objectIndex, int offset) {
    byte[] data = getData(objectIndex);
    int offsetBytes = getOffsetBytes(objectIndex);
    int startIndex = offsetBytes + offset;
    int length = ((data[startIndex] & 0x000000FF) << 8) + ((data[startIndex + 1] & 0x000000FF));
    byte[] b = Arrays.copyOfRange(data, offsetBytes + offset + 2, offsetBytes + offset + 2 + length);
    // return new String(b,0,0,b.length);//deprecated, aber deutlich schneller
    return new String(b, Charset.forName(CHARSET_ISO_8859_15));
  }


  public void setBytes(int objectIndex, int offset, byte[] value) {
    lockObject(objectIndex);
    try {
      if (state[objectIndex] == 'S' || state[objectIndex] == 'I') {
        return;
      }
      if (state[objectIndex] == 'E') {
        setState(objectIndex, 'M');
      }

      int writeObjectIndex = writeCopy[objectIndex];
      byte[] data = getData(writeObjectIndex);
      int offsetBytes = getOffsetBytes(objectIndex);
      System.arraycopy(value, 0, data, offsetBytes + offset, value.length);
    }
    finally {
      unlockObject(objectIndex);
    }
  }


  public void setInt(int objectIndex, int offset, int value) {
    lockObject(objectIndex);
    try {
      if (state[objectIndex] == 'S' || state[objectIndex] == 'I') {
        return;
      }
      if (state[objectIndex] == 'E') {
        setState(objectIndex, 'M');
      }
      int writeObjectIndex = writeCopy[objectIndex];
      byte[] data = getData(writeObjectIndex);
      int offsetBytes = getOffsetBytes(objectIndex);
      int startIndex = offsetBytes + offset;
      XSORUtil.setInt(value, data, startIndex);
    } finally {
      unlockObject(objectIndex);
    }
  }


  public void setLong(int objectIndex, int offset, long value) {
    lockObject(objectIndex);
    try {
      if (state[objectIndex] == 'S' || state[objectIndex] == 'I') {
        return;
      }
      if (state[objectIndex] == 'E') {
        setState(objectIndex, 'M');
      }
      int writeObjectIndex = writeCopy[objectIndex];
      byte[] data = getData(writeObjectIndex);
      int offsetBytes = getOffsetBytes(objectIndex);
      int startIndex = offsetBytes + offset;
      XSORUtil.setLong(value, data, startIndex);
    } finally {
      unlockObject(objectIndex);
    }
  }


  public void setString(int objectIndex, int offset, String value) {// auf maximallaenge beschränken
    lockObject(objectIndex);
    try {
      if (state[objectIndex] == 'S' || state[objectIndex] == 'I') {
        return;
      }
      if (state[objectIndex] == 'E') {
        setState(objectIndex, 'M');
      }
      int writeObjectIndex = writeCopy[objectIndex];
      byte[] data = getData(writeObjectIndex);
      int offsetBytes = getOffsetBytes(objectIndex);
      int startIndex = offsetBytes + offset;
      try {
        byte[] toCopy = value.getBytes(CHARSET_ISO_8859_15);
        System.arraycopy(toCopy, 0, data, startIndex + 2, toCopy.length);
        data[startIndex] = (byte) (toCopy.length >>> 8);
        data[startIndex + 1] = (byte) (toCopy.length);
      }
      catch (UnsupportedEncodingException e) {
        logger.error("CHARSET_ISO_8859_15 NOT SUPPORTED",e);
      }
    } finally {
      unlockObject(objectIndex);
    }
  }


  void setState(int objectIndex, char s) {
    state[objectIndex] = s;
    state[writeCopy[objectIndex]] = s;
  }


  public char getState(int objectIndex) {
    return state[objectIndex];
  }
  
  public char getWritecopyState(int objectIndex) {
    return state[writeCopy[objectIndex]];
  }

  public static class LogLockTimeThreadLocal extends ThreadLocal<Long> {
    
    private final String type;
    
    public LogLockTimeThreadLocal(String type) {
      this.type = type;
    }
    
    @Override
    protected Long initialValue() {
      return 0L;
    }
    
    public void logAndRemove(int id) {
      if (logger.isDebugEnabled()) {
        long took = (System.currentTimeMillis() - get());
        logger.debug(type + "lock " + id + " held for " + took + "ms");
        if (took > 10 && logger.isTraceEnabled()) {
          logger.trace("current stack", new Exception());
        }
        remove();
      }
    }

    public void set() {
      if (logger.isDebugEnabled()) {
        set(System.currentTimeMillis());
      }
    }
  }

  private LogLockTimeThreadLocal lastLock = new LogLockTimeThreadLocal("");
  private static final int timeOutInS = 60;
  private static final int maxSleepInMs = 50;
  

  void lockObject(int objectIndex) {
    lockObject(objectIndex, false);
  }
  
  
  /**
   * versucht das objekt zu locken. wenn dies nicht klappt, wird das lock
   * hart geunlocked und mit der eigenen tid gelockt. 
   */
  void lockObjectForceUnlock(int objectIndex) {
    lockObject(objectIndex, true);
  }


  private void lockObject(int objectIndex, boolean forceUnlock) {
    int i = 0;
    int sleepInMs = 0;
    long timeout = 0;
    long lockedBy = 0; //ungelockt
    
    //Der int Cast ist okay, falls ownTid!=0 . Das Eintragen einer (int)ThreadId satt z.B. 1 erleichert evtl. mal Debugging.
    int ownTid = (int) Thread.currentThread().getId();
    if (ownTid == 0) {
      ownTid = 1;
    }
    
    while (!lock.compareAndSet(objectIndex, 0, ownTid)) {
      i++;
      
      long currentTime = System.currentTimeMillis();
      if (i == 1) {
        timeout = currentTime + timeOutInS * 1000;
      }
      
      int tid = lock.get(objectIndex);
      int toTimeout = (int) (timeout - currentTime);
      if (toTimeout <= 0) {
        if (forceUnlock) {
          logger.error("Long waiting for Object lock " + name + ":" + objectIndex + ", held by unknown thread (tid="
                       + ownTid + ") freeing lock");
          lock.compareAndSet(objectIndex, tid, 0); //unlock bei ungeaenderter tid
          continue; // continue to try to get it ourself
        } else {
          throw new RuntimeException("Probable deadlock detected, have waited " + timeOutInS + " seconds for lock. Abort.");
        }
      }

      tid = lock.get(objectIndex);
      if (lockedBy != tid) {
        if (lockedBy != 0) {
          //jemand anderes hat das lock als vorher -> es geht vorwärts -> gleich wieder probieren
          sleepInMs = 0;
        }
        lockedBy = tid;
      }
      
      
      if (i % 100 == 1 && logger.isDebugEnabled()) {
        //ausführliches logging
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        if (tid != 0) { // 0 happens if the lock was released since the lock.compareAndSet
          ThreadInfo ti = tbean.getThreadInfo(tid, 1000);
          if (ti != null) {
            logger.debug("Long waiting for Object lock " + name + ":" + objectIndex + ", held by thread "
                + ti.getThreadName());
            if (logger.isTraceEnabled()) {
              Exception e = new Exception();
              e.setStackTrace(ti.getStackTrace());
              logger.trace("stack of thread holding lock: ", e);
            }
          } else {
            logger.debug("Long waiting for Object lock " + name + ":" + objectIndex + ", held by unknown thread (tid=" + tid + ")");
          }
        }
      }

      // ok, da es i.A. normalerweise nicht blockiert => TODO:Handbuch:
      if (sleepInMs > 5) {
        XSORUtil.sleep(Math.min(sleepInMs, toTimeout));
      }
      sleepInMs = Math.min(maxSleepInMs, sleepInMs + 5);
    }

    if (logger.isTraceEnabled()) {
      logger.trace("tid=" + ownTid + " locked " + objectIndex + ".");
    }
    lastLock.set();
  }


  public void corruptObject(int objectIndex) {

    final int sleepInMs = 100;
    final int timeOutInS = 60;
    int i = 0;

    //Der int Cast ist okay, falls ownTid!=0 . Das Eintragen einer (int)ThreadId satt z.B. 1 erleichert evtl. mal Debugging.
    int ownTid = 4712;
    if (ownTid == 0) {
      ownTid = 1;
    }
    while (!lock.compareAndSet(objectIndex, 0, ownTid)) {
      i++;
      if (i % 100 == 1 && logger.isDebugEnabled()) {
        //ausführliches logging
        ThreadMXBean tbean = ManagementFactory.getThreadMXBean();
        long tid = lock.get(objectIndex);
        if (tid != 0) { // 0 happens if the lock was released since the lock.compareAndSet
          ThreadInfo ti = tbean.getThreadInfo(tid, 1000);
          if (ti != null) {
            logger.debug("Long waiting for Object lock " + name + ":" + objectIndex + ", held by thread "
                + ti.getThreadName());
            if (logger.isTraceEnabled()) {
              Exception e = new Exception();
              e.setStackTrace(ti.getStackTrace());
              logger.trace("stack of thread holding lock: ", e);
            }
          } else {
            logger.debug("Long waiting for Object lock " + name + ":" + objectIndex + ", held by unknown thread (tid="
                + tid + ")");
          }
        }
      }
      logger.debug("Waiting for Lock");
      XSORUtil.sleep(sleepInMs);// ok, da es i.A. normalerweise nicht blockiert => Handbuch:
      if (i >= timeOutInS * 1000 / sleepInMs) {
        throw new RuntimeException("Probable deadlock detected, have waited " + timeOutInS
            + " seconds for lock. Abort.");
      }
    }
    if (logger.isTraceEnabled()) {
      logger.trace("tid=" + ownTid + " locked " + objectIndex + ".");
    }
  }
 
  
  
  public boolean testPkIndexPut(Object objectID, int objectIndex) {
    return pkIndex.put(objectID, objectIndex);
  }
  
  
  void removeFromPkIndex(Object objectID, int objectIndex) {
    pkIndex.delete(objectID, objectIndex);
  }


  int getObjectIndex(Object objectID) {
    return pkIndex.getUniqueValueForKey(objectID);
  }


  void unlockObject(int objectIndex) {
    lock.set(objectIndex, 0);
    lastLock.logAndRemove(objectIndex);
  }


  public void clear(int objectIndex) {
    byte[] data = getData(objectIndex);
    int startIndex = getOffsetBytes(objectIndex);
    Arrays.fill(data, startIndex, startIndex + recordSize, (byte) 0);
  }


  long updateChecksumAndModificationtime(int objectIndex) {
    objectIndex = writeCopy[objectIndex];
    byte[] data = getData(objectIndex);
    checksum[objectIndex] = XSORUtil.calculateChecksum(data, getOffsetBytes(objectIndex), recordSize);
    long modTime = System.currentTimeMillis();
    payloadModificationTime[objectIndex] = modTime;
    return modTime;
  }

  void updateChecksumAndModificationtimeAndReleaseTime(int objectIndex, long modificationTime, long releaseTimeNew) {
    objectIndex = writeCopy[objectIndex];
    updateChecksumAndModificationtime(objectIndex);
    payloadModificationTime[objectIndex] = modificationTime;
    updateReleaseTime(objectIndex, releaseTimeNew);
  }


  int sendMessage(Object objectId, int objectIndex, char newState, boolean payloadNeeded) {
    return sendMessage(objectId, objectIndex, newState, payloadNeeded, false);
  }
  
  private int sendMessage(Object objectId, int objectIndex, char newState, boolean payloadNeeded, boolean insertAtHeadOfQueue) {

    int backupIndex = objectIndex;
    objectIndex = writeCopy[objectIndex];
    byte[] objectIdAsByteArray=example.pkToByteArray(objectId);
    byte[] data = getData(objectIndex);
    int objectOffsetBytes = getOffsetBytes(objectIndex);
    int transactionCorrId = getNextCorrId();
    byte[] ret = null;
    int checkSum=0;
    
    byte[] lreEncodedPayload=null;
    int sizeTilPayload = 1 + 4 + 4 + 4 + 8 + 8 + 8 + 4+ objectIdAsByteArray.length;
    if (payloadNeeded) {
      lreEncodedPayload=XSORUtil.lreZeroEncode(data, objectOffsetBytes, recordSize);
      checkSum=XSORUtil.calculateChecksum(data, objectOffsetBytes, recordSize);
      ret = new byte[sizeTilPayload+ lreEncodedPayload.length+4];
    }
    else {
      // System.out.print("npayload"+state[objectIndex]+newState);
      ret = new byte[sizeTilPayload];
    }

    ret[0] = (byte) newState;
    XSORUtil.setInt(type, ret, 1);
    XSORUtil.setInt(transactionCorrId, ret, 5);
    XSORUtil.setInt(checksum[backupIndex], ret, 9);
    XSORUtil.setLong(payloadModificationTime[objectIndex], ret, 13);
    XSORUtil.setLong(releaseTime[backupIndex], ret, 21);
    XSORUtil.setLong(releaseTime[objectIndex], ret, 29);
    XSORUtil.setInt(objectIdAsByteArray.length, ret, 37);    
    System.arraycopy(objectIdAsByteArray, 0, ret, 41, objectIdAsByteArray.length);    

    if (payloadNeeded) {
      System.arraycopy(lreEncodedPayload, 0, ret, sizeTilPayload, lreEncodedPayload.length);
      XSORUtil.setInt(checkSum, ret, sizeTilPayload + lreEncodedPayload.length);
    }

    waitManagement.register(transactionCorrId, objectIndex);

    //merging von SIS->S. ISI wird nicht gemerged.
    boolean merged = false;
    if (queueModeMerging && newState == 'S') {
      BigInteger key = new BigInteger(objectIdAsByteArray);
      synchronized (mergeStateS) {
        if (mergeStateS.containsKey(key) && mergeStateI.containsKey(key)) {
          if (logger.isTraceEnabled()) {
            logger.trace("merging SIS->S for objectId=" + XSORUtil.prettyPrint(objectIdAsByteArray));
          }
          outgoingQueue.remove(mergeStateI.remove(key)); //altes I objekt entfernen
          
          byte[] oldMsg = mergeStateS.get(key);
          if (insertAtHeadOfQueue) {
            //dann wird auch SIS->S gemerged, aber das bereits in der queue vorhandene objekt ist das überbleibende objekt, weil es neuer ist
            byte[] tmp = oldMsg;
            oldMsg = ret;
            ret = tmp;
            //value von alter nachricht! z.b. wenn bei sync_master ein grab-release passiert, welches keine payload verschickt
            payloadNeeded = ret.length > sizeTilPayload;
          }
          
          int oldChecksum = XSORUtil.getInt(9, oldMsg);
          long oldBackupReleaseTime = XSORUtil.getLong(21, oldMsg);
          XSORUtil.setInt(oldChecksum, ret, 9);
          XSORUtil.setLong(oldBackupReleaseTime, ret, 21);
          outgoingQueue.remove(oldMsg);

          //evtl hat der alte übergang nach S eine payload gehabt und der neue nicht => übertragen
          if (!payloadNeeded && oldMsg.length > sizeTilPayload) {
            //altes bytearray wiederverwenden, das hat die richtige länge. einfach die ersten sizeTilPayload bytes überschreiben
            byte[] tmp = ret;
            ret = oldMsg;
            System.arraycopy(tmp, 0, ret, 0, sizeTilPayload);
          }
          mergeStateS.put(key, ret);
          merged = true;
          //Wegen Clusterzustandsübergang sollte im Merging Mode keiner auf einen Eintrag warten
        } else if (!mergeStateS.containsKey(key) && !mergeStateI.containsKey(key)) {
          mergeStateS.put(key, ret);
        } else if (mergeStateS.containsKey(key)) {
          //nur S vorhanden -> kann passieren, wenn durch die synchronisierung S-nachrichten eingestellt werden
          //beispiel:
          //       1. thread1 grab -> I, wird nicht eingetragen, weil kein S vorhanden
          //       2. thread2 (sync as sync_master), insertAsHead (S)
          //       3. thread1 release -> S
          //       (oder 2. und 3. vertauscht)
          //zum mergen fehlt die I nachricht, einfach die neuere der beiden S nachrichten behalten
          if (insertAtHeadOfQueue) {
            //ok, alte merging info behalten
          } else {
            mergeStateS.put(key, ret);
          }
        }
        //else nur I vorhanden -> sollte nicht vorkommen, weil I nur eingetragen wird, nachdem S vorhanden ist
      }
    } else if (queueModeMerging && newState == 'I') {
      if (logger.isTraceEnabled()) {
        logger.trace("merging I for objectId=" + XSORUtil.prettyPrint(objectIdAsByteArray));
      }
      BigInteger key = new BigInteger(objectIdAsByteArray);
      synchronized (mergeStateS) {
        if (mergeStateS.containsKey(key) && !mergeStateI.containsKey(key)) {
          mergeStateI.put(key, ret);
        }
        //else beides vorhanden -> sollte nicht vorkommen, bedeutet II
        //else nur I vorhanden -> sollte nicht vorkommen, bedeutet II
        //else keines vorhanden -> nicht eintragen, ISI wird nicht gemerged!
      }
    }

    if (insertAtHeadOfQueue) {
      if (!merged) {
        //wenn man gemerged hat, ist die korrekte nachricht noch in der queue
        sendQueueAtFirst(ret);
      }
    } else {
      sendQueue(ret);
    }

    return transactionCorrId;
  }

  /**
   * verschickt spezial-nachricht, die in {@link #processIncommingRequest(byte[], Replyable)} auch separat behandelt wird. 
   */
  private int sendSpecialMessage(char specialChar) {
    byte[] ret = new byte[1 + 4 + 4]; //nur state als kennzeichen für SYNC-Message, type für dispatching und corrId für correlation.
    int transactionCorrId = getNextCorrId();
    ret[0] = (byte) specialChar;
    XSORUtil.setInt(type, ret, 1);
    XSORUtil.setInt(transactionCorrId, ret, 5);
    sendQueue(ret);
    return transactionCorrId;
  }
  
  private int sendSpecialMessageAtFirst(char specialChar) {
    byte[] ret = new byte[1 + 4 + 4]; //nur state als kennzeichen für SYNC-Message, type für dispatching und corrId für correlation.
    int transactionCorrId = getNextCorrId();
    ret[0] = (byte) specialChar;
    XSORUtil.setInt(type, ret, 1);
    XSORUtil.setInt(transactionCorrId, ret, 5);
    sendQueueAtFirst(ret);
    return transactionCorrId;
  }



  public int getOffsetInChunk(int objectIndex) {
    int objectIndexInChunk = objectIndex % recordsPerChunk;
    return objectIndexInChunk;
  }
  
   public int getOffsetBytes(int objectIndex) {
     return getOffsetInChunk(objectIndex)*recordSize;
  }


  /**
   * finde dasjenige byte[], welches für den objectIndex zuständig ist. 
   */
  public byte[] getData(int objectIndex) {
    byte[] data = chunk[objectIndex / recordsPerChunk];
    return data;
  }


  void sendReplyMessage(ReplyCode replyCode, int corrId, int end, Replyable reply, String name) {
    // System.out.println("REPLY"+replyCode+":"+name+":"+end);
    byte[] ret = new byte[4 + 4];
    int ordinal = replyCode.ordinal();
    XSORUtil.setInt(ordinal, ret, 0);
    if (logger.isTraceEnabled()) {
      logger.trace(name+" sending reply: corrid=" + corrId + ", replyCode=" + replyCode+",reason="+end);
    }
    XSORUtil.setInt(corrId, ret, 4);
    reply.offer(ret, corrId);
  }


  private void sendQueue(byte[] message) {
    outgoingQueue.offer(message); 
    InterconnectSender.notifyQueue();
  }
  
  private void sendQueueAtFirst(byte[] message) {
    outgoingQueue.addFirst(message); 
    InterconnectSender.notifyQueue();
  }



  boolean hasOutstandingTransactions(Object objectID) {
    //in outgoing queue? oder in lastsent-puffer in interconnect?    
    return outgoingQueue.containsObjectId(example.pkToByteArray(objectID));
  }


  /**
   * aktualisiert die releasetime
   */
  void updateForRelease(Integer objectIndex, long releaseTimeNew) {
    releaseTime[writeCopy[objectIndex]] = releaseTimeNew;
  }

  public int getInternalIdOfWriteCopy(int objectIndex) {
    return writeCopy[objectIndex];
  }


  /**
   * gibt den backup-index in der writecopy wieder frei 
   */
  int releaseBackup(int objectIndex) {
    if (objectIndex == writeCopy[objectIndex]) {
      return objectIndex;
    }

    freeListPut(objectIndex);
    return writeCopy[objectIndex];
  }

  void updateReleaseTime(Integer objectIndex, long releaseTimeNew) {
    releaseTime[objectIndex] = releaseTimeNew;
  }


  void copyForWrite(Integer objectIndex) {
    int newIndex = freeListGet();
    System.arraycopy(getData(objectIndex), getOffsetBytes(objectIndex), getData(newIndex), getOffsetBytes(newIndex), recordSize);
    payloadModificationTime[newIndex] = payloadModificationTime[objectIndex];
    releaseTime[newIndex] = releaseTime[objectIndex];
    checksum[newIndex] = checksum[objectIndex];
    writeCopy[objectIndex] = newIndex;
    writeCopy[newIndex] = newIndex;
    state[writeCopy[objectIndex]] = state[objectIndex];
  }


  void delete(int objectIndex) {
    if (objectIndex != writeCopy[objectIndex]) {
      freeListPut(writeCopy[objectIndex]);
    }
    freeListPut(objectIndex);
  }


  public int getCheckSum(int objectIndex) {
    return checksum[objectIndex];
  }


  public long getReleaseTime(int objectIndex) {
    //releasetime gibts nicht extra für writecopy, weil beim releasen das backup ja verschwindet
    //es schadet aber auch nichts, getReleaseTime mit dem writecopy index aufzurufen
    return releaseTime[objectIndex];
  }

  /**
   * gibt neuen index gelockt zurück, falls newIndex != oldIndex => muss noch geunlockt werden.
   */
  int setPayloadAndUpdateIndexAndLockNewIndex(Object objectID, int objectIndex, byte[] payload, long modTime, long relTime, boolean create) {
    int newIndex = objectIndex;
    XSORPayload oldPayload = null;
    if (!create) {// Im Falle create kann der objectIndex weiter verwendet werden, weil er gerade erst erzeugt wurde 
      rollback(objectIndex); // just in case there was a write copy
      newIndex = freeListGet();
      oldPayload = copyFromXSORWithUnadjustedIndex(objectIndex);
    }
    byte[] data = getData(newIndex);    
    int offsetBytes = getOffsetBytes(newIndex);
    System.arraycopy(payload, 0, data, offsetBytes, payload.length);
    checksum[newIndex] = XSORUtil.calculateChecksum(data, offsetBytes, recordSize);
    payloadModificationTime[newIndex] = modTime;
    releaseTime[newIndex] = relTime;
    state[newIndex] = state[objectIndex];
    
    if (create) {
      updateIndex(newIndex, example.copyFromByteArray(payload, 0), -1, null);      
    } else {
      lockObject(newIndex);
      boolean success = false;
      try{
        updateIndex(newIndex, example.copyFromByteArray(payload, 0), objectIndex, oldPayload);
        freeListPut(objectIndex);
        success = true;
      } finally {
        if (!success) {
          freeListPut(newIndex);
          unlockObject(newIndex);
        }
      }
    }
    return newIndex;
  }


  public void rollback(int objectIndex) {
    int newIndex = writeCopy[objectIndex];
    if (newIndex != objectIndex) {
      writeCopy[objectIndex] = objectIndex;
      freeListPut(newIndex);
      checksum[objectIndex] = XSORUtil.calculateChecksum(getData(objectIndex), getOffsetBytes(objectIndex), recordSize);
    }
  }


  public long getModificationTime(int objectIndex) {
    return payloadModificationTime[objectIndex];
  }


  public long getModificationTimeOfWriteCopy(int objectIndex) {
    return payloadModificationTime[writeCopy[objectIndex]];
  }
  
  
  public LinkedBlockingDequeWithLockAccess getOutgoingQueue() {
    return outgoingQueue;
  }


  Set<Object> getAllKeys() {// Testing: public methode in xcmemorydebugger
    return pkIndex.keySet();
  }


  String getName() {
    return name;
  }


  public int getCurrentSize() {
    return pkIndex.getCurrentSize();
  }


  public void storeReplyCodeAndNotifyWaiting(final int corrId, ReplyCode replyCode) {
    if (syncLatch != null && corrId == syncCorrId) {
      CountDownLatch l = syncLatch;
      if (l != null) {
        CountDownLatch latch = syncLatch; //kann gleichzeitig interrupted werden
        if (latch != null) {
          debugger.debug(new Object() {
            public String toString() {
              return "Received reply from other node for correlationId " + corrId + ". Notifying waiting sync-thread.";
            }
          });
          latch.countDown();
          syncLatch = null;
          syncCorrId = -1;
        }
      }
    }
    getWaitManagement().storeReplyCodeAndNotifyWaiting(corrId, replyCode, this);
  }


  public void processIncommingRequest(byte[] received, int corrId, Replyable replyable) {
    if ('y' == (char) received[0]) {
      //sync-message
      sendReplyMessage(ReplyCode.OK, corrId, 1, replyable, name);
    } else  if ('c' == (char) received[0]) {
      //sync-message
      sendReplyMessage(ReplyCode.OK, corrId, 1, replyable, name);
    } else{
      XSORProcess.processIncommingRequest(this, received, replyable);
    }
  }

  /**
   * ungelockter zugriff aufs bytearray
   */
  protected XSORPayload copyFromXSOR(int objectIndex) {
    // if there is no write copy then: writeCopy[objectIndex] = objectIndex
    return copyFromXSORWithUnadjustedIndex(writeCopy[objectIndex]);
  }
  
  
  protected XSORPayload copyFromXSORWithUnadjustedIndex(int objectIndex) {
    return example.copyFromByteArray(getData(objectIndex), getOffsetBytes(objectIndex));
  }

  
  public void updateIndex(int objectIndex, XSORPayload oldPayload) {
    int objectIndexOfWriteCopy = writeCopy[objectIndex];
    if (objectIndex != objectIndexOfWriteCopy) {
      XSORPayload writeCopyPayload = copyFromXSOR(objectIndex); // copy will copy from writeCopy
      updateIndex(objectIndexOfWriteCopy, writeCopyPayload, objectIndex, oldPayload);
    }
  }
  
  
  public void updateIndex(int newObjectIndex, XSORPayload newXSORPayload, int oldObjectIndex, XSORPayload oldXSORPayload) {
    if (logger.isTraceEnabled()) {
      logger.trace("updateIndex " + oldObjectIndex + " -> " + newObjectIndex);
    }
    if (newXSORPayload == null) {
      indexManagement.delete(oldXSORPayload, oldObjectIndex);
      boolean success = false;
      try {
        pkIndex.delete(oldXSORPayload.getPrimaryKey(), oldObjectIndex);
        success = true;
      } finally {
        if (!success) {
          indexManagement.update(null, oldXSORPayload, -1, oldObjectIndex);
        }
      }
    } else {
      pkIndex.replace(newXSORPayload.getPrimaryKey(), newObjectIndex, oldObjectIndex);
      boolean success = false;
      try {
        indexManagement.update(oldXSORPayload, newXSORPayload, oldObjectIndex, newObjectIndex);
        success = true;
      } finally {
        if (!success) {
          pkIndex.replace(newXSORPayload.getPrimaryKey(), oldObjectIndex, newObjectIndex);
        }
      }
    }
  }


  public int getObjectType() {
    return type;
  }


  public void addAllObjectsToOutgoingQueue(boolean insertAtHeadOfQueue) {
    //füge alle objekte in outgoing queue ein
    int cnt = 0;
    for (Object pk : pkIndex.keySet()) {
      int objectIndex = getObjectIndex(pk);
      if (objectIndex > -1) {
        lockObject(objectIndex);
        try {
          //spec: ist lokaler zustand == I || S => zielzustand S und payload-transfer.
          //ist lokaler zustand E || M, so führe für diese objekte ein payload-rollback auf das backup durch)

          char state = getState(objectIndex);
          switch (state) {
            case 'I' :
            case 'S' :
              //normalzustand, wenn objekt gerade nicht in bearbeitung ist
            case 'E' :
              
              //objekt ist gerade in bearbeitung, aber evtl wird keine message vom anderen thread erstellt, die die payload enthält.
              //->remote mit S und payload anlegen
              sendMessage(pk, objectIndex, 'S', true, insertAtHeadOfQueue);
              cnt++;
              break;
            case 'M' :
              //state = 'M' gerade arbeitet ein thread auf diesem objekt, d.h. wenn er fertig ist, wird es eh in die queue
              //eingestellt.
              //ausser man ist hier gerade in disc_master. dann kommt aber wohl gleich sync und dann wird das objekt erneut überprüft.
              // (bei übergang von disc_master -> sync wird wieder addAllObjects aufgerufen)
            case 'N' :

              //state == 'N' -> ein anderer thread hat das objekt geändert oder gelöscht und ihm ggfs einen neuen index gegeben.
              // a) update -> anderer thread verschickt message mit payload -> hier ist nichts zu tun.
              // b) delete -> kann nur als disc_master passieren, dann kann man das einfach ignorieren, weil dann keine nachricht
              //              in die queue eingestellt wird und deshalb remote keine konflikte entstehen
              
              if (logger.isDebugEnabled()) {
                logger.debug("Unlocked " + example.getTableName() + " with pk = '" + pk + "' found in state " + state
                    + ". Object will be synchronized later.");
              }
              break;
          }
        } finally {
          unlockObject(objectIndex);
        }
      }
    }
    final int _cnt = cnt;
    debugger.debug(new Object() {

      public String toString() {
        return "Added " + _cnt + " objects to queue.";
      }
    });
  }


  public void waitForQutgoingMessages() throws InterruptedException {
    CountDownLatch latch;

    Lock queueLock = outgoingQueue.getLock(); //locken, damit in der zeit keine nachrichten eingestellt oder entnommen werden können
    queueLock.lock();
    try {
      if (outgoingQueue.size() == 0) {
        //nun weiß man nicht, wieviele messages gerade unterwegs sind. man will warten, bis das letzte reply zurück ist.
        // => neue message einstellen. und darauf warten.
        debugger.debug("outgoing queue empty. inserting sync-message");
        sendSpecialMessage('y'); //greift auf die queue zu, ist aber gleicher thread, deshalb geht das
      }
      byte[] lastElement = outgoingQueue.peekLast();
      syncCorrId = XSORUtil.getInt(5, lastElement);
      latch = new CountDownLatch(1);
      syncLatch = latch;

      debugger.debug(new Object() {
        private final int _corrId = syncCorrId;
        public String toString() {
          return "Last element in queue has correlation id " + _corrId; 
        }
      });
    } finally {
      queueLock.unlock();
    }
    try {
      latch.await(); //wird unterbrochen, falls sich der clusterstate ändert durch ein interrupt auf den thread (siehe cachecoherenceimpl)
      debugger.debug("Continuing ...");
    } catch (InterruptedException e) {
      syncLatch = null;
      syncCorrId = -1;
      throw e;
    }
  }


  public void changeQueueModeToMerging() {
    logger.trace("changeQueueModeToMerging()");
    queueModeMerging=true;    
    synchronized(mergeStateS){
      mergeStateS.clear();
      mergeStateI.clear();
      Iterator<byte[]> it=outgoingQueue.iterator();
      while(it.hasNext()){
        byte[] msg=it.next();
        char state=(char)msg[0];
        int objectIdLength=XSORUtil.getInt(37, msg);
        byte[] objectId=Arrays.copyOfRange(msg, 41,41+objectIdLength);
        BigInteger objectBigInteger=new BigInteger(objectId);
        switch (state){
          case 'S':
            mergeStateS.remove(objectBigInteger);
            mergeStateI.remove(objectBigInteger);
            mergeStateS.put(objectBigInteger,msg);
            break;
          case 'I':
            if (mergeStateS.containsKey(objectBigInteger) && !mergeStateI.containsKey(objectBigInteger)){
              mergeStateI.put(objectBigInteger,msg);
            } else if (!mergeStateS.containsKey(objectBigInteger) ){
              mergeStateS.remove(objectBigInteger);
              mergeStateI.remove(objectBigInteger);
            } else {
              logger.error("error while merging found unexpectedly state I"+ " objectId="+XSORUtil.prettyPrint(objectId));
              mergeStateS.remove(objectBigInteger);
              mergeStateI.remove(objectBigInteger);
            }
            break;
          default://should not happen
            logger.error("error while merging found state "+state+ " objectId="+XSORUtil.prettyPrint(objectId));
            mergeStateS.remove(objectBigInteger);
            mergeStateI.remove(objectBigInteger);
            
        }
      }
    }
  }


  public void changeQueueModeToSend() {
    logger.trace("changeQueueModeToSend()");
    queueModeMerging=false;    
    synchronized(mergeStateS){
      mergeStateS.clear();
      mergeStateI.clear();
    }
  }


  public String toString(int backupIndex, boolean skipData) {
    int objectIndex = writeCopy[backupIndex];
    
    StringBuilder sb = new StringBuilder();
    if (backupIndex == objectIndex) {
      sb.append("\nbackup identical");
    } else {
      sb.append("\nbackupIndex = ").append(backupIndex);
      sb.append("\nbackup-checksum = ").append(getCheckSum(backupIndex));
      sb.append("\nbackup-state = ").append(getState(backupIndex));
      if (!skipData) {
        sb.append("\nbackup-data = ").append(XSORUtil.prettyPrint(getData(backupIndex), getOffsetBytes(backupIndex), recordSize));
      }
      sb.append("\nbackup-modtime = ").append(getModificationTime(backupIndex));
      sb.append("\nbackup-reltime = ").append(getReleaseTime(backupIndex));
    }

    sb.append("\nobjectIndex(writeCopy) = ").append(objectIndex);
    sb.append("\nchecksum = ").append(getCheckSum(objectIndex));
    sb.append("\nstate = ").append(getState(objectIndex));
    if (!skipData) {
      sb.append("\ndata = ").append(XSORUtil.prettyPrint(getData(objectIndex), getOffsetBytes(objectIndex), recordSize));
    }
    sb.append("\nmodtime = ").append(getModificationTime(objectIndex));
    sb.append("\nreltime = ").append(getReleaseTime(objectIndex));
    return sb.toString();
  }

  public void sendSpecialStartedMessage() {
    sendSpecialMessageAtFirst('c');
  }

  
  XSORMemoryChecker xsorMemoryChecker;
  
  public void stopXSORMemoryChecker() {
    xsorMemoryChecker.stop();
  }


  public int getLockState(int objectIndex) {
    return lock.get(objectIndex);
  }


}
