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


import java.util.List;
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


public final class FactoryUtils {

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
  
  public static final Map<String, Supplier<Named<KeyAlgorithm>>> KeyAlgFactories =
      Map.ofEntries(Map.entry("ssh-rsa", com.hierynomus.sshj.key.KeyAlgorithms::SSHRSA),
                    Map.entry("ssh-rsa-cert-v01@openssh.com", com.hierynomus.sshj.key.KeyAlgorithms::SSHRSACertV01),
                    Map.entry("rsa-sha2-256", com.hierynomus.sshj.key.KeyAlgorithms::RSASHA256),
                    Map.entry("rsa-sha2-512", com.hierynomus.sshj.key.KeyAlgorithms::RSASHA512),
                    Map.entry("ssh-dss", com.hierynomus.sshj.key.KeyAlgorithms::SSHDSA),
                    Map.entry("ecdsa-sha2-nistp256", com.hierynomus.sshj.key.KeyAlgorithms::ECDSASHANistp256),
                    Map.entry("ecdsa-sha2-nistp256-cert-v01@openssh.com", com.hierynomus.sshj.key.KeyAlgorithms::ECDSASHANistp256CertV01),
                    Map.entry("ecdsa-sha2-nistp384", com.hierynomus.sshj.key.KeyAlgorithms::ECDSASHANistp384),
                    Map.entry("ecdsa-sha2-nistp384-cert-v01@openssh.com", com.hierynomus.sshj.key.KeyAlgorithms::ECDSASHANistp384CertV01),
                    Map.entry("ecdsa-sha2-nistp521", com.hierynomus.sshj.key.KeyAlgorithms::ECDSASHANistp521),
                    Map.entry("ecdsa-sha2-nistp521-cert-v01@openssh.com", com.hierynomus.sshj.key.KeyAlgorithms::ECDSASHANistp521CertV01),
                    Map.entry("ssh-ed25519", com.hierynomus.sshj.key.KeyAlgorithms::EdDSA25519),
                    Map.entry("ssh-ed25519-cert-v01@openssh.com", com.hierynomus.sshj.key.KeyAlgorithms::EdDSA25519CertV01));

  public static final Map<String, Supplier<Named<MAC>>> macFactories = 
      Map.ofEntries(Map.entry("hmac-md5", Macs::HMACMD5),
                    Map.entry("hmac-md5-96", Macs::HMACMD596),
                    Map.entry("hmac-md5-etm@openssh.com", Macs::HMACMD5Etm),
                    Map.entry("hmac-md5-96-etm@openssh.com", Macs::HMACMD596Etm),
                    Map.entry("hmac-ripemd160", Macs::HMACRIPEMD160),
                    Map.entry("hmac-ripemd160-96", Macs::HMACRIPEMD16096),
                    Map.entry("hmac-ripemd160-etm@openssh.com", Macs::HMACRIPEMD160Etm),
                    Map.entry("hmac-ripemd160@openssh.com", Macs::HMACRIPEMD160OpenSsh),
                    Map.entry("hmac-sha1", Macs::HMACSHA1),
                    Map.entry("hmac-sha1-96", Macs::HMACSHA196),
                    Map.entry("hmac-sha1-etm@openssh.com", Macs::HMACSHA1Etm),
                    Map.entry("hmac-sha1-96@openssh.com", Macs::HMACSHA196Etm),
                    Map.entry("hmac-sha2-256", Macs::HMACSHA2256),
                    Map.entry("hmac-sha2-256-etm@openssh.com", Macs::HMACSHA2256Etm),
                    Map.entry("hmac-sha2-512", Macs::HMACSHA2512),
                    Map.entry("hmac-sha2-512-etm@openssh.com", Macs::HMACSHA2512Etm)
                    );

  
  public static List<Named<KeyAlgorithm>> createKeyAlgsListDefault() {
    return java.util.Arrays.<net.schmizz.sshj.common.Factory.Named<com.hierynomus.sshj.key.KeyAlgorithm>> asList(
                         com.hierynomus.sshj.key.KeyAlgorithms.SSHDSA(),
                         com.hierynomus.sshj.key.KeyAlgorithms.SSHRSA(),
                         com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp521(), //This KeyAlgorithm is necessary
                         com.hierynomus.sshj.key.KeyAlgorithms.ECDSASHANistp256(),
                         com.hierynomus.sshj.key.KeyAlgorithms.RSASHA512(),
                         com.hierynomus.sshj.key.KeyAlgorithms.RSASHA256()
                          );
  }
  
  
  public static List<Named<MAC>> createMacListDefault() {
    return java.util.Arrays.<net.schmizz.sshj.common.Factory.Named<MAC>> asList(
                           Macs.HMACSHA2256(),
                           Macs.HMACSHA2256Etm(),
                           Macs.HMACSHA2512(),
                           Macs.HMACSHA2512Etm(),
                           Macs.HMACSHA1(),
                           Macs.HMACSHA1Etm(),
                           Macs.HMACSHA196(),
                           Macs.HMACSHA196Etm(),
                           Macs.HMACMD5(),
                           Macs.HMACMD5Etm(),
                           Macs.HMACMD596(),
                           Macs.HMACMD596Etm(),
                           Macs.HMACRIPEMD160(),
                           Macs.HMACRIPEMD160Etm(),
                           Macs.HMACRIPEMD16096(),
                           Macs.HMACRIPEMD160OpenSsh()
                           );
  }

}

