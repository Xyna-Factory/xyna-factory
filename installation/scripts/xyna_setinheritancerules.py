# -*- coding: utf-8 -*-

# - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
# Copyright 2023 Xyna GmbH, Germany
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
import subprocess
import re
import sys
import argparse
import json


def execShellCmd(command):
    try:
        proc = subprocess.check_output(command, shell=True)
        lines = proc.split('\n')
        print ("DEBUG: got command {0}".format(command))
        return(lines)
    except subprocess.CalledProcessError as exc:
        print ("ERROR: command \'{0}\' failed. Output: \'{1}\'".format(exc.cmd, exc.output))
        sys.exit(1)


def create_location_string(ws_name, app_name, app_version):
  return create_location_string_app(app_name, app_version) if len(ws_name) == 0 else create_location_string_ws(ws_name)

def create_location_string_app(app_name, app_version):
    return("-applicationName \"{0}\" -versionName \"{1}\"".format(app_name, app_version))

def create_location_string_ws(ws_name):
    return("-workspaceName \"{0}\"".format(ws_name))


class Config:    
    def __init__(self, path):
        with open(path, "r") as configAsJson:
            cfg = json.loads(configAsJson.read())
            self.be_path = cfg["be_path"] if "be_path" in cfg else "/etc/opt/xyna/environment/black_edition_001.properties"
            self.rule_mon_lvl = cfg["rule_mon_lvl"] if "rule_mon_lvl" in cfg else ""
            self.rule_filter = cfg["rule_filter"] if "rule_filter" in cfg else "*"
            self.rule_precedence = cfg["rule_precedence"] if "rule_precedence" in cfg else "0"
            self.rtcs = cfg["rtcs"] if "rtcs" in cfg else []
            self.excluded_workflows = cfg["excluded_workflows"] if "excluded_workflows" in cfg else []

    def __str__(self):
        output_format = "Config: [\n\tBlack Edition Properties Path: {0}\n\tMonLvl: {1}\n\tFilter: {2}\n\tPrecedence: {3}\n\tRTCs ({4}){5}\n\tExcluded Workflows ({6}){7}\n]"
        rtc_str = "" if len(self.rtcs) == 0 else ":\n\t\t{0}".format("\n\t\t".join(self.rtcs))
        excluded_str = "" if len(self.excluded_workflows) == 0 else ":\n\t\t{0}".format("\n\t\t".join(self.excluded_workflows))
        return output_format.format(self.be_path, self.rule_mon_lvl, self.rule_filter, self.rule_precedence, len(self.rtcs), rtc_str, len(self.excluded_workflows), excluded_str)

    @staticmethod
    def createConfig(path):
        config = {
            "be_path": "/etc/opt/xyna/environment/black_edition_001.properties",
            "rule_mon_lvl": "",
            "rule_filter": "",
            "rule_precedence": "",
            "rtcs": [],
            "excluded_workflows": []
        }
        with open(path, "w") as config_file:
            config_file.write(json.dumps(config, indent=2))

# class to get workspaces or applications, their versions and all their workflows
class xyna_application_workspace:

    # constructor uses boolean isapp to determine if it's an app or workspace.
    # Subsequent checks use len(workspace_name) to determine if it's an application.
    def __init__(self,exec_path,name,isapp):
        self.app_name=""
        self.version_name=""
        self.workspace_name=""
        if isapp:
            self.app_name=name
            self.version_name=self.get_version(exec_path)
            if (self.version_name is None):
                print("ERROR: Could not read version for application name '{0}'".format(self.app_name))
                sys.exit(1)
            #print("DEBUG: Found app {0} with version {1}".format(self.app_name,self.version_name))
        else:
            self.workspace_name=name
        self.all_workflows=self.get_workflows(exec_path)
        rtc = "application {0} with version {1}".format(self.app_name, self.version_name) if isapp else "workspace {0}".format(self.workspace_name)
        print("INFO: found {0} workflows in {1}".format(len(self.all_workflows), rtc))

    # determines version if application
    def get_version(self,exec_path):
        out = execShellCmd(exec_path+" listapplications")
        for lines in out:
            match = re.search(self.app_name+"\' \'"+'([a-zA-Z0-9.]+)'+"\'",lines)
            if match:
                return(match.group(1))

    def get_workflows(self,exec_path):
        workflows=[]
        location_string=create_location_string(self.workspace_name, self.app_name, self.version_name)
        out = execShellCmd("{0} listwfs {1}".format(exec_path, location_string))
        for lines in out:
            match = re.search("Name: ([a-zA-Z0-9.]+),",lines)
            if match:
                parsed = match.group(1).strip()
                workflows.append(parsed)
        return (workflows)

