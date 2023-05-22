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
package xact.telnet.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.telnet.TelnetClient;


public class TransientConnectionData {
    private TelnetClient session;
    private InputStream inputStream;
    private OutputStream outputStream;

    public void setSession(TelnetClient session) {
        this.session = session;
    }

    public void setChannelAndStreams() throws IOException {
        this.outputStream = session.getOutputStream();
        this.inputStream = session.getInputStream();
    }

    public TelnetClient getSession() {
        return session;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public void disconnect() {
        if (session != null) {
            try {
                if(session.isConnected()) {
                    session.disconnect();
                } 
            } catch (IOException e) {
                throw new RuntimeException("IO Exception during disconnect", e);
            }
        }
    }

    public boolean isSessionNullOrNotConnected(long timeout) throws IllegalArgumentException, IOException, InterruptedException {
        if(session == null) {
            return true;
        } else {
            
            return false;
            // session.isConnected => not reliable
            //return !session.sendAYT(timeout); ==> does not work in here, moved in the send-method of class TelnetConnection
        }

    }

}
