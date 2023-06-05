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

package com.gip.xyna.utils.logging.syslog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.logging.ErrorManager;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * Syslog dependent implementation of the java.util.logging.Handler Interface.
 * 
 * 
 */
@Deprecated
public class SyslogHandler extends Handler {
   private DatagramSocket socket;
   private SocketAddress address;

   /**
    * Erzeugt einen DatagramHandler, der seine Einträge an einen bestimmten Host
    * und Port schickt.
    */
   public SyslogHandler(String host, int port) throws SocketException {
      // Erzeuge DatagramSocket mit Verbindung zum Logging-Host
      socket = new DatagramSocket();
      address = new InetSocketAddress(host, port);
      socket.connect(address);
   }

   /**
    * Schließt den zugrundeliegenden Socket.
    */
   public void close() {
      // Schließen des Socket
      socket.close();
   }

   public void flush() {
      // Leeren von Puffern nicht notwendig,
      // da alle Daten direkt geschrieben werden.
   }

   /**
    * Schreibt den übergebenen LogRecord zum Ziel.
    */
   public void publish(LogRecord rec) {
      if (isLoggable(rec)) {
         try {
            // Formatiere Nachricht
            Formatter formatter = getFormatter();
            if (formatter instanceof SyslogFormatter) {
               String[] messages = ((SyslogFormatter) formatter)
                     .formatMultipleLines(rec);
               for (String m : messages) {
                  sendMessage(m);
               }
            } else {
               String message = formatter.format(rec);
               sendMessage(message);
            }
         } catch (Exception ex) {
            // TODO: check in test
            reportError(ex.getMessage(), ex, ErrorManager.WRITE_FAILURE);
         }
      }
   }

   protected void sendMessage(String message) throws IOException {
      byte[] data = message.getBytes();
      DatagramPacket packet = new DatagramPacket(data, data.length, address);
      socket.send(packet);
   }
}
