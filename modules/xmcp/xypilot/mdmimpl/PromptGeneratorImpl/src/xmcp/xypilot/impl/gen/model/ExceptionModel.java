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
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;

/**
 * A data model providing an ExceptionGeneration object.
 * It extends DomOrExceptionModel, therefore it also provides the DomOrExceptionGenerationBase object,
 * this is useful for template macros that can handle both DOMs and Exceptions.
 *
 * @see DomOrExceptionModel
 */
public class ExceptionModel extends DomOrExceptionModel {

    private ExceptionGeneration exception;

    public ExceptionModel(ExceptionGeneration exception) throws XynaException {
        super(exception);
        this.exception = exception;
    }


    public ExceptionGeneration getException() {
        return exception;
    }

}
