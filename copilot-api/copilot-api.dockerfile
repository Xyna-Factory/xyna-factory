FROM python:3.10-alpine

# Enable the following lines to specify a different user/id
# ENV USER=user
# ENV ID=1000
# RUN addgroup -S -g ${ID} ${USER} && adduser -S -u ${ID} ${USER}

### Install needed packages

RUN apk update
RUN apk add --no-cache bash neovim git nodejs openssl

# Enable the following lines to specify a different user
# USER ${USER}:${USER}
# COPY --chown=${USER}:${USER} .config /home/${USER}/.config

# Current user, disable the following lines to specify a different user
COPY .config ~/.config

WORKDIR /server

RUN pip3 install --upgrade pip
RUN pip3 install --no-cache-dir fastapi==0.82.0 pydantic==1.10.8 uvicorn==0.18.3

EXPOSE 5000

CMD ["uvicorn", "--host", "0.0.0.0", "--port", "5000", "app:app"]