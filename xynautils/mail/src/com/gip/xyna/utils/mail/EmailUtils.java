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
package com.gip.xyna.utils.mail;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import org.apache.oro.text.regex.*;

/**
 * Utils
 * 
 * 
 */
public class EmailUtils {

   public static String loginUsername = "to be set on login";
   public static String loginServername = "to be set on login";

   /**
    * Pruefen einer E-Mail Adresse auf syntaktische Korrektheit Im Fehlerfall
    * wird ein Error-String geliefert. Ist die Adresse ok, so wird ein
    * Leerstring zureuckgeliefert
    */
   public static String checkEmailAddressGetError(String addr) {
      Perl5Compiler p5c = new Perl5Compiler();
      Perl5Matcher p5m = new Perl5Matcher();
      Pattern ep, up, idp, sdp;
      MatchResult mr;

      String emailPattern = "^([^@]+)@(.+)$";
      String specialChars = "\\(\\)<>@,;:\\\\\\\"\\.\\[\\]";
      String validChars = "[^\\s" + specialChars + "]+";
      String quotedUser = "\"[^\"]*\"";
      String ipDomainPattern = "^\\[(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\]$";
      String word = "(" + validChars + "|" + quotedUser + ")";
      String userPattern = "^" + word + "(\\." + word + ")*$";
      String domainPattern = "^(" + validChars + ")(\\." + validChars + ")+$";

      try {
         // Zunaechst am '@' splitten
         try {
            ep = p5c.compile(emailPattern);
         } catch (MalformedPatternException mpe) {
            throw new Exception("Fehler in E-Mail-Check");
         }
         if (!(p5m.matches(addr, ep))) {
            throw new Exception(
                  "E-Mail-Adresse hat ungueltiges Format (fehlt '@' ?)");
         }

         // User und Domain betrachten
         mr = p5m.getMatch();
         String user = mr.group(1);
         String domain = mr.group(2);

         // Syntax von User pruefen
         try {
            up = p5c.compile(userPattern);
         } catch (MalformedPatternException mpe) {
            throw new Exception("Fehler in E-Mail-Check");
         }
         if (!(p5m.matches(user, up))) {
            throw new Exception(
                  "E-Mail-Adresse hat ungueltiges Format (Benutzername)");
         }

         // Syntax von Domain pruefen

         // Wenn im IP-Format ...
         try {
            idp = p5c.compile(ipDomainPattern);
         } catch (MalformedPatternException mpe) {
            throw new Exception("Fehler in E-Mail-Check");
         }
         if ((p5m.matches(domain, idp))) { // ist im IP-Format
            // testen ob alle Zahlen < 255
            mr = p5m.getMatch();
            for (int i = 1; i < mr.groups(); i++) {
               try {
                  if (new Integer(mr.group(i)).intValue() > 255)
                     throw new Exception(
                           "E-Mail-Adresse hat ungueltiges Format (IP-Adresse)");
               } catch (NumberFormatException nfe) {
               }
            }
            // Wenn wir hier sind ist die (IP-) Domainangabe ok und wir sind
            // fertig
            // System.out.println("*** EMail-Address is in IP-Format and
            // good!");
            return "";
         }

         // Wenn im symbolischen Format ...
         try {
            sdp = p5c.compile(domainPattern);
         } catch (MalformedPatternException mpe) {
            throw new Exception("Fehler in E-Mail-Check");
         }
         if (!(p5m.matches(domain, sdp))) { // ist nicht im symbolischen
            // Format
            throw new Exception(
                  "E-Mail-Adresse hat ungueltiges Format (Domain)");
         }

         // System.out.println("Domain: " + domain);
         // Jetzt noch pruefen ob host und domain vorhanden und domain
         // aus 2 oder 3 Buchstaben besteht
         mr = p5m.getMatch();
         if (mr.groups() < 3) {
            throw new Exception(
                  "E-Mail-Adresse hat ungueltiges Format (Domain: nicht a.b)");
         }
         String dm = mr.group(mr.groups() - 1);
         if (dm.length() - 1 < 2 || dm.length() - 1 > 3) { // lenght()-1
            // wegen dem '.'
            // in domain
            throw new Exception(
                  "E-Mail-Adresse hat ungueltiges Format\n(Domain muss in einem Wort mit 2 oder 3 Buchstaben enden)");
         }
      } catch (Exception e) {
         return e.getMessage();
      }
      return "";
   }

   /** Splittet einen String und liefert ein String-Array */
   public static String[] split(String s, String delim) {
      if (null == s || s.length() < 1)
         return new String[0];

      String[] tokens;
      StringTokenizer st = new StringTokenizer(s, delim);
      tokens = new String[st.countTokens()];
      for (int i = 0; st.hasMoreTokens(); i++) {
         tokens[i] = st.nextToken();
      }
      return tokens;
   }

   /** Splittet einen String und liefert ein long-Array */
   public static Long[] splitLong(String s, String delim) {
      String[] a = split(s, delim);
      if (a.length < 1)
         return new Long[0];
      Long[] longs = new Long[a.length];
      for (int i = 0; i < a.length; i++) {
         try {
            longs[i] = new Long(a[i]);
         } catch (NumberFormatException e) {
            longs[i] = null;
         }
      }
      return longs;
   }

