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

@dataclass
class ApplicationInfo:
  path: str
  applicationName: str
  versionName: str
  
@dataclass
class ProcessedDependencyInfo:
  status: str
  name: str
  fromVersion: str
  toVersion: str

def link_applicationxml_to_apps(apps_paths, application_name, verbose):
  if verbose:
    print(apps_paths, application_name)
  all_application_info = create_all_application_info(apps_paths, verbose)
  all_application_info_by_name = create_all_application_info_by_name(all_application_info)

  if not application_name in all_application_info_by_name:
    raise Exception(f"Application-Name {application_name} not found!")

  application_info_by_name = all_application_info_by_name[application_name]

  if len(application_info_by_name) > 1:
    raise Exception(f"Application-Name {application_name} not unique!")

  path = application_info_by_name[0].path
  processed_dependency_info_list = update_RuntimeContextRequirements(path, all_application_info_by_name)

  print(f"Path: {path}: Processed RuntimeContextRequirements")
  for processed_dependency_info in processed_dependency_info_list:
    print(processed_dependency_info)

def create_all_application_info(apps_paths, verbose):
  target_list = []  
  app_path_list = apps_paths.split(",")
  if verbose:
    print(f"Determine application.xml in Path: {apps_paths}")
  for app_path in app_path_list:
    for path in pathlib.Path(app_path).rglob('application.xml'):
      tree = etree.parse(str(path))
      root = tree.getroot()
      if 'applicationName' in root.attrib and 'versionName' in  root.attrib:
        app_info = ApplicationInfo(str(path), root.attrib['applicationName'], root.attrib['versionName'])
        target_list.append(app_info)
        if verbose:
          print(app_info)
  return target_list

def create_all_application_info_by_name(all_application_info):
  target_dict = {}
  for application_info in all_application_info:
    applicationName = application_info.applicationName
    if not applicationName in target_dict:
      target_list = []
      target_list.append(application_info)
      target_dict[applicationName] = target_list
    else: 
      target_dict[applicationName].append(application_info)
  return target_dict

def update_RuntimeContextRequirements(path, all_application_info_by_name):
  processed_dependency_info_list = []
  parser = etree.XMLParser(remove_blank_text=True)
  tree = etree.parse(path, parser=parser)
  root = tree.getroot()
  update_required = False
  for rtcr in root.iter('RuntimeContextRequirement'):
    rtcr_name = get_value_from_rtcr(rtcr, 'ApplicationName')
    rtcr_type = 'Application'
    if not rtcr_name:
      rtcr_name = get_value_from_rtcr(rtcr, 'WorkspaceName')
      rtcr_type = 'Workspace'
    if not rtcr_name:
      continue

    from_version_name = get_value_from_rtcr(rtcr, 'VersionName')
    processed_dependency_info = ProcessedDependencyInfo('',rtcr_name, str(from_version_name) if from_version_name is not None else "", '')

    if rtcr_name not in all_application_info_by_name:
      processed_dependency_info.status = 'Warning!: No associated application.xml found!'
      processed_dependency_info_list.append(processed_dependency_info)
      continue

    application_info_by_name = all_application_info_by_name[rtcr_name]
    if len(application_info_by_name) > 1:
      count = len(application_info_by_name)
      processed_dependency_info.status = f"Warning!: Not updated, because more then one associated application.xml found! (Count: {count})"
      processed_dependency_info_list.append(processed_dependency_info)
      continue

    to_version_name = application_info_by_name[0].versionName
    processed_dependency_info.toVersion = to_version_name
    if from_version_name != to_version_name:
      update_required = True
      processed_dependency_info.status = 'Updated'
      if rtcr_type == 'Application':
        set_value_to_rtcr(rtcr, 'VersionName', to_version_name)
      else:
        add_tag_to_rtcr(rtcr, 'ApplicationName', rtcr_name)
        add_tag_to_rtcr(rtcr, 'VersionName', to_version_name)
        remove_tag_from_rtcr(rtcr, 'WorkspaceName')
    else:
      processed_dependency_info.status = 'No update required'

    processed_dependency_info_list.append(processed_dependency_info)
  if update_required:
    write_xml(path, tree)

  return processed_dependency_info_list

def get_value_from_rtcr(rtcr, tag_name):
  value = None
  for entry in rtcr:
    if entry.tag == tag_name:
      value = entry.text
      break
  return value

def set_value_to_rtcr(rtcr, tag_name, value):
  for entry in rtcr:
    if entry.tag == tag_name:
      entry.text = value
      break

def add_tag_to_rtcr(rtcr, tag_name, value):
  child = etree.Element(tag_name)
  child.text = value
  rtcr.append(child)

def remove_tag_from_rtcr(rtcr, tag_name):
  for entry in rtcr:
    if entry.tag == tag_name:
      rtcr.remove(entry)
      break

def write_xml(path, tree):
  tree.write(path, pretty_print=True, xml_declaration=False)

  with open(path, 'r') as src:
    data = src.read()
  with open(path, 'w') as dest:
    dest.write('<?xml version="1.0" encoding="UTF-8" standalone="no"?>\n' + data)

if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('--apps_paths',type=str,required=True,help='Directories in which the application.xml files are searched (comma separated)')
  parser.add_argument('--application_name',type=str,required=True,help='Application name whose RuntimeContextRequirements should be updated')
  parser.add_argument('-v', '--verbose', action='store_true')

  args=parser.parse_args()
  link_applicationxml_to_apps(args.apps_paths, args.application_name, args.verbose)
