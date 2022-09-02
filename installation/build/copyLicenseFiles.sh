#!/bin/bash

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2022 GIP SmartMercial GmbH, Germany
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

#copy license files in input dir to output dir
#copy alle entries of GROUP-file

mkdir "$2"
for i in $(find "$1" -name "*GROUP*" | grep -v "jar\$" | xargs grep -v "^#"); do cp "$1/$i" "$2"; done
for i in $(find "$1" -iname "*LICENSE*" | grep -v "jar\$"); do cp "$i" "$2"; done
for i in $(find "$1" -iname "*LICENCE*" | grep -v "jar\$"); do cp "$i" "$2"; done
for i in $(find "$1" -iname "*NOTICE*" | grep -v "jar\$"); do cp "$i" "$2"; done
for i in $(find "$1" -name "*GROUP*" | grep -v "jar\$"); do cp "$i" "$2"; done
for i in $(find "$1" -iname "*COPYRIGHT*" | grep -v "jar\$"); do cp "$i" "$2"; done

