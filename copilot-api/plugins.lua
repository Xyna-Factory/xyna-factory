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