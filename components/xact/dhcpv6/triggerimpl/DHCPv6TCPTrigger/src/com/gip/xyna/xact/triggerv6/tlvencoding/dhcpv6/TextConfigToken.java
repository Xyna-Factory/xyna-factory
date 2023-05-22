/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6;

/**
 * Text config token.
 */
public final class TextConfigToken {

    private final int level;
    private final String key;
    private final String value;

    public TextConfigToken(final int level, final String key, final String value) {
        if (level < 0) {
            throw new IllegalArgumentException("Level may not be less than zero.");
        } else if (key == null) {
            throw new IllegalArgumentException("Key may not be null.");
        } else if ("".equals(key)) {
            throw new IllegalArgumentException("Key may not be empty string.");
        }
        this.level = level;
        this.key = key;
        this.value = value;
    }

    public int getLevel() {
        return level;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("{level:<");
        sb.append(level);
        sb.append(">,key:<");
        sb.append(key);
        sb.append(">,value:<");
        sb.append(value);
        sb.append(">}");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + level;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TextConfigToken other = (TextConfigToken) obj;
        if (key == null) {
            if (other.key != null) {
                return false;
            }
        } else if (!key.equals(other.key)) {
            return false;
        }
        if (level != other.level) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }
}
