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
package xact.ssh;



import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.securestorage.SecureStorage;
import com.gip.xyna.xnwh.securestorage.SecuredStorable;



public class SecureStorablePassphraseStore implements PassphraseStore {

  private final static Logger logger = CentralFactoryLogging.getLogger(SecureStorablePassphraseStore.class);


  public void store(String identityName, String passphrase) {
    try {
      SecureStorage.getInstance().store("xact.ssh", identityName, passphrase == null ? "" : passphrase);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


  public String retrieve(String identityName) {
    try {
      String retrieve = (String) SecureStorage.getInstance().retrieve("xact.ssh", identityName);
      return retrieve;
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


  public void remove(String identityName) {
    try {
      SecureStorage.getInstance().remove("xact.ssh", identityName);
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }


}