# Fabricord
A modern message style like DiscordSRV will be reproduced as a Fabric version mod.

![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/Elysium-7/Fabricord/build.yml?style=plastic&logo=github&logoColor=white) ![GitHub release (with filter)](https://img.shields.io/github/v/release/Elysium-7/Fabricord?style=plastic) ![GitHub commit activity (branch)](https://img.shields.io/github/commit-activity/t/Elysium-7/Fabricord?style=plastic)


## Overview
Fabricord is a mod for Fabric servers that bridges the gap between Minecraft and Discord. It enables seamless integration, allowing messages from Discord to be read directly within the Minecraft server. This mod enhances the communication experience, making it more interactive and engaging.

## Features
- Read each other's chat messages between Fabric servers and Discord channels.
- Modern, visually active message style.
- Easy user mentions system from within Minecraft using role IDs and user IDs.
- [Alpha] Synchronize console to Discord channel and control Minecraft server from Discord.

## Installation
It is very easy to set up!
The data folder and configuration file will be generated upon initial startup. After that, the server is stopped,
make sure that the "fabricord" folder is in the server root directory. It contains the configuration file (config.yml).

<config.yml>
```yml
# Thank you for introducing the mod.
# You can bridge Discord and Minecraft chat!

# Click here for various links↓.
# GitHub -> https://github.com/KT-Ruxy/Fabricord
# modrinth -> https://modrinth.com/mod/fabricord



# Do not change this.
Config_Version: 1.0



# BotToken for use with Discord.
# To get a BotToken -> https://discord.com/developers/applications
BotToken: ""


# Channel for sending Minecraft logs.
#if Blank, the mod will not send Minecraft Server General Chat to Discord.
# To get a Channel ID -> https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID
Log_Channel_ID: ""


# The bot's online status can be changed.
# "ONLINE", "IDLE" and "DO_NOT_DISTURB" are available.
# If blank, "ONLINE" is automatically select.
Bot_Online_Status: ""


# The Log Style can be changed.
# "classic", "modern" are available.
# If blank, "classic" is automatically select.
Message_Style: ""


# If Message_Style is "modern", you need to set the Webhook URL.
# To get a Webhook URL -> https://support.discord.com/hc/en-us/articles/228383668-Intro-to-Webhooks
Webhook_URL: ""



# >----- Advanced Settings -----< #



# If you want to use the console bridge, set it to true.
Enable_Console_Bridge: false


# If you want to use the console bridge, set the channel ID.
Console_Log_Channel_ID: ""


# The bot's activity can be changed.
# "PLAYING", "STREAMING", "LISTENING" and "WATCHING" are available.
# If blank, "PLAYING" is automatically select.
Bot_Activity_Status: ""


# The bot's activity text can be changed.
# If blank, "Minecraft" is automatically select.
Bot_Activity_Message: ""



# >-----------------------------< #
```

## License
Fabricord is available under the [Apache 2.0 License](https://github.com/KT-Ruxy/Fabricord/blob/main/LICENSE).
