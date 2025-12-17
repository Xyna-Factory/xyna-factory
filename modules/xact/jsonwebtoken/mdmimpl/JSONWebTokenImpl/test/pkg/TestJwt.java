/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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

package pkg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import javax.crypto.SecretKey;

import org.junit.Test;

import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate;
import com.gip.xyna.xprc.xsched.xynaobjects.DateFormat;
import com.gip.xyna.xprc.xsched.xynaobjects.Now;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import xact.http.Header;
import xact.http.HeaderField;
import xact.http.jwt.JSONWebToken;
import xact.http.jwt.JWTClaims;
import xact.http.jwt.JWTException;
import xact.http.jwt.Key;
import xact.http.jwt.KeyGenerator;
import xact.http.jwt.impl.JSONWebTokenInstanceOperationImpl;
import xact.http.jwt.impl.KeyGeneratorInstanceOperationImpl;


public class TestJwt {
  
  public static class DateFormatImpl extends DateFormat {
    private static final long serialVersionUID = 1L;
    @Override
    public DateFormat clone() {
      return new DateFormatImpl();
    }
    @Override
    public DateFormat clone(boolean deep) {
      return new DateFormatImpl();
    }
    @Override
    public String getFormat() {
      return "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    }
  }
  
  
  @Test
  public void test1() {
    try {
      SecretKey key = Jwts.SIG.HS256.key().build();
      JwtBuilder builder = Jwts.builder().subject("Test1");
      String token = builder.signWith(key).compact();
      log(token);
      Jws<Claims> jws = Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      Claims claims = jws.getPayload();
      io.jsonwebtoken.Header header = jws.getHeader();
      log(claims.toString());
      log(header.toString());
      assertEquals("{sub=Test1}", claims.toString());
      assertEquals("{alg=HS256}", header.toString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void test2() {
    try {
      Container keys = new KeyGeneratorInstanceOperationImpl(new KeyGenerator()).generateKeyPair();
      assertEquals(2, keys.size());
      Key publicKey = (Key) keys.get(1);
      Key privateKey = (Key) keys.get(0);
      
      JWTClaims claims = new JWTClaims();
      claims.setIssuer("Test2");
      long millis = System.currentTimeMillis();
      claims.setIssuedAt(toAbsoluteDate(millis));
      claims.setExpiration(toAbsoluteDate(millis + 300000L));
      claims.setNotBefore(toAbsoluteDate("1970-01-01T01:00:00.000Z"));
      
      JSONWebToken token = new JSONWebToken();
      token.setJWTClaims(claims);
      token = new JSONWebTokenInstanceOperationImpl(token).createAndSignJWSToken(privateKey);
      log(token.getJWTClaims().getIssuer());
      
      Header header = new Header();
      HeaderField field = new HeaderField();
      field.setName("Authorization");
      field.setValue("Bearer " + token.getToken());
      header.addToHeaderField(field);
      token = new JSONWebTokenInstanceOperationImpl(new JSONWebToken()).extractBearerToken(header);
      
      JSONWebToken token2 = new JSONWebTokenInstanceOperationImpl(token).defaultDateFormat(new DateFormatImpl())
                                                                        .parseTokenUnsecured();
      log(token2.getJWTClaims().getIssuer());
      log(token2.getJWTHeader().getHeaderFields().get(0).getName());
      log(token2.getJWTHeader().getHeaderFields().get(0).getValueAsJSONString());
      assertEquals("Test2", token2.getJWTClaims().getIssuer());
      
      token = new JSONWebTokenInstanceOperationImpl(token).defaultDateFormat(new DateFormatImpl()).
                                                           validateAndParseJWSToken(publicKey);
      log(token.getJWTClaims().getIssuer());
      log(token.getJWTHeader().getHeaderFields().get(0).getName());
      log(token.getJWTHeader().getHeaderFields().get(0).getValueAsJSONString());
      assertEquals("Test2", token.getJWTClaims().getIssuer());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testExpired() {
    try {
      Container keys = new KeyGeneratorInstanceOperationImpl(new KeyGenerator()).generateKeyPair();
      assertEquals(2, keys.size());
      Key publicKey = (Key) keys.get(1);
      Key privateKey = (Key) keys.get(0);
      
      JWTClaims claims = new JWTClaims();
      claims.setIssuer("Test2");
      claims.setIssuedAt(toAbsoluteDate("2025-12-01T16:45:07.614Z"));
      claims.setExpiration(toAbsoluteDate("2025-12-02T16:14:46.340Z"));
      claims.setNotBefore(toAbsoluteDate("1970-01-01T01:00:00.000Z"));
      JSONWebToken token = new JSONWebToken();
      
      token.setJWTClaims(claims);
      token = new JSONWebTokenInstanceOperationImpl(token).createAndSignJWSToken(privateKey);
      log(token.getJWTClaims().getIssuer());
      
      Header header = new Header();
      HeaderField field = new HeaderField();
      field.setName("Authorization");
      field.setValue("Bearer " + token.getToken());
      header.addToHeaderField(field);
      
      token = new JSONWebTokenInstanceOperationImpl(new JSONWebToken()).extractBearerToken(header);
      try {
        token = new JSONWebTokenInstanceOperationImpl(token).defaultDateFormat(new DateFormatImpl()).
                                                             validateAndParseJWSToken(publicKey);
        fail("Expected exception due to expired JWT");
      } catch (JWTException e) {
        log(e.getMessage());
        assertTrue(e.getMessage().contains("XACT_JWT-00001"));
        assertTrue(e.getMessage().contains("JWT expired"));
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testGetClaims() {
    try {
      SecretKey key = Jwts.SIG.HS256.key().build();
      JwtBuilder builder = Jwts.builder().subject("Subj-1").issuer("Iss-1");
      String token = builder.signWith(key).compact();
      log(token);
      String[] parts = token.split("\\.");
      assertEquals(3, parts.length);
      String compactClaims = parts[1];
      log(compactClaims);
      
      builder = Jwts.builder().subject("Subj-2");
      String token2 = builder.compact();
      log(token2);
      String unsignedHeader = token2.split("\\.")[0];
      log(unsignedHeader);
      Jwt<io.jsonwebtoken.Header, Claims> jwt = Jwts.parser().unsecured().build().parseUnsecuredClaims(token2);
      log(jwt.getHeader().toString());
      log(jwt.getPayload().toString());
      assertEquals("{sub=Subj-2}", jwt.getPayload().toString());
      assertEquals("{alg=none}", jwt.getHeader().toString());
      
      String newToken = unsignedHeader + "." + compactClaims + ".";
      log(newToken);
      jwt = Jwts.parser().unsecured().build().parseUnsecuredClaims(newToken);
      log(jwt.getHeader().toString());
      log(jwt.getPayload().toString());
      assertEquals("{sub=Subj-1, iss=Iss-1}", jwt.getPayload().toString());
      assertEquals("{alg=none}", jwt.getHeader().toString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  @Test
  public void testDate() {
    Now now = new Now();
    now.setFormat(new DateFormatImpl());
    log(now.getDate());
    
    String date = "2001-01-01T01:01:01.001Z";
    AbsoluteDate ad1 = toAbsoluteDate(date);
    log("" + ad1.toMillis());
    AbsoluteDate ad2 = toAbsoluteDate(ad1.toMillis());
    log(ad2.getDate());
    assertEquals(date, ad2.getDate());
    
    AbsoluteDate ad3 = toAbsoluteDate(ad1.toMillis() + 3600L*1000L);
    log(ad3.getDate());
    assertEquals("2001-01-01T02:01:01.001Z", ad3.getDate());
    
    AbsoluteDate ad4 = toAbsoluteDate(0L);
    log(ad4.getDate());
    assertEquals("1970-01-01T01:00:00.000Z", ad4.getDate());
  }
  
  
  private AbsoluteDate toAbsoluteDate(long millis) {
    DateFormat df = new DateFormatImpl();
    AbsoluteDate ad = new AbsoluteDate();
    ad.setFormat(df);
    ad.fromMillis(millis);
    return ad;
  }
  
  private AbsoluteDate toAbsoluteDate(String date) {
    DateFormat df = new DateFormatImpl();
    AbsoluteDate ad = new AbsoluteDate();
    ad.setFormat(df);
    ad.setDate(date);
    return ad;
  }
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new TestJwt().test2();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
