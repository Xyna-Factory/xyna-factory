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
package com.gip.xyna.xint.crypto;

import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;

import com.gip.xyna.xint.crypto.exceptions.KeyNotUniqueException;
import com.gip.xyna.xint.crypto.exceptions.NoSuchKeyException;

public class PGPKeyStoreParameter {
  
  private final Optional<String> userID;
  private final Optional<PublicKeyStoreProvider> publicProvider;
  private final Optional<SecretKeyStoreProvider> secretProvider;
  
  
  private PGPKeyStoreParameter(Optional<String> userID, Optional<PublicKeyStoreProvider> publicProvider, Optional<SecretKeyStoreProvider> secretProvider) {
    this.userID = userID;
    this.publicProvider = publicProvider;
    this.secretProvider = secretProvider;
  }
  
  // constructor for encryption
  public PGPKeyStoreParameter(String userID, PublicKeyStoreProvider provider) {
    this(Optional.of(userID), Optional.of(provider), Optional.empty());
  }
  
  //constructor for signatur
  public PGPKeyStoreParameter(String userID, SecretKeyStoreProvider provider) {
    this(Optional.of(userID), Optional.empty(), Optional.of(provider));
  }
  
  // constructor for decryption
  public PGPKeyStoreParameter(SecretKeyStoreProvider provider) {
    this(Optional.empty(), Optional.empty(), Optional.of(provider));
  }
  
  // constructor for decryption with potential signature verification
   public PGPKeyStoreParameter(Optional<PublicKeyStoreProvider> publicProvider, SecretKeyStoreProvider provider) {
     this(Optional.empty(), publicProvider, Optional.of(provider));
   }
  

  public String getUserID() {
    return userID.get();
  }
  
  public PGPPublicKey findPublicKey(PublicKeyCapability desiredCapability) throws NoSuchKeyException {
    try {
      return findKey(publicProvider.get().openPuplicKeyStore(), getUserID(), true, new CapabilityCheckingRingMergingExtractor(desiredCapability));
    } catch (IOException e) {
      throw new NoSuchKeyException(getUserID(), e);
    }
  }
  
  public PGPPublicKey findPublicKey(PublicKeyCapability desiredCapability, String keyId) throws NoSuchKeyException {
    try {
      return findKey(publicProvider.get().openPuplicKeyStore(), getUserID(), true, new KeyIdCheckingRingMergingExtractor(desiredCapability, keyId));
    } catch (IOException e) {
      throw new NoSuchKeyException(getUserID(), e);
    }
  }
  
  public PGPSecretKey findSecretKey() throws NoSuchKeyException {
    try {
      return findKey(secretProvider.get().openSecretKeyStore(), getUserID(), true);
    } catch (IOException e) {
      throw new NoSuchKeyException(getUserID(), e);
    }
  }
  
  public PGPPrivateKey findPrivateKey() throws NoSuchKeyException {
    try {
      return secretProvider.get().derivePrivateKey(findSecretKey());
    } catch (PGPException e) {
      throw new NoSuchKeyException(getUserID(), e);
    }
  }

  public PGPPrivateKey getPrivateKey(long keyID) throws NoSuchKeyException {
    try {
      PGPSecretKey secKey = secretProvider.get().openSecretKeyStore().getSecretKey(keyID);
      if (secKey == null) {
        throw new NoSuchKeyException(keyID);
      }
      return secretProvider.get().derivePrivateKey(secKey);
    } catch (IOException | PGPException e) {
      throw new NoSuchKeyException(keyID, e);
    }
  }

