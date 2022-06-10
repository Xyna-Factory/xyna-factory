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

public class FileRemove {

   /**
    * @param args
    */
   public static void main(String[] args) {
      if (args == null || args.length != 5) {
         System.out
               .println("Aufruf: FileRemove <server> <user> <passwd> <dir> <file>");
         System.exit(1);
      }
      String server = args[0];
      String user = args[1];
      String passwd = args[2];
      String dir = args[3];
      String filename = args[4];

      IOLogger iol = new IOLogger() {
         public void debug(String msg) {
         }

         public void logFTPReply(String reply) {
            System.out.println(reply);
         }

         public void logTelnetCmd(String cmd) {
         }

         public void logTelnetOutput(String output) {
         }
      };

      FileTransferFTP ft = null;
      try {
         ft = (FileTransferFTP) FileTransferFactory.FTP(iol, server, user,
               passwd, dir);
         boolean del = ft.delete(filename);
         System.out.println("File " + filename + " wurde "
               + (del ? "" : "nicht ") + "geloescht");
      } catch (Exception e) {
         e.printStackTrace();
         return;
      } finally {
         if (ft != null) {
            ft.close();
         }
      }

   }

}
