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


-- This script is purely for testing purposes with a local neovim installation, including packer and the copilot lua/nvim plugin.
-- It is used by the python tests


vim.cmd [[packadd copilot.lua]]
-- vim.cmd [[packadd copilot.vim]]

return require('packer').startup(function(use)
    -- Packer can manage itself
    use 'wbthomason/packer.nvim'

    use 'zbirenbaum/copilot.lua'

    -- use {
    --     "github/copilot.vim",
    --     cmd = "Copilot",
    -- }
end)
