"""<!--
 * - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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

import json
import random
import string
import os
import time
import logging

from model import (
    CompletionRequest,
    CompletionResponse,
    CompletionResponseChoice,
    CompletionResponseUsage,
    CompletionResult,
)

app_logger = logging.getLogger("uvicorn")


class CopilotProxy:
    """
    This class is responsible for handling completion requests using the copilot plugin for nvim.
    """

    __LUA_SCRIPT_PATH: str = "./script.lua"

    __working_dir: str
    __max_tokens: int
    __debug_mode: bool

    def __init__(
        self, working_dir: str = ".", max_tokens: int = 4096, debug_mode: bool = False
    ):
        """
        Creates the CopilotProxy object.

        Args:
            working_dir (str, optional): The working directory. Defaults to ".".
            max_tokens (int, optional): The maximum number of tokens to generate with each completion. Defaults to 4096.
            debug_mode (bool, optional): If enabled, temporary files will not be deleted. Useful for testing. Defaults to False.

        Raises:
            FileNotFoundError: If the script template file is not found in the working directory.
        """

        self.__working_dir = working_dir
        self.__max_tokens = max_tokens
        self.__debug_mode = debug_mode

        # create prompts and solutions folder if they dont exist yet
        CopilotProxy.create_folder_if_not_exists(f"{self.__working_dir}/prompts")
        CopilotProxy.create_folder_if_not_exists(f"{self.__working_dir}/solutions")

        # check if script exists
        if not os.path.exists(self.__LUA_SCRIPT_PATH):
            raise FileNotFoundError(f"Script file {self.__LUA_SCRIPT_PATH} not found")

    @staticmethod
    def create_folder_if_not_exists(folder_path: str):
        """
        Creates a folder if it doesn't exist.

        Args:
            folder_path (str): The folder path.
        """

        if not os.path.exists(folder_path):
            os.makedirs(folder_path)

    @staticmethod
    def random_id() -> str:
        """
        Generates a random 29 character string.
        Each completion request is assigned a unique execution id. This id is used to name the temporary files corresponding to the completion.
        The id is also returned as part of the completion response.
        Returns:
            str: The random id.
        """

        return "".join(
            random.choice(string.ascii_letters + string.digits) for _ in range(29)
        )

    @staticmethod
    def trim_with_stopwords(output: str, stopwords: list) -> tuple[str, bool]:
        """
        Trims the output string at the first occurrence of any of the stopwords.

        Args:
            output (str): The output string to trim.
            stopwords (list): A list of stopwords to trim at.

        Returns:
            str: The trimmed output string.
            bool: True if a stopword was found, False otherwise.
        """

        stop_index = len(output)

        for stopword in stopwords:
            index_of_stopword = output.find(stopword)
            if index_of_stopword != -1:
                stop_index = min(stop_index, index_of_stopword)
        return [output[:stop_index], stop_index < len(output)]

    @staticmethod
    def num_tokens(s: str) -> int:
        """
        Counts the number of tokens in a string, where 1 token is 4 characters.

        Args:
            s (str): The string to count tokens in.

        Returns:
            int: The number of tokens.
        """
        return -(len(s) // -4)

    @staticmethod
    def num_characters(num_tokens: int) -> int:
        """
        Returns the number of characters for a given number of tokens, where 1 token is 4 characters.

        Args:
            num_tokens (int): The number of tokens.

        Returns:
            int: The number of characters.
        """
        return num_tokens * 4

    @staticmethod
    def post_process_completion(
        completion: str, max_tokens: int, stopwords: list
    ) -> CompletionResult:
        """
        Trims the completion at the first occurrence of any of the stopwords or at the maximum number of tokens whichever comes first.
        Determines the finish reason, which is one of the following:
            "length": copilot generated more than (or equal to) max_completion_tokens
            "stop": copilot generated a stopword, before or by reaching max_completion_tokens
            "timeout": copilot stopped generating before reaching max_completion_tokens or a stopword

        Args:
            completion (str): The completion to post process.
            max_completion_tokens (int): The maximum number of tokens to generate for the completion.
            stopwords (list): The list of stopwords to trim at.

        Returns:
            The trimmed completion, the finish reason and the number of completion tokens.
        """
        # trim at stopwords
        [completion, found_stopword] = CopilotProxy.trim_with_stopwords(
            completion, stopwords
        )

        # determine finish reason
        # "length": copilot generated more than (or equal to) max_tokens
        # "stop": copilot generated a stopword, before or by reaching max_tokens
        # "timeout": copilot stopped generating before reaching max_tokens or a stopword
        completion_tokens = CopilotProxy.num_tokens(completion)
        finish_reason = None
        if completion_tokens > max_tokens:
            finish_reason = "length"
        elif found_stopword:
            finish_reason = "stop"
        elif completion_tokens == max_tokens:
            finish_reason = "length"
        else:
            finish_reason = "timeout"

        # trim at maximum number of tokens
        if completion_tokens >= max_tokens:
            completion = completion[
                : CopilotProxy.num_characters(max_completion_tokens)
            ]
            completion_tokens = max_tokens

        return CompletionResult(
            completion=completion,
            completion_tokens=completion_tokens,
            finish_reason=finish_reason,
        )

    def __path_to_prompt_file(self, exec_id: str, ext: str) -> str:
        """
        Returns the path to the prompt file for a given execution id and file extension.

        Args:
            exec_id (str): The execution id.
            ext (str): The file extension.

        Returns:
            str: The path to the prompt file.
        """

        return f"{self.__working_dir}/prompts/{exec_id}.{ext}"

    def __path_to_solution_file(self, exec_id: str) -> str:
        """
        Returns the path to the solution json-file for a given execution id.

        Args:
            exec_id (str): The execution id.

        Returns:
            str: The path to the solution json-file.
        """

        return f"{self.__working_dir}/solutions/{exec_id}.json"

    def run_copilot(self, path_to_file: str, line: int, col: int):
        """
        Runs the nvim copilot lua extension on a file at the given line and column number.

        Args:
            path_to_file (str): The path to the file to run the copilot extension on.
            line (int): The line number to run the copilot extension on.
            col (int): The column number to run the copilot extension on.
        """
        #  - :set virtualedit=onemore to allow cursor to be placed after the last character in the line
        #  - :call cursor({line},{col}) to place the cursor at the given line and column
        #  - :luafile {self.__LUA_SCRIPT_PATH} to run the copilot extension
        #  - {path_to_file} to run the script on the given file
        os.system(
            f'nvim --headless "+:set virtualedit=onemore" "+:call cursor({line},{col})" "+:luafile {self.__LUA_SCRIPT_PATH}" {path_to_file}'
        )

    def read_completions(self, path_to_file: str) -> list[str]:
        """
            Reads the completions from the given solution file.
            The solution file is created by the copilot lua extension in the following json format:
            {
                "num_solutions": 10,
                "solutions": [
                    {
                        "range": {
                            "start": {
                                "line": 45,
                                "character": 17
                            },
                            "end": {
                                "line": 45,
                                "character": 17
                            }
                        },
                        "panelId": "copilot",
                        "score": 0,
                        "docVersion": 5,
                        "displayText": "def hello_world():\n    print(\"Hello, world!\")\n\n",
                        "solutionId": "e8d8eabdad9b30bcaeaf6f094b9923dfbf8bbcec2861006c9637b9dc5b80afca",
                        "completionText": "print(\"Hello, world!\")",
                    },
                ...
                ]
            }

        Args:
            path_to_file (str): The path to the file containing the solutions.

        Returns:
            list[str]: List of completionTexts.
        """

        try:
            with open(path_to_file, "r") as f:
                result = json.load(f)
        except OSError:
            return []

        return [solution["completionText"] for solution in result["solutions"]]

    def completion(self, request: CompletionRequest) -> CompletionResponse:
        """
        Generates a completion for the given request using the nvim copilot extension.

        Args:
            request (CompletionRequest): The completion request.
                The following parameters are supported:
                 - model: The model to use for completion. The model name must be of the form copilot-<lang-ext>, e.g. copilot-py.
                 - prompt: The code to complete.
                 - suffix: The code following the line on which to execute copilot.
                 - max_tokens: The maximum number of tokens to generate for the completion. The number of tokens generated will always be less than self.__max_tokens.
                 - echo: Whether to include the prompt in the response.
                 - stop: A list of stopwords at which to trim the completion, the stopword itself is not included in the completion.

        Returns:
            CompletionResponse: The response to the completion request.
                The following parameters are returned:
                 - id: The completion id.
                 - object: The type of completion, is always "text_completion".
                 - created: The timestamp at which the response was created.
                 - model: The model that generated the completion, this is equal to the model specified in the corresponding request.
                 - choices: A list of completion choices.
                    - text: The completion text, including the prompt if echoed.
                    - index: The index in the list of choices returned in the completion response.
                    - finish_reason: "length" if max_tokens reached, "stop" if a stopword was found, None otherwise.
                - usage: The usage statistics of the model, including the number of tokens generated.
        """

        # generate a random execution id
        exec_id = self.random_id()

        # get prompt and suffix from request
        prompt = request.prompt if request.prompt is not None else ""
        suffix = request.suffix if request.suffix is not None else ""
        n = max(request.n, 1) if request.n is not None else 1

        # get stopwords from request
        if request.stop is None:
            stopwords = []
        else:
            stopwords = (
                request.stop if isinstance(request.stop, list) else [request.stop]
            )

        # get maximum number of tokens from request
        max_tokens = (
            request.max_tokens if request.max_tokens is not None else self.__max_tokens
        )
        max_tokens = min(max_tokens, self.__max_tokens)

        # get language extension from model name copilot-{ext}
        ext = request.model.split("-")[1]

        # create temporary file with prompt and suffix
        path_to_prompt_file = self.__path_to_prompt_file(exec_id, ext)
        with open(path_to_prompt_file, "w") as file:
            file.write(prompt)
            file.write(suffix)

        print(f"Generating completion for request {exec_id} ...", flush=True)

        # set line number to the end of the prompt
        # and column number to the number of characters in the last line of the prompt
        prompt_lines = prompt.count("\n")
        line = prompt_lines + 1
        col = len(prompt.split("\n")[-1]) + 1

        # run copilot lua script on temporary file
        self.run_copilot(path_to_prompt_file, line, col)

        # print newline to end single-line prints from lua script
        print("", flush=True)

        # read completions from solution file generated by the lua script
        path_to_solution_file = self.__path_to_solution_file(exec_id)
        completions = self.read_completions(path_to_solution_file)

        # trim completions at stopwords and maximum number of tokens, returns a list of CompletionResult
        results = [
            self.post_process_completion(completion, max_tokens, stopwords)
            for completion in completions[:n]
        ]

        # remove temporary files
        if not self.__debug_mode:
            os.remove(path_to_prompt_file)
            os.remove(path_to_solution_file)

        # create n choices, keep only the first n completions
        choices = [
            CompletionResponseChoice(
                text=prompt + result.completion if request.echo else result.completion,
                index=i,
                logprobs=None,
                finish_reason=result.finish_reason,
            )
            for result, i in zip(results, range(n))
        ]

        # usage statistics
        # count number of tokens in prompt, suffix and completions, using a simple heuristic based on the number of characters
        prompt_tokens = CopilotProxy.num_tokens(prompt)
        suffix_tokens = CopilotProxy.num_tokens(suffix)
        completion_tokens = sum([result.completion_tokens for result in results])

        usage = CompletionResponseUsage(
            completion_tokens=completion_tokens,
            prompt_tokens=prompt_tokens,
            total_tokens=completion_tokens + prompt_tokens + suffix_tokens,
        )

        # return completion response
        return CompletionResponse(
            id="cmpl-" + exec_id,
            object="text_completion",
            created=int(time.time()),
            model=request.model,
            choices=choices,
            usage=usage,
        )
