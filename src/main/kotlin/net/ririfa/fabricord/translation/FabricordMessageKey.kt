package net.ririfa.fabricord.translation

import net.minecraft.text.Text
import net.ririfa.langman.MessageKey

sealed class FabricordMessageKey : MessageKey<FabricordMessageProvider, Text> {
	sealed class Discord : FabricordMessageKey() {
		sealed class Config : Discord() {
			object LogChannelIDIsBlank : Config()
		}
	}
}