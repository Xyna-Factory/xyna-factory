#!/bin/bash
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




PWD="$(dirname "$(readlink -f "$0")")"


# removes the running container of a given image:tag
_rm() {
    local CID=$(docker ps -a | grep "$1:$2" | tr -s ' ' | cut -d ' ' -f1)
    if [[ ! -z "$CID" ]]; then
        echo "rm -f $CID"
        docker rm -f "$CID"
    fi
}


# removes the image of a given image:tag
_rmi() {
    local IID=$(docker images | grep "$1"'.*'"$2" | tr -s ' ' | cut -d ' ' -f3)
    if [[ ! -z "$IID" ]]; then
        echo "rmi -f $IID"
        docker rmi -f "$IID"
    fi
}


# builds an image for a given image:tag
_build() {
    docker build . \
        --network host \
        --tag "$1":"$2" \
        --file "$PWD"/"$1".dockerfile
}


# starts a container for a given copilot image:tag
_run() {
    docker run -dit \
        --name "$3" \
        --hostname "$1" \
        --volume $PWD/copilot:/server \
        "${@:4}" \
        "$1":"$2" \
        bash
}


# execs into a container with a given name
_exec() {
    local CNAME=$1; shift
    docker exec -it "$CNAME" bash "$@"
}


# main function
_main() {
    TAG="latest"
    CNAME="copilot-api-dev-$TAG"

    # read argument
    local CMD=$1; shift
    if [[ -z "$CMD" ]]; then
        echo "Usage: $0 [rm|rmi|build|run|exec|start]"
        return 1
    fi

    if [ "$CMD" = "rm" ]; then
        _rm "copilot-api" "$TAG"
    fi
    if [ "$CMD" = "rmi" ]; then
        _rmi "copilot-api" "$TAG"
    fi
    if [ "$CMD" = "build" ]; then
        _rm "copilot-api" "$TAG"
        _rmi "copilot-api" "$TAG"
        _build "copilot-api" "$TAG"
        return
    fi
    if [ "$CMD" = "run" ]; then
        _rm "copilot-api" "$TAG"
        _run "copilot-api" "$TAG" "$CNAME" "$@"
        return
    fi
    if [ "$CMD" = "exec" ]; then
        _exec "$CNAME" "$@"
        return
    fi
    if [ "$CMD" = "start" ]; then
        _exec "$CNAME" -c "python app.py $* 2>&1 | tee app.log; bash"
        return
    fi
}


_main "$@"
