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
package com.gip.xyna.xfmg.xopctrl.usermanagement;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User.ChangeReason;


public class PasswordExpiration implements Serializable{
  
  private static final long serialVersionUID = 1L;

  private ExpirationState state;
  private Duration remainingDuration; //verbleibende Zeitdauer, bis das Passwort abläuft, falls state=limited

  
  public static enum ExpirationState {
    unlimited(false),   // unbegrenzt gültig
    limited(false),     // läuft nach einer bestimmten Zeit ab
    expired(true),      // bereits abgelaufen (d.h. Gültigkeitsdauer überschritten)
    setByAdmin(true);   // ungültig weil durch Admin gesetzt
    
    private boolean invalid; //gibt an, ob das Passwort ungültig ist und nicht mehr verwendet werden darf

    private ExpirationState(boolean invalid) {
      this.invalid = invalid;
    }
    
    public boolean isInvalid() {
      return invalid;
    }
  }
  
  
  public Duration getRemainingDuration() {
    return remainingDuration;
  }
  
  public ExpirationState getState() {
    return state;
  }
  
  
  public static PasswordExpiration expired() {
    PasswordExpiration expiration = new PasswordExpiration();
    expiration.state = ExpirationState.expired;
    return expiration;
  }

  public static PasswordExpiration unlimited() {
    PasswordExpiration expiration = new PasswordExpiration();
    expiration.state = ExpirationState.unlimited;
    return expiration;
  }
  
  public static PasswordExpiration limited(Duration remainingDuration) {
    PasswordExpiration expiration = new PasswordExpiration();
    expiration.state = ExpirationState.limited;
    expiration.remainingDuration = remainingDuration;
    return expiration;
  }
  
  public static PasswordExpiration setByAdmin() {
    PasswordExpiration expiration = new PasswordExpiration();
    expiration.state = ExpirationState.setByAdmin;
    return expiration;
  }
  
  
  /**
   * Ermittelt die aktuelle Gültigkeitsdauer für das Passwort des Users.
   * @param user
   * @return
   */
  public static PasswordExpiration calculate(User user) {
    if (passwordMustBeChanged(user.getPasswordChangeReasonEnum())) {
      return PasswordExpiration.setByAdmin(); //Passwort wurde durch Admin gesetzt und muss nun durch Benutzer geändert werden
    }
    
    int expirationDays = XynaProperty.PASSWORD_EXPIRATION_DAYS.get();
    if (expirationDays < 0) {
      return PasswordExpiration.unlimited();
    }
    
    return calculate(user.getPasswordChangeDate(), expirationDays);
  }

  /**
   * Ermittelt, ob das Passwort des Users noch geändert werden darf.
   * @param user
   * @return
   */
  public static PasswordExpiration changeAllowed(User user) {
    int expirationDays = XynaProperty.PASSWORD_EXPIRATION_DAYS.get();
    int changeAllowedDays = XynaProperty.PASSWORD_EXPIRATION_CHANGEALLOWED_DURATION_DAYS.get();

    if (passwordMustBeChanged(user.getPasswordChangeReasonEnum())) {
      expirationDays = 0; //Passwort wurde durch Admin gesetzt und muss nun durch Benutzer innerhalb der changeAllowedDays geändert werden
    }
    
    if (expirationDays < 0 || changeAllowedDays < 0) {
      return PasswordExpiration.unlimited(); //nicht abgelaufen oder Änderung immer erlaubt
    }
    
    return calculate(user.getPasswordChangeDate(), expirationDays + changeAllowedDays);
  }

  private static PasswordExpiration calculate(long passwordChangeDate, int expirationDays) {
    long millis = passwordChangeDate + TimeUnit.MILLISECONDS.convert(expirationDays, TimeUnit.DAYS) - System.currentTimeMillis();
    if (millis <= 0) {
      return PasswordExpiration.expired();
    }
    
    return PasswordExpiration.limited(new Duration(millis));
  }
  
  /**
   * Stellt fest, ob ein Passwort erneuert werden muss, weil es durch einen Admin gesetzt wurde
   * und die XynaProperty xyna.xfmg.xopctrl.usermanagement.password.setbyadmin.invalid true ist.
   * @return
   */
  private static boolean passwordMustBeChanged(ChangeReason passwordChangeReason) {
    if (XynaProperty.PASSWORD_SETBYADMIN_INVALID.get()) {
      return ChangeReason.NEW_USER.equals(passwordChangeReason)
                      || ChangeReason.SET_PASSWORD.equals(passwordChangeReason);
    } else {
      return false;
    }
  }
  
}
