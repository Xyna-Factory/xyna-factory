/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package xact.ssh.server.auth;

import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.pubkey.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

import com.gip.xyna.utils.ByteUtils;

public class XynaAuthenticator implements PublickeyAuthenticator, PasswordAuthenticator {

  private Logger logger = null;

  private Map<String, String> userkeys = new HashMap<String, String>();
  private Map<String, String> userpasswords = new HashMap<String, String>();
  private boolean alwaysauthenticate = false;
  private boolean useOTC = false;

  private OneTimeCredentialsCache otcc = new OneTimeCredentialsCache(500);


  public XynaAuthenticator(HashMap<String, String> publickeys, HashMap<String, String> passwords, boolean alwaysauth, boolean useOTC,
                           Logger log) {
    this.userpasswords = passwords;
    this.userkeys = publickeys;
    this.alwaysauthenticate = alwaysauth;
    this.useOTC = useOTC;
    this.logger = log;
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

  private void logInfo(String message) {
    if (logger != null && logger.isInfoEnabled()) {
      logger.info(message);
    }
  }

  private void logDebug(String message) {
    if (logger != null && logger.isDebugEnabled()) {
      logger.debug(message);
    }
  }

  private void logWarn(String message) {
    if (logger != null && logger.isWarnEnabled()) {
      logger.warn(message);
    }
  }

  private void logError(String message) {
    if (logger != null && logger.isErrorEnabled()) {
      logger.error(message);
    }
  }

  public boolean authenticate(String username, PublicKey key, ServerSession session) {
    logInfo("Authenticating insecure with PublicKey ...");
    try {
      logInfo("UserName: " + username);
      logInfo("PublicKey: " + ByteUtils.toHexString(key.getEncoded(), true, null, true));

      String userkey = userkeys.get(username);

      if (userkey != null) {
        if (!keysEqual(key, ByteUtils.fromHexStringWithLeading0x(userkey))) {
          logWarn("Public Key given (" + ByteUtils.toHexString(key.getEncoded(), true, null, true)
              + ") does not match expected Public Key (" + userkey + ")");
          logWarn("Authentication failed!");
          return false;
        }

      } else {
        logWarn("User not found!");
        logWarn("Authentication failed!");
        return false; // kein Userkey gefunden
      }

    } catch (Exception e) {
      logError("Problems authenticating: " + e);
      return false;
    }

    logInfo("Authentication successful!");
    return true;
  }

  public boolean authenticate(String username, String password, ServerSession session) {
    if (useOTC) {
      UserData ud = otcc.get(username);
      if (ud == null) {
        return false;
      } else {
        if (ud.getPassword().equals(password)) {
          otcc.remove(username);
          return true;
        } else {
          return false;
        }
      }
    }

    if (alwaysauthenticate) {
      logInfo("Skipping Authentication ...");
      logInfo("UserName: " + username);
      return true;
    }

    logInfo("Authenticating (Password) ...");
    try {
      logInfo("UserName: " + username);
      logDebug("Password: " + password);

      String passwordindb = userpasswords.get(username);

      if (passwordindb != null) {
        if (!password.equals(passwordindb)) {
          logDebug("Password given (" + password + ") does not match expected password (" + passwordindb + ")");
          logWarn("Authentication failed!");
          return false;
        }

      } else {
        logWarn("User not found!");
        logWarn("Authentication failed!");
        return false; // kein Password gefunden
      }

    } catch (Exception e) {
      logError("Problems authenticating: " + e);
      return false;
    }

    logInfo("Authentication successful!");
    return true;
  }

  public boolean addOneTimeCredentials(String user, String password, String expectedIp, String expectedPort) {
    return otcc.add(new UserData(user, password, expectedIp, expectedPort));
  }
}
