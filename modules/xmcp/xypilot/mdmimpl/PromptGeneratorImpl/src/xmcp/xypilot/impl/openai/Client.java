/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.openai;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;


public class Client {
    private static Logger logger = Logger.getLogger("XyPilot");

    /**
     * Makes a completion reqquest and returns the suggested completion texts.
     *
     * @param body
     * @return list of completions or an empty list if any error occurs
     */
    public static List<String> getCompletion(CompletionBody body, String baseUri) {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                        logger.debug("Got list of " + certs.length + " certs");
                        for (int i = 0; i < certs.length; i++) {
                            logger.debug(certs[i].getSubjectX500Principal().getName());
                        }
                    }
                }
        };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            logger.warn("Couldn't install trust manager", e);
        }

        // Create all-trusting host name verifier
        try {
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(String hostname, SSLSession session) {
                    logger.debug("Verifying hostname " + hostname);
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            logger.warn("Couldn't set HostnameVerifyer", e);
        }

        try {
            String uri = baseUri + "/v1/completions";
            uri = uri.replace("http:", "https:");
            URL url = new URL(uri);
            HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setConnectTimeout(1000 * 60);
            conn.setReadTimeout(1000 * 60); // wait at most 1 minute

            String bodyJSON = body.toJSON();
            logger.warn("Send body:\n" + bodyJSON);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream(), "UTF-8"));
            bw.write(bodyJSON);
            bw.flush();
            bw.close();

            DataInputStream input;
            try {
                input = new DataInputStream(conn.getInputStream());
            } catch (IOException e) {
                input = new DataInputStream(conn.getErrorStream());
                InputStreamReader sr = new InputStreamReader(input);
                BufferedReader br = new BufferedReader(sr);
                String msg = "Got response code " + conn.getResponseCode() + "\n";
                throw new RuntimeException(msg + br.lines().collect(Collectors.joining("\n")), e);
            }

            InputStreamReader sr = new InputStreamReader(input);
            BufferedReader br = new BufferedReader(sr);
            String res = br.lines().collect(Collectors.joining("\n"));
            CompletionResponse comp = new CompletionResponse(res);

            logger.debug(res);

            return comp.res
                    .getObjectList("choices")
                    .stream()
                    .map((choice) -> choice.getAttribute("text").getFirst())
                    .collect(Collectors.toList());
        } catch (Throwable e) {
            logger.warn("Couldn't get connection.", e);
            return new ArrayList<>();
        }
    }

    public static String printList(List<String> li) {
        return "[" + li
                .stream()
                .collect(Collectors.joining(", ")) + "]";
    }
}
