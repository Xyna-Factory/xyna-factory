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
package com.gip.xyna.xmcp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xact.rmi.GenericRMIAdapter;
import com.gip.xyna.xact.rmi.RMIConnectionFailureException;

/**
 * Kapselt den Zugriff auf eine Xyna Blackedition Instanz über RMI. Bietet Möglichkeiten für Fallback auf zweite Instanz,
 * falls eine Instanz nicht erreichbar ist über URLChooser Interface. Zwei einfache Implementierungen davon stehen
 * über statische Methoden zur Verfügung.
 * TODO: falls server neu gestartet wird, verbindung automatisch wieder reconnecten?!
 * codebeispiel:
 * <code>
 *  RMIAdapter rmiAdapter = new RMIAdapter("localhost", 1099, "TestUser", "TestPass");
    rmiAdapter.getRmiInterface().addCapacity(rmiAdapter.getUserName(), rmiAdapter.getPasswordHashed(), "mycap", 5, State.ACTIVE);
 * </code>
 */
public class RMIAdapter extends GenericRMIAdapter<XynaRMIChannel> {
  
  public static final Logger logger = CentralFactoryLogging.getLogger(RMIAdapter.class);

  //TODO factory für multiple urls wo man hostname+port tupels angibt.
  //TODO validierung der urls
  
  private String user;
  private String passwordHashed;
  
  public RMIAdapter(URLChooser urlChooser, String user, String password) throws RMIConnectionFailureException {
    super(urlChooser, false);
    passwordHashed = generateHashedPassword(password);
    this.user = user;
  }
  
  /**
   * benutzt den singleurlchooser
   */
  public RMIAdapter(String url, String user, String password) throws RMIConnectionFailureException {
    this(getSingleURLChooser(url), user, password);
  }
  
  
  /**
   * benutzt den singleurlchooser
   */
  public RMIAdapter(String hostname, int port, String user, String password) throws RMIConnectionFailureException {
    this(getSingleURLChooser(hostname, port, "XynaRMIChannel"), user, password);
  }

  public String getUserName() {
    return user;
  }
  
  public String getPasswordHashed() {
    return passwordHashed;
  }
  
  
  // FIXME does only generate legacy hashes, if different HashParams are specified this will fail
  // those params could not be dynamically retrieved as a hashedPassword would be necessary for such a request
  // either allow for passing it in the constructor or request it from factory via an unsecured channel
  private static String generateHashedPassword(String password) {
    StringBuilder passwordBuilder = new StringBuilder();
    try {
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update((password).getBytes());
      byte[] result = md5.digest();

      for (int i = 0; i < result.length; i++) {
        int halfbyte = (result[i] >>> 4) & 0x0F;
        int two_halfs = 0;
        do {
          if ((0 <= halfbyte) && (halfbyte <= 9))
            passwordBuilder.append((char) ('0' + halfbyte));
          else
            passwordBuilder.append((char) ('a' + (halfbyte - 10)));
          halfbyte = result[i] & 0x0F;
        } while (two_halfs++ < 1);
      }

    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error during hash generation", e);
    }
    if (passwordBuilder.length() == 0) {
      throw new RuntimeException("Error during hash generation. generated password had length 0.");
    }

    return passwordBuilder.toString();
  }
}
