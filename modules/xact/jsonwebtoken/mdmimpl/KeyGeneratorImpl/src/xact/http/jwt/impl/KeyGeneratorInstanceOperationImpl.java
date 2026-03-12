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

package xact.http.jwt.impl;

import java.security.KeyPair;
import java.util.Base64;

import javax.crypto.SecretKey;

import com.gip.xyna.xdev.xfractmod.xmdm.Container;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.MacAlgorithm;
import io.jsonwebtoken.security.SecureDigestAlgorithm;
import io.jsonwebtoken.security.SignatureAlgorithm;
import xact.http.jwt.Key;
import xact.http.jwt.KeyGenerator;
import xact.http.jwt.KeyGeneratorInstanceOperation;
import xact.http.jwt.KeyGeneratorSuperProxy;


public class KeyGeneratorInstanceOperationImpl extends KeyGeneratorSuperProxy implements KeyGeneratorInstanceOperation {

  private static final long serialVersionUID = 1L;

  public KeyGeneratorInstanceOperationImpl(KeyGenerator instanceVar) {
    super(instanceVar);
  }

  
  public Key generateKey() {
    String algorithm = getInstanceVar().getAlgorithm();
    if( algorithm == null ) {
      algorithm = "HS256";
    }
    SecureDigestAlgorithm<?,?> sda = Jwts.SIG.get().get(algorithm);
    if (!(sda instanceof MacAlgorithm)) {
      throw new RuntimeException("Unexpected algorithm: " + algorithm);
    }
    SecretKey key = ((MacAlgorithm) sda).key().build();
    return new Key(keyToString(key));
  }

  
  public Container generateKeyPair() {
    String algorithm = getInstanceVar().getAlgorithm();
    if( algorithm == null ) {
      algorithm = "RS256";
    }
    SecureDigestAlgorithm<?,?> sda = Jwts.SIG.get().get(algorithm);
    if (!(sda instanceof SignatureAlgorithm)) {
      throw new RuntimeException("Unexpected algorithm: " + algorithm);
    }
    KeyPair keyPair = ((SignatureAlgorithm) sda).keyPair().build();
    return new Container(new Key(keyToString(keyPair.getPrivate())), new Key(keyToString(keyPair.getPublic())));
  }

  
  private String keyToString(java.security.Key key) {
    return Base64.getEncoder().encodeToString(key.getEncoded());
  }
  
  
  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

}
