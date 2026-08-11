/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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


import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate;
import com.gip.xyna.xprc.xsched.xynaobjects.DateFormat;

import base.Text;
import base.date.CustomDateFormat;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtBuilder.BuilderHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Locator;
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

  private static Logger _logger = Logger.getLogger(JSONWebTokenInstanceOperationImpl.class);
  private DateFormat _defaultDateFormat = new CustomDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

  private static final long serialVersionUID = 1L;

  public JSONWebTokenInstanceOperationImpl(JSONWebToken instanceVar) {
    super(instanceVar);
  }

  
  public JSONWebTokenInstanceOperationImpl defaultDateFormat(DateFormat defaultDateFormat) {
    this._defaultDateFormat = defaultDateFormat;
    return this;
  }
  
  
  @Override
  public JSONWebToken extractFromHeader(Header header, Text key, Text prefix) {
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
    return new JSONWebToken.Builder().token(token).instance();
  }

  @Override
  public JSONWebToken extractBearerToken(Header header) {
    String token = null;
    for( xact.http.HeaderField f : header.getHeaderField() ) {
      if( "Authorization".equalsIgnoreCase(f.getName()) ) {
        token = f.getValue().substring("Bearer ".length()).trim();
      }
    }
    return new JSONWebToken.Builder().token(token).instance();
  }


  @Override
  public JSONWebToken parseTokenUnsecured() throws JWTException {
    String token = this.getInstanceVar().getToken();
    ObjectMapper objectMapper = new ObjectMapper();

    String[] parts = token.split("\\.", -1);
    Map<String, Object> headerMap = parseJsonSegment(parts[0], objectMapper);
    Map<String, Object> claimsMap = parseJsonSegment(parts[1], objectMapper);

    io.jsonwebtoken.Header header = Jwts.header().add(headerMap).build();
    Claims claims = Jwts.claims().add(claimsMap).build();

    return new JSONWebToken.Builder()
        .token(token)
        .jWTHeader(toHeader(header))
        .jWTClaims(toClaims(claims))
        .instance();
  }


  private static Map<String, Object> parseJsonSegment(String segment, ObjectMapper objectMapper) {
    try {
      byte[] decoded = Base64.getUrlDecoder().decode(segment);
      String json = new String(decoded, StandardCharsets.UTF_8);
      return objectMapper.readValue(json, MAP_TYPE);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid JWT segment JSON.", e);
    }
  }


  @Override
  public JSONWebToken validateAndParseJWSToken(Key key) throws JWTException {
    String token = this.getInstanceVar().getToken();
    Jws<Claims> jws = null;
    try {
      jws = Jwts.parser().verifyWith(createPublicKey(key)).build().parseSignedClaims(token);
    } catch( Exception e) {
      throw new JWTException(e.getMessage(), "validateAndParseJWSToken", "", e);
    }

    return new JSONWebToken.Builder()
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
    return new JSONWebToken.Builder().token(token).jWTHeader(header).jWTClaims(claims).instance();
  }
 

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }

  @SuppressWarnings("deprecation")
  private void fillWithClaims(JwtBuilder jwtBuilder, JWTClaims claims) {
    jwtBuilder.expiration(toDate(claims.getExpiration()))
      .issuedAt(toDate(claims.getIssuedAt()))
      .issuer(claims.getIssuer())
      .id(claims.getJWTID())
      .notBefore(toDate(claims.getNotBefore()))
      .subject(claims.getSubject());
    if ((claims.getAudienceArray() != null) && (claims.getAudienceArray().size() > 0)) {
      if ((claims.getAudienceSingle() != null) && _logger.isWarnEnabled()) {
        _logger.warn("Since attribute AudienceArray is set, value in AudienceSingle will be ignored: " + 
                     claims.getAudienceSingle());
      }
      for (String s : claims.getAudienceArray()) {
        if ((s != null) && (!s.isBlank())) {
          jwtBuilder.audience().add(s);
        }
      }
    } else if (claims.getAudienceSingle() != null) {
      //call to .single() is deprecated and jwtBuilder.audience().add() should be used instead
      //however, older systems may only accept an audience string instead of an array
      jwtBuilder.audience().single(claims.getAudienceSingle());
    }
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
    if (claims.getAudience() != null) {
      List<String> list = new ArrayList<>();
      for (String s : claims.getAudience()) {
        list.add(s);
      }
      builder.audienceArray(list);
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
    JWTHeader.Builder builder =  new JWTHeader.Builder();

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


  private PublicKey createPublicKey(Key key) throws JWTException {
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
