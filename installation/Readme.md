## Requirements
* java version 11
* mvn version 3.9
* ant version 1.10
* lib maven-resolver-ant-tasks version 1.5
* lib ant-contrib-tasks version 1.0
* nvm
* zip

### libs

The libs maven-resolver-ant-tasks and ant-contrib-tasks are required and must be located in the directory "${HOME}/.ant/lib".
The used versions are defined in the file build.conf und these libs can be installed by the following command:

```
cd installation
./build.sh install_libs
```


## Build
`> ./build.sh all`

## Result
Creates a delivery bundle containing the xyna-factory (including GUI) and prerequisites for initial installation.

```
xyna-factory
  - installation # this folder
  - *XynaFactory_v<Version>_<Date>_bundle.zip # e.g. XynaFactory_v9.0.2.0_230601_0800_bundle
    - XynaFactory_v<Version>_<Date>.zip # xyna factory, including gui
    - XBE_Prerequisites_<Version>_<Date>.zip # xyna prerequisites, required for initial installation
```

## Additional Build Options
Instead of calling `./build.sh all`, you can pass other options to create parts of the build. What options are available is printed by calling `./build.sh` without a parameter.
