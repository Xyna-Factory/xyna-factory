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
  IS_ABSTRACT = 'IsAbstract'

class CheckTypeConstants(Enum):
  CODE_NUMBER = 'code_number'
  CODE_PREFIX = 'code_prefix'

class ProcessedExceptionInfoStatusConstants(Enum):
  OK = 'OK'
  NOK = 'NOK'

@dataclass
class ExceptionInfo:
  path: str
  type_name: str
  type_path: str
  is_abstract: bool
  code_prefix: str
  code_number: str
  
@dataclass
class ProcessedExceptionInfo:
  path: str
  check_type: str
  status: str
  info: str

  def __repr__(self):
     return f"\'{self.status}\', '{self.check_type}\', '{self.info}\', \'{self.path}\')"

class ExceptionXmlUtils:

  def check_exception_codes(self, path, verbose):
    all_exception_info_by_xmom_path = self.create_all_exception_info_by_xmom_path(path, verbose)
    processed_exception_info_list = []
    for xmom_path, exception_info_list in all_exception_info_by_xmom_path.items():
      processed_exception_info_list = self.check_code_prefix(xmom_path, exception_info_list)
      processed_exception_info_list.extend(self.check_code_number(xmom_path, exception_info_list))
      print('Check:' , xmom_path)
      for processed_exception_info in processed_exception_info_list:
        if verbose or (not verbose and processed_exception_info.status == ProcessedExceptionInfoStatusConstants.NOK.value): 
          print(processed_exception_info)


  def check_code_prefix(self, xmom_path, exception_info_list):
    processed_exception_info_list = []
    code_prefix = None
    for exception_info in exception_info_list:
      if not code_prefix:
        code_prefix = exception_info.code_prefix
      if code_prefix == exception_info.code_prefix:
        processed_exception_info = ProcessedExceptionInfo(exception_info.path, CheckTypeConstants.CODE_PREFIX.value, ProcessedExceptionInfoStatusConstants.OK.value, '')
      else:
        processed_exception_info = ProcessedExceptionInfo(exception_info.path, CheckTypeConstants.CODE_PREFIX.value, ProcessedExceptionInfoStatusConstants.NOK.value, 'Expected: ' + code_prefix + ', actual: ' + exception_info.code_prefix)
      processed_exception_info_list.append(processed_exception_info)
    return processed_exception_info_list

  def check_code_number(self, xmom_path, exception_info_list):
    processed_exception_info_list = []
    code_number_dict = {}
    for exception_info in exception_info_list:
      if exception_info.is_abstract == False and not exception_info.code_number:
        processed_exception_info = ProcessedExceptionInfo(exception_info.path, CheckTypeConstants.CODE_NUMBER.value, ProcessedExceptionInfoStatusConstants.NOK.value, 'Not defined')
      elif exception_info.code_number not in code_number_dict:
        code_number_dict[exception_info.code_number] = exception_info
        processed_exception_info = ProcessedExceptionInfo(exception_info.path, CheckTypeConstants.CODE_NUMBER.value, ProcessedExceptionInfoStatusConstants.OK.value, '')
      else:
        processed_exception_info = ProcessedExceptionInfo(exception_info.path, CheckTypeConstants.CODE_NUMBER.value, ProcessedExceptionInfoStatusConstants.NOK.value, 'Not unique: ' + exception_info.code_number + ', see path: ' + code_number_dict[exception_info.code_number].path)
      processed_exception_info_list.append(processed_exception_info)
    return processed_exception_info_list

  def create_all_exception_info_by_xmom_path(self, path, verbose):
    target_dict = {}
    for xmom_path in pathlib.Path(path).rglob('XMOM'):
      if verbose:
        print(xmom_path)
      target_list = []
      target_dict[xmom_path] = target_list
      for xml_path in pathlib.Path(xmom_path).rglob('*.xml'):
        tree = etree.parse(str(xml_path))
        root = tree.getroot()
        if root.tag == ExceptionTagConstants.EXCEPTIONS_STORE.value:
          for exception_type in root.iter(ExceptionTagConstants.EXCEPTION_TYPE.value):
            is_abstract = False
            if ExceptionAttribConstants.IS_ABSTRACT.value in exception_type.attrib and exception_type.attrib[ExceptionAttribConstants.IS_ABSTRACT.value] == 'true':
              is_abstract = True
            else:
              is_abstract = False
            
            code_prefix = ''
            code_number = ''
            if ExceptionAttribConstants.CODE.value in exception_type.attrib:
              code_split = exception_type.attrib[ExceptionAttribConstants.CODE.value].rsplit('-', 1)
              if len(code_split) > 1:
                code_prefix = code_split[0]
                code_number = code_split[1]

            exception_info = ExceptionInfo(str(xml_path),
                                           exception_type.attrib[ExceptionAttribConstants.TYPE_NAME.value],
                                           exception_type.attrib[ExceptionAttribConstants.TYPE_PATH.value],
                                           is_abstract,
                                           code_prefix,
                                           code_number)
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
