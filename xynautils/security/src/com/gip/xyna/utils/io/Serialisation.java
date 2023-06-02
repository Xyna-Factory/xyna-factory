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
package com.gip.xyna.utils.io;

import java.io.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * Serialisation
 */
public class Serialisation {

   /**
    * serialisiert Object in Byte Array
    * 
    * @param object
    * @throws IOException
    */
   public static byte[] serialize(Object object) throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      ObjectOutputStream os = new ObjectOutputStream(out);
      os.writeObject(object);
      os.flush();
      return out.toByteArray();
   }

   /**
    * serialisiert Object in Byte Array
    * 
    * @param ba
    * @throws IOException
    * @throws ClassNotFoundException
    */
   public static Object deserialize(byte ba[]) throws IOException,
         ClassNotFoundException {
      ByteArrayInputStream in = new ByteArrayInputStream(ba);
      ObjectInputStream is = new ObjectInputStream(in);
      Object ret = is.readObject();
      is.close();
      return ret;
   }

}
