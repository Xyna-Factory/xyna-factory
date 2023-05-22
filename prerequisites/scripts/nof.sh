#!/bin/bash

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


# erstellt datei mit xyna debug informationen (threaddump, etc)
# und loescht alte dateien

# target directory sollte man so waehlen, dass in der partition
# genug platz für die dateien ist. die dateien koennen je nach
# anzahl threads in xyna bis zum mehrere megabyte groß werden.
# (ein einzelner thread verbraucht ca 1kb)
# idealerweise liegt das verzeichnis nicht in einer partition, in
# der es negative auswirkungen auf andere komponenten hat, wenn die
# partition voll laeuft (z.B. xyna)
#TARGET_DIRECTORY=$(dirname "$0")
TARGET_DIRECTORY=/var/log/xyna/xyna_001

# xyna server verzeichnis, welches xynafactory.sh enthaelt
XYNAFACTORY_HOME=$(dirname "$0")/../server

# wie lange werden die erzeugten files aufgehoben
DAYS_KEEP=2

# create file:
filename="$TARGET_DIRECTORY/nof/nof`date +"%Y%m%d%H%M%S_%N"`.txt"

# append date
date +"%Y%m%d%H%M%S_%N" >>$filename

# count of all open files (excludes sockets and pipes)
echo "===lsof `/usr/sbin/lsof -u xyna | grep "/" |sort -k9 -u | wc -l`" >>$filename

# append threaddump
echo "===listthreadinfo" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listthreadinfo >>$filename

# append threadpoolinfo
echo "===listthreadpoolinfo" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listthreadpoolinfo >>$filename

# append schedulerinfo
echo "===listschedulerinfo" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listschedulerinfo >>$filename

# append systeminfo
echo "===listsysteminfo" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listsysteminfo >>$filename

# append connectionpoolinfo
echo "===listconnectionpools" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listconnectionpools >>$filename

# append connectionpoolstatistics
echo "===listconnectionpoolstatistics" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listconnectionpoolstatistics -t >>$filename

# append trigger maxEvent config
echo "===listconfigtriggermaxevents" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listconfigtriggermaxevents >>$filename

# append capacity information
echo "===listcapacities" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listcapacities >>$filename

# append veto information
echo "===listvetos" >>$filename
$XYNAFACTORY_HOME/xynafactory.sh listvetos >>$filename

# append date
date +"%Y%m%d%H%M%S_%N" >>$filename

# cleanup of old files
find "$TARGET_DIRECTORY"/nof/ -mtime +$DAYS_KEEP -type f -delete
