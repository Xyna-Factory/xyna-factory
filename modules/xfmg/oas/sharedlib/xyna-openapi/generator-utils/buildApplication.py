#!/usr/bin/env python3
"""
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
"""
import re
from os import makedirs, remove, path, walk, getcwd, chdir
import argparse
import shutil
import zipfile
import subprocess

scriptDir = getcwd()

# init argument parser #######################################################

parser = argparse.ArgumentParser(
    prog='buildApplication',
    description='build application .zip-file from generated output (WIP)')
parser.add_argument(
    '--app-dir', metavar='', required=True,
    help='path to the application directory that contains the application.xml')
parser.add_argument(
    '--filter-dir', metavar='', required=False,
    help='path to the filter project directory that contains the build.xml')
args = parser.parse_args()

# split xml files ############################################################

# get xml files in XMOM directory
xmomDir = path.join(args.app_dir, 'XMOM')
xmlFiles = []
for dirpath, dirnames, filenames in walk(xmomDir):
    xmlFiles.extend([path.join(dirpath,f) for f in filenames if f.endswith('_toSplit.xml')])

# write separate files
for fileName in xmlFiles:
    with open(fileName, 'r') as inputFile:
        print(f'converting file:\n"{fileName}"')
        # determine if data type or workflow
        xmomType = re.search('<(\w+) xmlns', inputFile.readline()).group(1) 
        inputFile.seek(0);  # go back to the begining of the file
        
        content = ''
        for line in inputFile:
            content += line
            if line.startswith(f'</{xmomType}>'):  # end of the data type or workflow
                xmlPath = re.search('\WTypePath="([^"]*)"', content).group(1).replace('.', path.sep)
                fileDir = path.join(xmomDir, xmlPath)
                makedirs(fileDir, exist_ok=True)  # create subdirectories
                xmlFileName = re.search('\WTypeName="([^"]*)"', content).group(1) + '.xml'
                # write new file
                with open(path.join(fileDir, xmlFileName), 'w') as outputFile:
                    outputFile.write(content)
            
                content = ''
    
    remove(fileName)  # remove original file

# compile filter #############################################################

# compile filter if path to filter directory was provided
if args.filter_dir:
    # copy generated .java file
    filterOuptuDir = path.join(args.app_dir, 'filter/OASFilter'.replace('/', path.sep))
    filterJavaFile = path.join(filterOuptuDir, 'OASFilter.java')
    filterSrcDir = path.join(args.filter_dir,'src/com/gip/xyna/xact/filter'.replace('/', path.sep))

    if path.exists(filterJavaFile):
        shutil.copy2(filterJavaFile, filterSrcDir)

        # build filter
        chdir(args.filter_dir)
        subprocess.run(['ant', 'build'])

        # copy .jar file
        chdir(scriptDir)
        filterJar = path.join(args.filter_dir,'deploy/OASFilter.jar'.replace('/', path.sep))
        shutil.copy2(filterJar, filterOuptuDir)

        remove(filterJavaFile)  # remove generated java file

# create zip archive #########################################################

chdir(args.app_dir)

with zipfile.ZipFile(path.basename(args.app_dir) + '.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    archive.write('application.xml')
    for dirpath, dirnames, filenames in walk('.'):
        if not path.basename(dirpath).startswith('.'):
            for f in filenames:
                if not f.startswith('.'):
                    archive.write(path.join(dirpath, f))

print("created file: ", args.app_dir + '.zip')