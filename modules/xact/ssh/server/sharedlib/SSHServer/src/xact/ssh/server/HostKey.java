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
package xact.ssh.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import com.gip.xyna.utils.misc.Base64;

public class HostKey {

  private PublicKey publicKey;
  private String sshRsaKey;

  public HostKey(PublicKey publicKey) {
    this.publicKey =  publicKey;
    if( publicKey instanceof RSAPublicKey ) {
      try {
        this.sshRsaKey = Base64.encode( encode((RSAPublicKey)publicKey) ) ;
      } catch (IOException e) {
        this.sshRsaKey = "Failed to encode";
      }
    } else {
      this.sshRsaKey = "";
    }
  }

  public PublicKey getPublicKey() {
    return publicKey;
  }
  
  public String getSshRsaKey() {
    return sshRsaKey;
  }
  
  
  
  private static byte[] encode(RSAPublicKey key) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream();
    write("ssh-rsa".getBytes("US-ASCII"), buf);
    write(key.getPublicExponent().toByteArray(), buf);
    write(key.getModulus().toByteArray(), buf);
    return buf.toByteArray();
  }

  private static void write(byte[] str, OutputStream os) throws IOException {
    for (int shift = 24; shift >= 0; shift -= 8)
      os.write((str.length >>> shift) & 0xFF);
    os.write(str);
  }

  
}
