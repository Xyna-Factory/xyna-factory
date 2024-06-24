import subprocess
import sys
import os
import logging
import json
import time

import uvicorn
import argparse

from fastapi import FastAPI, Request, Response
from fastapi.responses import JSONResponse

from error import CopilotException
from model import CompletionRequest
from proxy import CopilotProxy

app_logger = logging.getLogger("uvicorn")

app = FastAPI(
    title="Copilot",
    description="OpenAI API for Copilot",
    docs_url="/"
)

parser = argparse.ArgumentParser()
parser.add_argument('-d', '--debug', action='store_true', help='Debug mode')
parser.add_argument('-s', '--secure', action='store_true', help='Enable TLS')
parser.add_argument('-t', '--maxtokens', default='4096', help='Max tokens to generate')
args = parser.parse_args()

if args.debug:
    app_logger.info("Running in debug mode...")

copilot = CopilotProxy(max_tokens=int(args.maxtokens), debug_mode=args.debug)


@app.exception_handler(CopilotException)
async def copilot_handler(request: Request, exc: CopilotException) -> JSONResponse:
    """
    Handles CopilotException and returns a JSONResponse with the exception's code and json
    This is the response that is returned to the client in case of an error in the copilot proxy

    Args:
        request (Request): The original request that caused the exception
        exc (CopilotException): The exception that was raised

    Returns:
        JSONResponse: JSONResponse with the exception's code and json
    """
    return JSONResponse(
        status_code=exc.code,
        content=exc.json()
    )


@app.post("/v1/completions")
async def completions(request: CompletionRequest) -> JSONResponse:
    """
    Forwards completion request to the copilot proxy and returns the response
    Request and response are logged if debug mode is enabled

    Args:
        request (CompletionRequest): CompletionRequest object

    Raises:
        CopilotException: Thrown when copilot proxy raises an exception
    Returns:
        JSONResponse: JSONResponse with the completion response from the copilot proxy
    """
    try:
        if args.debug:
            app_logger.info(f"Request:\n{json.dumps(request.dict(), indent=4)}")
            if request.suffix:
                app_logger.info(f"Prompt:\n{request.prompt}[FILL]{request.suffix}")
            else:
                app_logger.info(f"Prompt:\n{request.prompt}")

        st = time.time()
        response = copilot.completion(request)
        ed = time.time()

        app_logger.info(f"Returned completion in: {((ed - st)*1000)} ms")
        if args.debug:
            app_logger.info(f"Response:\n{json.dumps(response.dict(), indent=4)}")
            for choice, i in zip(response.choices, range(len(response.choices))):
                app_logger.info(f"Choice {i}:\n{choice.text}\n")

        return Response(
            status_code=200,
            content=json.dumps(response.dict()),
            media_type="application/json"
        )
    except OSError as e:
        app_logger.error(e.strerror)
        raise CopilotException(
            code=500,
            error_type="OSError",
            message=e.strerror
        )


def setup_certificates() -> tuple[str, str]:
    """
    Writes SSL Certificate into `/tmp/ssl`

    Returns:
        [str, str]: First string is path to certificate-file, second is path to key-file
    """
    dirpath = "/tmp/ssl"
    certpath = dirpath + "/cert.pem"
    keypath = certpath

    # setup /tmp/ssl
    if not os.path.exists(dirpath):
        os.mkdir(dirpath)

    ip = subprocess.run(["hostname", "-i"], stdout=subprocess.PIPE).stdout.decode("utf-8").strip()
    os.system(f"openssl req -newkey rsa:4069 -nodes -keyout {keypath} -x509 -days 3650 -out {certpath} -subj \"/CN={ip}\"")

    return [keypath, certpath]


if __name__ == "__main__":
    if args.secure:
        paths = setup_certificates()
        print(f"path[0]: {paths[0]}")
        print(f"path[1]: {paths[1]}")
        uvicorn.run("app:app", host="0.0.0.0", port=5000, ssl_keyfile=paths[1], ssl_certfile=paths[0])
    else:
        uvicorn.run("app:app", host="0.0.0.0", port=5000)
