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
package com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.encoders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Pattern;

import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.Node;
import com.gip.xyna.xact.triggerv6.tlvencoding.dhcpv6.TlvEncoder;




/**
 * Abstract TLV encoder - all TLV:s have a type encoding.
 *
 * @author Fredrik Lindberg / GIP Exyr GmbH
 */
public abstract class AbstractTlvEncoder implements TlvEncoder {

  protected static final Pattern SPLIT_AT_DOT_PATTERN = Pattern.compile("\\.");
  protected static final Pattern SPLIT_AT_COMMA_PATTERN = Pattern.compile(",");


  private final int typeEncoding;

    public AbstractTlvEncoder(final int typeEncoding) {
        if (typeEncoding < 0) {
            throw new IllegalArgumentException("Illegal type encoding (less than 0): <" + typeEncoding + ">.");
        } else if (typeEncoding > 65536) {
            //throw new IllegalArgumentException("Illegal type encoding (larger than 255): <" + typeEncoding + ">.");
        }
        this.typeEncoding = typeEncoding;        
    }

    public final void write(final Node node, final OutputStream target) throws IOException {
        if (node == null) {
            throw new IllegalArgumentException("Node may not be null.");
        } else if (target == null) {
            throw new IllegalArgumentException("Output stream may not be null.");
        }
        this.writeNode(node, target);
    }

    public int getTypeEncoding() {
        return this.typeEncoding;
    }

    protected abstract void writeNode(final Node node, final OutputStream target)
            throws IOException;
}
