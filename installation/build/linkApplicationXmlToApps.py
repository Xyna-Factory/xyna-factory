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

def link_applicationxml_to_apps(apps_paths, application_name, verbose):
  if verbose:
    print(apps_paths, application_name)
  application_dict_list = create_application_dict_list(apps_paths, verbose)
  application_name_dict = create_application_name_dict(application_dict_list)

  if not application_name in application_name_dict:
    raise Exception(f"Application-Name {application_name} not found!")

  application_dict_list = application_name_dict[application_name]

  if len(application_dict_list) > 1:
    raise Exception(f"Application-Name {application_name} not unique!")

  path = application_dict_list[0]['Path']
  target_list = update_RuntimeContextRequirements(path, application_name_dict)

  print(f"Path: {path}: Processed RuntimeContextRequirements")
  if target_list:
    for target_dict in target_list:
      print(target_dict)

def create_application_dict_list(apps_paths, verbose):
  target_list = []  
  app_path_list = apps_paths.split(",")
  if verbose:
    print(f"Determine application.xml in Path: {apps_paths}")
  for app_path in app_path_list:
    for path in pathlib.Path(app_path).rglob('application.xml'):
      tree = etree.parse(str(path))
      root = tree.getroot()
      if 'applicationName' in root.attrib and 'versionName' in  root.attrib:
        target_dict = {}
        target_dict['Path'] = str(path)
        target_dict['ApplicationName'] = root.attrib['applicationName']
        target_dict['VersionName'] =  root.attrib['versionName']
        target_list.append(target_dict)
        if verbose:
          print(target_dict)
  return target_list

def create_application_name_dict(application_dict_list):
  target_dict = {}
  for application_dict in application_dict_list:
    applicationName = application_dict['ApplicationName']
    if not applicationName in target_dict:
      target_list = []
      target_list.append(application_dict)
      target_dict[applicationName] = target_list
    else: 
      target_dict[applicationName].append(application_dict)
  return target_dict
       
def update_RuntimeContextRequirements(path, application_name_dict):
  target_list = []
  parser = etree.XMLParser(remove_blank_text=True)
  tree = etree.parse(path, parser=parser)
  root = tree.getroot()
  update_required = False
  for rtcr in root.iter('RuntimeContextRequirement'):
    rtcr_name = get_application_name_from_rtcr(rtcr)
    rtcr_type = 'Application'
    if not rtcr_name:
      rtcr_name = get_workspace_name_from_rtcr(rtcr)
      rtcr_type = 'Workspace'
    if not rtcr_name:
      continue

    from_version_name = get_version_name_from_rtcr(rtcr)
    target_dict = {}
    target_dict['Status'] = ''
    target_dict['Name'] = rtcr_name
    target_dict['From Version'] = str(from_version_name) if from_version_name is not None else ""
    target_dict['To Version'] = ''

    if rtcr_name not in application_name_dict:
      target_dict['Status'] = 'Warning!: No associated application.xml found!'
      target_list.append(target_dict)
      continue

    application_dict_list = application_name_dict[rtcr_name]
    if len(application_dict_list) > 1:
      count = len(application_dict_list)
      target_dict['Status'] = f"Warning!: Not updated, because more then one associated application.xml found! (Count: {count})"
      target_list.append(target_dict)
      continue

    to_version_name = application_dict_list[0]['VersionName']
    target_dict['To Version'] = to_version_name
    if from_version_name != to_version_name:
      update_required = True
      target_dict['Status'] = 'Updated'
      if rtcr_type == 'Application':
        set_version_name_to_rtcr(rtcr, to_version_name)
      else:
        add_application_name_to_rtcr(rtcr, rtcr_name)
        add_version_name_to_rtcr(rtcr, to_version_name)
        remove_workspace_name_from_rtcr(rtcr)
    else:
      target_dict['Status'] = 'No update required'

    target_list.append(target_dict)
  if update_required:
    write_xml(path, tree)

  return target_list

def get_application_name_from_rtcr(rtcr):
  applicaton_name = None
  for entry in rtcr:
    if entry.tag == 'ApplicationName':
      applicaton_name = entry.text
  return applicaton_name

def add_application_name_to_rtcr(rtcr, applicaton_name):
  child = etree.Element("ApplicationName")
  child.text = applicaton_name
  rtcr.append(child)

def get_version_name_from_rtcr(rtcr):
  version_name = None
  for entry in rtcr:
    if entry.tag == 'VersionName':
      version_name = entry.text
  return version_name

def set_version_name_to_rtcr(rtcr, version_name):
  for entry in rtcr:
    if entry.tag == 'VersionName':
      entry.text = version_name

def add_version_name_to_rtcr(rtcr, version_name):
  child = etree.Element("VersionName")
  child.text = version_name
  rtcr.append(child)

def get_workspace_name_from_rtcr(rtcr):
  workspace_name = None
  for entry in rtcr:
    if entry.tag == 'WorkspaceName':
      workspace_name = entry.text
  return workspace_name

def remove_workspace_name_from_rtcr(rtcr):
  for entry in rtcr:
    if entry.tag == 'WorkspaceName':
      rtcr.remove(entry)
      break

def write_xml(path, tree):
  tree.write(path, pretty_print=True, xml_declaration=False)

  with open(path, 'r') as src:
    data = src.read()
  with open(path, 'w') as dest:
    dest.write('<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n' + data)

parser = argparse.ArgumentParser()
parser.add_argument('--apps_paths',type=str,required=True,help='Directories in which the application.xml files are searched (comma separated)')
parser.add_argument('--application_name',type=str,required=True,help='Application name whose RuntimeContextRequirements should be updated')
parser.add_argument('-v', '--verbose', action='store_true')

args=parser.parse_args()
link_applicationxml_to_apps(args.apps_paths, args.application_name, args.verbose)
