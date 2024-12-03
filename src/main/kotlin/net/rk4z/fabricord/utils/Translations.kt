@file:Suppress("MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT", "ClassName", "unused")

package net.rk4z.fabricord.utils

import net.rk4z.s1.swiftbase.fabric.FabricMessageKey

open class System : FabricMessageKey {
    open class Log : System() {
        object LOADING : Log()
        object ENABLING : Log()
        object DISABLING : Log()
        object INITIALIZED : Log()
        object NOT_INITIALIZED : Log()

        open class MissingRequiredParam : Log() {
            object ITEM_0 : MissingRequiredParam()
            object ITEM_1 : MissingRequiredParam()
            object ITEM_2 : MissingRequiredParam()
        }

        object STILL_STARTING_UP : Log()
        object FAILED_TO_START : Log()
        object FAILED_TO_STOP : Log()

        object FAILED_TO_LOGIN : Log()
        object FAILED_TO_START_BOT : Log()
        object WEBHOOK_NOT_CONFIGURED : Log()
        object BOT_ONLINE : Log()
        object BOT_OFFLINE : Log()
        object BOT_NOT_INITIALIZED : Log()
        object GUILD_NOT_FOUND : Log()

        object CHECKING_UPDATE : Log()
        object ALL_VERSION_COUNT : Log()
        object NEW_VERSION_COUNT : Log()
        object VIEW_LATEST_VER : Log()
        object LATEST_VERSION_FOUND : Log()
        object YOU_ARE_USING_LATEST : Log()
        object FAILED_TO_CHECK_UPDATE : Log()
        object ERROR_WHILE_CHECKING_UPDATE : Log()

        open class Other : Log() {
            object DATE_PARSE_ERROR : Other()
            object UNKNOWN : Other()
            object UNKNOWN_ERROR : Other()
            object ERROR : Other()
        }
    }

    open class Command : System() {
        open class Online_Players : Command() {
            object TITLE : Online_Players()
            object DESCRIPTION : Online_Players()
            object NO_PLAYER : Online_Players()
            object CANT_GET_PLAYER_LIST : Online_Players()
        }

        open class Ban : Command() {
            object CANT_GET_SERVER : Ban()
            object SUCCESS : Ban()
        }

        object NO_PLAYER_FOUND : Command()
    }
}

open class Main : FabricMessageKey {
    object NOT_LINKED : Main()
}