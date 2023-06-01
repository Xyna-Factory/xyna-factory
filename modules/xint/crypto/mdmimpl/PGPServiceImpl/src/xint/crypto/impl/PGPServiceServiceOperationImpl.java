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
package xint.crypto.impl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStore;
import com.gip.xyna.xfmg.exceptions.XFMG_UnknownKeyStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyManagement;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStore;
import com.gip.xyna.xfmg.xfctrl.keymgmt.ModuleManagedKeyStore;
import com.gip.xyna.xint.crypto.FileBasedPublicKeyStoreProvider;
import com.gip.xyna.xint.crypto.FileBasedSecretKeyStoreProvider;
import com.gip.xyna.xint.crypto.PGPDecryptionParameter;
import com.gip.xyna.xint.crypto.PGPDecryptionParameter.PGPDecryptionParameterBuilder;
import com.gip.xyna.xint.crypto.PGPEncryptionParameter;
import com.gip.xyna.xint.crypto.PGPEncryptionParameter.PGPEncryptionParameterBuilder;
import com.gip.xyna.xint.crypto.PGPOperations;
import com.gip.xyna.xint.crypto.PGPOperations.DecryptionResult;
import com.gip.xyna.xint.crypto.PGPSignatureParameter;
import com.gip.xyna.xint.crypto.PGPSignatureParameter.PGPSignatureParameterBuilder;
import com.gip.xyna.xint.crypto.PublicKeyStoreProvider;
import com.gip.xyna.xint.crypto.SecretKeyStoreProvider;

import java.lang.IllegalArgumentException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSignature;

import xact.templates.Document;
import xact.templates.PlainText;
import xint.crypto.parameter.DecryptionParameter;
import xint.crypto.parameter.EncryptionParameter;
import xint.crypto.parameter.PasswordBasedDecryptionParameter;
import xint.crypto.parameter.PasswordBasedEncryptionParameter;
import xint.crypto.parameter.PublicKeyDecryptionParameter;
import xint.crypto.parameter.PublicKeyEncryptionParameter;
import xint.crypto.parameter.SignatureParameter;
import xint.crypto.parameter.SignatureVerificationResult;
import xint.crypto.parameter.enums.AES_256;
import xint.crypto.parameter.enums.BLOWFISH;
import xint.crypto.parameter.enums.CAMELLIA_256;
import xint.crypto.parameter.enums.CAST5;
import xint.crypto.parameter.enums.DES;
import xint.crypto.parameter.enums.DOUBLE_SHA;
import xint.crypto.parameter.enums.HAVAL_5_160;
import xint.crypto.parameter.enums.HashAlgorithm;
import xint.crypto.parameter.enums.IDEA;
import xint.crypto.parameter.enums.MD2;
import xint.crypto.parameter.enums.MD5;
import xint.crypto.parameter.enums.RIPEMD160;
import xint.crypto.parameter.enums.SAFER;
import xint.crypto.parameter.enums.SHA1;
import xint.crypto.parameter.enums.SHA224;
import xint.crypto.parameter.enums.SHA256;
import xint.crypto.parameter.enums.SHA384;
import xint.crypto.parameter.enums.SHA512;
import xint.crypto.parameter.enums.SymmetricKeyAlgorithm;
import xint.crypto.parameter.enums.TIGER_192;
import xint.crypto.parameter.enums.TRIPLE_DES;
import xint.crypto.parameter.enums.TWOFISH;
import xint.crypto.PGPServiceServiceOperation;
import xint.crypto.exceptions.AlgorithmExecutionException;
import xint.crypto.exceptions.IOException;
import xint.crypto.exceptions.KeyStoreAccessException;
import xint.crypto.exceptions.NoSuchKeyException;


public class PGPServiceServiceOperationImpl implements ExtendedDeploymentTask, PGPServiceServiceOperation {

  private final static Logger logger = CentralFactoryLogging.getLogger(PGPServiceServiceOperationImpl.class);
  
  
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
  
  private final static Charset charset = Charset.forName("UTF-8");

  public Container decrypt(Document document, DecryptionParameter decryptionParameter) throws XynaExceptionBase {
    try {
      DecryptionResult result = PGPOperations.decrypt(documentToByte(document), 
                                                      convertDecryptionParameter(decryptionParameter));
     if (result.getLiteralData().size() > 1) {
       logger.warn("Decryption found several literals ("+result.getLiteralData().size()+"), only returning the first.");
     }
     Document doc;
     if (result.getLiteralData().size() <= 0) {
       logger.debug("PGPPacket contained no literal, returnig input document as result");
       doc = document;
     } else {
       doc = byteToDocument(result.getLiteralData().get(0));
     }
     return new Container(doc,
                          convertSignatureVerificationResult(result.getSignatureVerifactionResults()));
    } catch (com.gip.xyna.xint.crypto.exceptions.NoSuchKeyException e) {
      throw new NoSuchKeyException(e.getUserOrKeyID());
    } catch (java.io.IOException e) {
       throw new IOException(e.getMessage(), e);
    } catch (PGPException e) {
      throw new AlgorithmExecutionException(e.getMessage(), e);
    }
  }


