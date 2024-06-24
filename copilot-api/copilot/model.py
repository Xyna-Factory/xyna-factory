from pydantic import BaseModel, constr

# copilot-<lang-ext>
ModelType = constr(regex="^(copilot-.+)$")


class CompletionRequest(BaseModel):
    model: ModelType
    prompt: str | None
    suffix: str | None
    max_tokens: int | None
    temperature: float | None = 0.6
    top_p: float | None = 1.0
    n: int | None = 1
    stream: bool | None
    logprobs: int | None = None
    echo: bool | None
    stop: str | list[str] | None
    presence_penalty: float | None = 0
    frequency_penalty: float | None = 1
    best_of: int | None = 1
    logit_bias: dict[str, float] | None
    user: str | None


class CompletionResponseUsage(BaseModel):
    completion_tokens: int
    prompt_tokens: int
    total_tokens: int


class CompletionResponseChoiceLogprobs(BaseModel):
    tokens: list[str] | None
    token_logprobs: list[float] | None
    top_logprobs: list[dict] | None
    text_offset: list[int] | None


class CompletionResponseChoice(BaseModel):
    text: str | None
    index: int | None
    logprobs: CompletionResponseChoiceLogprobs | None
    finish_reason: str | None


class CompletionResponse(BaseModel):
    id: str
    object: str
    created: int
    model: ModelType
    choices: list[CompletionResponseChoice]
    usage: CompletionResponseUsage | None


class CompletionResult(BaseModel):
    completion: str
    completion_tokens: int
    finish_reason: str