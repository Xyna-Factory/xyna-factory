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

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TelnetTest {

   private static String server;
   private static String user;
   private static String passwd;

   private static IOLogger iol;

   /**
    * @param args
    */
   public static void main(String[] args) {
      if (args == null || args.length != 3) {
         System.out.println("Aufruf: TelnetTest <server> <user> <passwd>");
         System.exit(1);
      }
      server = args[0];
      user = args[1];
      passwd = args[2];

      TelnetTest tt = new TelnetTest();
      iol = new IOLogger() {
         public void debug(String msg) {
         }

         public void logFTPReply(String reply) {
         }

         public void logTelnetCmd(String cmd) {
            System.err.println(cmd);
         }

         public void logTelnetOutput(String output) {
            System.out.println(output);
         }
      };
      try {
         tt.test();
      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private void test() throws Exception {
      Telnet t = new Telnet();
      t.setIOLogger(iol);

      t.connect(server, 23);
      t.setPrompt(" > ");
      t.login(user, passwd);

      // System.err.println( "Prompt: " + t.getFullPrompt() );

      // System.err.println( "#" + t.exec( "ls" ) + "#" );

      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      System.out.println("Abbruch mit \"exit\"");
      while (true) {
         String cmd = in.readLine();
         // System.err.println( "#" + t.exec( cmd ) + "#" );
         t.exec(cmd);
         if ((cmd != null) && cmd.equals("exit")) {
            break;
         }
      }

      t.disconnect();
   }

}
