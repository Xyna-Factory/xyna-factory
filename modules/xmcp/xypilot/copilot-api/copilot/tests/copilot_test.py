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


import unittest

from proxy import CopilotProxy
from model import CompletionRequest

class TestCopilotProxy(unittest.TestCase):
    copilot = CopilotProxy(working_dir="./copilot", debug_mode=True)

    def test_run_copilot_java(self):
        self.copilot.run_copilot('./copilot/tests/IPv4Address_insert.java', 46, 18)


    def test_completion(self):
        completion = self.copilot.completion(CompletionRequest(
            model="copilot-py",
            prompt="def hello_world():\n",
            suffix="",
            stop="\n"
        ))
        self.assertEqual(len(completion.choices), 1)
        print(f"\n{completion.choices[0].text}\n\n({completion.id}, finish_reason: {completion.choices[0].finish_reason})")
        print(f"usage: {completion.usage.completion_tokens}/{completion.usage.total_tokens}")


    def test_completion_file(self):
        file = open('./copilot/tests/IPv4Address_append.java', 'r')
        content = file.read()
        completion = self.copilot.completion(CompletionRequest(
            model="copilot-java",
            prompt=content,
            stop="\n  }"
        ))
        print(f"\n{completion.choices[0].text}\n\n({completion.id}, finish_reason: {completion.choices[0].finish_reason})")
        print(f"usage: {completion.usage.completion_tokens}/{completion.usage.total_tokens}")


if __name__ == "__main__":
    unittest.main()
