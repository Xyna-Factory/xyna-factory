# * - - - - - - - - - - - - - - - - - - - - - - - - - -
# * Copyright 2024 Xyna GmbH, Germany
# *
# * Licensed under the Apache License, Version 2.0 (the "License");
# * you may not use this file except in compliance with the License.
# * You may obtain a copy of the License at
# *
# * http://www.apache.org/licenses/LICENSE-2.0
# *
# * distributed under the License is distributed on an "AS IS" BASIS,
# * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# * See the License for the specific language governing permissions and
# * limitations under the License.
# * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -



FROM python:3.10-alpine

ENV XYNA_USER=xcce
ENV XYNA_ID=1000

### create xyna group and user

RUN addgroup -S -g ${XYNA_ID} ${XYNA_USER} && adduser -S -u ${XYNA_ID} ${XYNA_USER}

### Install needed packages

RUN apk update
RUN apk add --no-cache bash neovim git nodejs openssl

USER ${XYNA_USER}:${XYNA_USER}

### install copilot extension for neovim
# RUN git clone https://github.com/github/copilot.vim \
#    ~/.config/nvim/pack/github/start/copilot.vim

# RUN git clone --depth 1 https://github.com/wbthomason/packer.nvim\
#     ~/.local/share/nvim/site/pack/packer/start/packer.nvim

# RUN nvim --headless -c 'autocmd User PackerComplete quitall' -c 'PackerSync'

COPY --chown=${XYNA_USER}:${XYNA_USER} .config /home/${XYNA_USER}/.config

WORKDIR /server

RUN pip3 install --upgrade pip
RUN pip3 install --no-cache-dir fastapi==0.82.0 pydantic==1.10.8 uvicorn==0.18.3

EXPOSE 5000

CMD ["uvicorn", "--host", "0.0.0.0", "--port", "5000", "app:app"]

