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
package xact.ssh.file.impl;


import org.apache.log4j.Level;

import com.jcraft.jsch.Logger;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;


public class PassphraseRetrievingUserInfo implements UserInfo, UIKeyboardInteractive {
  
  private final static String EXPECTED_PASSPHRASE_PROMPTMESSAGE = "Passphrase for ";
  
  private final PassphraseStore store;
  private final Logger logger;
  
  private String lastPromptedIdentity;
  private String password = "";
  private boolean interactivePasswordReturnedOnce = false;
  
  
  
  public PassphraseRetrievingUserInfo(PassphraseStore store, Logger logger) {
    this.store = store;
    this.logger = logger;
  }

  public String getPassphrase() {
    return store.retrieve(lastPromptedIdentity);
  }


  public String getPassword() {
    return password;
  }
  
  
  public void setPassword(String password) {
    this.password = password;
  }


  public boolean promptPassphrase(String message) {
    if (message.startsWith(EXPECTED_PASSPHRASE_PROMPTMESSAGE)) {
      lastPromptedIdentity = message.substring(EXPECTED_PASSPHRASE_PROMPTMESSAGE.length()).trim();
      return true;
    } else {
      return false;
    }
  }


  public boolean promptPassword(String arg0) {
    return password != null;
  }


  public boolean promptYesNo(String message) {
    if (message.startsWith("The authenticity of host ") &&
        message.endsWith("Are you sure you want to continue connecting?")) {
      return true;
    } else {
    /* The authenticity of host 'x.x.x.x' can't be established.
RSA key fingerprint is 6b:d6:11:38:28:57:74:16:58:9e:74:42:57:bd:f5:c8.
Are you sure you want to continue connecting?*/
    
    /* WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!
IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!
Someone could be eavesdropping on you right now (man-in-the-middle attack)!
It is also possible that the RSA host key has just been changed.
The fingerprint for the RSA key sent by the remote host is
6b:d6:11:38:28:57:74:16:58:9e:74:42:57:bd:f5:c8.
Please contact your system administrator.
Add correct host key in XynaHostKeyRepository@7860099 to get rid of this message.
Do you want to delete the old key and insert the new key?*/
      return false;
    }
  }


  public void showMessage(String message) {
    logger.log(LogAdapter.logLevelToCode(Level.DEBUG), message);
  }

  public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
    if (prompt != null && prompt.length == 1 && prompt[0].equals("Password: ") && !interactivePasswordReturnedOnce) {
      interactivePasswordReturnedOnce = true;
      return new String[] {password};
    } else {
      return null;
    }
    
  }

}
