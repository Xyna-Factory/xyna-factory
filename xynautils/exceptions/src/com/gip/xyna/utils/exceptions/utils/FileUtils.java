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
package com.gip.xyna.utils.exceptions.utils;



import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;

import com.gip.xyna.utils.exceptions.xmlstorage.ExceptionStorageParserBase;



public class FileUtils {

  public static void writeToFile(String text, String fileName) throws IOException {
    File f = new File(fileName);
    new File(f.getParent()).mkdirs();
    BufferedWriter out = new BufferedWriter(new FileWriter(f));
    out.write(text);
    out.close();
  }


  public static void copyFile(File in, File out) throws IOException {
    FileChannel inChannel = new FileInputStream(in).getChannel();
    FileChannel outChannel = new FileOutputStream(out).getChannel();
    try {
      inChannel.transferTo(0, inChannel.size(), outChannel);
    } finally {
      if (inChannel != null)
        inChannel.close();
      if (outChannel != null)
        outChannel.close();
    }
  }


  public static InputStream getInputStreamToResource(String fileName) throws ResourceNotFoundException {
    URL url = ExceptionStorageParserBase.class.getResource("/" + fileName);
    if (url != null) {

      URLConnection urlcon;
      try {
        urlcon = url.openConnection();
      } catch (IOException e) {
        throw new ResourceNotFoundException(e);
      }
      //deactivate cache to not get an old version
      boolean b = urlcon.getUseCaches();
      urlcon.setUseCaches(false);
      try {
        InputStream is;
        try {
          is = urlcon.getInputStream();
        } catch (IOException e) {
          throw new ResourceNotFoundException(e);
        }
        if (is == null) {
          throw new ResourceNotFoundException();
        } else {
          return is;
        }
      } finally {
        //reset caching!
        urlcon.setDefaultUseCaches(b);
      }
    } else {
      throw new ResourceNotFoundException();
    }    
  }
  
  public static class ResourceNotFoundException extends Exception {

    public ResourceNotFoundException(IOException e) {
      super(e);
    }

    public ResourceNotFoundException() {
    }

    private static final long serialVersionUID = -2209363492851971323L;
    
  }
}
