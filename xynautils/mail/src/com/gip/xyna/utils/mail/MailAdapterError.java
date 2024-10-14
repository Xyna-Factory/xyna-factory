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
package com.gip.xyna.utils.mail;

/**
 * 
 * MailAdapterError containes ErrorMessages in the Xyna-style and extends this
 * with the standard mail server ErrorMessages
 * 
 * 
 */
public class MailAdapterError extends Exception {

   private static final long serialVersionUID = 1L; // What ever that may
                                                      // mean.

   private java.lang.String code;

   private java.lang.String summary;

   private java.lang.String details;

   public static final String MailAdapterService = "MailAdapterServiceError: Connection to mailserver failed - see details";
   public static final String MailRecipientAddress = "Unknown RecipientAddress: Mailseverer refused to process mail - see details";
   public static final String MailSenderAddress = "Unknown SenderAddress: Mailserver refued to process mail - see details";
   public static final String MailServiceProvider = "Unknown ServiceProvider: Please ensure that you're requesting the right mailservice (POP/IMAP/SMTP) - see details";
   public static final String MailUnknownError = "Unknown Error - You shouldn't see this. - see details";
   public static final String MailInputOutputError = "INPUT-OUTPUT Error occured. Could not read/open/write file - see details";
   public static final String MailMessagingError = "MessagingError: Could not get an handle on the message -  see details";
   public static final String MAE_211 = "System Status: ";
   public static final String MAE_214 = "HelpMessage: ";
   public static final String MAE_220 = "Service ready: ";
   public static final String MAE_221 = "Connection closed by server: ";
   public static final String MAE_250 = "Requested mail acction approved: ";
   public static final String MAE_251 = "Not a local user. Mail will be forwarded: ";
   public static final String MAE_354 = "Starting mail injection; stop with [CRLF].[CRLF]";
   // can be thrown at any point!
   public static final String MAE_421 = "Service not read. Connection closed.";
   public static final String MAE_450 = "Requested mail action not possible. Mailbox can not be reached: ";
   public static final String MAE_451 = "Requested mail action aborded: ";
   public static final String MAE_452 = "Requested mail action aborded: Not enough memory: ";
   public static final String MAE_500 = "SyntaxError, Command recognized: ";
   public static final String MAE_501 = "SyntaxError in parameteres or arguments: ";
   public static final String MAE_502 = "Service not available: ";
   public static final String MAE_503 = "Unusable command sequence: ";
   public static final String MAE_504 = "Command parameter not implemented: ";
   public static final String MAE_550 = "Action aborted. Mailbox unreachable: ";
   public static final String MAE_551 = "Not a local user. Forwarding mail: ";
   public static final String MAE_552 = "Aborted requested action - not enough memory: ";
   public static final String MAE_553 = "Command aborded. Not permitted: ";
   public static final String MAE_554 = "Transmission failed: ";

   public MailAdapterError() {

   }

   public MailAdapterError(java.lang.String code, java.lang.String summary,
         java.lang.String details) {

      this.code = code;

      this.summary = summary;

      this.details = details;

   }

   public java.lang.String getDetails() {

      return details;

   }

   public void setCode(java.lang.String code) {

      this.code = code;

   }

   public void setSummary(java.lang.String summary) {

      this.summary = summary;

   }

   public void setDetails(java.lang.String details) {

      this.details = details;

   }

   public java.lang.String getCode() {

      return code;

   }

   public java.lang.String getSummary() {

      return summary;

   }

}
