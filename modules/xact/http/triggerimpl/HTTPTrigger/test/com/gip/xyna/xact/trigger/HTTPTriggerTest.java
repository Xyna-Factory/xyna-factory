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
package com.gip.xyna.xact.trigger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


public class HTTPTriggerTest {

  public static void main(String[] args) throws Exception {
    int numberOfRequests = 30000;
    int parallelity = 40;
    
    final ConcurrentHashMap<String, AtomicLong> times = new ConcurrentHashMap<String, AtomicLong>();    
    final ConcurrentHashMap<String, AtomicLong> cnt = new ConcurrentHashMap<String, AtomicLong>();    
    
    ThreadPoolExecutor tpe = new ThreadPoolExecutor(parallelity, parallelity, 30, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new ThreadFactory() {

      public Thread newThread(Runnable r) {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
      }
      
    });
    tpe.prestartAllCoreThreads();
    
    final CountDownLatch latch = new CountDownLatch(numberOfRequests);

    Runnable r = new Runnable() {

      public void run() {
        try {
          long t = System.currentTimeMillis();
          String file = "/test/bla.txt";
          if (Math.random() < 0.1) {
            file = "/abc";
          } 
          final URL url = new URL("http", "localhost", 4251, file);
          HttpURLConnection httpConn = (HttpURLConnection)url.openConnection(); 
          // Connection für Input und Output einrichten
          httpConn.setDoInput(true);
          httpConn.setDoOutput(true);
          // kein Cache, da dynamische Daten
          httpConn.setUseCaches(false);
          // Angaben zur Übertragung (siehe http://www.ietf.org/rfc/rfc2068.txt)
          httpConn.setRequestMethod("GET");
          httpConn.getOutputStream().write("TEST".getBytes());
          httpConn.getOutputStream().flush();
          httpConn.getOutputStream().close();
          String response = "";
          httpConn.connect();
          try {
            try {
              response = readerToString(new InputStreamReader(httpConn.getInputStream()));
            } catch (IOException e) {
              try {
                if (httpConn.getResponseCode() == 500) {
                  response = readerToString(new InputStreamReader(httpConn.getErrorStream()));
                } else {
                  throw e;
                }
              } catch (Exception f) {
                System.out.println("errors:");
                 e.printStackTrace();
                 f.printStackTrace();
              }
            }
          } finally {
            httpConn.disconnect();
           // httpConn.getInputStream().close();
           // httpConn.getOutputStream().close();
          }
          long took = System.currentTimeMillis() -t;
          AtomicLong lnew = new AtomicLong(took);
          AtomicLong l = times.putIfAbsent(response, lnew);
          if (l != null) {
            l.addAndGet(took);
          }
          
          lnew = new AtomicLong(1);
          l = cnt.putIfAbsent(response, lnew);
          if (l != null) {
            l.incrementAndGet();
          }

        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          latch.countDown();
        }
      }
      
    };
    
    long t = System.currentTimeMillis();
    for (int i = 0; i<numberOfRequests; i++) {
      boolean started = false;
      while (!started) {
        try {
          tpe.execute(r);
          started = true;
        } catch (RejectedExecutionException e) {
          
        }
      }
    }
    
    latch.await();
    t = System.currentTimeMillis() - t;
    for (Entry<String, AtomicLong> entry : times.entrySet()) {
      System.out.println("got " + cnt.get(entry.getKey()).get() + " times and took " + (entry.getValue().get()/cnt.get(entry.getKey()).get()) + "ms per request:" + entry.getKey());
    }
    System.out.println("executed " + numberOfRequests + " requests in " + t + "ms (" + (numberOfRequests*1000/t) + " requests per second)");
  }
  
  private static String readerToString(Reader reader) throws IOException {
    BufferedReader br = new BufferedReader(reader);
    StringBuffer sb = new StringBuffer();
    String line;
    try {
      while (null != ((line = br.readLine()))) {
        sb.append(line).append("\n");
      }
    } catch (SocketException e) {
      if (e.getMessage().equals("Connection reset")) {
        e.printStackTrace();
        return sb.toString();
      } else {
        throw e;
      }
    }
    br.close();
    return sb.toString();
  }
}
