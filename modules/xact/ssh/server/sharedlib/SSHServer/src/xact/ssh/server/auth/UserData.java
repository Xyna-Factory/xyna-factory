/*---------------------------------------------------
 * Copyright GIP AG 2015
 * (http://www.gip.com)
 *
 * Hechtsheimer Str. 35-37
 * 55131 Mainz
 *----------------------------------------------------
 * $Revision: 221111 $
 * $Date: 2018-04-04 15:19:33 +0200 (Mi, 04 Apr 2018) $
 *----------------------------------------------------
 */
package xact.ssh.server.auth;


public class UserData {
  
  private String user;
  private String password;
  private String expectedIp;
  private String expectedPort;
  
  public UserData(String user, String password, String expectedIp, String expectedPort) {
    this.user = user;
    this.password = password;
    this.expectedIp = expectedIp;
    this.expectedPort = expectedPort;
  }

  
  public String getUser() {
    return user;
  }

  
  public String getPassword() {
    return password;
  }

  
  public String getExpectedIp() {
    return expectedIp;
  }

  
  public String getExpectedPort() {
    return expectedPort;
  }
  
}