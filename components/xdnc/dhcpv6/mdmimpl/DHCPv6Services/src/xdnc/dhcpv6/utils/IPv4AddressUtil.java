/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xdnc.dhcpv6.utils;




public class IPv4AddressUtil {

  String ipstring;
  long iplong;
  
  public String getIpstring(){
    return this.ipstring;
  }
  
  public IPv4AddressUtil(String ip) {
    ipstring = ip;
    String[] stringParts = ip.split("\\.");
    if (stringParts.length != 4) {
      throw new RuntimeException("IPv4-Check: invalid block count != 4");
    }
    iplong = Integer.parseInt(stringParts[3]) + (Integer.parseInt(stringParts[2]) << 8) + ((0l + Integer.parseInt(stringParts[1])) << 16) + ((0l + Integer.parseInt(stringParts[0])) << 24);
  }
  
  public IPv4AddressUtil(int i, int j, int k, int l) {
    //0l + j, weil aus int sonst kein long wird.
    //andere klammern, weil bitshift operator niedrigere prio hat als +.
    iplong = l + (k << 8) + ((0l + j) << 16) + ((0l + i) << 24);
  }
  
  
  public int compareTo(IPv4AddressUtil otherIp) {

    long diff = iplong - otherIp.iplong;
    return diff == 0 ? 0 : diff < 0 ? -1 : 1;
  }
  
  public static void main(String[] args) {
    String ip = "0.0.1.0"; 
    System.out.println((new IPv4AddressUtil(ip)).iplong);
  }
  
  
}
