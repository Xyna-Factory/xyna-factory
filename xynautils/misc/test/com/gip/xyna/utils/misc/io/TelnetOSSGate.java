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

public class TelnetOSSGate {

   private static final String CONTROL_B = "\002";
   private static String server;
   private static String user;
   private static String passwd;

   private static IOLogger iol;

   /**
    * @param args
    */
   public static void main(String[] args) {
      if (args == null || args.length != 3) {
         System.out.println("Aufruf: TelnetOSSGate <server> <user> <passwd>");
         System.exit(1);
      }
      server = args[0];
      user = args[1];
      passwd = args[2];

      TelnetOSSGate tt = new TelnetOSSGate();
      iol = new IOLogger() {
         public void debug(String msg) {
         }

         public void logFTPReply(String reply) {
         }

         public void logTelnetCmd(String cmd) {
            // System.err.println( cmd );
         }

         public void logTelnetOutput(String output) {
            System.out.print(output);
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
      BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      String cmd;

      t.connect(server, 23);

      t.readUntil(new StringCondition("login: "), 10000);
      t.write(user);
      t.readUntil(new StringCondition("Password: "), 10000);
      t.write(passwd);

      t.readUntil(new StringCondition("continue."), 10000); // Welcome-Meldung
                                                            // abwarten
      t.write(""); // Enter anstatt zu warten

      t.readUntil(new StringCondition("> "), 10000); // Login für OSSGate

      // Zweite Anmeldung
      cmd = in.readLine();
      t.setPrompt("> ");
      t.exec(cmd);

      System.out
            .println("  -> Abbruch mit \"lwExit\", \"strgB command\" statt \"^B command\"");
      t.exec("");

      while (true) {
         cmd = in.readLine();
         if ((cmd != null) && cmd.equals("lwExit")) {
            break;
         }
         if ((cmd != null) && cmd.startsWith("strgB")) {
            t.write(CONTROL_B);
            t.readUntil(new StringCondition("?"), 10000);
            t.exec(cmd.substring(6));
            continue; // verhindert zweite Ausführung
         }
         t.exec(cmd);
      }

      System.out.println("Beginne Logout");

      t.write(CONTROL_B);
      t.readUntil(new StringCondition("?"), 10000);
      t.write("logout");
      t.readUntil(new StringCondition("logged out."), 10000);
      t.write(CONTROL_B);
      t.readUntil(new StringCondition("?"), 10000);
      t.write("convterm");
      t.readUntil(new StringCondition("SESSION TERMINATED."), 10000);

      System.out.println();

      t.disconnect();
   }

}
