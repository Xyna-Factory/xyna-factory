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
package xact.ssh.generatekeypackage;



import java.io.IOException;
import java.io.StringWriter;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.util.Base64;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;


//Conversion of the RSA/DSA keys into a format compatible with the JSCH-based SSH application.
public class ExtdKeyGenerationHelperClass {

  private static int Len(int len) {
    int Response = 1;
    if (len <= 0x7f) {
      return Response;
    }
    while (len > 0) {
      len >>>= 8;
      Response++;
    }
    return Response;
  }


  private static int getLen(byte[] aByte, int ind, int len) {
    int counter = Len(len) - 1;
    if (counter == 0) {
      aByte[ind++] = (byte) len;
      return ind;
    }
    aByte[ind++] = (byte) (0x80 | counter);
    int response = ind + counter;
    while (counter > 0) {
      aByte[ind + counter - 1] = (byte) (len & 0xff);
      len >>>= 8;
      counter--;
    }
    return response;
  }


  private static int getSeq(byte[] aByte, int ind, int len) {
    aByte[ind++] = 0x30;
    ind = getLen(aByte, ind, len);
    return ind;
  }


  private static int getInt(byte[] aByteI, int ind, byte[] aByteII) {
    aByteI[ind++] = 0x02;
    ind = getLen(aByteI, ind, aByteII.length);
    System.arraycopy(aByteII, 0, aByteI, ind, aByteII.length);
    ind += aByteII.length;
    return ind;
  }


  private static byte[] getPrivateKeyDSA(PrivateKey privateKey, PublicKey publicKey) {
    byte[] X = ((DSAPrivateKey) privateKey).getX().toByteArray();
    byte[] Y = ((DSAPublicKey) publicKey).getY().toByteArray();
    DSAParams parameters = ((DSAKey) privateKey).getParams();
    byte[] P = parameters.getP().toByteArray();
    byte[] Q = parameters.getQ().toByteArray();
    byte[] G = parameters.getG().toByteArray();
    int inSize = 1 + Len(1) + 1 + 1 + Len(P.length) + P.length + 1 + Len(Q.length) + Q.length + 1 + Len(G.length) + G.length + 1
        + Len(Y.length) + Y.length + 1 + Len(X.length) + X.length;
    int size = 1 + Len(inSize) + inSize;
    byte[] Response = new byte[size];
    int Counter = 0;
    Counter = getSeq(Response, Counter, inSize);
    Counter = getInt(Response, Counter, new byte[1]);
    Counter = getInt(Response, Counter, P);
    Counter = getInt(Response, Counter, Q);
    Counter = getInt(Response, Counter, G);
    Counter = getInt(Response, Counter, Y);
    Counter = getInt(Response, Counter, X);
    return Response;
  }


  public static String getPrivateKeyDSA_PEM(PrivateKey privateKey, PublicKey publicKey) {
    try {
      byte[] encoded = getPrivateKeyDSA(privateKey, publicKey);
      byte[] encodedBase64 = Base64.getEncoder().encode(encoded);

      String firstLine = "-----BEGIN DSA PRIVATE KEY-----";
      String lastLine = "-----END DSA PRIVATE KEY-----";
      String CR = "\n";

      java.io.ByteArrayOutputStream Private = new java.io.ByteArrayOutputStream();
      Private.write(firstLine.getBytes());
      Private.write(CR.getBytes());
      int counter = 0;
      while (counter < encodedBase64.length) {
        if (counter + 64 < encodedBase64.length) {
          Private.write(encodedBase64, counter, 64);
          Private.write(CR.getBytes());
          counter += 64;
          continue;
        }
        Private.write(encodedBase64, counter, encodedBase64.length - counter);
        Private.write(CR.getBytes());
        break;
      }
      Private.write(lastLine.getBytes());
      Private.write(CR.getBytes());

      String Response = new String(Private.toByteArray(), "UTF-8");
      return Response;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static ExtdKeyPairElement transformKeyPairDSA(java.security.KeyPair Pair) {
    try {
      PublicKey publicKey = Pair.getPublic();
      PrivateKey privateKey = Pair.getPrivate();

      // PrivateKey PEM
      String privateKeyString = getPrivateKeyDSA_PEM(privateKey, publicKey);

      // PublicKey
      byte[] b = new net.schmizz.sshj.common.Buffer.PlainBuffer().putPublicKey(publicKey).getCompactData();
      String publicKeyString = xact.ssh.EncryptionType.DSA.getSshStringRepresentation() + " " + Base64.getEncoder().encodeToString(b);

      ExtdKeyPairElement keyPair = new ExtdKeyPairElement(privateKeyString, publicKeyString);
      return keyPair;

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  public static ExtdKeyPairElement transformKeyPairRSA(java.security.KeyPair Pair) {
    PublicKey pubKey = Pair.getPublic();
    PrivateKey prvKey = Pair.getPrivate();

    // PrivateKey PEM
    String privateKeyString = null;
    try {
      PrivateKey rsapriv = prvKey;
      byte[] rsaprivBytes = rsapriv.getEncoded();

      PrivateKeyInfo rsapkInfo = PrivateKeyInfo.getInstance(rsaprivBytes);
      ASN1Encodable rsaencodable = rsapkInfo.parsePrivateKey();
      ASN1Primitive rsaprimitive = rsaencodable.toASN1Primitive();
      byte[] rsaprivateKeyPKCS1 = rsaprimitive.getEncoded();
      PemObject rsapemObject = new PemObject("RSA PRIVATE KEY", rsaprivateKeyPKCS1);

      StringWriter rsastringWriter = new StringWriter();
      PemWriter rsapemWriter = new PemWriter(rsastringWriter);
      rsapemWriter.writeObject(rsapemObject);
      rsapemWriter.close();
      privateKeyString = rsastringWriter.toString();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    // PublicKey
    byte[] b = new net.schmizz.sshj.common.Buffer.PlainBuffer().putPublicKey(pubKey).getCompactData();
    String publicKeyString = xact.ssh.EncryptionType.RSA.getSshStringRepresentation() + " " + Base64.getEncoder().encodeToString(b);

    ExtdKeyPairElement KeyPair = new ExtdKeyPairElement(privateKeyString, publicKeyString);
    return KeyPair;
  }

};