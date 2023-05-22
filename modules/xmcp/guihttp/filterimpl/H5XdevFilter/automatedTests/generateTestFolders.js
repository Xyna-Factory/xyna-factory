/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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
 */
const fs = require('fs');
const args = process.argv.slice(2);

if(args.length < 3) {
    throw 'Usage: node ./generateTestFolders.js [start-index] [end-index] [name] [?root-path]'
}

const startIndex = args[0];
const endIndex = args[1];
const testSeriesName = args[2];
const rootPath = args[3] || './testcases/raygression';
const indexCharLength = endIndex >= 100 ? 4 : 3;
const fillWithZeros = (x, length) => ['0'.repeat(length - x.toString().length), ...x.toString().split('')].join('');

fs.mkdirSync(`${rootPath}/${testSeriesName}`);
const testseries = {testseries: []};
for (let i = startIndex; i <= endIndex; i++) {
    const index = i;
    const seriesContent = JSON.stringify({ tests: [`${testSeriesName}_${fillWithZeros(index, indexCharLength)}.json`] });
    fs.mkdirSync(`${rootPath}/${testSeriesName}/${testSeriesName}_${fillWithZeros(index, indexCharLength)}`);
    testseries.testseries.push(`${testSeriesName}_${fillWithZeros(index, indexCharLength)}/testseries.json`)
    fs.writeFileSync(`${rootPath}/${testSeriesName}/${testSeriesName}_${fillWithZeros(index, indexCharLength)}/testseries.json`, seriesContent);
    fs.writeFileSync(`${rootPath}/${testSeriesName}/${testSeriesName}_${fillWithZeros(index, indexCharLength)}/${testSeriesName}_${fillWithZeros(index, indexCharLength)}.json`, '');
}

fs.writeFileSync(`${rootPath}/${testSeriesName}/${testSeriesName}.json`, JSON.stringify(testseries));