  public Document encrypt(Document document, EncryptionParameter encryptionParameter) throws XynaExceptionBase {
    try {
      byte[] result = PGPOperations.encrypt(documentToByte(document), buildEncryptionParameter(encryptionParameter).build());
      return byteToDocument(result);
    } catch (com.gip.xyna.xint.crypto.exceptions.NoSuchKeyException e) {
      throw new NoSuchKeyException(e.getUserOrKeyID());
    } catch (java.io.IOException e) {
       throw new IOException(e.getMessage(), e);
    } catch (PGPException e) {
      throw new AlgorithmExecutionException(e.getMessage(), e);
    }
  }

  public Document encryptAndSign(Document document, EncryptionParameter encryptionParameter, SignatureParameter signatureParameter) throws XynaExceptionBase {
    try {
      byte[] result = PGPOperations.encrypt(documentToByte(document), appendSignatureParameter(buildEncryptionParameter(encryptionParameter), signatureParameter).build());
      return byteToDocument(result);
    } catch (com.gip.xyna.xint.crypto.exceptions.NoSuchKeyException e) {
      throw new NoSuchKeyException(e.getUserOrKeyID());
    } catch (java.io.IOException e) {
       throw new IOException(e.getMessage(), e);
    } catch (PGPException e) {
      throw new AlgorithmExecutionException(e.getMessage(), e);
    }
  }
  
  

  private static byte[] documentToByte(Document document) {
    return document.getText().getBytes(charset);
  }
  
  private static Document byteToDocument(byte[] message) {
    return new Document.Builder().documentType(new PlainText()).text(new String(message, charset)).instance();
  }

  private static PGPDecryptionParameter convertDecryptionParameter(DecryptionParameter decryptionParameter) throws KeyStoreAccessException {
    PGPDecryptionParameterBuilder builder = PGPDecryptionParameter.builder();
    if (decryptionParameter instanceof PasswordBasedDecryptionParameter) {
      builder.password(((PasswordBasedDecryptionParameter) decryptionParameter).getPassword());
    }
    if (decryptionParameter instanceof PublicKeyDecryptionParameter) {
      PublicKeyDecryptionParameter pkdp = (PublicKeyDecryptionParameter) decryptionParameter;
      builder.provider(openAsSecretKeyStoreProvider(resolveKeyStoreName(pkdp.getSecretKeystore())));
      if (pkdp.getSignatureVerificationKeystore() != null &&
          !pkdp.getSignatureVerificationKeystore().isEmpty()) {
        builder.signatureProvider(openAsPublicKeyStoreProvider(resolveKeyStoreName(pkdp.getSignatureVerificationKeystore())));
      }
    }
    return builder.build();
  }
  
  private static ModuleManagedKeyStore resolveKeyStoreName(String keyStoreName) throws KeyStoreAccessException {
    KeyManagement keyMgmt = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getKeyManagement();
    try {
      KeyStore keyStore = keyMgmt.getKeyStore(keyStoreName);
      if (keyStore != null &&
          keyStore instanceof ModuleManagedKeyStore) {
        return (ModuleManagedKeyStore) keyStore;
      }
    } catch (XFMG_UnknownKeyStore | XFMG_UnknownKeyStoreType e) {
      throw new KeyStoreAccessException(keyStoreName, e);
    }
    throw new KeyStoreAccessException(keyStoreName, new XFMG_UnknownKeyStore(keyStoreName));
  }
  
  private static SecretKeyStoreProvider openAsSecretKeyStoreProvider(ModuleManagedKeyStore mmks) {
    Optional<String> passphrase;
    if (mmks.getPassphrase() == null ||
        mmks.getPassphrase().isEmpty()) {
      passphrase = Optional.empty();
    } else {
      passphrase = Optional.of(mmks.getPassphrase());
    }
    return new FileBasedSecretKeyStoreProvider(mmks.getFile(), passphrase);
  }
  
  private static PublicKeyStoreProvider openAsPublicKeyStoreProvider(ModuleManagedKeyStore mmks) {
    return new FileBasedPublicKeyStoreProvider(mmks.getFile());
  }
  
