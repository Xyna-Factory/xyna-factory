# Copilot API

Provides a limited implementation of the [OpenAI API](https://beta.openai.com/docs/api-reference/) for [GitHub Copilot](https://copilot.github.com/) using the [Lua Copilot Plugin](https://github.com/zbirenbaum/copilot.lua) in [Neovim](https://neovim.io/).


## Server


### Prerequisites

- [Docker](https://docs.docker.com/get-docker/)

### Config

To run `docker.sh` without `sudo` you need to add your user to the docker group:

```bash
sudo usermod -aG docker $USER
```

To use a different user than the current user adjust the corresponding lines in the `copilot-api.dockerfile`.

### Build

To build the `copilot-api:latest` docker image run:

```bash
./docker.sh build
```

### Run

To create and run the `copilot-api-dev-latest` docker container run:

```bash
./docker.sh run
```

To publish a different port (e.g. 443) run
```bash
./docker.sh run -p 443:5000
```

### Setup

The copilot lua plugin is downloaded the first time Neovim is started and must be authorized before starting the server for the first time.
To authorize the extension with your github accout open nvim (wait for the plugins to download) and run `:Copilot auth`.

```bash
./docker.sh exec
nvim
:Copilot auth
```

### Start

To start the server run:

```bash
./docker.sh start
```
#### Options

`-d --debug`: enable verbose logging and keep generated files

`-s --secure`: enable TLS

`-t --maxtokens`: Maximum number of tokens to generate. Defaults to 4096.

## Client

Once everything is up and running, you should have a server listening for requests on `http://localhost:5000`. You can now talk to it using the standard [OpenAI API](https://beta.openai.com/docs/api-reference/) (although the full API isn't implemented yet). For example, from Python, using the [OpenAI Python bindings](https://github.com/openai/openai-python):

### API

```python

import openai

openai.api_key = 'dummy'

openai.api_base = 'http://127.0.0.1:5000/v1'

# to generate python code use copilot-py, for java use copilot-java, etc.
result = openai.Completion.create(model='copilot-py', prompt='def helloWorld():\n', stop=["\n\n"])

print(result)
```
```python
<OpenAIObject text_completion id=cmpl-8rg2LMzENI7V4alxA08BL2hKgdouT ...>
JSON: {
  "choices": [
    {
      "finish_reason": "stop",
      "index": 0,
      "logprobs": null,
      "text": "  print(\"Hello, World!\")"
    }
  ],
  "created": 1685968477,
  "id": "cmpl-8rg2LMzENI7V4alxA08BL2hKgdouT",
  "model": "copilot-py",
  "object": "text_completion"
}
```

### Curl with RESTful APIs

```bash
curl -s -H "Accept: application/json" -H "Content-type: application/json" -d '{"model":"copilot-py","prompt":"def helloWorld():\n","stop":["\n\n"]}' http://localhost:5000/v1/completions
```

