package net.ririfa.fabricord.translation

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

open class FabricordMessageKey : MessageKey<FabricordMessageProvider, Text> {
	open class System : FabricordMessageKey() {
		object Initializing : System()

		open class MissingRequiredProp : System() {
			object ITEM1 : MissingRequiredProp()
			object ITEM2 : MissingRequiredProp()
			object ITEM3 : MissingRequiredProp()
		}

		object Initialized : System()

		object BotNotInitialized : System()

		object FailedToStartBot : System()
		object FailedToStopBot : System()

		object CreatingConfigDir : System()
		object FailedToCreateConfigDirBySec : System()
		object FailedToCreateConfigDirByIO : System()
		object FailedToCreateConfigDirByUnknown : System()

		object LoadingConfig : System()
		object ConfigFileNotFound : System()
		object FailedToLoadConfigByIO : System()
		object FailedToLoadConfigByUnknown : System()

		open class Discord {
			object BotNowOnline : System()
			object BotNowOffline : System()

			object WebHookUrlNotConfigured : System()

			object FailedToStartBotByLoginExc : System()
			object FailedToStartBotByUnknown : System()

			object DiscordBotIsNotInitialized : System()
		}
	}
}