  public PGPPublicKey getPublicKey(long keyID) throws NoSuchKeyException {
    try {
      PGPPublicKey pupKey = publicProvider.get().openPuplicKeyStore().getPublicKey(keyID);
      if (pupKey == null) {
        throw new NoSuchKeyException(keyID);
      }
      return pupKey;
    } catch (IOException | PGPException e) {
      throw new NoSuchKeyException(keyID, e);
    }
  }

  
  
  
  private static PGPPublicKey findKey(PGPPublicKeyRingCollection openPupKeyStore, String recipient, boolean acceptMostLikely, SubKeyExtractor<PGPPublicKeyRing, PGPPublicKey> subKeyExtractor) throws NoSuchKeyException {
    return PGPKeyStoreParameter.<PGPPublicKeyRingCollection,
                                 PGPPublicKeyRing,
                                 PGPPublicKey,
                                 KeyRingCollectionGetter<PGPPublicKeyRingCollection, PGPPublicKeyRing>,
                                 SubKeyExtractor<PGPPublicKeyRing, PGPPublicKey>,
                                 KeyUserIDGetter<PGPPublicKey>>
                                   findKeyGeneric(openPupKeyStore,
                                                  PGPKeyStoreParameter::getPublicKeyRings,
                                                  subKeyExtractor,
                                                  PGPKeyStoreParameter::getUserIDs,
                                                  recipient,
                                                  acceptMostLikely);
  }
  
  
  private static PGPSecretKey findKey(PGPSecretKeyRingCollection openSecKeyStore, String sender, boolean acceptMostLikely) throws NoSuchKeyException {
    return PGPKeyStoreParameter.<PGPSecretKeyRingCollection,
                                 PGPSecretKeyRing,
                                 PGPSecretKey,
                                 KeyRingCollectionGetter<PGPSecretKeyRingCollection, PGPSecretKeyRing>,
                                 SubKeyExtractor<PGPSecretKeyRing, PGPSecretKey>,
                                 KeyUserIDGetter<PGPSecretKey>>
                                   findKeyGeneric(openSecKeyStore,
                                                  PGPKeyStoreParameter::getSecretKeyRings,
                                                  PGPKeyStoreParameter::getInnerKey,
                                                  PGPKeyStoreParameter::getUserIDs,
                                                  sender,
                                                  acceptMostLikely);
  }
  
  
  private static <C, R, K, G extends KeyRingCollectionGetter<C, R>, U extends SubKeyExtractor<R, K>, I extends KeyUserIDGetter<K>> K 
                    findKeyGeneric(C keyRingCollection,
                                   G keyRingCollectionGetter,
                                   U ringUnwrapper,
                                   I userIdGetter,
                                   String userID,
                                   boolean acceptMostLikely) throws NoSuchKeyException {
    Iterator<R> keyRings;
    try {
      keyRings = keyRingCollectionGetter.getKeyRings(keyRingCollection, userID, true, true);
    } catch (PGPException e) {
      throw new NoSuchKeyException(userID, e);
    }
    if (!keyRings.hasNext()) {
      throw new NoSuchKeyException(userID);
    }
    int visitedKeys = 0;
    K mostLikelyKey = null;
    int mostLikelyScore = Integer.MAX_VALUE;
    while(keyRings.hasNext()) {
      R ring = keyRings.next();
      visitedKeys++;
      K key = ringUnwrapper.getInnerKey(ring);
      if (key == null) {
        continue;
      }
      Iterator<String> userIds = userIdGetter.getUserIDs(key);
      while(userIds.hasNext()) {
        int distance = unlimitedCompare(userID, userIds.next());
        if (distance < mostLikelyScore) {
          mostLikelyKey = key;
        }
      }
    }
    if (mostLikelyKey == null) {
      throw new NoSuchKeyException(userID);
    }
    if (!acceptMostLikely &&
        visitedKeys > 1) {
      throw new KeyNotUniqueException(userID);
    }
    return mostLikelyKey;
  }
  
  
  
  // findKey SAMs
  public interface KeyRingCollectionGetter<C, R> {
    public Iterator<R> getKeyRings(C keyRingCollection, String userID, boolean matchPartial, boolean ignoreCase) throws PGPException;
  }
  
  public interface SubKeyExtractor<R, K> {
    public K getInnerKey(R keyRing);
  }
  
  public interface KeyUserIDGetter<K> {
    public Iterator<String> getUserIDs(K key);
  }
  
