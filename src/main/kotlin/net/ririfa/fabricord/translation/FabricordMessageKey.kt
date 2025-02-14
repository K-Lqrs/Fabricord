package net.ririfa.fabricord.translation

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

sealed class FabricordMessageKey : MessageKey<FabricordMessageProvider, Text> {
	sealed class Discord : FabricordMessageKey() {
		sealed class Config : Discord() {
			object LogChannelIDIsBlank : Config()
		}
	}

	sealed class Exception : FabricordMessageKey() {
		sealed class Config : Exception() {
			object WebHookUrlIsNotConfiguredOrInvalid : Config()
		}
	}

	sealed class Command : FabricordMessageKey() {
		sealed class Help : Command() {
			sealed class Group : Help() {
				// /grp create <name|str> <open|bool> [players|ServerPlayerEntity]
				object Create : Group()

				// /grp delete <name or ID|str>
				object Delete : Group()

				// /grp join <name or ID|str>
				object Join : Group()

				// /grp leave <name or ID|str>
				object Leave : Group()

				// /grp list
				object List : Group()

				// /grp pendingList <[OPTIONAL] name or ID|str>
				object PendingList : Group()

				// /grp info <name or ID|str>
				object Info : Group()

				// /grp setDefault <name or ID|str>
				object SetDefaultGrp : Group()

				// /grp switch <[OPTIONAL] name or ID|str>
				object Switch : Group()
			}
		}
	}
}