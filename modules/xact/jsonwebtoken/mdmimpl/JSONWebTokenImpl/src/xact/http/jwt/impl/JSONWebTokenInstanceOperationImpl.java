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

package xact.http.jwt.impl;


import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate;
import com.gip.xyna.xprc.xsched.xynaobjects.DateFormat;

import base.Text;
import base.date.CustomDateFormat;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtBuilder.BuilderHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.SigningKeyResolver;
import xact.http.Header;
import xact.http.jwt.JSONWebToken;
import xact.http.jwt.JSONWebTokenInstanceOperation;
import xact.http.jwt.JSONWebTokenSuperProxy;
import xact.http.jwt.JWTClaims;
import xact.http.jwt.JWTException;
import xact.http.jwt.JWTHeader;
import xact.http.jwt.Key;
import xact.http.jwt.PrivateClaim;


public class JSONWebTokenInstanceOperationImpl extends JSONWebTokenSuperProxy implements JSONWebTokenInstanceOperation {

  private DateFormat _defaultDateFormat = new CustomDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  
  private static final long serialVersionUID = 1L;

  public JSONWebTokenInstanceOperationImpl(JSONWebToken instanceVar) {
    super(instanceVar);
  }

  
  public JSONWebTokenInstanceOperationImpl defaultDateFormat(DateFormat defaultDateFormat) {
    this._defaultDateFormat = defaultDateFormat;
    return this;
  }
  
  
  @Override
  public xact.http.jwt.JSONWebToken extractFromHeader(Header header, Text key, Text prefix) {
    String name = key.getText();
    String token = null;
    if( name != null ) {
      for( xact.http.HeaderField f : header.getHeaderField() ) {
        if( name.equalsIgnoreCase(f.getName()) ) {
          token = f.getValue().trim();
          if( prefix.getText() != null && token.startsWith(prefix.getText()) ) {
            token = token.substring(prefix.getText().length()).trim();
          }
        }
      }
    }
    return new xact.http.jwt.JSONWebToken.Builder().token(token).instance();
  }

  @Override
  public JSONWebToken extractBearerToken(Header header) {
    String token = null;
    for( xact.http.HeaderField f : header.getHeaderField() ) {
      if( "Authorization".equalsIgnoreCase(f.getName()) ) {
        token = f.getValue().substring("Bearer ".length()).trim();
      }
    }
    return new xact.http.jwt.JSONWebToken.Builder().token(token).instance();
  }


  @Override
  public JSONWebToken parseTokenUnsecured() throws JWTException {
    String token = this.getInstanceVar().getToken();
    UnsecureJWTParser jwt = new UnsecureJWTParser().parseToken(token);
    JWTClaims claims = extractClaimsFromJwsUnsecured(token);
    return new xact.http.jwt.JSONWebToken.Builder()
        .token(token)
        .jWTHeader(toHeader(jwt.getHeader()))
        .jWTClaims(claims)
        .instance();
  }
  
   
  // The JJWT library does not allow to parse a Jws without the verification key;
  // but since the Jws-token is not actually encrypted, the content can be accessed with a workaround.
  // The incoming Jws-token is a string in the following format:
  // header + "." + claims + "." + signature
  // where header and claims are base64-encoded json.
  // (see https://github.com/jwtk/jjwt?tab=readme-ov-file#jws-example)
  // The workaround is implemented in this way:
  // Extract the claims-substring from the token;
  // then extract the header-substring from a new-built Jwt-token without signature;
  // then concat those two strings, separated by ".";
  // this creates a new token without signature that contains the original claims and
  // can now be parsed by the JJWT library
  private JWTClaims extractClaimsFromJwsUnsecured(String token) {
    try {
      String[] parts = token.split("\\.");
      String compactClaims = parts[1];
      JwtBuilder builder = Jwts.builder().subject("tmp");
      String token2 = builder.compact();
      String unsignedHeader = token2.split("\\.")[0];
      Jwt<io.jsonwebtoken.Header, Claims> jwt = Jwts.parser().unsecured().build().parseUnsecuredClaims(token2);
      String newToken = unsignedHeader + "." + compactClaims + ".";
      jwt = Jwts.parser().unsecured().build().parseUnsecuredClaims(newToken);
      Claims claims = jwt.getPayload();
      return toClaims(claims);
    } catch (Exception e) {
      return null;
    }
  }
  
  
  @Override
  public xact.http.jwt.JSONWebToken validateAndParseJWSToken(Key key) throws JWTException {
    String token = this.getInstanceVar().getToken();
    Jws<Claims> jws = null;
    try {
      jws = Jwts.parser().verifyWith(createPublicKey(key)).build().parseSignedClaims(token);
    } catch( Exception e) {
      throw new JWTException(e.getMessage(), "validateAndParseJWSToken", "", e);
    }

    return new xact.http.jwt.JSONWebToken.Builder()
        .token(token)
        .jWTHeader(toHeader(jws.getHeader()))
        .jWTClaims(toClaims(jws.getPayload()))
        .instance();
  }
  
  @Override
  public JSONWebToken createAndSignJWSToken(Key key) throws JWTException {
    JSONWebToken jwt = getInstanceVar();
    JWTClaims claims = jwt.getJWTClaims();
    JWTHeader header = jwt.getJWTHeader();
    
    JwtBuilder jwtBuilder = Jwts.builder();
    fillWithClaims(jwtBuilder, claims);
    fillWithHeader(jwtBuilder, header);
    String token = null;
    try {
      token = jwtBuilder.signWith(createPrivateKey(key)).compact();
    } catch( Exception e) {
      throw new JWTException(e.getMessage(), "createAndSignJWSToken", "", e);
    }
    return new xact.http.jwt.JSONWebToken.Builder().token(token).jWTHeader(header).jWTClaims(claims).instance();
  }
 

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

