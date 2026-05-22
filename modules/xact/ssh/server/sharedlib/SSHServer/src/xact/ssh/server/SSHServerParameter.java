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
package xact.ssh.server;

import java.util.List;

import org.apache.sshd.common.cipher.BuiltinCiphers;
import org.apache.sshd.common.mac.BuiltinMacs;
import org.apache.sshd.common.kex.BuiltinDHFactories;
import org.apache.sshd.common.signature.BuiltinSignatures;

import com.gip.xyna.utils.timing.Duration;

public interface SSHServerParameter {

  public enum Auth {
    publickey, password, both, needless;
  }

  String getHostKeyFilename();

  String getHostkeyAlgorithm();

  int getHostkeySize();

  int getPort();

  String getHost();

  public boolean getPasswordAuth();

  public boolean getPublicKeyAuth();

  public boolean getAlwaysAuth();

  public boolean getOTCAuth();

  Duration getIdleTimeout();

  public List<BuiltinSignatures> getAuthAlgoFactories();

  public List<BuiltinDHFactories> getKexFactories();

  public List<BuiltinMacs> getMacFactories();

  public List<BuiltinCiphers> getCipherFactories();

  public boolean isEnableShell();

}
