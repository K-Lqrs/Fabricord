package net.ririfa.fabricord.translation

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

sealed class FabricordMessageKey : MessageKey<FabricordMessageProvider, Text> {
	sealed class System : FabricordMessageKey() {
		object Initializing : System()

		sealed class MissingRequiredProp : System() {
			object ITEM1 : MissingRequiredProp()
			object ITEM2 : MissingRequiredProp()
			object ITEM3 : MissingRequiredProp()
		}

		object Initialized : System()

		object CreatingConfigDir : System()
		object FailedToCreateConfigDirBySec : System()
		object FailedToCreateConfigDirByIO : System()
		object FailedToCreateConfigDirByUnknown : System()

		object LoadingConfig : System()
		object ConfigFileNotFound : System()
		object FailedToLoadConfigByIO : System()
		object FailedToLoadConfigByUnknown : System()

		object SwitchedLocalChatState : System()

		sealed class GRP : System() {
			object LoadingGroupFile : GRP()
			object  GroupFileSaved : GRP()

			object GroupCreated : GRP()
			object GroupCreatedWithMembers : GRP()

			object NoGroupFoundWithID : GRP()
			object NoGroupFoundWithName : GRP()
			object LoadedGroups : GRP()

			object MultipleGroupsFound : GRP()

			object PlayerNotFound : GRP()
			object GroupFileNotFound : GRP()

			object FailedToLoadGroupFile : GRP()
			object FailedToSaveGroupFile : GRP()
		}

		sealed class Discord : System() {
			object BotNowOnline : Discord()
			object BotNowOffline : Discord()

			object BotNotInitialized : Discord()

			object FailedToStartBot : Discord()
			object FailedToStopBot : Discord()

			object WebHookUrlNotConfigured : Discord()

			object FailedToStartBotByLoginExc : Discord()
			object FailedToStartBotByUnknown : Discord()

			object DiscordBotIsNotInitialized : Discord()

			object CantGetPlayerList : Discord()

			object MinecraftServerNotInitialized : Discord()

			object CantProcessMessage : Discord()
			object CantProcessCommand : Discord()

			object WebHookUrlNotConfiguredOrBlank : Discord()
			object ErrorDuringWebHookSend : Discord()
		}
	}
}