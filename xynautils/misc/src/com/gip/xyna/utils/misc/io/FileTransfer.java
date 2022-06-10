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
package com.gip.xyna.utils.misc.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Interface für mehrere geplante Klassen, die eine Datei hochladen sollen
 */
public interface FileTransfer {

   /**
    * Upload einer Datei
    * 
    * @param file
    * @return
    * @throws FileNotFoundException
    * @throws IOException
    */
   public int upload(File file) throws FileNotFoundException, IOException;

   /**
    * Upload unter dem Namen filename, Datenquelle ist der Stream content
    * 
    * @param filename
    * @param content
    * @return
    * @throws IOException
    */
   public int upload(String filename, InputStream content) throws IOException;

   /**
    * Löschen einer Datei auf dem Server
    * 
    * @param name
    * @return
    * @throws IOException
    */
   public boolean delete(String name) throws IOException;

   /**
    * Schließen der Verbindung
    */
   public void close();

}