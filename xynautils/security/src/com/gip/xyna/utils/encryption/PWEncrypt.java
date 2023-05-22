/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

/**
 * PW7
 */
public class PWEncrypt {

   /** lat aus dem C-Algo von Mali, benoetigt fuer CISCO-pw7 */
   // static int lat[] = {
   // 0x64, 0x73, 0x66, 0x64, 0x3b, 0x6b, 0x66, 0x6f,
   // 0x41, 0x2c, 0x2e, 0x69, 0x79, 0x65, 0x77, 0x72,
   // 0x6b, 0x6c, 0x64, 0x4a, 0x4b, 0x44, 0x48, 0x53,
   // 0x55, 0x42, 0x73, 0x67, 0x76, 0x63, 0x61, 0x36,
   // 0x39, 0x38, 0x33, 0x34, 0x6e, 0x63, 0x78 };
   static int lat[] = { 0x64, 0x73, 0x66, 0x64, 0x3b, 0x6b, 0x66, 0x6f, 0x41,
         0x2c, 0x2e, 0x69, 0x79, 0x65, 0x77, 0x72, 0x6b, 0x6c, 0x64, 0x4a,
         0x4b, 0x44, 0x48, 0x53, 0x55, 0x42, 0x73, 0x67, 0x76, 0x63, 0x61,
         0x36, 0x39, 0x38, 0x33, 0x34, 0x6e, 0x63, 0x78, 0x9c, 0xb5, 0x4f,
         0x9c, 0x15, 0x23, 0x21, 0x5d, 0x55, 0x76, 0x93, 0xa1, 0x8b, 0x1d,
         0xd9, 0x48, 0x34, 0x18, 0x78, 0x7d, 0x7a, 0x40, 0x6c, 0x87, 0x19,
         0x1f, 0x81, 0x10, 0x86, 0xe4, 0x5f, 0x25, 0xf9, 0x25, 0xdb, 0x5e,
         0xca, 0x29, 0x9a, 0x9e, 0x61, 0x73, 0xab, 0xf1, 0x5b, 0xb0, 0x86,
         0x5e, 0x68, 0x24, 0x77, 0x2c, 0x23, 0x1e, 0x6b, 0x43, 0xb4, 0x46,
         0x2d, 0x60, 0xa0, 0xcb, 0xdb, 0xc6, 0x9b, 0x9d, 0x40, 0x21, 0x78,
         0x2d, 0x25, 0x52, 0x56, 0x3e, 0xdd, 0x52, 0xc4, 0xe8, 0x4c, 0x4b,
         0x5d, 0x81, 0x4e, 0x8b, 0x3d, 0x37, 0xaa, 0x9e, 0x40 };

   /** Default PWD-Laenge */
   private static int OFFSET_LENGTH = 12;

   /**
    * Gibt einen zufaelligen pw7-Offset zurueck
    * 
    * @return pw7-Offset
    */
   public static int genPW7Offset() throws Exception {
      // Offset generieren, zw. 0-15
      // bugz 638 OffSet-Laenge als Konstante
      double rand;
      rand = Math.random();
      if (rand == 1.0) {
         rand = 0.999;
      }
      rand *= OFFSET_LENGTH;
      return (int) (rand);
   }

   /**
    * Gibt einen zufaelligen pw7-Offset als String mit 2 Zeichen zurueck
    * 
    * @return pw7-Offset
    */
   public static String genPW7OffsetStr() throws Exception {
      // Offset holen, zw. 0-15
      String offsetStr = "" + genPW7Offset();
      if (offsetStr.length() == 1) {
         offsetStr = "0" + offsetStr;
      }
      return offsetStr;
   }

   /**
    * Verschluesselt das Passwort mit Hilfe des Cisco-Algorithmus und gibt es
    * zurueck.
    * 
    * @param plainPw
    *              Passwort im Klartext
    * @return Cisco-verschluesseltes Passwort
    */
   public static String encrypt_pw7(String plainPw) throws Exception {
      return encrypt_pw7(plainPw, genPW7OffsetStr());
   }

   /**
    * Verschluesselt das Passwort mit Hilfe des Cisco-Algorithmus und gibt es
    * zurueck
    * 
    * @param plainPw
    *              Passwort im Klartext
    * @param offSet
    *              String, nur Ziffern, 1 oder 2 Zeichen lang
    * @return Cisco-verschluesseltes Passwort
    */
   public static String encrypt_pw7(String plainPw, String offSet)
         throws Exception {
      String encrPw = "";
      int intChar;
      String hexChar;
      String c = "";
      int offset = Integer.parseInt(offSet); // gibt einen Error, wenn a-z, A-Z
                                             // im String enthalten ist;
      if (offset < 0 || offset > 15) {
         throw new Exception("Das pw7-Offset ist nicht korregt");
      }

      // checken ob pwd im gueltigen Bereich ist
      if (plainPw.length() == 0) {
         throw new Exception("Kein Passwort angegeben");
      }
      if (plainPw.length() > 100) {
         throw new Exception(
               "Der pw7-Algorithmus unsterstuetzt nur Passwoerter mit einer max. Laenge von 100 Zeichen");
         // plainPw = plainPw.substring(0,24); // orginal-Algorithmus
      }

      // Offset holen, zw. 00-15
      c = offSet;
      if (c.length() == 1) {
         c = "0" + c;
      }
      if (c.length() != 2) {
         throw new Exception("Offset-Error");
      }

      encrPw += c;
      for (int j = 0; j < plainPw.length(); j++) {
         int aktOffs = j + offset;

         intChar = plainPw.charAt(j) ^ lat[aktOffs];
         hexChar = Integer.toHexString(intChar);

         c = "" + hexChar;
         if (c.length() == 1) {
            c = "0" + c;
         }
         encrPw += c;
      }
      return encrPw.toUpperCase();
   }

   /**
    * Entschluesselt das Passwort mit Hilfe des Cisco-Algorithmus und gibt es
    * zurueck.
    * 
    * @param pw7
    *              verschluesseltes Passwort
    * @return Passwort im Klartext
    */
   public static String decrypt_pw7(String pw7) throws Exception {
      StringBuffer decrPw = new StringBuffer(100);
      int offSet = Integer.parseInt(pw7.substring(0, 2)); // 00-15
      int aktOffs, aktChar;
      pw7 = pw7.substring(2); // encrPW ohne Offset

      for (int j = 0; j < pw7.length(); j += 2) { // 2 Zeichen sind sind ein
                                                   // Char
         aktOffs = j / 2 + offSet; // aktueller Offset bilden
         aktChar = Integer.parseInt(pw7.substring(j, j + 2), 16); // aktuelles
                                                                  // Zeichen (zB
                                                                  // '5e') auf
                                                                  // Hex-Basis
                                                                  // parsen
         aktChar = aktChar ^ lat[aktOffs]; // akt. Zeichen XOR
                                             // Wert(Offset-Array)
         char ch = (char) aktChar; // int2char
         decrPw.append(ch); // String zusammenbauen
      }

      return decrPw.toString();
   }

}
