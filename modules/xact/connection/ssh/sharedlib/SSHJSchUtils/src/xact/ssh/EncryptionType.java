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
package xact.ssh;

import com.jcraft.jsch.KeyPair;


public enum EncryptionType {
  RSA("RSA", KeyPair.RSA, "ssh-rsa", "xact.ssh.RSA"), 
  DSA("DSA", KeyPair.DSA, "ssh-dss", "xact.ssh.DSA"),
  UNKNOWN("UNKNOWN", KeyPair.UNKNOWN, "unknown", "xact.ssh.EncryptionAlgorithmType");
  
  private final String stringRepresentation;
  private final int numericRepresentation;
  private final String sshStringRepresentation;
  private final String xynaFqClassName;
  
  private EncryptionType(String stringRepresentation, int numericRepresentation, String sshStringRepresentation, String xynaFqClassName) {
    this.stringRepresentation = stringRepresentation;
    this.numericRepresentation = numericRepresentation;
    this.sshStringRepresentation = sshStringRepresentation;
    this.xynaFqClassName = xynaFqClassName;
  }
  
  public String getStringRepresentation() {
    return stringRepresentation;
  }
  
  public int getNumericRepresentation() {
    return numericRepresentation;
  }
  
  public String getSshStringRepresentation() {
    return sshStringRepresentation;
  }
  
  public static EncryptionType getByStringRepresentation(String representation) {
    for (EncryptionType mode : values()) {
      if (mode.stringRepresentation.equalsIgnoreCase(representation)) {
        return mode;
      }
    }
    return UNKNOWN;
  }
  
  public static EncryptionType getBySshStringRepresentation(String representation) {
    for (EncryptionType mode : values()) {
      if (mode.sshStringRepresentation.equals(representation)) {
        return mode;
      }
    }
    return UNKNOWN;
  }
  
  public static EncryptionType getByNumericRepresentation(int representation) {
    for (EncryptionType mode : values()) {
      if (mode.numericRepresentation == representation) {
        return mode;
      }
    }
    return UNKNOWN;
  }
  
  
  public static EncryptionType getByXynaFqClassNamen(String fqClassName) {
    for (EncryptionType mode : values()) {
      if (mode.xynaFqClassName.equals(fqClassName)) {
        return mode;
      }
    }
    return UNKNOWN;
  }
  
}
