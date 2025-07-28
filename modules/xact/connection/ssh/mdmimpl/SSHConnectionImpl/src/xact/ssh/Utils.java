/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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



import java.util.Map;
import java.util.function.Supplier;

import com.hierynomus.sshj.key.KeyAlgorithm;
import com.hierynomus.sshj.transport.cipher.BlockCiphers;
import com.hierynomus.sshj.transport.cipher.GcmCiphers;
import com.hierynomus.sshj.transport.cipher.StreamCiphers;
import com.hierynomus.sshj.transport.mac.Macs;

import net.schmizz.sshj.common.Factory.Named;
import net.schmizz.sshj.transport.cipher.Cipher;
import net.schmizz.sshj.transport.mac.MAC;
import xact.connection.SSHException;




public final class Utils {

  private Utils() {
  }
  
  
  public static SSHException toSshException(net.schmizz.sshj.common.SSHException sshjException) {
    switch (sshjException.getDisconnectReason()) {
      case CONNECTION_LOST :
        return new ConnectionLostException(sshjException.getMessage());
      case HOST_KEY_NOT_VERIFIABLE :
        return new HostKeyNotVerifiableException(sshjException.getMessage());
      case HOST_NOT_ALLOWED_TO_CONNECT :
        return new HostNotAllowedToConnectException(sshjException.getMessage());
      case ILLEGAL_USER_NAME :
        return new IllegalUserNameException(sshjException.getMessage());
      case KEY_EXCHANGE_FAILED :
        return new KeyExchangeFailedException(sshjException.getMessage());
      case AUTH_CANCELLED_BY_USER :
        return new UserAuthException(sshjException.getMessage());
      default :
        return new SSHException(sshjException.getMessage());
    }
  }

}