   /** Fuegt ein Long-Array zu einem String */
   public static String joinLong(Long[] l, String delim) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < l.length; i++) {
         sb.append(l[i].toString());
         if (i < l.length - 1)
            sb.append(delim);
      }
      return sb.toString();
   }

   /** Fuegt ein long-Array zu einem String */
   public static String joinlong(long[] l, String delim) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < l.length; i++) {
         sb.append(l[i]);
         if (i < l.length - 1)
            sb.append(delim);
      }
      return sb.toString();
   }

   /** Fuegt ein Sting-Array zu einem String */
   public static String joinString(String[] s, String delim) {
      StringBuffer sb = new StringBuffer();
      for (int i = 0; i < s.length; i++) {
         sb.append(s[i].toString());
         if (i < s.length - 1)
            sb.append(delim);
      }
      return sb.toString();
   }

   /**
    * Testet zwei Datumsstrings die im Format dd:MM:yyyy erwartet werden
    * Liefert: 0: Die beiden Daten sind gleich 1: begin liegt vor end -1: end
    * liegt vor begin 2: begin ist ungueltig -2: end ist ungueltig Wenn einer
    * der Werte null oder leer ist, wird kein Vergleich durchgefuehrt und die
    * Methode liefert 1 (wenn Datum gueltig)
    */
   public static int checkDates(String begin, String end) {
      // System.out.println("*** " + begin + ", " + end);
      // Die Daten werden von JobList im Format NLS_DATETIME_FORMAT
      // = "DD.MM.YYYY HH24:MI:SS" geliefert
      SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
      SimpleDateFormat sdf0 = new SimpleDateFormat("dd.MM.yyyy HH:mm");
      long tBegin, tEnd;
      if (null != begin && begin.length() > 0) {
         try {
            tBegin = sdf.parse(begin).getTime();
         } catch (Exception e) {
            try {
               tBegin = sdf0.parse(begin).getTime();
            } catch (Exception e0) {
               return 2;
            }
         }
      } else {
         tBegin = -1;
      }
      if (null != end && end.length() > 0) {
         try {
            tEnd = sdf.parse(end).getTime();
         } catch (Exception e) {
            try {
               tEnd = sdf0.parse(end).getTime();
            } catch (Exception e0) {
               return -2;
            }
         }
      } else {
         tEnd = -1;
      }
      if (tBegin == -1 || tEnd == -1)
         return 1;
      return (tBegin == tEnd ? 0 : (tBegin < tEnd ? 1 : -1));
   }

   /**
    * Liefert das aktuelle Arbeitsverzeichnis
    * 
    * @return
    */
   public static String getCwd() {
      return new File(new File(".").getAbsolutePath()).getParent();
   }

   /**
    * Check-Methode fuer ein Kennwort
    * 
    * @param userNames
    *              Name des Anwenders, bzw. Login-Name oder Vorname
    * @param password
    *              Zu checkendes Kennwort
    * @param oldPassword
    *              altes Passwort
    * @throws Exception
    *               Wenn das Feld nicht den Vorgaben entspricht
    */
   public static void checkPassword(String[] userNames, String password,
         String oldPassword) throws Exception {
      // minlength
      int minLength = 8;
      if (password.length() < minLength) {
         throw new Exception("Kennwort muss mindestens " + minLength
               + " Zeichen lang sein.");
      }

      int maxLength = 14;
      if (password.length() > maxLength) {
         throw new Exception("Kennwort darf h�chstens " + maxLength
               + " Zeichen lang sein.");
      }

      String allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#$_.-";
      for (int i = 0; i < password.length(); i++) {
         if (allowedChars.indexOf(password.charAt(i)) < 0) {
            throw new Exception("Kennwort darf das Zeichen '"
                  + password.charAt(i) + " nicht enthalten");
         }
      }

      // neededChars + noNeededChars
      String neededChars = "0123456789_.-";
      int noNeededChars = 1;
      int countChars = 0;
      for (int i = 0; i < password.length() && countChars < noNeededChars; i++) {
         if (neededChars.indexOf(password.charAt(i)) >= 0)
            countChars++;
      }
      if (countChars < noNeededChars) {
         throw new Exception("Das Kennwort muss mindestens " + noNeededChars
               + " Zeichen aus '" + neededChars + "' enthalten.");
      }

      // nicht benutzername
      for (int i = 0; i < userNames.length; i++) {
         if (password.indexOf(userNames[i]) > -1) {
            throw new Exception(
                  "Das Kennwort darf den Benutzernamen nicht enthalten.");
         }
      }

      checkPasswordDifference(password, oldPassword, 3);
   }

   /**
    * @param newPassword
    * @param oldPassword
    * @param identicalChars
    * @throws Exception
    */
   public static void checkPasswordDifference(String newPassword,
         String oldPassword, int identicalChars) throws Exception {
      if (identicalChars > 0 && oldPassword.length() > 0) {
         if (oldPassword.equals(newPassword))
            throw new Exception(
                  "Altes und neues Kennwort d�rfen nicht �bereinstimmen.");

         for (int i = 0; i < oldPassword.length() - identicalChars; i++) {
            String idChars = oldPassword.substring(i, i + identicalChars);
            if (newPassword.indexOf(idChars) >= 0) {
               throw new Exception("Das neue Kennwort enth�lt mehr als "
                     + idChars.length()
                     + " aufeinanderfolgende Zeichen des alten Kennworts.");
            }
         }
      }
   }

}
