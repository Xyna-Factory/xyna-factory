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
  
  public static final Map<String, Supplier<Named<Cipher>>> CipherFactories =
      Map.ofEntries(Map.entry("aes128-ctr", BlockCiphers::AES128CTR),
                    Map.entry("aes192-ctr", BlockCiphers::AES192CTR),
                    Map.entry("aes256-ctr", BlockCiphers::AES256CTR),
                    Map.entry("aes128-cbc", BlockCiphers::AES128CBC),
                    Map.entry("aes192-cbc", BlockCiphers::AES192CBC),
                    Map.entry("aes256-cbc", BlockCiphers::AES256CBC),
                    Map.entry("blowfish-cbc", BlockCiphers::BlowfishCTR),
                    Map.entry("twofish128-ctr", BlockCiphers::Twofish128CTR),
                    Map.entry("twofish192-ctr", BlockCiphers::Twofish192CTR),
                    Map.entry("twofish256-ctr", BlockCiphers::Twofish256CTR),
                    Map.entry("twofish128-cbc", BlockCiphers::Twofish128CBC),
                    Map.entry("twofish192-cbc", BlockCiphers::Twofish192CBC),
                    Map.entry("twofish256-cbc", BlockCiphers::Twofish256CBC),
                    Map.entry("twofish-cbc", BlockCiphers::TwofishCBC),
                    Map.entry("serpent128-ctr", BlockCiphers::Serpent128CTR),
                    Map.entry("serpent192-ctr", BlockCiphers::Serpent192CTR),
                    Map.entry("serpent256-ctr", BlockCiphers::Serpent256CTR),
                    Map.entry("serpent128-cbc", BlockCiphers::Serpent128CBC),
                    Map.entry("serpent192-cbc", BlockCiphers::Serpent192CBC),
                    Map.entry("serpent256-cbc", BlockCiphers::Serpent256CBC),
                    Map.entry("idea-ctr", BlockCiphers::IDEACTR),
                    Map.entry("idea-cbc", BlockCiphers::IDEACBC),
                    Map.entry("cast128-ctr", BlockCiphers::Cast128CTR),
                    Map.entry("cast128-cbc", BlockCiphers::Cast128CBC),
                    Map.entry("3des-ctr", BlockCiphers::TripleDESCTR),
                    Map.entry("3des-cbc", BlockCiphers::TripleDESCBC),

                    Map.entry("aes128-gcm@openssh.com", GcmCiphers::AES128GCM),
                    Map.entry("aes256-gcm@openssh.com", GcmCiphers::AES256GCM),

                    Map.entry("arcfour", StreamCiphers::Arcfour),
                    Map.entry("arcfour128", StreamCiphers::Arcfour128),
                    Map.entry("arcfour256", StreamCiphers::Arcfour256)
                    );


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

