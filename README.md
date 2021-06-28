# OfflineCommands [![GitHub](https://img.shields.io/github/license/Jochyoua/OfflineCommands?style=plastic)](https://github.com/Jochyoua/OfflineCommands/blob/main/LICENSE) [![GitHub last commit](https://img.shields.io/github/last-commit/Jochyoua/OfflineCommands?style=plastic)](https://github.com/Jochyoua/OfflineCommands/commits/) [![Github Release](https://img.shields.io/github/v/release/Jochyoua/OfflineCommands?style=plastic)](https://github.com/Jochyoua/OfflineCommands/releases/latest)

OfflineCommands is a plugin built to run commands for specific players when they connect.\
This plugin can also run the command if they're online as well.\
All contributions are very much appreciated! \
[SpigotMC Resource](https://www.spigotmc.org/resources/offlinecommands.93671/ "SpigotMC Resource")
## Commands

OfflineCommands has basic commands for you to use.\
All commands below require the permission `offlinecommands.use`

| Command | Description |
| --- | --- |
| `offlinecommands list` | Lists all current users and their commands |
| `offlinecommands help` | Show all available commands |
| `offlinecommands add` | Adds a command for the specified player |
| `offlinecommands remove` | Removes a command from a user |

### Command examples

* Adding
    * `offlinecommands add user="USERNAME/UUID" command="COMMAND"`
    * `offlinecommands add user="USERNAME/UUID" command="COMMAND" executor="player" no-feedback`\
      *no-feedback makes it so that the sender does not recieve any mesages*\
      *executor="player" is optional and if it is anything other than CONSOLE it executes it as a player*
* Removing
    * `offlinecommands remove 069a79f4-44e9-4726-a5be-fca90e38aaf5 73e389a5`

***
This guide finishes here, thank you for reading!