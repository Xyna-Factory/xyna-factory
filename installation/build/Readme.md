## Requirements
* java (11)
* mvn (3.6.3)
* ant (1.10.12)
* nvm
* zip

## Build
`> ./build.sh all`

## Result
Creates a delivery bundle containing the xyna-factory (including GUI) and prerequisites for initial installation.

```
xyna-factory
  - installation # this folder
  - *release # created by calling ./build.sh all
    - XynaFactory_v<Version>_<Date>_bundle.zip # e.g. XynaFactory_v9.0.2.0_230601_0800_bundle
      - XynaFactory_v<Version>_<Date>.zip # xyna factory, including gui
      - XBE_Prerequisites_<Version>_<Date>.zip # xyna prerequisites, required for initial installation
```

## Additional Build Options
Instead of calling `./build.sh all`, you can pass other options to create parts of the build. What options are available is printed by calling `./build.sh` without a parameter.