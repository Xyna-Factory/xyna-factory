#!/usr/bin/python3
import argparse
import os
import shutil
from pathlib import Path

class AppCreator:

  def __init__(self):
    self.this_dir = Path(__file__).parent.absolute()

  def copy_template(self, target_dir: str, file_name: str):
    path_to_template = os.path.join(self.this_dir, "templates", f"{file_name}.template")
    shutil.copy(path_to_template, os.path.join(target_dir, file_name))

  def copy_and_set_application_xml(self, appDir: str, appName: str, appVersion: str):
    application_xml_template = os.path.join(self.this_dir, "templates", "application.xml.template")
    with open(application_xml_template, 'r') as file:
      filedata = file.read()
    filedata = filedata.replace('{{APPNAME}}', appName)
    filedata = filedata.replace('{{VERSIONNAME}}', appVersion)
    application_xml = os.path.join(appDir, "application.xml")
    with open(application_xml, 'w') as file:
      file.write(filedata)

  def copy_and_set_project_bom_xml(self, projectDir: str, groupId: str):
    relpath = os.path.relpath(self.this_dir.parent.parent.absolute(), Path(app_dir).absolute())
    relpath = relpath.replace("\\", "/")
    relpath += "/installation/build/pom.xml"
    project_bom_xml_template = os.path.join(self.this_dir, "templates", "project.pom.xml.template")
    with open(project_bom_xml_template, 'r') as file:
      filedata = file.read()
    filedata = filedata.replace('{{XYNABOM}}', relpath)
    filedata = filedata.replace('{{groupId}}', groupId)
    project_bom_xml = os.path.join(projectDir, "pom.xml")
    with open(project_bom_xml, 'w') as file:
      file.write(filedata)

  def copy_and_set(self, target_dir: str, file_name: str, replacements: list[[str, str]]):
    path_to_template = os.path.join(self.this_dir, "templates", f"{file_name}.template")
    with open(path_to_template, 'r') as file:
      filedata = file.read()
    for (key, value) in replacements:
      filedata = filedata.replace(key, value)
    result_file = os.path.join(target_dir, file_name)
    with open(result_file, 'w') as file:
      file.write(filedata)

  def createApp(self, projectDir: str, appDir: str, appName: str, appVersion: str):
    next_steps = []
    app_dir = os.path.join(projectDir, appDir)
    print(f"creating application {appName}/{appVersion} at {app_dir}")
    if not os.path.isdir(projectDir):
      print("project directory does not exist. Creating it and placing server.properties and pom.xml in it.")
      os.makedirs(projectDir, exist_ok= True)
      self.copy_template(projectDir, "server.properties" )
      self.copy_and_set_project_bom_xml(projectDir, "myproject")
      next_steps.append("* set server.properties")
      next_steps.append("* set groupId in pom.xml")
    else:
      print("project directory exists already.")
    if os.path.isdir(app_dir):
      print("application exists already! Terminating")
      return
    
    relpath = os.path.relpath(self.this_dir.parent.parent.absolute(), Path(app_dir).absolute())
    relpath = relpath.replace("\\", "/")
    print(f"relpath: {relpath}")
    
    os.makedirs(app_dir)
    self.copy_template(app_dir, "application.properties" )
    self.copy_and_set(app_dir, "application.xml", [("{{APPNAME}}", appName),("{{VERSIONNAME}}", appVersion)])
    self.copy_and_set(app_dir, "build.xml", [("{{ROOTDIRPATH}}", relpath)])
    next_steps.append("* set application.properties")
    print("Done. Next steps:")
    for step in next_steps:
      print(step)
    



if __name__ == '__main__':
  parser = argparse.ArgumentParser()
  parser.add_argument('--projectDir',type=str,required=True,help='project directory - root of your project, probably next to xyna-factory')
  parser.add_argument('--appDir',type=str,required=True,help='application directory relative to projectDir')
  parser.add_argument('--appName',type=str,required=True,help='name of the application')
  parser.add_argument('--appVersion',type=str,required=True,help='version of the application')
  args=parser.parse_args()
  creator = AppCreator()
  creator.createApp(args.projectDir, args.appDir, args.appName, args.appVersion)