  private void fillWithClaims(JwtBuilder jwtBuilder, JWTClaims claims) {
    jwtBuilder.expiration(toDate(claims.getExpiration()))
      .issuedAt(toDate(claims.getIssuedAt()))
      .issuer(claims.getIssuer())
      .id(claims.getJWTID())
      .notBefore(toDate(claims.getNotBefore()))
      .subject(claims.getSubject());
    jwtBuilder.audience().add(claims.getAudience());
    if( claims.getPrivateClaim() != null ) {
      for( PrivateClaim pc : claims.getPrivateClaim() ) {
        jwtBuilder.claim(pc.getName(), pc.getValueAsJSONString() );
      }
    }
  }
 
  private JWTClaims toClaims(Claims claims) {
    JWTClaims.Builder builder =  new xact.http.jwt.JWTClaims.Builder();
    builder.expiration(toAbsoluteDate(claims.getExpiration()))
      .issuedAt(toAbsoluteDate(claims.getIssuedAt()))
      .issuer(claims.getIssuer())
      .jWTID(claims.getId())
      .notBefore(toAbsoluteDate(claims.getNotBefore()))
      .subject(claims.getSubject());
    if( claims.getAudience() != null ) {
      builder.audience(String.valueOf(claims.getAudience()));
    }
    ArrayList<PrivateClaim> pcs = new ArrayList<PrivateClaim>();
    ObjectMapper mapper = new ObjectMapper();
    Set<String> knownClaims = Set.of(Claims.AUDIENCE, Claims.EXPIRATION, Claims.ID, Claims.ISSUED_AT, Claims.ISSUER, Claims.NOT_BEFORE, Claims.SUBJECT);
    for( Map.Entry<String, Object> e : claims.entrySet() ) {
      if( ! knownClaims.contains(e.getKey()) ) {
        String json;
        try {
          json = mapper.writeValueAsString(e.getValue());
        } catch (JsonProcessingException jpe) {
          json = jpe.getMessage();
        }
        if( json.startsWith("\"") && json.endsWith("\"") ) {
            json = json.substring(1,json.length()-1);
        }
        pcs.add(new PrivateClaim(e.getKey(), json ) );
      }
    }
    builder.privateClaim(pcs);
    return builder.instance();
  }

  private void fillWithHeader(JwtBuilder jwtBuilder, JWTHeader header) {
    if( header == null ) {
      return;
    }
    BuilderHeader headerBuilder = jwtBuilder.header();
    for( PrivateClaim pc : header.getHeaderFields() ) {
      headerBuilder.add(pc.getName(), pc.getValueAsJSONString() );
    }
  }
  
  private JWTHeader toHeader(io.jsonwebtoken.Header header) {
    JWTHeader.Builder builder =  new xact.http.jwt.JWTHeader.Builder();

    ArrayList<PrivateClaim> pcs = new ArrayList<PrivateClaim>();
    ObjectMapper mapper = new ObjectMapper();
    for( Map.Entry<String, Object> e : header.entrySet() ) {
      String json;
      try {
        json = mapper.writeValueAsString(e.getValue());
      } catch (JsonProcessingException jpe) {
        json = jpe.getMessage();
      }
      if( json.startsWith("\"") && json.endsWith("\"") ) {
          json = json.substring(1,json.length()-1);
      }
      pcs.add(new PrivateClaim(e.getKey(), json ) );
    }
    builder.headerFields(pcs);
    return builder.instance();
  }
  
  private Date toDate(AbsoluteDate absoluteDate) {
    if( absoluteDate == null ) {
      return null;
    } else {
      return new Date(absoluteDate.toMillis());
    }
  }
  
  private AbsoluteDate toAbsoluteDate(Date date) {
    if( date == null ) {
      return null;
    } else {
      AbsoluteDate ad = new AbsoluteDate("", _defaultDateFormat);
      ad.fromMillis(date.getTime());
      return ad;
    }
  }


  private java.security.PublicKey createPublicKey(Key key) throws JWTException {
    try {
      byte[] decodedKey = Base64.getDecoder().decode(key.getKey());
      KeyFactory kf = KeyFactory.getInstance("RSA");

      X509EncodedKeySpec  x509EncodedKeySpec = new X509EncodedKeySpec(decodedKey);
      PublicKey pubKey = kf.generatePublic( x509EncodedKeySpec);
      return pubKey;
    } catch( Exception e) {
      throw new JWTException(e.getMessage(), "createPublicKey", "", e);
    }
  }
  
  private java.security.Key createPrivateKey(Key key) throws JWTException {
    try {
      byte[] decodedKey = Base64.getDecoder().decode(key.getKey());
      KeyFactory kf = KeyFactory.getInstance("RSA");

      PKCS8EncodedKeySpec  keySpec = new PKCS8EncodedKeySpec(decodedKey);
      PrivateKey privKey = kf.generatePrivate( keySpec);
      return privKey;
    } catch( Exception e) {
      throw new JWTException(e.getMessage(), "createPrivateKey", "", e);
    }
  }


  public static class UnsecureJWTParser implements Locator<java.security.Key> {
    private JwsHeader header;
    
    public UnsecureJWTParser parseToken(String token) {
      try {
        Jwts.parser().keyLocator(this).build().parseSignedClaims(token);
      } catch( Exception e) {
        //intentionally empty
      }
      return this;
    }
    
    public JwsHeader getHeader() {
      return header;
    }
    
    @Override
    public java.security.Key locate(io.jsonwebtoken.Header header) {
      if (header instanceof JwsHeader) {
        this.header = (JwsHeader) header;
      }
      return null;
    }
  }

}
