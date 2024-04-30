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
package xmcp.xypilot.impl.gen.openai;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;

import xmcp.xypilot.impl.openai.CompletionBody;
import xmcp.xypilot.impl.util.ResourceUtils;

public class CompletionBodyTest {

    @Test
    public void toJSONTest() throws Ex_FileWriteException {
        CompletionBody completionBody = new CompletionBody();
        completionBody.model = "copilot-java";
        completionBody.n = Optional.of(1);
        completionBody.stop = Optional.of(List.of("\n */"));
        completionBody.prompt = "System.out.println(\"hello";
        completionBody.suffix = " world\");";
        String json = completionBody.toJSON();
        FileUtils.writeStringToFile(json, new File(ResourceUtils.resourceDirectory, "completion_body.json"));
    }

}
