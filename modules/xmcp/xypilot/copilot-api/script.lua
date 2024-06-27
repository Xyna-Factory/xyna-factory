-- This script is purely for testing purposes with a local neovim installation, including packer and the copilot lua plugin.
-- It is used by the python tests

--- This is not needed in the actual script as setup is done in init.lua
require("plugins")
require("copilot").setup()
---

vim.cmd [[luafile ./copilot/script.lua]]
