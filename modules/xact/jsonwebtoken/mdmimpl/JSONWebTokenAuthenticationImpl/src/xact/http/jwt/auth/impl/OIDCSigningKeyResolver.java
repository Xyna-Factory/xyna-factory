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
package xact.http.jwt.auth.impl;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Key;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.xfmg.xopctrl.usermanagement.jwt.JWTDomainSpecificData;

import io.jsonwebtoken.Header;
import io.jsonwebtoken.Locator;
import io.jsonwebtoken.ProtectedHeader;



/**
 * Fetches public keys from JWKS with caching.
 * The JWKS URI is taken from config or auto-discovered via OIDC discovery.
 */
public class OIDCSigningKeyResolver implements Locator<Key> {

  private static final Logger logger = CentralFactoryLogging.getLogger(OIDCSigningKeyResolver.class);
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final JWTDomainSpecificData domainSpecificData;
  private volatile JwkProvider jwkProvider;
  private final Map<String, Key> keyCache = new ConcurrentHashMap<>();
  private volatile long keyCacheExpiryMs = 0L;


  public OIDCSigningKeyResolver(JWTDomainSpecificData domainSpecificData) {
    this.domainSpecificData = domainSpecificData;
  }


  @Override
  public Key locate(Header header) {
    String kid = (header instanceof ProtectedHeader) ? ((ProtectedHeader) header).getKeyId() : "";
    try {
      long now = System.currentTimeMillis();
      if (now > keyCacheExpiryMs) {
        keyCache.clear();
        keyCacheExpiryMs = now + TimeUnit.HOURS.toMillis(24);
        if (logger.isDebugEnabled()) {
          logger.debug("JWK key cache cleared/reset.");
        }
      }
      return keyCache.computeIfAbsent(kid, k -> {
        try {
          Jwk jwk = getOrCreateProvider().get(k.isEmpty() ? null : k);
          return jwk.getPublicKey();
        } catch (Exception e) {
          throw new IllegalStateException("Failed to fetch JWK for kid='" + k + "'", e);
        }
      });
    } catch (Exception e) {
      throw new IllegalStateException("No matching key found in JWKS for kid='" + kid + "'", e);
    }
  }


  private synchronized JwkProvider getOrCreateProvider() throws Exception {
    if (jwkProvider == null) {
      String jwksUri = resolveJwksUri();
      jwkProvider = new JwkProviderBuilder(new URL(jwksUri)).cached(false).rateLimited(false).build();
      if (logger.isInfoEnabled()) {
        logger.info("JwkProvider initialized for: " + jwksUri);
      }
    }
    return jwkProvider;
  }


  private String resolveJwksUri() throws Exception {
    if (domainSpecificData.getJwksUri().isPresent()) {
      String uri = domainSpecificData.getJwksUri().get();
      if (logger.isDebugEnabled()) {
        logger.debug("Using configured jwksUri: " + uri);
      }
      return uri;
    }

    List<String> issuers = domainSpecificData.getTrustedIssuers();
    if (issuers == null || issuers.isEmpty()) {
      throw new IllegalStateException("No trustedIssuers configured and no jwksUri set - cannot discover JWKS URI.");
    }

    String issuer = issuers.get(0);
    String discoveryUrl = issuer.endsWith("/") ? issuer + ".well-known/openid-configuration" : issuer + "/.well-known/openid-configuration";

    if (logger.isDebugEnabled()) {
      logger.debug("Fetching OIDC Discovery document: " + discoveryUrl);
    }
    HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    HttpResponse<String> response =
        client.send(HttpRequest.newBuilder().uri(URI.create(discoveryUrl)).timeout(Duration.ofSeconds(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
    if (response.statusCode() != 200) {
      throw new IllegalStateException("HTTP " + response.statusCode() + " from " + discoveryUrl);
    }

    String jwksUri = OBJECT_MAPPER.readTree(response.body()).path("jwks_uri").asText(null);
    if (jwksUri == null) {
      throw new IllegalStateException("No 'jwks_uri' in OIDC Discovery response from " + discoveryUrl);
    }
    if (logger.isInfoEnabled()) {
      logger.info("Discovered jwksUri: " + jwksUri);
    }
    return jwksUri;
  }

}

