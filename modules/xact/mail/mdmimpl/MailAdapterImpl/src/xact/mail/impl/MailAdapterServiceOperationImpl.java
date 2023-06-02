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
package xact.mail.impl;


import java.util.List;

import javax.mail.MessagingException;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import xact.mail.CreateMailException;
import xact.mail.InvalidMailAddressException;
import xact.mail.Mail;
import xact.mail.MailAccount;
import xact.mail.MailAdapterServiceOperation;
import xact.mail.ReceiveMailException;
import xact.mail.ReceiveOptions;
import xact.mail.ReceivedMail;
import xact.mail.Receiver;
import xact.mail.SendMailException;
import xact.mail.account.MailAccountData;
import xact.mail.account.MailAccountNotRegisteredException;
import xact.mail.account.MailAccountParameter;
import xact.mail.account.MailAccountStorage;
import xact.mail.internal.MailServer;
import xact.mail.internal.SMTPImpl;


public class MailAdapterServiceOperationImpl implements ExtendedDeploymentTask, MailAdapterServiceOperation {

  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.;
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

  @Override
  public List<? extends Mail> receive(MailAccount mailAccount, ReceiveOptions receiveOptions) throws ReceiveMailException {
    MailAccountData mailAccountData;
    try {
      mailAccountData = getMailAccountData(mailAccount);
    } catch (MailAccountNotRegisteredException e) {
      throw new ReceiveMailException(e);
    }
    MailServer mailServer = new MailServer(mailAccountData);
    Receiver receiver = mailServer.getReceiver();
    AdapterReceiveHandler arh = new AdapterReceiveHandler(receiver, receiveOptions);
    try {
      
      List<ReceivedMail> mails = receiver.receive(arh);
      if( receiveOptions.getDeleteAfterRead() ) {
        arh.deleteMails();
      }
      return mails;
    } catch (MessagingException e) {
      throw new ReceiveMailException(e);
    }
  }

  @Override
  public void send(Mail mail, MailAccount mailAccount) throws InvalidMailAddressException, CreateMailException, SendMailException {
    MailAccountData mailAccountData;
    try {
      mailAccountData = getMailAccountData(mailAccount);
    } catch (MailAccountNotRegisteredException e) {
      throw new SendMailException(e);
    }
    MailServer mailServer = new MailServer(mailAccountData);
    SMTPImpl sender = mailServer.createSender();
    try {
      sender.send(MailConverter.convertFromXmom(mail));
    } catch (xact.mail.exceptions.InvalidMailAddressException e) {
      throw new InvalidMailAddressException(e.getMessage(), e);
    } catch (xact.mail.exceptions.CreateMailException e) {
      throw new CreateMailException(e);
    } catch (xact.mail.exceptions.SendMailException e) {
      throw new SendMailException(e);
    }
  }

  private MailAccountData getMailAccountData(MailAccount mailAccount) throws MailAccountNotRegisteredException {
    MailAccountData mailAccountData = null; 
    if( mailAccount instanceof MailAccountParameter ) {
      mailAccountData = MailAccountConverter.convertFromXmom((MailAccountParameter)mailAccount);
    } else {
      String name = mailAccount.getName();
      mailAccountData = MailAccountStorage.getInstance().getMailAccount(name);
      if( mailAccountData == null ) {
        throw new MailAccountNotRegisteredException(name);
      }
    }
    return mailAccountData;
  }

}
