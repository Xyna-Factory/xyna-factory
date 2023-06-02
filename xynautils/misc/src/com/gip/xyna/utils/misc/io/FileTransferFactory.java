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
package com.gip.xyna.utils.misc.io;

/**
 * Factory, um auf leicht austauschbare Weise verschiedene FileTransfers zu
 * bauen.
 * 
 * Geplant ist Upload über FTP, SCP und HTTP. TODO SCP, HTTP, Erzeugung über
 * Property
 * 
 */
public class FileTransferFactory {

   /**
    * Anlegen einer neuen FTP-Instanz
    * 
    * @param ioLogger
    * @param server
    * @param username
    * @param password
    * @param directory
    * @return
    * @throws Exception
    */
   public static FileTransfer FTP(IOLogger ioLogger, String server,
         String username, String password, String directory) throws Exception {

      FileTransferFTP ftp = new FileTransferFTP();
      ftp.setIOLogger(ioLogger);

      ftp.connect(server, username, password, FileTransferFTP.BINARY);

      ftp.changeDir(directory);
      return ftp;
   }

   // TODO
   public static FileTransfer HTTP() throws Exception {
      return null;
   }

   public static FileTransfer SCP() throws Exception {
      return null;
   }

}