  // findKey SAM-Impls
  private static Iterator<PGPPublicKeyRing> getPublicKeyRings(PGPPublicKeyRingCollection keyRingCollection, String userID, boolean matchPartial, boolean ignoreCase) throws PGPException {
    return keyRingCollection.getKeyRings(userID, matchPartial, ignoreCase);
  }
  
  private static Iterator<PGPSecretKeyRing> getSecretKeyRings(PGPSecretKeyRingCollection keyRingCollection, String userID, boolean matchPartial, boolean ignoreCase) throws PGPException {
    return keyRingCollection.getKeyRings(userID, matchPartial, ignoreCase);
  }
  
  private static PGPPublicKey getInnerKey(PGPPublicKeyRing ring) {
    return ring.getPublicKey();
  }
  
  private static PGPSecretKey getInnerKey(PGPSecretKeyRing ring) {
    return ring.getSecretKey();
  }
  
  private static Iterator<String> getUserIDs(PGPPublicKey key) {
    return key.getUserIDs();
  }
  
  private static Iterator<String> getUserIDs(PGPSecretKey key) {
    return key.getUserIDs();
  }
  
  
  /**
   * Source: Apache Commons Text
   * https://commons.apache.org/sandbox/commons-text/jacoco/org.apache.commons.text.similarity/LevenshteinDistance.java.html
   * 
   * <p>Find the Levenshtein distance between two Strings.</p>
   *
   * <p>A higher score indicates a greater distance.</p>
   *
   * <p>The previous implementation of the Levenshtein distance algorithm
   * was from <a href="https://web.archive.org/web/20120526085419/http://www.merriampark.com/ldjava.htm">
   * https://web.archive.org/web/20120526085419/http://www.merriampark.com/ldjava.htm</a></p>
   *
   * <p>This implementation only need one single-dimensional arrays of length s.length() + 1</p>
   *
   * <pre>
   * unlimitedCompare(null, *)             = IllegalArgumentException
   * unlimitedCompare(*, null)             = IllegalArgumentException
   * unlimitedCompare("","")               = 0
   * unlimitedCompare("","a")              = 1
   * unlimitedCompare("aaapppp", "")       = 7
   * unlimitedCompare("frog", "fog")       = 1
   * unlimitedCompare("fly", "ant")        = 3
   * unlimitedCompare("elephant", "hippo") = 7
   * unlimitedCompare("hippo", "elephant") = 7
   * unlimitedCompare("hippo", "zzzzzzzz") = 8
   * unlimitedCompare("hello", "hallo")    = 1
   * </pre>
   *
   * @param left the first String, must not be null
   * @param right the second String, must not be null
   * @return result distance, or -1
   * @throws IllegalArgumentException if either String input {@code null}
   */
  private static int unlimitedCompare(CharSequence left, CharSequence right) {
      if (left == null || right == null) {
          throw new IllegalArgumentException("Strings must not be null");
      }

      /*
         This implementation use two variable to record the previous cost counts,
         So this implementation use less memory than previous impl.
       */

      int n = left.length(); // length of left
      int m = right.length(); // length of right

      if (n == 0) {
          return m;
      } else if (m == 0) {
          return n;
      }

      if (n > m) {
          // swap the input strings to consume less memory
          final CharSequence tmp = left;
          left = right;
          right = tmp;
          n = m;
          m = right.length();
      }

      int[] p = new int[n + 1];

      // indexes into strings left and right
      int i; // iterates through left
      int j; // iterates through right
      int upper_left;
      int upper;

      char rightJ; // jth character of right
      int cost; // cost

      for (i = 0; i <= n; i++) {
          p[i] = i;
      }

      for (j = 1; j <= m; j++) {
          upper_left = p[0];
          rightJ = right.charAt(j - 1);
          p[0] = j;

          for (i = 1; i <= n; i++) {
              upper = p[i];
              cost = left.charAt(i - 1) == rightJ ? 0 : 1;
              // minimum of cell to the left+1, to the top+1, diagonally left and up +cost
              p[i] = Math.min(Math.min(p[i - 1] + 1, p[i] + 1), upper_left + cost);
              upper_left = upper;
          }
      }

      return p[n];
  }

}
