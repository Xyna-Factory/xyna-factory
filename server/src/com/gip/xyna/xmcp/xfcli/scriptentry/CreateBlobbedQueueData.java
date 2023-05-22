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
package com.gip.xyna.xmcp.xfcli.scriptentry;



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueConnectData;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueManagement;
import com.gip.xyna.xfmg.xfctrl.queuemgmnt.QueueType;
import com.gip.xyna.xnwh.persistence.xml.XMLPersistenceLayer;



/***
 * Creates a XML-PersistenceLayer conform string for the following queue parameters: queue type, connect data. 
 * If the input contains exactly one string, it returns the queue type. If multiple strings are provided, 
 * the result is the encoded connect data.
 */
public class CreateBlobbedQueueData {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Input: <queue type> [<list of connect parameters>]");
      throw new RuntimeException();
    }
    CreateBlobbedQueueData executor = new CreateBlobbedQueueData();
    try {
      executor.createQueueConnectData(args);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private void createQueueConnectData(String[] args) throws Exception {
    QueueType qtype = null;
    try {
      qtype = QueueType.valueOf(args[0]);
    } catch (Exception e) {
      throw new IllegalArgumentException("", e);
    }
    String[] connectData = new String[args.length - 1];
    for (int i = 0; i < connectData.length; i++) {
      connectData[i] = args[i + 1];
    }

    SerializableClassloadedObject obj = null;
    if (connectData.length == 0) {
      obj = new SerializableClassloadedObject(qtype);
    } else {
      QueueConnectData resultData = QueueManagement.createQueueConnectData(qtype, connectData);
      obj = new SerializableClassloadedObject(resultData);
    }

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    try {
      oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
    } catch (IOException e) {
      throw new RuntimeException("unexpected problem serializing QueueConnectData");
    }
    byte[] bytes = baos.toByteArray();
    String encoded = XMLPersistenceLayer.encodeBytes(bytes);
    System.out.println(encoded);
  }

}
