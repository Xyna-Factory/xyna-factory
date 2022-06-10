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

package com.gip.xyna.xint.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;

class MergedPublicKey extends PGPPublicKey {

  private final PGPPublicKey functionalKey;
  private final PGPPublicKey userIdProvider;


  MergedPublicKey(PGPPublicKey functionalKey, PGPPublicKey userIdProvider) throws PGPException {
    super(functionalKey.getPublicKeyPacket(), new BcKeyFingerprintCalculator());
    this.functionalKey = functionalKey;
    this.userIdProvider = userIdProvider;
  }


  public int getVersion() {
    return functionalKey.getVersion();
  }


  public Date getCreationTime() {
    return functionalKey.getCreationTime();
  }


  public int getValidDays() {
    return functionalKey.getValidDays();
  }


  public byte[] getTrustData() {
    return functionalKey.getTrustData();
  }


  public long getValidSeconds() {
    return functionalKey.getValidSeconds();
  }


  public long getKeyID() {
    return functionalKey.getKeyID();
  }


  public byte[] getFingerprint() {
    return functionalKey.getFingerprint();
  }


  public boolean isEncryptionKey() {
    return functionalKey.isEncryptionKey();
  }


  public boolean isMasterKey() {
    return functionalKey.isMasterKey();
  }


  public int getAlgorithm() {
    return functionalKey.getAlgorithm();
  }


  public int getBitStrength() {
    return functionalKey.getBitStrength();
  }


  public Iterator<String> getUserIDs() {
    return userIdProvider.getUserIDs();
  }


  public Iterator<byte[]> getRawUserIDs() {
    return userIdProvider.getRawUserIDs();
  }


  public Iterator<PGPUserAttributeSubpacketVector> getUserAttributes() {
    return functionalKey.getUserAttributes();
  }


  public Iterator<PGPSignature> getSignaturesForID(String id) {
    return functionalKey.getSignaturesForID(id);
  }


  public Iterator<PGPSignature> getSignaturesForID(byte[] rawID) {
    return functionalKey.getSignaturesForID(rawID);
  }


  public Iterator<PGPSignature> getSignaturesForKeyID(long keyID) {
    return functionalKey.getSignaturesForKeyID(keyID);
  }


  public Iterator getSignaturesForUserAttribute(PGPUserAttributeSubpacketVector userAttributes) {
    return functionalKey.getSignaturesForUserAttribute(userAttributes);
  }


  public Iterator getSignaturesOfType(int signatureType) {
    return functionalKey.getSignaturesOfType(signatureType);
  }


  public Iterator getSignatures() {
    return functionalKey.getSignatures();
  }


  public Iterator getKeySignatures() {
    return functionalKey.getKeySignatures();
  }


  public PublicKeyPacket getPublicKeyPacket() {
    return functionalKey.getPublicKeyPacket();
  }


  public byte[] getEncoded() throws IOException {
    return functionalKey.getEncoded();
  }


  public byte[] getEncoded(boolean forTransfer) throws IOException {
    return functionalKey.getEncoded();
  }


  public void encode(OutputStream outStream) throws IOException {
    functionalKey.encode(outStream);
  }


  public void encode(OutputStream outStream, boolean forTransfer) throws IOException {
    functionalKey.encode(outStream, forTransfer);
  }


  public boolean isRevoked() {
    return functionalKey.isRevoked();
  }


  public boolean hasRevocation() {
    return functionalKey.hasRevocation();
  }

}