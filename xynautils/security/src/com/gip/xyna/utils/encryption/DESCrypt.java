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
package com.gip.xyna.utils.encryption;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

/**
 * DESCrypt
 */
public class DESCrypt {

   protected String passphrase;

   public DESCrypt(String passphrase) throws Exception {
      this.passphrase = passphrase;
      if ((this.passphrase == null) || this.passphrase.length() == 0) {
         throw new Exception("Die passphrase ist leer");
      }
   }

   /**
    * Rueckgabe der passphrase aus den ServerProperties
    * 
    * @return passphrase
    */
   /*
    * public static String getPassPhrase() { return
    * ServerProperties.getProperty("serverpassword.passphrase"); }
    */

   /**
    * Verschluesseln mit DES
    * 
    * @param conn
    * @param s
    *              der zu verschluesselnde Text
    * @return der verschluesselnde Text
    * @throws Exception
    */
   public String encrypt(Connection conn, String s) throws Exception {
      String encrypt = "";
      try {
         // ConsoleLogger.log.debug(context, "IpNetUtils.desEncrypt(?,?,?)");
         CallableStatement stmt = conn
               .prepareCall("{call IpNetUtils.desEncrypt(?,?,?)}");
         stmt.setString(1, s);
         stmt.setString(2, passphrase);
         stmt.registerOutParameter(3, Types.VARCHAR);
         stmt.execute();
         encrypt = stmt.getString(3);
         stmt.close();
      } catch (Exception e) {
         e.printStackTrace();
         throw new Exception(e.getMessage());
      }
      return encrypt;
   }

   /**
    * Entschluesseln mit DES
    * 
    * @param conn
    * @param s
    *              der zu entschluesselnde Text
    * @return der entschluesselnde Text
    * @throws Exception
    */
   public String decrypt(Connection conn, String s) throws Exception {
      String decrypt = "";
      try {
         // ConsoleLogger.log.debug(context, "IpNetUtils.desDecrypt(?,?,?)");
         CallableStatement stmt = conn
               .prepareCall("{call IpNetUtils.desDecrypt(?,?,?)}");
         stmt.setString(1, s);
         stmt.setString(2, passphrase);
         stmt.registerOutParameter(3, Types.VARCHAR);
         stmt.execute();
         decrypt = stmt.getString(3);
         stmt.close();
      } catch (Exception e) {
         e.printStackTrace();
         throw new Exception(s + " ist keine gueltige Eingabe");
      }
      return decrypt;
   }

}