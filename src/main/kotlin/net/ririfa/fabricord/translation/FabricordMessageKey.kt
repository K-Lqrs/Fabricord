package net.ririfa.fabricord.translation

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

sealed class FabricordMessageKey : MessageKey<FabricordMessageProvider, Text> {
	sealed class System : FabricordMessageKey() {
		sealed class Initialization : System() {
			sealed class DirectoriesAndFiles : Initialization() {
				object ModDirDoesNotExist : DirectoriesAndFiles()
				object ConfigFileDoesNotExist : DirectoriesAndFiles()
			}
		}
	}

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
		sealed class Group : Command() {

		}

		sealed class Help : Command() {
			sealed class Group : Help() {
				// /grp create <name|str> <open|bool> [players|ServerPlayerEntity]
				sealed class Create : Group() {
					object About : Create()
					object Usage : Create()
				}

				// /grp delete <name or ID|str>
				sealed class Delete : Group() {
					object About : Delete()
					object Usage : Delete()
				}

				// /grp join <name or ID|str>
				sealed class Join : Group() {
					object About : Join()
					object Usage : Join()
				}

				// /grp leave <name or ID|str>
				sealed class Leave : Group() {
					object About : Leave()
					object Usage : Leave()
				}

				// /grp list
				sealed class List : Group() {
					object About : List()
					object Usage : List()
				}

				// /grp joinedList
				sealed class JoinedList : Group() {
					object About : JoinedList()
					object Usage : JoinedList()
				}

				// /grp pendingList <[OPTIONAL] name or ID|str>
				sealed class PendingList : Group() {
					object About : PendingList()
					object Usage : PendingList()
				}

				// /grp info <name or ID|str>
				sealed class Info : Group() {
					object About : Info()
					object Usage : Info()
				}

				// /grp setDefault <name or ID|str>
				sealed class SetDefaultGrp : Group() {
					object About : SetDefaultGrp()
					object Usage : SetDefaultGrp()
				}

				// /grp switch <[OPTIONAL] name or ID|str>
				sealed class Switch : Group() {
					object About : Switch()
					object Usage : Switch()
				}

				// /grp invite <name or ID|str> <player|ServerPlayerEntity>
				sealed class Invite : Group() {
					object About : Invite()
					object Usage : Invite()
				}

				// /grp kick <name or ID|str> <player|ServerPlayerEntity>
				sealed class Kick : Group() {
					object About : Kick()
					object Usage : Kick()
				}

				// /grp /grp acceptrequest <pendingID|str>
				sealed class AcceptRequest : Group() {
					object About : AcceptRequest()
					object Usage : AcceptRequest()
				}

				// /grp /grp denyrequest <pendingID|str>
				sealed class DenyRequest : Group() {
					object About : DenyRequest()
					object Usage : DenyRequest()
				}

				// /grp setPublic <name or ID|str> <public|bool>
				sealed class SetPublic : Group() {
					object About : SetPublic()
					object Usage : SetPublic()
				}

				// /grp transferOwner <name or ID|str> <MCID or UUID|str>
				sealed class TransferOwner : Group() {
					object About : TransferOwner()
					object Usage : TransferOwner()
				}
			}
		}
	}
}