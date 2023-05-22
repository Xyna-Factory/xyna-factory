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
package xact.ssh.mock.impl;


import java.util.concurrent.ConcurrentHashMap;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import xact.connection.Command;
import xact.connection.Response;
import xact.ssh.mock.MockedDevice;
import xact.ssh.mock.NoSessionFoundException;
import xact.ssh.mock.ParseBehaviorException;
import xact.ssh.mock.Prompt;
import xact.ssh.mock.SSHMockServiceOperation;
import xact.ssh.mock.SessionStoreKey;
import xact.ssh.mock.SessionStoreParameter;
import xact.ssh.mock.impl.qa.CurrentQAData;
import xact.ssh.mock.impl.qa.QA;
import xact.ssh.mock.result.ExecutionResult;
import xact.ssh.mock.result.ResponseResult;
import xact.ssh.mock.result.Result;
import xact.ssh.mock.result.UnknownRequestResult;
import xact.ssh.server.SSHSession;
import xact.ssh.server.SSHSessionCustomization;
import xact.templates.Document;


public class SSHMockServiceOperationImpl implements ExtendedDeploymentTask, SSHMockServiceOperation {

  private static ConcurrentHashMap<String, MockData> mockData;
  private static TemporarySessionStore temporarySessionStore;
  
  
  public void onDeployment() throws XynaException {
    // TODO do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    
    mockData = new ConcurrentHashMap<String, MockData>();
    temporarySessionStore = new TemporarySessionStore();
    
  }

  public void onUndeployment() throws XynaException {
    // TODO do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
    mockData.clear();
    temporarySessionStore.clear();
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
  
  @Override
  public Result executeCommand(SSHSession sshSession, Command command) throws NoSessionFoundException {
    String request = command.getContent();
    
    MockData md = getMockData(sshSession);
    CurrentQAData data = new CurrentQAData(request, md);
    
    for( QA qa : md.getQas() ) {
      if( qa.matches(data) ) {
        qa.handle(data);
        if( qa.getExecution() != null ) {
          return new ExecutionResult( qa.getExecution(), data.getQuestionParameter(), qa.getPrompt(data) );
        } else {
          return new ResponseResult( qa.getResponse(data), qa.getPrompt(data) );
        }
      }
    }
    
    return new UnknownRequestResult();
  }

  @Override
  public ResponseResult getMOTD(SSHSession sshSession) throws NoSessionFoundException {
    MockData md = getMockData(sshSession);
    CurrentQAData data = new CurrentQAData("", md);
    for( QA qa : md.getMotds() ) {
      if( qa.matches(data) ) {
        qa.handle(data);
        return new ResponseResult( qa.getResponse(data), qa.getPrompt(data) );
      }
    }
    return new ResponseResult( "No MOTD, "+md.getQas().size() , ">>" );
  }

  private MockData getMockData(SSHSession sshSession) throws NoSessionFoundException {
    MockData md = mockData.get(sshSession.getUniqueId());
    if( md == null ) {
      throw new NoSessionFoundException();
    }
    return md;
  }

  @Override
  public Response combineToResponse(SSHSession sshSession, ResponseResult result) throws NoSessionFoundException {
    String prompt = result.getPrompt();
    if( prompt == null ) {
      MockData md = getMockData(sshSession);
      prompt = md.getCurrentPrompt();
    }
    String response = result.getResponse();
    if( response != null && response.length() > 0 ) {
      return new Response( response+"\n"+prompt );
    } else {
      return new Response( prompt );
    }
  }

  @Override
  public Prompt getCurrentPrompt(SSHSession sshSession) throws NoSessionFoundException {
    MockData md = getMockData(sshSession);
    return new Prompt( md.getCurrentPrompt() );
  }

  public MockedDevice getMockedDevice(SSHSession sshSession) throws NoSessionFoundException {
    MockData md = getMockData(sshSession);
    return md.getMockedDevice();
  }

  @Override
  public void removeSessionData(SSHSession sshSession) {
    MockData md = mockData.remove(sshSession.getUniqueId());
    if( md != null ) {
      String tempKey = md.getTempKey();
      if( tempKey != null ) {
        temporarySessionStore.remove(tempKey);
      }
    }
  }

  @Override
  public void storeSessionData(SSHSession sshSession, SessionStoreParameter storeParam) {
    MockData md = mockData.remove(sshSession.getUniqueId());
    if( md != null ) {
      String tempKey = md.getTempKey();
      if( tempKey != null ) {
        temporarySessionStore.remove(tempKey);
      }
      if( ! storeParam.getDoNotStore() ) {
        tempKey = storeParam.getSessionStoreKey().getKey();
        md.setTempKey(tempKey);
        temporarySessionStore.store( tempKey, storeParam.getDuration().toAbsRelTime(), md );
      }
    }
  }

  @Override
  public MockedDevice retrieveStoredSessionData(SessionStoreKey sessionStoreKey, SSHSession sshSession) {
    MockData md = temporarySessionStore.get(sessionStoreKey.getKey());
    if( md == null ) {
      return null;
    }
    mockData.put(sshSession.getUniqueId(), md);
    return md.getMockedDevice();
  }

  @Override
  public SSHSessionCustomization retrieveSessionCustomization(SSHSession sshSession) {
    MockData md = mockData.get(sshSession.getUniqueId());
    if( md == null ) {
      return null;
    }
    return md.getSSHSessionCustomization();
  }

  @Override
  public void createSessionData(SSHSession sshSession, Document document, 
      MockedDevice mockedDevice, SSHSessionCustomization sshSessionCustomization) 
  throws ParseBehaviorException {
    QAParser qap = new QAParser();
    qap.parse(document.getText() );
    MockData md = new MockData(qap.getQas(), qap.getMotds(), mockedDevice, sshSessionCustomization);
    mockData.put(sshSession.getUniqueId(), md);
  }

}
