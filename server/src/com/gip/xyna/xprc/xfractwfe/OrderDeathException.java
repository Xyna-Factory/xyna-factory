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
package com.gip.xyna.xprc.xfractwfe;

/**
 * falls server sich im cluster befindet, und die datenbank nicht mehr erreichbar ist, übernimmt der andere knoten
 * die aufgaben für diesen auftrag. deshalb soll der auftrag möglichst nichts mehr tun, sondern einfach nur noch 
 * geordnet verschwinden. 
 * der andere knoten resumed auf dem letzten backup-stand.
 */
public class OrderDeathException extends RuntimeException {

  private static final long serialVersionUID = -4054910114364242279L;

  public OrderDeathException(Throwable cause) {
    super(cause);
  }

}
