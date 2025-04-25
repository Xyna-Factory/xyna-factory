#!/usr/bin/python3

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2024 Xyna GmbH, Germany
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

import argparse
import pathlib
from lxml import etree
from dataclasses import dataclass
from enum import Enum

class ExceptionTagConstants(Enum):
  EXCEPTIONS_STORE = '{http://www.gip.com/xyna/3.0/utils/message/storage/1.1}ExceptionStore'
  EXCEPTION_TYPE = '{http://www.gip.com/xyna/3.0/utils/message/storage/1.1}ExceptionType'

class ExceptionAttribConstants(Enum):
  CODE = 'Code'
  TYPE_NAME = 'TypeName'
  TYPE_PATH = 'TypePath'

@dataclass
class ExceptionInfo:
  path: str
  type_name: str
  type_path: str
  code_prefix: str
  code_number: str
  
@dataclass
class ProcessedExceptionInfo:
  xmom_path: str
  status: str

  def __repr__(self):
     return f"'{self.status}\', \'{self.xmom_path}\'')"


class ExceptionXmlUtils:

  def check_exception_codes(self, path, verbose):
    if verbose:
      print(path)

    all_exception_info_by_xmom_path = self.create_all_exception_info_by_xmom_path(path, verbose)
    processed_exception_info_list = []
    for xmom_path, exception_info_list in all_exception_info_by_xmom_path.items():
      processed_exception_info_list = self.check_code_prefix(xmom_path, exception_info_list)
      processed_exception_info_list.append(self.check_code_number(xmom_path, exception_info_list))

    for processed_exception_info in processed_exception_info_list:
      print(processed_exception_info)

  def check_code_prefix(self, xmom_path, exception_info_list):
    processed_exception_info_list = []
    code_prefix = None
    for exception_info in exception_info_list:
      if not code_prefix:
        code_prefix = exception_info.code_prefix
      if code_prefix == exception_info.code_prefix:
        processed_exception_info = ProcessedExceptionInfo(xmom_path, 'OK')
      else:
        processed_exception_info = ProcessedExceptionInfo(xmom_path, 'NOK:code_prefix (expected: ' + code_prefix + ', actual: ' + exception_info.code_prefix + ') path: ' + exception_info.path)
      processed_exception_info_list.append(processed_exception_info)
    return processed_exception_info_list

  def check_code_number(self, xmom_path, exception_info_list):
    processed_exception_info_list = []
    code_number_dict = {}
    for exception_info in exception_info_list:
      if exception_info.code_number not in code_number_dict:
        code_number_dict[exception_info.code_number] = exception_info
        processed_exception_info = ProcessedExceptionInfo(xmom_path, 'OK')
      else:
        processed_exception_info = ProcessedExceptionInfo(xmom_path, 'NOK:code_number (not unique: ' + exception_info.code_number + ') paths: ' + exception_info.path + ', ' + code_number_dict)
      processed_exception_info_list.append(processed_exception_info)
    return processed_exception_info_list

  def create_all_exception_info_by_xmom_path(self, path, verbose):
    target_dict = {}
    for xmom_path in pathlib.Path(path).rglob('XMOM'):
      target_list = []
      target_dict[xmom_path] = target_list
      for xml_path in pathlib.Path(xmom_path).rglob('*.xml'):
        tree = etree.parse(str(xml_path))
        root = tree.getroot()
        if root.tag == ExceptionTagConstants.EXCEPTIONS_STORE.value:
          for exception_type in root.iter(ExceptionTagConstants.EXCEPTION_TYPE.value):
            code_split = exception_type.attrib[ExceptionAttribConstants.CODE.value].s.rsplit('-', 1)
            
            exception_info = ExceptionInfo(str(xml_path),
                                           exception_type.attrib[ExceptionAttribConstants.TYPE_NAME.value],
                                           exception_type.attrib[ExceptionAttribConstants.TYPE_PATH.value],
                                           code_split[0],
                                           code_split[1])
            target_list.append(exception_info)
            if verbose:
              print(exception_info)

    return target_dict 

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('--path',type=str,required=True,help='Path under which the XMOM directory with the contained exceptions is checked')
  parser.add_argument('-v', '--verbose', action='store_true')

  args=parser.parse_args()
  exception_xml_utils = ExceptionXmlUtils()
  exception_xml_utils.check_exception_codes(args.path, args.verbose)
