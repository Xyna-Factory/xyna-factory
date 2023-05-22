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
package com.gip.xyna.utils.mail;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Generate Authenticator / Authentication for mail server. allow for user /
 * password specified authentication in various cases which may be independent
 * of what is specified in property-object. To enable this change
 * store.connect() and similar requests to store.connect(String hst, int port,
 * String user, String pwd)
 * 
 * 
 */
public class MailAuthenticator extends Authenticator {
   public MailAuthenticator() {
   }

   public PasswordAuthentication getPasswordAuthentication(String user1,
         String password1) {
      return new PasswordAuthentication(user1, password1);
   }
}
