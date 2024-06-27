"""<!--
 * - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
--> """


class CopilotException(Exception):
    message: str
    error_type: str | None
    param: str | None
    code: int


    def __init__(self, message: str, error_type: str | None, param: str | None, code: int = 500):
        self.message = message
        self.error_type = error_type
        self.param = param
        self.code = code


    def __str__(self):
        return repr(self.message)


    def toJson(self):
        return {
            'error': {
                'message': self.message,
                'type': self.error_type,
                'param': self.param,
            }
        }
