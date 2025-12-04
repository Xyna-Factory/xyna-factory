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

import java.util.Date;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.gip.xyna.xprc.xsched.xynaobjects.AbsoluteDate;

import base.date.CustomDateFormat;
import xact.http.jwt.impl.JSONWebTokenInstanceOperationImpl.UnsecureJWTParser;

public class MainTest {

  String privkey = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQCJvXGCxkXc0W6gX1eHfRsn+1qWBxRHrTm6nKj3wHNe8CHy6OepMrFWZBIIzQLv6O8aaatx7wpTT8FkWnFme4fIpPF5uDpLmJdrIg8LM8hwGB0vVVSDffpsoGWl63j/8zTAKD45PK+EcSx6SwpC5AB6dF+B6afIlh/JUJiDjyVuEdjSvPMfgdt55Xrol6eRO93IOXkx4ocmTDNZoUJinF64mmdsdj+D7OVXC8XvDTCWZzlK1Imjrb7zxo6gZxsPpNZ7VaZIDWZHD/3HcuI0pIr0oPDmBFKES1an8Gl7mbjYr+vP+q4Am3kqZtvsxoVWT6fkf8DexrEmdpEemcioY/f/AgMBAAECggEAMs6MRj64mmEuOz2v3axKDg9rNJfb5SyCdNFAilKUu9o7y4wZl5G5VDKJbHGiubfkKov7z4Tq/BwZK9kopzK9vUcKQ2mvJx9cCwp/HhS1xJNAgKsrB226p8Lqe69F7LNbK94sRZSa8XGFhH6VhGLxwjmW2SbE2vp1Mx7lgvGUTur6usSEmgGtf6UP0G9hckqEN6r8mWG31il6KMO7kAe4/2Mu58EoiSiIdGHCbL+gnTvFkwOFae7dF5QwfDse3lEW2URxGVlbNIwWgifunIE+RBGFfM6xs+5CiunITCaTcK06Qw0Xv76sKxUiNBykqNfCu/ia8geyGJTVF2vZKFlOQQKBgQD2JHuEi32mGgEf8f5vQR3W1bb8mITNHkfH99+5pDMf9nO8Um6kL7EApCtoPp240HlZCiMGcrf6UdQW+3vWIG6NGu3pHQIbkQh9mbpRh++UBVAj5w2K7L7uZOP4ywFasUtyiTCr1FlgDawqEhSFuOvEMQyrRc5hdelUQL12bry2jwKBgQCPQZcRh6T9TDXGsF/453fooxgONMetHOhlLHKX+X008L6hU+8/Nv6aaVvxAqefSRdsEsOYcLq6rGV5Dw4ZLtYpEto2NnnzSC1hUSvWq0+BWhs4ete0GZe7wuhQ23mZEBED9XHD2mTVd3MdWYLH1SxcTS3sNJ0GbQVPNKU5+ObfkQKBgQDOyC1gV5NyRGxnevRGYM0Bm79Di5ode+/PxaSVH6W3l2L5dcLvegVYRZMV22zDQ5h+Pe7Yzu1ShQYJXLJXXB2ju1jdOvp3UCty4P/O2MVjc+c0kjolWOi5+9YtP9Zea423phtWb2m6MSJOoavN/2FKC+7ZwCY2aElh95VzdmphbwKBgASgFYSXYJhdEY9sumyK7LAyM+Chi6DnQgmiOcD2aTiAXWdQEnV0DRxnMnTBpS/baEoxAOe3HBBS6KPT/JyLipag6TK+54kXV3/A4y13MoX2ptJYBQbnnKKPVHjn3TE395StJBMyxfaueKUF8tcI5vnD9CugN0Gx224HQR5yDuJBAoGBAJULtDIyu4u2504n+IpXujTYmOeydrHMzNUrQgsNc0u5GnnJ8HpzpZ++gOJC4BzyQ4Xy50FB66fYUpowp2EjOoV7Bq60UgatydTVCwJz4itm5/W0jPbzpm792zN6+efKr5lP6ZJpYx/P1VkBogTxbkCdORWJoJe8/bcSgX4UZdvY";
  
  
  public static void main(String[] args) {
    init(true);
    
    // We need a signing key, so we'll create one just for this example. Usually
    // the key would be read from your application configuration instead.
    //SecretKey key = Jwts.SIG.HS256.key().build();
    //String token = Jwts.builder().subject("Joe").signWith(key).compact();
    String token = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJKb2UifQ.nmKGh5kmXKqHS3r4SqHhpVO7OpECtE1MUjVO_VU_dpU";
    System.out.println(token);

    UnsecureJWTParser jwt = new UnsecureJWTParser().parseToken(token);
    System.out.println(jwt.getClaims());
    System.err.println(jwt.getHeader());

    
    //System.out.println(toAbsoluteDate( new Date() ).asString());
    
    
    
 byte b = -0x7e;
 
    System.out.println( ((char)b) + 0 + "  # " + (char)b + " # " + b + " # " + (b+256) );
  }
  
  private static AbsoluteDate toAbsoluteDate(Date date) {
    if( date == null ) {
      return null;
    } else {
      CustomDateFormat cdf = new CustomDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
      AbsoluteDate ad = new AbsoluteDate("",cdf);
      ad.fromMillis(date.getTime());
      return ad;
    }
  }


public static void init(boolean debug) {
    
    LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
    Configuration config = ctx.getConfiguration();
    LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
    loggerConfig.setLevel(debug ? Level.TRACE : Level.INFO);
    for (String appender : loggerConfig.getAppenders().keySet()) {
      loggerConfig.removeAppender(appender);
    }
    //org.apache.logging.log4j.core.layout.PatternLayout layout = org.apache.logging.log4j.core.layout.PatternLayout.createLayout("%r XYNA %-5p [%t] (%F:%L) - %x %m%n", null, config, null, null, false, false, null, null);
    org.apache.logging.log4j.core.layout.PatternLayout layout = org.apache.logging.log4j.core.layout.PatternLayout.createLayout(
		    "%r XYNA %-5p [%t] (%F:%L) - %x %m%n", null, config, null, null, false, false, null, null);
    ConsoleAppender consoleAppender = ConsoleAppender.createAppender(layout, null, "SYSTEM_OUT", "console", null, null);
    loggerConfig.addAppender(consoleAppender, debug ? Level.TRACE : Level.INFO, null);
    ctx.updateLoggers();
    
  }

}
