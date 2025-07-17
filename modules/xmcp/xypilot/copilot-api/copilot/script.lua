--[[
 * - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 ]]--


local copilot = {
    api = require("copilot.api"),
    util = require("copilot.util"),
    client = require("copilot.client"),
    command = require("copilot.command"),
    suggestion = require("copilot.suggestion"),
    panel = require("copilot.panel"),
}

local json = require("json")

local api = {
    result = {
        -- array of copilot_panel_solution_data
        num_solutions = 0,
        solutions = {}
    },

    bufnr = vim.api.nvim_get_current_buf()
}

function api.enable_on_buffer()
    vim.bo[api.bufnr].buflisted = true
end

function api.trigger_copilot()
    copilot.suggestion.toggle_auto_trigger()
    -- insert & remove some character to trigger copilot
    vim.cmd('startinsert')
    vim.cmd [[normal i ]]
    vim.cmd [[normal x]]
end

function api.get_panel_completion(line, col)
    vim.cmd('stopinsert')

    local panelId = "copilot"
    local params = copilot.util.get_doc_params({panelId = panelId})

    -- get utf16 index of the cursor (line, col)
    -- local _, utf16_index = vim.str_utfindex(vim.api.nvim_get_current_line(), col - 1)
    -- params.doc.position.character = utf16_index
    -- params.position.character = params.doc.position.character

    copilot.panel.handlers.register_panel_handlers(panelId, {
        ---@param result copilot_panel_solution_data
        on_solution = function(result)
            api.result.num_solutions = api.result.num_solutions + 1
            api.result.solutions[api.result.num_solutions] = result
        end,
        ---@param result copilot_panel_solutions_done_data
        on_solutions_done = function(result)
            if result.status == "OK" and api.result.num_solutions == 0 then
                print("no completions found")
            elseif result.message then
                print(string.format("done...%s: %s", result.status, result.message))
            else
                print(string.format("done...%s", result.status))
            end


            if result.status == "OK" and api.result.num_solutions > 0 then
                -- get buffer name
                local bufname = vim.api.nvim_buf_get_name(api.bufnr)
                -- get file directory, without filename
                local filedir = vim.fn.fnamemodify(bufname, ":h")
                -- get file name without extension
                local filename = vim.fn.fnamemodify(bufname, ":t:r")

                -- create a json file with the result in solutions folder
                local json_result = json.encode(api.result)
                local json_file = io.open(string.format("%s/../solutions/%s.json", filedir, filename), "w")
                json_file:write(json_result)
                json_file:close()
            end

            vim.cmd [[q!]]
            os.exit()
        end
    })

    local _, id = copilot.api.get_panel_completions(
        copilot.client.get(),
        params,
        ---@param result copilot_get_panel_completions_data
        function(err, result)
            if err then
                print(string.format("%s", err))
            elseif result.solutionCountTarget == 0 then
                print("no completions found...")
            else
                print(string.format("requesting %d completions...", result.solutionCountTarget))
            end
        end
    )
end


-- get index of active line
local line = vim.fn.line(".")
-- get index of the active column
local col = vim.fn.col(".")

-- use schedule to run after lsp_start (see copilot.client.setup())
vim.schedule(
    function()
        print("Copilot: waiting for client...")
        copilot.client.use_client(function()
            print("ready...")
            api.enable_on_buffer()
            api.trigger_copilot()
            print(string.format("cursor at line %d, col %d...", line, col))
            api.get_panel_completion(line, col)
        end)
    end
)

return api
