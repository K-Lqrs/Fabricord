# Fabricord
A modern message style like DiscordSRV will be reproduced as a Fabric version mod.

![Discord](https://img.shields.io/discord/1177249059623411742?logo=discord&logoColor=white&style=plastic)  ![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/Elysium-7/Fabricord/build.yml?style=plastic&logo=github&logoColor=white) ![GitHub release (with filter)](https://img.shields.io/github/v/release/Elysium-7/Fabricord?style=plastic) ![GitHub commit activity (branch)](https://img.shields.io/github/commit-activity/t/Elysium-7/Fabricord?style=plastic)


## Overview
Fabricord is a mod for Fabric servers that bridges the gap between Minecraft and Discord. It enables seamless integration, allowing messages from Discord to be read directly within the Minecraft server. This mod enhances the communication experience, making it more interactive and engaging.

## Features
- Mutual transfer of Discord chat,Minecraft chat.
- Modern and visually appealing message presentation.
- Easy referencing of Discord user IDs within Minecraft.

## Installation
It is very easy to set up!
The data folder and configuration file will be generated upon initial startup. After that, the server is stopped,
make sure that the "fabricord" folder is in the server root directory. It contains the configuration file (config.yml).

<config.yml>
```yml
# Thank you for introducing the mod.
# You can bridge Discord and Minecraft chat!

# Click here for various links↓.
# GitHub -> https://github.com/Elysium-7/Fabricord
# Discord -> https://discord.gg/s6wzAHJKA3

# BotToken for use with Discord.
# To get a BotToken -> https://discord.com/developers/applications
BotToken: ""

# Channel for sending Minecraft logs.
Log_Channel_ID: ""

# The bot's online status can be changed.
# "ONLINE", "IDLE" and "DO_NOT_DISTURB" are available.
# If blank, "ONLINE" is automatically select.
Bot_Online_Status: ""

```

## License
Fabricord is available under the [Apache 2.0 License](https://github.com/KT-Ruxy/Fabricord/blob/main/LICENSE).
