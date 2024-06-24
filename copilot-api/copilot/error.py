

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