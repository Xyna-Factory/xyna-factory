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
package xact.ssh.impl;



import java.io.IOException;
import java.util.Base64;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.edec.EdECObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKey;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil;
import org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import base.Text;
import xact.ssh.KeyInfo;
import xact.ssh.KeyInfoServiceServiceOperation;



public class KeyInfoServiceServiceOperationImpl implements ExtendedDeploymentTask, KeyInfoServiceServiceOperation {

  private static final Logger _logger = CentralFactoryLogging.getLogger(KeyInfoServiceServiceOperationImpl.class);


  public enum KeyKind {
    PUBLIC, PRIVATE
  }


  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }


  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public KeyInfo getKeyInfo(Text inputbase64Blob) {

    KeyInfo keyInfoOut = new KeyInfo();

    try {
      ParsedKeyClass keyparsed = new ParsedKeyClass(inputbase64Blob.getText().trim());
      KeyInfoClass keyinfoparsed = new KeyInfoClass(keyparsed);
      keyInfoOut.unversionedSetType(keyinfoparsed.key.kind.toString());
      keyInfoOut.unversionedSetAlgorithm(keyinfoparsed.algorithm);
      keyInfoOut.unversionedSetCurve(keyinfoparsed.curve);
      keyInfoOut.unversionedSetLength(keyinfoparsed.bitLength);
    } catch (IOException e) {

    }
    return keyInfoOut;
  }


  //getKeyInfo: KeyInfoClass helper to get algorithms etc.
  private class KeyInfoClass {

    private final ParsedKeyClass key;
    private String algorithm;
    private String curve;
    private int bitLength;


    private KeyInfoClass(ParsedKeyClass pkey) throws IOException {
      this.key = pkey;
      if (this.key.kind == KeyKind.PUBLIC) {
        extractPublicKeyInfo();
      } else {
        extractPrivateKeyInfo();
      }
    }


    ParsedKeyClass getKey() {
      return this.key;
    }


    @Override
    public String toString() {
      return this.curve == null ? key.kind + " " + this.algorithm + ", " + this.bitLength + " bits" : key.kind + " " + this.algorithm + " ("
          + this.curve + "), " + this.bitLength + " bits";
    }


    private void extractPublicKeyInfo() throws IOException {

      // parse down to ASN1 representation to distinguish algorithm with OIDs from BC
      SubjectPublicKeyInfo pubk = (SubjectPublicKeyInfo) this.getKey().key;
      ASN1ObjectIdentifier alg = pubk.getAlgorithm().getAlgorithm();

      // RSA (length of modulus)
      if (alg.getId().equals("1.2.840.113549.1.1.1")) {
        RSAPublicKey rsa = RSAPublicKey.getInstance(pubk.parsePublicKey());
        this.algorithm = "RSA";
        this.bitLength = rsa.getModulus().bitLength();
        return;
      }

      // DSA (length of P)
      if (alg.getId().equals("1.2.840.10040.4.1")) {
        DSAParameter params = DSAParameter.getInstance(pubk.getAlgorithm().getParameters());
        this.algorithm = "DSA";
        this.bitLength = params.getP().bitLength();
        return;
      }

      // ECDSA (length of N)
      if (alg.getId().equals("1.2.840.10045.2.1")) {
        ASN1ObjectIdentifier curveOid = ASN1ObjectIdentifier.getInstance(pubk.getAlgorithm().getParameters());
        X9ECParameters ec = ECNamedCurveTable.getByOID(curveOid);
        this.algorithm = "ECDSA";
        this.bitLength = ec.getN().bitLength();
        this.curve = ECNamedCurveTable.getName(curveOid);
        return;
      }

      // Ed25519 (fixed 256 bit for public)
      if (alg.equals(EdECObjectIdentifiers.id_Ed25519)) {
        this.algorithm = "Ed25519";
        this.bitLength = 256;
        this.curve = "Ed25519";
        return;
      }

      _logger.error("KeyInfo Service: a key algorithm of a public key could not be determined");
      throw new IllegalArgumentException("Unsupported key algorithm");
    }


    private void extractPrivateKeyInfo() throws IOException {

      // parse down to ASN1 representation to distinguish algorithm with OIDs from BC
      PrivateKeyInfo prik = (PrivateKeyInfo) this.getKey().key;
      ASN1ObjectIdentifier alg = prik.getPrivateKeyAlgorithm().getAlgorithm();

      if (alg.getId().equals("1.2.840.113549.1.1.1")) {
        RSAPrivateKey rsa = RSAPrivateKey.getInstance(prik.parsePrivateKey());
        this.algorithm = "RSA";
        this.bitLength = rsa.getModulus().bitLength();
        return;
      }

      // DSA (length of P)
      if (alg.getId().equals("1.2.840.10040.4.1")) {
        DSAParameter params = DSAParameter.getInstance(prik.getPrivateKeyAlgorithm().getParameters());
        this.algorithm = "DSA";
        this.bitLength = params.getP().bitLength();
        return;
      }

      // ECDSA (length of N)
      if (alg.getId().equals("1.2.840.10045.2.1")) {
        ASN1ObjectIdentifier curveOid = ASN1ObjectIdentifier.getInstance(prik.getPrivateKeyAlgorithm().getParameters());
        X9ECParameters ec = ECNamedCurveTable.getByOID(curveOid);
        this.algorithm = "ECDSA";
        this.bitLength = ec.getN().bitLength();
        this.curve = ECNamedCurveTable.getName(curveOid);
        return;
      }

      // Ed25519 (fixed 256 bit for public)
      if (alg.equals(EdECObjectIdentifiers.id_Ed25519)) {
        this.algorithm = "Ed25519";
        this.bitLength = 256;
        this.curve = "Ed25519";
        return;
      }

      _logger.error("KeyInfo Service: a key algorithm of a private key could not be determined");
      throw new IllegalArgumentException("Unsupported key algorithm");
    }

  }

  // getKeyInfo: ParsedKeyClass helper for parsing keys from a base64Blob (only OpenSSH for now)
  private class ParsedKeyClass {

    private final String base64Blob;
    private ASN1Encodable key;
    private KeyKind kind;


    ParsedKeyClass(String base64String) throws IOException {
      this.base64Blob = this.preprocessBlob(base64String);
      this.parseBase64(base64Blob);
    }


    String preprocessBlob(String blob) {
      if (blob.startsWith("ssh-")) {
        String[] algsplit = blob.split("\\s");
        if (algsplit.length == 2 && !algsplit[1].trim().isEmpty()) {
          return (algsplit[1].trim());
        }
      }
      return blob;
    }


    private void parseBase64(String input) throws IOException {

      // get byte representation of base64 blob
      byte[] decoded = Base64.getDecoder().decode(input);

      // ---- Attempt OpenSSH public key ----
      try {
        AsymmetricKeyParameter akp = OpenSSHPublicKeyUtil.parsePublicKey(decoded);
        SubjectPublicKeyInfo spki = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(akp);
        this.key = spki;
        this.kind = KeyKind.PUBLIC;
        return;
      } catch (Exception ignored) {
        if (_logger.isDebugEnabled())
          _logger.debug("An exception occured during parsing", ignored);
      }

      // ---- Attempt OpenSSH private key ----
      try {
        AsymmetricKeyParameter akp = OpenSSHPrivateKeyUtil.parsePrivateKeyBlob(decoded);
        PrivateKeyInfo pki = PrivateKeyInfoFactory.createPrivateKeyInfo(akp);
        this.key = pki;
        this.kind = KeyKind.PRIVATE;
        return;
      } catch (Exception ignored) {
        if (_logger.isDebugEnabled())
          _logger.debug("An exception occured during parsing", ignored);
      }

      _logger.error("KeyInfo Service: a key format could not be read");
      throw new IllegalArgumentException("Unsupported key encoding");
    }
  }

}
