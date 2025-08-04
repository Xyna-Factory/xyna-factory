/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025  Xyna GmbH, Germany
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

import base.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.LabelAnnotation;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.Class;
import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.IllegalArgumentException;
import java.lang.NoSuchFieldException;
import java.lang.NoSuchMethodException;
import java.lang.SecurityException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;
import xint.crypto.exceptions.AESCryptoException;
import xint.crypto.AESCryptoServiceOperation;
import com.gip.xyna.XynaFactory;
import xint.crypto.parameter.AESCryptoParameter;

import java.security.MessageDigest;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.Cipher;
import java.util.Base64;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.BadPaddingException;



public class AESCryptoServiceOperationImpl implements ExtendedDeploymentTask, AESCryptoServiceOperation {
    private static Logger logger = Logger.getLogger(AESCryptoServiceOperationImpl.class);

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

    public Text aESDecrypt(Text encryptedStringIn, AESCryptoParameter aESCryptoParameter2) throws AESCryptoException {
	byte[] key;
	MessageDigest sha = null;
	SecretKeySpec secretKey;
	int keySize = aESCryptoParameter2.getKeySize(); // in bytes: AES128 = 16B, AES192 = 24B, AES256 = 32B
        if( keySize != 128 && keySize != 192 && keySize != 256){
            logger.warn("AES Decrypt: Provided key size with AESCryptoParameter invalid (" + keySize + " vs. 128|192|256)! Defaulting to 256bit.");
            keySize = 256; // fallback to AES256 in case the parameter contains invalid values

        }
	String secret = retrieveAESSecret(aESCryptoParameter2.getSecureStorageIdentifier());
	String strToDecrypt = encryptedStringIn.getText();
	Text originalString = new Text();

	try {
	    key = secret.getBytes("UTF-8");
	    sha = MessageDigest.getInstance("SHA-256");
	    key = sha.digest(key);
	    key = Arrays.copyOf(key, keySize/8); 
	    secretKey = new SecretKeySpec(key, "AES");
	    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    cipher.init(Cipher.DECRYPT_MODE, secretKey);
	    String decryptedStr = new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
	    originalString.setText(decryptedStr);
	} 
	catch (NoSuchAlgorithmException e) {
	    throw new AESCryptoException("AES Decrypt: No such algorithm");
	} 
	catch (UnsupportedEncodingException e) {
	    throw new AESCryptoException("AES Decrypt: Unsupported encoding");
	}
	catch (NoSuchPaddingException e) {
	    throw new AESCryptoException("AES Decrypt: No such padding");
	}
	catch (InvalidKeyException e) {
	    throw new AESCryptoException("AES Decrypt: Invalid Key");
	}
	catch (IllegalBlockSizeException e) {
	    throw new AESCryptoException("AES Decrypt: Illegal block size");
	}
	catch (BadPaddingException e) {
	    throw new AESCryptoException("AES Decrypt: Bad padding");
	}

	return originalString;
    }

    public Text aESEncrypt(Text originalStringIn, AESCryptoParameter aESCryptoParameter) throws AESCryptoException {
	byte[] key;
        MessageDigest sha = null;
        SecretKeySpec secretKey;
	int keySize = aESCryptoParameter.getKeySize(); // in bytes: AES128 = 16B, AES192 = 24B, AES256 = 32B
	if( keySize != 128 && keySize != 192 && keySize != 256){
	    logger.warn("AES Encrypt: Provided key size with AESCryptoParameter invalid (" + keySize + " vs. 128|192|256)! Defaulting to 256bit.");
	    keySize = 256; // fallback to AES256 in case the parameter contains invalid values

	}
        String secret = retrieveAESSecret(aESCryptoParameter.getSecureStorageIdentifier());
	String strToEncrypt = originalStringIn.getText();
	Text encryptedString = new Text();

	try {
	    key = secret.getBytes("UTF-8");
	    sha = MessageDigest.getInstance("SHA-256");
	    key = sha.digest(key);
	    key = Arrays.copyOf(key, keySize/8); 
	    secretKey = new SecretKeySpec(key, "AES");
	    Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	    cipher.init(Cipher.ENCRYPT_MODE, secretKey);
	    encryptedString.setText(Base64.getEncoder().encodeToString(cipher.doFinal(strToEncrypt.getBytes("UTF-8"))));
	} 
	catch (NoSuchAlgorithmException e) {
	    throw new AESCryptoException("AES Encrypt: No such algorithm");
	} 
	catch (UnsupportedEncodingException e) {
	    throw new AESCryptoException("AES Encrypt: Unsupported encoding");
	}
	catch (NoSuchPaddingException e) {
	    throw new AESCryptoException("AES Encrypt: No such padding");
	}
	catch (InvalidKeyException e) {
	    throw new AESCryptoException("AES Encrypt: Invalid Key");
	}
	catch (IllegalBlockSizeException e) {
	    throw new AESCryptoException("AES Encrypt: Illegal block size");
	}
	catch (BadPaddingException e) {
	    throw new AESCryptoException("AES Encrypt: Bad padding");
	}

	return encryptedString;
    }

    private String retrieveAESSecret(String location) throws AESCryptoException {
	if(location == null || location.trim().isEmpty()){
	    throw new AESCryptoException("AES Secret: No identifier for decryption/encryption secret provided (secure storage location \"crypto.aes\").");
	}
        String secret = (String) XynaFactory.getInstance().getXynaMultiChannelPortal().getSecureStorage().retrieve("crypto.aes", location);
	if(secret == null || secret.isEmpty()){
	    String msg = "AES Secret: Retrieving decryption/encryption secret identified by \"" + location + "\" failed! Secret is null or empty.";
            logger.error(msg);
	    throw new AESCryptoException(msg);
	}
	return secret;
    }
}
