/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
const args = process.argv.slice(2);

if (args.length < 3) {
    throw 'Usage: node ./generateTestFolders.js [start-index] [end-index] [name] [?root-path]';
}

const startIndex = Number(args[0]);
const endIndex = Number(args[1]);
const testSeriesName = args[2];
const rootPath = args[3] || './testcases/raygression';
const indexCharLength = endIndex >= 100 ? 4 : 3;
const fillWithZeros = (x, length) => ['0'.repeat(length - x.toString().length), ...x.toString().split('')].join('');
const spawn = require('child_process').spawn;
const fs = require('fs');
const tests = [];
for (let i = startIndex; i <= endIndex; i++) {
    const index = i;
    const caseName = `Raygression: ${testSeriesName}_${fillWithZeros(index, indexCharLength)}`;
    const testFile = `testcases/raygression/${testSeriesName}/${testSeriesName}_${fillWithZeros(index, indexCharLength)}/testseries.json`;

    const template = JSON.parse(fs.readFileSync('./generate_test_data_template.json', {encoding: 'utf-8'}));
    const runTemplate = JSON.parse(fs.readFileSync('./generate_test_data_template.json', {encoding: 'utf-8'}));
    // console.log(template['operations'][0]['data']['!Raygression:_exceptions_001!']);
    template['operations'][0]['data'][6]['!Raygression:_exceptions_001!'] = caseName;
    template['operations'][0]['data'][3]['!testcases/raygression/exceptions/exceptions_001/!'] = testFile;
    runTemplate['operations'][0]['data'][2]['!RAYGRESSIONTESTNAME!'] = caseName;
    tests.push(`./tmp/tmp_${index}.json`);
    // tests.push(`./tmp/tmp_run_${index}.json`);
    fs.writeFileSync(`./tmp/tmp_${index}.json`, JSON.stringify(template));
    // fs.writeFileSync(`./tmp/tmp_run_${index}.json`, JSON.stringify(runTemplate));
}

fs.writeFileSync('./testseries.json', JSON.stringify({tests}))
const pythonProcess = spawn('python', ['../../autoTester.py', './testseries.json']);
pythonProcess.on('exit', () => {
    console.log(`Generated ${tests.length} tests`);
    tests.forEach(test => {
        fs.unlinkSync(test)
    })
    fs.unlinkSync('./testseries.json')
})
