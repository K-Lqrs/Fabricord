package net.ririfa.fabricord.translation

import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.text.Text
import net.ririfa.langman.IMessageProvider
import net.ririfa.langman.LangMan
import net.ririfa.langman.MessageKey
import kotlin.reflect.full.isSubclassOf

// Based https://github.com/SwiftStorm-Studio/SwiftBase/blob/main/integrations/fabric/src/main/kotlin/net/rk4z/s1/swiftbase/fabric/FabricPlayer.kt
class FabricordMessageProvider(val player: ServerPlayerEntity) : IMessageProvider<Text> {
	private val languageManager: LangMan<FabricordMessageProvider, Text>
		get() {
			if (!LangMan.isInitialized()) {
				throw IllegalStateException("LangMan is not initialized but you are trying to use it.")
			}
			val languageManager = LangMan.Companion.getOrNull<FabricordMessageProvider, Text>()
				?: throw IllegalStateException("LangMan is not initialized but you are trying to use it.")

			return languageManager
		}

	override fun getLanguage(): String {
		// https://maven.fabricmc.net/docs/yarn-1.21.4+build.8/net/minecraft/network/packet/c2s/common/SyncedClientOptions.html
		// en_US -> en
		return player.clientOptions.comp_1951.split("_")[0]
	}

	override fun getMessage(key: MessageKey<*, *>, vararg args: Any): Text {
		val messages = languageManager.messages
		val expectedMKType = languageManager.expectedMKType
		val textComponentFactory = languageManager.textComponentFactory

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		val message = messages[lang]?.get(key)
		val text = message?.let { String.format(it, *args) } ?: key.rc()
		return textComponentFactory(text)
	}

	override fun getRawMessage(key: MessageKey<*, *>): String {
		val messages = languageManager.messages
		val expectedMKType = languageManager.expectedMKType

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		return messages[lang]?.get(key) ?: key.rc()
	}

	override fun hasMessage(key: MessageKey<*, *>): Boolean {
		val messages = languageManager.messages
		val expectedMKType = languageManager.expectedMKType

		require(key::class.isSubclassOf(expectedMKType)) { "Unexpected MessageKey type: ${key::class}. Expected: $expectedMKType" }
		val lang = this.getLanguage()
		return messages[lang]?.containsKey(key) ?: false
	}
}

fun ServerPlayerEntity.adapt(): FabricordMessageProvider {
	return FabricordMessageProvider(this)
}

fun FabricordMessageProvider.getAPlayer(): ServerPlayerEntity {
	return this.player
}