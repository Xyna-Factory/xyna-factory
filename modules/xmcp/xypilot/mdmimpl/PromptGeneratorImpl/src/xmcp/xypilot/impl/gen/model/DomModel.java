/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xmcp.xypilot.impl.gen.model;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;


/**
 * A data model providing a DOM object.
 * It extends DomOrExceptionModel, therefore it also provides the DomOrExceptionGenerationBase object,
 * this is useful for template macros that can handle both DOMs and Exceptions.
 *
 * @see DomOrExceptionModel
 */
public class DomModel extends DomOrExceptionModel {

    private DOM dom;

    public DomModel(DOM dom) throws XynaException {
        super(dom);
        getUtils().getImportHandler().addImports(dom.getImports());

        this.dom = dom;
    }

    public DOM getDom() {
        return dom;
    }

}
