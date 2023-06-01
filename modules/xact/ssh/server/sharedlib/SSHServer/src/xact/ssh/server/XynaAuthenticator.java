package xact.ssh.server;



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


import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import com.gip.xyna.utils.ByteUtils;
import com.gip.xyna.utils.collections.LruCache;



public class XynaAuthenticator implements PublickeyAuthenticator, PasswordAuthenticator {

  private final static LruCache<ServerSession, String> SESSION_PASSWORD_CACHE = new LruCache<ServerSession, String>(200);
  
  private Map<String, String> userkeys = new HashMap<String, String>();
  private Map<String, String> userpasswords = new HashMap<String, String>();
  private boolean alwaysauthenticate = false;
  

  private org.apache.log4j.Logger logger = null;


  public XynaAuthenticator(HashMap<String, String> publickeys, HashMap<String, String> passwords, boolean alwauth,
                           org.apache.log4j.Logger log) {
    userpasswords = passwords;
    userkeys = publickeys;
    alwaysauthenticate = alwauth;
    logger = log;
  }


  public void addUserKey(String username, String publickey) {
    userkeys.put(username, publickey);
  }


  public void addUserPassword(String username, String password) {
    userpasswords.put(username, password);
  }


  private boolean keysEqual(PublicKey givenkey, byte[] savedkey) {
    byte[] keya = givenkey.getEncoded();
    byte[] keyb = savedkey;

    if (keya.length != keyb.length)
      return false;


    for (int i = 0; i < keya.length; i++) {
      if (keya[i] != keyb[i])
        return false;
    }

    return true;
  }


  private void logMessage(String message) {
    if (logger != null) {
      logger.info(message);
    }
  }


  public boolean authenticate(String username, PublicKey key, ServerSession session) {
    logMessage("Authenticating (PublicKey)...");
    try {
      logMessage("UserName: " + username);
      logMessage("PublicKey: " + ByteUtils.toHexString(key.getEncoded(), true, null, true));

      String userkey = userkeys.get(username);

      if (userkey != null) {
        if (!keysEqual(key, ByteUtils.fromHexStringWithLeading0x(userkey))) {
          logMessage("Public Key given (" + ByteUtils.toHexString(key.getEncoded(), true, null, true) + ") does not match expected Public Key (" + userkey + ")");
          logMessage("Authentication failed!");
          return false;
        }

      } else {
        logMessage("User not found!");
        logMessage("Authentication failed!");
        return false; // kein Userkey gefunden
      }

    } catch (Exception e) {
      logMessage("Problems authenticating: " + e);
      return false;
    }


    logMessage("Authentication successful!");
    return true;
  }


  public boolean authenticate(String username, String password, ServerSession session) {
    if (alwaysauthenticate) {
      logMessage("Skipping Authentication, setting Password ...");
      SESSION_PASSWORD_CACHE.put(session, password);
      return true;
    }


    logMessage("Authenticating (Password) ...");
    try {
      logMessage("UserName: " + username);
      logMessage("Password: " + password);

      String passwordindb = userpasswords.get(username);

      if (passwordindb != null) {
        if (!password.equals(passwordindb)) {
          logMessage("Password given (" + password + ") does not match expected password (" + passwordindb + ")");
          logMessage("Authentication failed!");
          return false;
        }

      } else {
        logMessage("User not found!");
        logMessage("Authentication failed!");
        return false; // kein Password gefunden
      }

    } catch (Exception e) {
      logMessage("Problems authenticating: " + e);
      return false;
    }


    logMessage("Authentication successful!");
    SESSION_PASSWORD_CACHE.put(session, password);
    return true;
  }


  public static String getPassword(ServerSession session) {
    return SESSION_PASSWORD_CACHE.get(session);
  }
  
  // TODO and a remove and call it together with RequestContext removal
  // they're one time passwords after all...

}
