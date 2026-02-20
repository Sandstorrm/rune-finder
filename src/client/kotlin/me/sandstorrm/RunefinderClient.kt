package me.sandstorrm

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import org.slf4j.LoggerFactory
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos



object RunefinderClient : ClientModInitializer {

	const val MODID = "rune-finder"

	lateinit var CONFIG: ModConfig
		private set

	private val logger = LoggerFactory.getLogger(MODID)
	private var previousScreen: Screen? = null
	public var lookingFor: String? = null

	object ModState {
		var enabled = false
		fun toggle() {
			enabled = !enabled
		}
	}

	override fun onInitializeClient() {
		// This entrypoint is suitable for setting up client-specific logic, such as rendering.
		log("Im on daddy!")
		ClientTickEvents.END_CLIENT_TICK.register(::onClientTick)
		ClientTickEvents.END_CLIENT_TICK.register(ClientTickEvents.EndTick { TaskScheduler.tick() })
		ClientCommandRegistrationCallback.EVENT.register(RFCommand())


		// register the config system
		AutoConfig.register(
			ModConfig::class.java,
			::JanksonConfigSerializer
		)

		// load the config
		CONFIG = AutoConfig.getConfigHolder(ModConfig::class.java).config

	}

	fun log(log_text: String){
		logger.info(log_text)
	}

	fun msg(msg_text: String){
		MinecraftClient.getInstance().player?.sendMessage(Text.of(msg_text))
	}

	private fun isLookingAtContainerInsideArray(client: MinecraftClient): Boolean {

		if (!CONFIG.restrictToArrays) {
			return true
		}

		val hit = client.crosshairTarget as? BlockHitResult ?: return false

		val pos = hit.blockPos

		for (array in CONFIG.arrays.values) {

			val minX = minOf(array.ax, array.bx)
			val maxX = maxOf(array.ax, array.bx)

			val minY = minOf(array.ay, array.by)
			val maxY = maxOf(array.ay, array.by)

			val minZ = minOf(array.az, array.bz)
			val maxZ = maxOf(array.az, array.bz)

			if (
				pos.x in minX..maxX &&
				pos.y in minY..maxY &&
				pos.z in minZ..maxZ
			) {
				return true
			}
		}

		return false
	}


	fun onClientTick(client: MinecraftClient) {
		if (ModState.enabled == false){
			return
		}

		val currentScreen = client.currentScreen

		if (currentScreen != null && currentScreen != previousScreen) {
			if (currentScreen is GenericContainerScreen) {
				log("Chest GUI opened!")

				if (!isLookingAtContainerInsideArray(client)) {
					log("Chest is outside arrays — ignoring")
					previousScreen = currentScreen
					return
				}

				// Access the ScreenHandler from the GUI
				val screenHandler = currentScreen.screenHandler
				val slotsToDrop = mutableListOf<Int>()

				// Get all slots and their contents
				for (i in 0 until screenHandler.slots.size) {
					val stack = screenHandler.getSlot(i).stack

					if (!stack.isEmpty && lookingFor != null) {
						log("Slot $i contains: ${stack.name.string}")
						if (stack.name.string.lowercase().contains(lookingFor!!.lowercase())) {
							// append to an array
							slotsToDrop.add(i)
						}
					}
				}

				if (!slotsToDrop.isEmpty()) {
					//schedule dropping items here
					val delayBetweenDrops = CONFIG.delayBetweenDrops
					var initialWait = CONFIG.delayBeforeStartDrop
					for (i in slotsToDrop) {
						// schedule to run in waitingFor ticks
						TaskScheduler.schedule(initialWait) {
							drop(client, screenHandler, i)
						}

						if (i == slotsToDrop.last()) {
							TaskScheduler.schedule(initialWait + CONFIG.delayAfrerChestClear) {
								client.player?.closeHandledScreen()
							}
						}

						// drop(client, screenHandler, i)
						initialWait += delayBetweenDrops
					}
					previousScreen = currentScreen
				}
			}
		}
	}

	fun drop(client: MinecraftClient, screenHandler: ScreenHandler, slot: Int){
		if (client != null && screenHandler != null && slot != null){
			log("dropping item")
			client.interactionManager?.clickSlot(
				screenHandler.syncId,          // the ID of the current screen handler
				slot,                     // index of the slot to drop
				1,                             // button (1 means full stack, 0 = single item)
				SlotActionType.THROW,          // simulate the "throw" action (Q)
				client.player                  // the player
			)
		}
	}

}

object TaskScheduler {
	private val tasks = mutableListOf<ScheduledTask>()

	data class ScheduledTask(val ticksRemaining: Int, val action: () -> Unit)

	fun schedule(ticks: Int, action: () -> Unit) {
		tasks.add(ScheduledTask(ticks, action))
	}

	fun tick() {
		val iterator = tasks.listIterator()
		while (iterator.hasNext()) {
			val task = iterator.next()
			val remaining = task.ticksRemaining - 1
			if (remaining <= 0) {
				task.action()
				iterator.remove()
			} else {
				iterator.set(task.copy(ticksRemaining = remaining))
			}
		}
	}
}