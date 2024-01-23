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
package xact.mail;

import javax.mail.Message;
import javax.mail.MessagingException;


/**
 * Die vom MailServer empfangenen Messages m�ssen schnell weiterverabeitet werden, nach dem Schlie�en 
 * der Verbindung sind die Messages invalide. �ber dieses Interface k�nnen sie in eine dauerhaftere 
 * Form �bertragen werden, beispielsweise in ein xact.mail.impl.Mail-Objekt.
 *
 * @param <M>
 */
public interface ReceiveHandler<M> {

  M handle(String messageId, Message message) throws MessagingException;
  
  boolean receiveNext();
  
}
