package net.rk4z.fabricord.utils

import net.rk4z.s1.swiftbase.fabric.FabricMessageKey

@Suppress("MISSING_DEPENDENCY_SUPERCLASS_IN_TYPE_ARGUMENT", "ClassName", "unused")
open class System : FabricMessageKey {
    open class Log : System() {
        object LOADING : Log()
        object ENABLING : Log()
        object DISABLING : Log()
        object INITIALIZED : Log()

        open class MissingRequiredParam : Log() {
            object ITEM_0 : MissingRequiredParam()
            object ITEM_1 : MissingRequiredParam()
            object ITEM_2 : MissingRequiredParam()
        }

        object STILLSTARTINGUP : Log()
        object FAILEDSTART : Log()
        object FAILEDSTOP : Log()

        object CHECKING_UPDATE : Log()
        object ALL_VERSION_COUNT : Log()
        object NEW_VERSION_COUNT : Log()
        object VIEW_LATEST_VER : Log()
        object LATEST_VERSION_FOUND : Log()
        object YOU_ARE_USING_LATEST : Log()
        object FAILED_TO_CHECK_UPDATE : Log()
        object ERROR_WHILE_CHECKING_UPDATE : Log()

        open class Other : Log() {
            object UNKNOWN : Other()
            object UNKNOWN_ERROR : Other()
            object ERROR : Other()
        }
    }
}