# class for creating inheritance rules as objects
class inheritance_rule:

    def __init__(self,exec_path,level,prec,filter):
        self.monitoring_level=self.set_monitoring_level(level,exec_path)
        self.precedence=self.set_precedence(prec)
        self.child_filter=self.set_filter(filter)

    # monitoring level can also be a property.
    # This method checks if it contains a number consistent with a monitoring level first
    def set_monitoring_level(self,level,exec_path):
        if (isinstance(level, int) or level.isdigit()) and (0 <= int(level) <= 20):
            return (int(level))
        else:
            out = execShellCmd("{0} get -key \"{1}\"".format(exec_path, level))
            for lines in out:
                match = re.match("Value of property \'"+str(level)+"\': ([0-9]{1,2})",lines)
                if match and (0 <= int(match.group(1)) <= 20):
                    #print("DEBUG: property '{0}' found with monitoring value '{1}' set".format(level,int(match.group(1))))
                    return ("\"{0}\"".format(level))
                else:
                    print("ERROR: Could not read property value for monitoring")
                    sys.exit(1)

    def set_precedence(self,prec):
        if prec.isdigit() and (0 <= int(prec)):
            return (prec)
        else:
            print("ERROR: Could not read precedence for rule")
            sys.exit(1)

    def set_filter(self,filter):
        if filter is not None:
            return (filter)
        else:
            print("ERROR: Could not read filter for rule")
            sys.exit(1)

# class to save a single workflow/orderType and its inheritance rules and location
class workflow_tuple:

    def __init__(self,name):
        self.workflow_fqn=name
        self.workflow_application=""
        self.workflow_application_version=""
        self.workflow_workspace=""
        self.workflow_inheritance_rules=[]

    def get_application_or_workspace(self,xyna_app_obj):
        for appwfname in xyna_app_obj.all_workflows:
            if (str(self.workflow_fqn)==str(appwfname)):
                if (len(xyna_app_obj.workspace_name)==0):
                    self.workflow_application=xyna_app_obj.app_name
                    self.workflow_application_version=xyna_app_obj.version_name
                else:
                    self.workflow_workspace=xyna_app_obj.workspace_name
                break

    # If child filter is left empty, the inheritance rule applies to the orderType of the Workflow itself.
    def get_inheritance_rules(self,exec_path):
        location_string=create_location_string(self.workflow_workspace, self.workflow_application, self.workflow_application_version)
        out = execShellCmd("{0} listinheritancerules {1} -orderType \"{2}\" -parameterType MonitoringLevel".format(exec_path, location_string, self.workflow_fqn))
        for lines in out:
            if re.match("No",lines):
                break
            rulematch = re.search("(child filter: \'\\S+\'|own order type), value: \'([A-Za-z0-9.]+)\', precedence: \'([0-9]+)\'",lines)
            if rulematch:
                childmatch = re.match("child filter: \'(\\S+)\'",rulematch.group(1))
                self.workflow_inheritance_rules.append(inheritance_rule(exec_path,rulematch.group(2),rulematch.group(3),childmatch.group(1) if childmatch else ""))

