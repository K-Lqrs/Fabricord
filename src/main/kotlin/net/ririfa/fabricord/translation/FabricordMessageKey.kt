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

		object ServerStart : System()
		object ServerStop : System()
		object PlayerJoin : System()
		object PlayerLeave : System()
	}
}