  private static XynaObject convertSignatureVerificationResult(List<Pair<PGPSignature, Boolean>> signatureVerifactionResults) {
    SignatureVerificationResult.Builder builder = new SignatureVerificationResult.Builder();
    builder.messageWasSigned(signatureVerifactionResults.size() > 0);
    for (Pair<PGPSignature, Boolean> pair : signatureVerifactionResults) {
      if (!pair.getSecond()) {
        return builder.signatureWasVerified(false).instance();
      }
    }
    return builder.signatureWasVerified(true).instance();
  }
  
  private static PGPEncryptionParameterBuilder buildEncryptionParameter(EncryptionParameter encryptionParameter) throws KeyStoreAccessException {
    PGPEncryptionParameterBuilder builder = PGPEncryptionParameter.builder();
    if (encryptionParameter.getEncryptionAlgorithm() != null) {
      builder.encryption(convertEncryptionAlgorithm(encryptionParameter.getEncryptionAlgorithm()));
    }
    if (encryptionParameter instanceof PasswordBasedEncryptionParameter) {
      builder.password(((PasswordBasedEncryptionParameter) encryptionParameter).getPassword());
    }
    if (encryptionParameter instanceof PublicKeyEncryptionParameter) {
      PublicKeyEncryptionParameter pkeParams = (PublicKeyEncryptionParameter) encryptionParameter;
      builder.recipient(pkeParams.getRecipient())
             .provider(openAsPublicKeyStoreProvider(resolveKeyStoreName(pkeParams.getPublicKeystore())));
    }
    return builder;
  }
  
  private static PGPEncryptionParameterBuilder appendSignatureParameter(PGPEncryptionParameterBuilder build,
                                                                 SignatureParameter signatureParameter) throws KeyStoreAccessException {
    return build.signatureParams(convertSignatureParameter(signatureParameter));
  }

  private static PGPSignatureParameter convertSignatureParameter(SignatureParameter signatureParameter) throws KeyStoreAccessException {
    PGPSignatureParameterBuilder builder = PGPSignatureParameter.builder()
                                                                .provider(openAsSecretKeyStoreProvider(resolveKeyStoreName(signatureParameter.getSecretKeystore())))
                                                                .sender(signatureParameter.getSigner());
    if (signatureParameter.getHashAlgorithm() != null) {
      builder.hash(convertHashAlgorithm(signatureParameter.getHashAlgorithm()));
    }
    return builder.build();
  }


  private static com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm convertEncryptionAlgorithm(SymmetricKeyAlgorithm encryptionAlgorithm) {
    if (encryptionAlgorithm instanceof AES_256) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.AES_256;
    }
    if (encryptionAlgorithm instanceof BLOWFISH) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.BLOWFISH;
    }
    if (encryptionAlgorithm instanceof CAMELLIA_256) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.CAMELLIA_256;
    }
    if (encryptionAlgorithm instanceof CAST5) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.CAST5;
    }
    if (encryptionAlgorithm instanceof DES) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.DES;
    }
    if (encryptionAlgorithm instanceof IDEA) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.IDEA;
    }
    if (encryptionAlgorithm instanceof SAFER) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.SAFER;
    }
    if (encryptionAlgorithm instanceof TRIPLE_DES) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.TRIPLE_DES;
    }
    if (encryptionAlgorithm instanceof TWOFISH) {
      return com.gip.xyna.xint.crypto.SymmetricKeyAlgorithm.TWOFISH;
    }
    throw new IllegalArgumentException("Unmatched SymmetricKeyAlgorithm '" + encryptionAlgorithm.getClass().getName() + "'");
  }
  
  private static com.gip.xyna.xint.crypto.HashAlgorithm convertHashAlgorithm(HashAlgorithm hashAlgorithm) {
    if (hashAlgorithm instanceof DOUBLE_SHA) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.DOUBLE_SHA;
    }
    if (hashAlgorithm instanceof HAVAL_5_160) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.HAVAL_5_160;
    }
    if (hashAlgorithm instanceof MD2) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.MD2;
    }
    if (hashAlgorithm instanceof MD5) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.MD5;
    }
    if (hashAlgorithm instanceof RIPEMD160) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.RIPEMD160;
    }
    if (hashAlgorithm instanceof SHA1) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.SHA1;
    }
    if (hashAlgorithm instanceof SHA224) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.SHA224;
    }
    if (hashAlgorithm instanceof SHA256) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.SHA256;
    }
    if (hashAlgorithm instanceof SHA384) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.SHA384;
    }
    if (hashAlgorithm instanceof SHA512) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.SHA512;
    }
    if (hashAlgorithm instanceof TIGER_192) {
      return com.gip.xyna.xint.crypto.HashAlgorithm.TIGER_192;
    }
    throw new IllegalArgumentException("Unmatched HashAlgorithm '" + hashAlgorithm.getClass().getName() + "'");
  }

}