# class to handle script functionality
class XynaWorkflowMonitoringHandler:
    # Regular constructor to read workspaces or applications
    def __init__(self,filearg, config, setinapps):
        self.config = config
        self.setforapps=setinapps
        self.rtc_source_type = "application" if self.setforapps else "workspace"
        self.xyna_exec_path=self.read_factorydir(config.be_path)
        self.workflow_file_path=filearg
        self.workflows_list=[]

    def load(self, all):
        if all:
            print("WARNING: This may take a short while!")
            self.read_all_workflows(config.rtcs)
        else:
            # default branch reads file and only parses workflows given in file
            self.read_workflow_file(config.excluded_workflows)
            self.read_application_or_workspaces_for_workflows(config.rtcs)
        self.read_inheritance_rules_for_workflows()
        print("INFO: Handler loaded for {0}. Found {1} valid Workflows in {2}.".format(self.rtc_source_type, len(handler.workflows_list), "total" if all else "file"))

    def apply(self):
        self.read_new_inheritance_rule(self.config.rule_mon_lvl,self.config.rule_precedence,self.config.rule_filter)
        print("INFO: Got new rule with filter {0}, precedence {1} and level {2}".format(self.new_rule.child_filter,self.new_rule.precedence,self.new_rule.monitoring_level))
        self.print_rules_for_workflows_in_app_or_ws()
        print("WARNING: If inheritance rules were present these may be replaced by the new rule. Please check after installation!")
        self.apply_inheritanceRule()

    def remove(self):
        self.read_new_inheritance_rule(self.config.rule_mon_lvl,self.config.rule_precedence,self.config.rule_filter)
        print("WARNING: All rules with filter {0} will be removed!".format(self.new_rule.child_filter))
        self.remove_inheritanceRule()    

    def execute(self, action):
        actions = {
            "apply": self.apply,
            "remove": self.remove,
            "list": self.print_rules_for_workflows_in_app_or_ws,
            "check": self.print_rules_for_workflows_in_app_or_ws
        }
        toExecute = actions.get(action)
        if toExecute is None:
            print("ERROR: Unknown action: {0}".format(action))
            sys.exit(1)
        self.load(action == "list")
        actions[action]()

    # Reads file, workflow names preceded by '#' or '//' are ignored.
    # Workflows manually excluded in the script header are also ignored.
    def read_workflow_file(self,exclusion_list):
        with open(self.workflow_file_path,'r') as file:
            for line in file:
                parsed = line.strip().strip('\n')
                if len(parsed) > 0 and not self.exclude_workflow_from_file(line,exclusion_list):
                    entry=workflow_tuple(parsed)
                    self.workflows_list.append(entry)
                    #print("DEBUG: found {0} entry in file".format(entry.workflow_fqn))
            if (len(self.workflows_list)==0):
                print("ERROR: No workflows could be read from file")
                sys.exit(1)
        #print("DEBUG: found {0} workflows in file".format(len(self.workflows_list)))
        return

    def exclude_workflow_from_file(self,line,exclusion_list):
        commentmatch=re.match("#|//",line)
        if commentmatch:
            return (True)
        for wf in exclusion_list:
            exclusionmatch=re.match(wf,line)
            if exclusionmatch:
                return(True)
        return (False)

    # Determines xyna execurable path from BE properties file.
    def read_factorydir(self, be_path):
        with open(be_path,'r') as file:
            for line in file:
                match = re.search("^installation.folder",line)
                if match:
                    part = line.split('=')
                    result=part[1].strip().strip('\n')+"/server/xynafactory.sh"
                    #print("DEBUG: exec path found {0}".format(result))
                    return result
        print("ERROR: Could not determine installation path")
        sys.exit(1)

    def read_new_inheritance_rule(self,level,precedence,filter):
        self.new_rule=inheritance_rule(self.xyna_exec_path,level,precedence,filter)

    # Reads application or workspaces for workflows in file.
    # Uses only workspaces unless option for application is set when calling script ("self.setforapps" is True).
    def read_application_or_workspaces_for_workflows(self,apps_ws_list):
        originalsize=len(self.workflows_list)
        for name in apps_ws_list:
            x=xyna_application_workspace(self.xyna_exec_path,name,self.setforapps)
            for wf in self.workflows_list:
                wf.get_application_or_workspace(x)
        self.workflows_list = filter(self.filter_bad_entries,self.workflows_list)
        print("INFO: Skipped {0} problematic workflows".format(originalsize-len(self.workflows_list)))

    def filter_bad_entries(self, tuple):
        if self.setforapps:
            if (len(tuple.workflow_fqn)==0 or len(tuple.workflow_application)==0 or len(tuple.workflow_application_version)==0):
                print("WARNING: Could not find application or version for workflow '{0}', the workflow is ignored.".format(tuple.workflow_fqn))
                return(False)
        else:
            if (len(tuple.workflow_fqn)==0 or len(tuple.workflow_workspace)==0):
                print("WARNING: Could not find workspace for workflow '{0}', the workflow is ignored.".format(tuple.workflow_fqn))
                return(False)
        return(True)

    def read_inheritance_rules_for_workflows(self):
        for tuple in self.workflows_list:
            tuple.get_inheritance_rules(self.xyna_exec_path)

    # for list option: gets all workflows in application or workspace
    def read_all_workflows(self,apps_ws_list):
        for name in apps_ws_list:
            x=xyna_application_workspace(self.xyna_exec_path,name,self.setforapps)
            for wf in x.all_workflows:
                entry=workflow_tuple(str(wf))
                entry.workflow_application=x.app_name
                entry.workflow_application_version=x.version_name
                entry.workflow_workspace=x.workspace_name
                self.workflows_list.append(entry)

    # for list, check and apply option: prints all inheritance rules
    def print_rules_for_workflows_in_app_or_ws(self):
        countrules=0
        for tuple in self.workflows_list:
            if len(tuple.workflow_inheritance_rules)==0:
                continue
            for rule in tuple.workflow_inheritance_rules:
                countrules+=1
                found_rule_warning="WARNING: Found rule for workflow '{0}' in {1} '{2}': filter '{3}', precedence {4}, level {5}"
                print(found_rule_warning.format(tuple.workflow_fqn,self.rtc_source_type,tuple.workflow_application,rule.child_filter,rule.precedence,rule.monitoring_level))
        print("INFO: Found a total of {0} inheritance rules.".format(countrules))

    # remove inheritance rule
    def remove_inheritanceRule(self):
        removerule="{0} removeinheritancerule {1} -orderType {2} -childFilter \"{3}\" -parameterType MonitoringLevel"
        removalcounter=0
        location_string=""
        for tuple in self.workflows_list:
            if len(tuple.workflow_inheritance_rules)>0:
                for rule in tuple.workflow_inheritance_rules:
                    if (str(rule.child_filter)==str(self.new_rule.child_filter)):
                        location_string = create_location_string(tuple.workflow_workspace, tuple.workflow_application, tuple.workflow_application_version)
                        out = execShellCmd(removerule.format(self.xyna_exec_path, location_string, tuple.workflow_fqn, self.new_rule.child_filter))
                        removalcounter+=1
                print("DEBUG: Removed rule in Workflow '{0}'".format(tuple.workflow_fqn))
        print("INFO: Removed rule for filter '{0}' in {1} Workflows. Skipped {2} Workflows.".format(self.new_rule.child_filter,removalcounter,len(self.workflows_list)-removalcounter))

    # apply inheritance rule as defined in script header
    def apply_inheritanceRule(self):
        addrule_cmd="{0} addinheritancerule {1} -orderType \"{2}\" -childFilter \"{3}\" -precedence {4} -parameterType MonitoringLevel -value {5}"
        newRule = "new rule with precedence {0} for level '{1}'".format(self.new_rule.precedence,self.new_rule.monitoring_level)
        superseded_warning = "WARNING: Rule for workflow '{0}' in {1} '{2}' with filter '{3}', precedence {4}, level {5} superseded by " + newRule
        for tuple in self.workflows_list:
            if len(tuple.workflow_inheritance_rules)>0:
                for rule in tuple.workflow_inheritance_rules:
                    if (str(rule.child_filter)==str(self.new_rule.child_filter)):
                        print(superseded_warning.format(tuple.workflow_fqn,self.rtc_source_type,tuple.workflow_application,rule.child_filter,rule.precedence,rule.monitoring_level))
            location_string=create_location_string(tuple.workflow_workspace, tuple.workflow_application, tuple.workflow_application_version)
            out = execShellCmd(addrule_cmd.format(self.xyna_exec_path, location_string, tuple.workflow_fqn, self.new_rule.child_filter, self.new_rule.precedence, self.new_rule.monitoring_level))
            print("DEBUG: Applied new rule to Workflow '{0}'".format(tuple.workflow_fqn))
        print("INFO: Applied new rule to {0} Workflows".format(len(self.workflows_list)))


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="Script to install inheritance rules for workflows from file",formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument("-a", "--action", action="store", nargs=1, required=True, help="Must be supplied. Action to be performed. Valid values are: apply, remove, list, check, create_config")
    parser.add_argument("-c", "--config", action="store", nargs=1, required=True, help="Must be supplied. Absolute path to file containing configuration. Syntax: -o=<filepath>")
    parser.add_argument("-f", "--file", action="store", nargs=1, required=True, help="Must be supplied. Absolute path to file containing workflow paths. Syntax: -f=<filepath>")
    parser.add_argument("-p", "--application", action='store_true', help="Supply this option to use applications instead of workspaces")
    args = parser.parse_args()

    if args.action[0] == "create_config":
        Config.createConfig(args.config[0])
        print("Created conifg file at {0}.".format(args.config[0]))
        exit(0)

    config = Config(args.config[0])
    print("INFO: {0}".format(config))
    handler = XynaWorkflowMonitoringHandler(args.file[0], config, args.application)
    print("INFO: {0} option".format(args.action[0]))
    handler.execute(args.action[0])
    print("SUCCESS")  
    sys.exit(0)