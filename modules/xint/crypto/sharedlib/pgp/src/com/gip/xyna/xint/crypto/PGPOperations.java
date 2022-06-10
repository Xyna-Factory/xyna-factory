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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPBEEncryptedData;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBEDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPBEKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyKeyEncryptionMethodGenerator;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.streams.StreamUtils;
import com.gip.xyna.utils.streams.TeeInputStream;
import com.gip.xyna.xint.crypto.exceptions.NoSuchKeyException;


public class PGPOperations {
  
  private final static Logger logger = CentralFactoryLogging.getLogger(PGPOperations.class);
  
  
  private final static int BUFFER_SIZE = 8192;
  
  public static byte[] encrypt(byte[] clearMsg, PGPEncryptionParameter encryptionParams) throws IOException, PGPException, NoSuchKeyException {
    ByteArrayInputStream bais = new ByteArrayInputStream(clearMsg);
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      encrypt(bais, baos, encryptionParams);
      return baos.toByteArray();
    }
  }


  public static void encrypt(InputStream in, OutputStream out, PGPEncryptionParameter encryptionParams) throws IOException, PGPException, NoSuchKeyException {
    try (OutputStream potentiallyArmored = encryptionParams.doWrapInArmor() ? new ArmoredOutputStream(out) : out) {
      BcPGPDataEncryptorBuilder encBuilder = new BcPGPDataEncryptorBuilder(encryptionParams.getEncryption().getPgpTag())
                      .setWithIntegrityPacket(true) // configurable?
                      //.setSecureRandom(???)
                      ;
      PGPEncryptedDataGenerator encGen = new PGPEncryptedDataGenerator(encBuilder);
      if (encryptionParams.getRecipientParameter().isPresent()) {
        PGPPublicKey key = encryptionParams.getRecipientParameter().get().findPublicKey(PublicKeyCapability.ENCRYPT);
        BcPublicKeyKeyEncryptionMethodGenerator pubKeyKeyMethodGen = new BcPublicKeyKeyEncryptionMethodGenerator(key);
        encGen.addMethod(pubKeyKeyMethodGen);
      }
      if (encryptionParams.getPassword().isPresent()) {
        BcPBEKeyEncryptionMethodGenerator passwordKeyMethodGen = new BcPBEKeyEncryptionMethodGenerator(encryptionParams.getPassword().get().toCharArray());
        encGen.addMethod(passwordKeyMethodGen);
      }
      try (OutputStream encOut = encGen.open(potentiallyArmored, new byte[BUFFER_SIZE])) {
        PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(encryptionParams.getCompression().getPgpTag());
        try (OutputStream compOut = comData.open(encOut)) {
          if (encryptionParams.getSignatureParams().isPresent()) {
            writeSignedObject(in, compOut, encryptionParams.getSignatureParams().get());
          } else {
            writeLiteralData(in, compOut);
          }
        }
      }
    }
  }
  
  
  public static DecryptionResult decrypt(InputStream in, OutputStream out, PGPDecryptionParameter decryptionParameter) throws IOException, PGPException, NoSuchKeyException {
    DecryptionContext decContext = new DecryptionContext(decryptionParameter.getUserParameter(), decryptionParameter.getPassword(), decryptionParameter.createDebugResult(), out); 
    decrypt(in, decContext);
    return decContext.getResult();
  }
  
  
  public static DecryptionResult decrypt(byte[] encryptedMsg, PGPDecryptionParameter decryptionParameter) throws IOException, PGPException, NoSuchKeyException {
    ByteArrayInputStream bais = new ByteArrayInputStream(encryptedMsg);
    DecryptionContext decContext = new DecryptionContext(decryptionParameter.getUserParameter(), decryptionParameter.getPassword(), decryptionParameter.createDebugResult()); 
    decrypt(bais, decContext);
    return decContext.getResult();
  }
  
  private static void decrypt(InputStream in, DecryptionContext decContext) throws IOException, PGPException, NoSuchKeyException {
    InputStream decIn = PGPUtil.getDecoderStream(in);
    PGPObjectFactory pgpFact = new PGPObjectFactory(decIn, new BcKeyFingerprintCalculator());
    for (Object object : pgpFact) {
      DecryptionStructureLogNode child = handleObject(object, decContext);
      decContext.logRoot.getChildren().add(child);
    }
  }
  
  
  private static DecryptionStructureLogNode handleObject(Object object, DecryptionContext context) throws PGPException, IOException, NoSuchKeyException {
    logger.trace("handleObject: " + object);
    PGPObjectType type = PGPObjectType.determineByObject(object);
    DecryptionStructureLogNode node = new DecryptionStructureLogNode(type);
    switch (type) {
      case PGPCompressedData :
        PGPCompressedData compData = (PGPCompressedData) object;
        PGPObjectFactory uncompressedFact = new JcaPGPObjectFactory(compData.getDataStream());
        for (Object newObject : uncompressedFact) {
          DecryptionStructureLogNode child = handleObject(newObject, context);
          node.getChildren().add(child);
        }
        break;
      case PGPEncryptedDataList :
        PGPEncryptedDataList encDataList = (PGPEncryptedDataList) object;
        for (Object encData : encDataList) {
          DecryptionStructureLogNode child = handleObject(encData, context);
          node.getChildren().add(child);
        }
        break;
      case PGPPublicKeyEncryptedData :
        PGPPublicKeyEncryptedData pkEnc = (PGPPublicKeyEncryptedData) object;
        PGPPrivateKey privKey;
        if (pkEnc.getKeyID() == 0) {
          //privKey = findHiddenRecipient(context, pkEnc);
          // will not work as testing advances the stream...
          // test till we encounter hidden and then repeat process and force use of each known private key
          // or support user given keys (not only stores for decryption and only support hidden if one has been given)
          throw new UnsupportedOperationException("Hidden recipients not supported");
        } else {
          try {
            privKey = context.getPrivateKey(pkEnc.getKeyID());
          } catch (NoSuchKeyException e) {
            // might be multi recipient
            context.rememberException(e);
            return node;
          }
        }
        InputStream clear = pkEnc.getDataStream(new BcPublicKeyDataDecryptorFactory(privKey));
        PGPObjectFactory decryptedFact = new JcaPGPObjectFactory(clear);
        for (Object newObject : decryptedFact) {
          DecryptionStructureLogNode child = handleObject(newObject, context);
          node.getChildren().add(child);
        }
        break;
      case PGPPBEEncryptedData :
        PGPPBEEncryptedData pbeEnc = (PGPPBEEncryptedData) object;
        if (context.getPassword().isEmpty()) {
          throw new IllegalStateException("Password based encryption encountered but no password is given");
        }
        try {
        InputStream clearPbe = pbeEnc.getDataStream(new BcPBEDataDecryptorFactory(context.getPassword().get().toCharArray(), new BcPGPDigestCalculatorProvider()));
        PGPObjectFactory pbeDecryptedFact = new JcaPGPObjectFactory(clearPbe);
        for (Object newObject : pbeDecryptedFact) {
          DecryptionStructureLogNode child = handleObject(newObject, context);
          node.getChildren().add(child);
        }
        } catch (PGPException e) {
          throw e;
        }
        break;
      case PGPOnePassSignatureList :
        PGPOnePassSignatureList onePassSignatureList = (PGPOnePassSignatureList)object;
        for (PGPOnePassSignature signature : onePassSignatureList) {
          DecryptionStructureLogNode child = handleObject(signature, context);
          node.getChildren().add(child);
        }
        break;
      case PGPOnePassSignature :
        PGPOnePassSignature onePassSignature = (PGPOnePassSignature) object;
        context.addSignature(onePassSignature);
        break;
      case PGPLiteralData :
        PGPLiteralData literalData = (PGPLiteralData)object;
        InputStream dIn = literalData.getInputStream();
        logger.trace("reading literal: " + literalData.getFileName() + "@" + literalData.getModificationTime());
        // read in and update signatures
        context.readLiteralData(dIn);
        break;
      case PGPSignatureList :
        PGPSignatureList signatureList = (PGPSignatureList) object;
        for (PGPSignature signature : signatureList) {
          DecryptionStructureLogNode child = handleObject(signature, context);
          node.getChildren().add(child);
        }
        break;
      case PGPSignature :
        PGPSignature signature = (PGPSignature) object;
        context.verify(signature);
        break;
      default :
        logger.warn("Unhandled PGPObjectType " + type);
        break;
    }
    return node;
  }
  
  
  private static class DecryptionContext {

    private final Optional<PGPKeyStoreParameter> userParameter;
    private final Optional<String> symmetricPassword;
    private final boolean createDebugResult;
    private final List<PGPOnePassSignature> onePassSignatures;
    private final List<byte[]> literalData;
    private final List<Pair<PGPSignature, Boolean>> signatureVerifactionResults;
    private final DecryptionStructureLogNode logRoot;
    private final OutputStream customOutput;
    private NoSuchKeyException exceptionIfNoOutput;
    
    public DecryptionContext(Optional<PGPKeyStoreParameter> userParameter, Optional<String> symmetricPassword, boolean createDebugResult) { 
      this(userParameter, symmetricPassword, createDebugResult, null);
    }
    
    public DecryptionContext(Optional<PGPKeyStoreParameter> userParameter, Optional<String> symmetricPassword, boolean createDebugResult, OutputStream customOutput) { 
      this.userParameter = userParameter;
      this.symmetricPassword = symmetricPassword;
      this.createDebugResult = createDebugResult;
      this.customOutput = customOutput;
      onePassSignatures = new ArrayList<>();
      literalData = new ArrayList<>();
      signatureVerifactionResults = new ArrayList<>();
      logRoot = new DecryptionStructureLogNode(null);
    }

    public PGPPrivateKey getPrivateKey(long keyID) throws NoSuchKeyException {
      return userParameter.orElseThrow(() -> new NoSuchKeyException(keyID)).getPrivateKey(keyID);
    }
    
    public PGPPublicKey getPublicKey(long keyID) throws NoSuchKeyException {
      return userParameter.orElseThrow(() -> new NoSuchKeyException(keyID)).getPublicKey(keyID);
    }
    
    public Optional<String> getPassword() {
      return symmetricPassword;
    }

    public void verify(PGPSignature signature) throws PGPException {
      // we can have booth several onePassSignatures and several signatures
      // let's assume at least one signature should match
      for (PGPOnePassSignature onePassSignature : onePassSignatures) {
        boolean verification = onePassSignature.verify(signature);
        if (verification) {
          signatureVerifactionResults.add(Pair.of(signature, Boolean.TRUE));
          return;
        }
      }
      signatureVerifactionResults.add(Pair.of(signature, Boolean.FALSE));
    }

    
    private boolean hasCustomOutputStream() {
      return customOutput != null;
    }
    
    
    public void readLiteralData(InputStream is) throws IOException, PGPException, NoSuchKeyException {
      for (PGPOnePassSignature onePassSignature : onePassSignatures) {
        onePassSignature.init(new BcPGPContentVerifierBuilderProvider(), getPublicKey(onePassSignature.getKeyID()));
      }
      
      ByteArrayOutputStream baos = null;
      OutputStream os;
      if (hasCustomOutputStream()) {
        os = wrapOutputForSignatures(customOutput);
      } else {
        baos = new ByteArrayOutputStream();
        os = wrapOutputForSignatures(baos);  
      }
      
      StreamUtils.copy(is, os);
      
      if (!hasCustomOutputStream()) {
        literalData.add(baos.toByteArray());
      }
    }
    
    
    private OutputStream wrapOutputForSignatures(OutputStream os) {
      OutputStream current = os;
      for (PGPOnePassSignature onePassSignature : onePassSignatures) {
        current = new OnePassSignatureUpdateStream(current, onePassSignature);
      }
      return current;
    }

    public void addSignature(PGPOnePassSignature onePassSignature) {
      onePassSignatures.add(onePassSignature);
    }
    
    public DecryptionResult getResult() throws NoSuchKeyException {
      if (!hasCustomOutputStream() &&
          literalData.size() <= 0 &&
          exceptionIfNoOutput != null) {
        throw exceptionIfNoOutput;
      }
      if (createDebugResult) {
        return new DebugDecryptionResult(literalData, signatureVerifactionResults, logRoot);
      } else {
        return new DecryptionResult(literalData, signatureVerifactionResults);
      }
    }
    
    
    public void rememberException(NoSuchKeyException e) {
      exceptionIfNoOutput = e;
    }
    
  }
  
  
  private static void writeLiteralData(InputStream in, OutputStream out) throws IOException, PGPException {
    PGPLiteralDataGenerator lData = new PGPLiteralDataGenerator();
    try (OutputStream  pOut = lData.open(out, PGPLiteralData.BINARY, "_CONSOLE", new Date(), new byte[BUFFER_SIZE])) {
      StreamUtils.copy(in, pOut);
    }
  }
  
  
  private static void writeSignedObject(InputStream in, OutputStream out, PGPSignatureParameter signatureParams)
                  throws PGPException, IOException, NoSuchKeyException {
    try (BCPGOutputStream bcOut = new BCPGOutputStream(out)) {
      PGPSecretKey signatureKey = signatureParams.getSignatureSecretKey();
      PGPContentSignerBuilder sGenBuilder = new BcPGPContentSignerBuilder(signatureKey.getPublicKey().getAlgorithm(),
                                                                          signatureParams.getHashAlgorithm().getPgpTag());
      PGPSignatureGenerator sGen = new PGPSignatureGenerator(sGenBuilder);
      sGen.init(PGPSignature.BINARY_DOCUMENT, signatureParams.getSignaturePrivateKey());
      sGen.generateOnePassVersion(false).encode(bcOut);
      try (SignatureCreationStream signatureUpdater = new SignatureCreationStream(sGen);
           TeeInputStream tInput = new TeeInputStream(in, signatureUpdater)) {
        writeLiteralData(tInput, bcOut);
        sGen.generate().encode(bcOut);
      }
    }
  }
  
  
  public static class DecryptionResult {
    
    private final List<byte[]> literalData; // will be empty if a custom OutputStream was specified
    private final List<Pair<PGPSignature, Boolean>> signatureVerifactionResults;
    
    protected DecryptionResult(List<byte[]> literalData, List<Pair<PGPSignature, Boolean>> signatureVerifactionResults) {
      this.literalData = literalData;
      this.signatureVerifactionResults = signatureVerifactionResults;
    }
    
    public List<byte[]> getLiteralData() {
      return literalData;
    }

    public List<Pair<PGPSignature, Boolean>> getSignatureVerifactionResults() {
      return signatureVerifactionResults;
    }
    
  }
  
  
  public static class DebugDecryptionResult extends DecryptionResult {

    private final DecryptionStructureLogNode logRoot;
    
    protected DebugDecryptionResult(List<byte[]> literalData,
                                    List<Pair<PGPSignature, Boolean>> signatureVerifactionResults,
                                    DecryptionStructureLogNode logRoot) {
      super(literalData, signatureVerifactionResults);
      this.logRoot = logRoot;
    }
    
    public DecryptionStructureLogNode getLogRoot() {
      return logRoot;
    }
  }
  
  
  public static class DecryptionStructureLogNode {
    
    public final static char CHILDREN_START_MARKER = '[';
    public final static char CHILDREN_SEPERATION_MARKER = ',';
    public final static char CHILDREN_END_MARKER = ']';
    
    private final PGPObjectType type;
    private final List<DecryptionStructureLogNode> children;
    
    DecryptionStructureLogNode(PGPObjectType type) {
      this.type = type;
      this.children = new ArrayList<>();
    }
    
    public PGPObjectType getType() {
      return type;
    }
    
    public List<DecryptionStructureLogNode> getChildren() {
      return children;
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (type != null) {
        sb.append(type.name());  
      }
      if (children.size() > 0) {
        sb.append(CHILDREN_START_MARKER);
        for (DecryptionStructureLogNode child : children) {
          sb.append(child.toString())
            .append(CHILDREN_SEPERATION_MARKER);
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(CHILDREN_END_MARKER);
      }
      return sb.toString();
    }
  }
  
}
