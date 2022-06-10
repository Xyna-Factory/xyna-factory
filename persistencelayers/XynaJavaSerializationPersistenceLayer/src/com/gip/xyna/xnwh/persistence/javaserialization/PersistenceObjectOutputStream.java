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

package com.gip.xyna.xnwh.persistence.javaserialization;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.Storable;



public class PersistenceObjectOutputStream extends ObjectOutputStream {

  private static final Logger logger = CentralFactoryLogging.getLogger(PersistenceObjectOutputStream.class);


  private ByteArrayOutputStream buffer;
  private ObjectOutputStream testStream;
  private FileChannel fileChannel;
  public static final int STREAMHEADER_SIZE = 4; 
  
  
  public PersistenceObjectOutputStream(FileOutputStream out) throws IOException {
    super(out);
    fileChannel = out.getChannel();
    buffer = new ByteArrayOutputStream();
    testStream = new ObjectOutputStream(buffer);
  }


  public void skipTo(long offset) throws IOException {
    fileChannel.position(offset);
  }


  //for now we'll be saving blindBlobs and not care about querys
  public RowObject writeStorable(Storable storable, RowObject row) throws PersistenceLayerException {
    //save position in file
    long startPos;
    buffer.reset();

    RowObject createdRow = new RowObject(storable.getPrimaryKey());

    try {
      startPos = fileChannel.position();
      Object o = getClassLoadedStorable(storable);
      testStream.writeObject(o);
      testStream.flush();
      //As long as we'll be writing blobs every row will only have 1 entry
      if (row != null) {
        //this would explain why we need the streamheader size in that case! (this being not writing the streamheader on update)
        if (buffer.size() + STREAMHEADER_SIZE != row.get(0).getSize()) {
          testStream.close();
          throw new XNWH_UnsupportedPersistenceLayerFeatureException("updates of variable size");
        }
      }
      this.writeObject(o);

      //compare position and create RowObject
      createdRow.add(new ColumnObject(storable.getClass().getSimpleName(), Object.class, fileChannel.position()
                      - startPos + STREAMHEADER_SIZE));

    } catch (IOException e) {
      throw new XNWH_GeneralPersistenceLayerException("error accessing file", e);
    } finally {
      try {
        testStream.close();
      } catch (IOException e) {
        // this should never happen since we only address a ByteArrayOutputStream
        logger.error("Unexpected error while closing stream.", e);
      }
    }

    return createdRow;
  }


  public Object getClassLoadedStorable(Storable storable) {
    if (storable.getClass().getClassLoader() instanceof ClassLoaderBase) {
      return new SerializableClassloadedObject(storable, storable.getClass().getClassLoader());
    } else {
      return storable;
    }
  }

}
