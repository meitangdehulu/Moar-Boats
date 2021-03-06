package org.jglrxavpok.moarboats.common.modules.inventories

import net.minecraft.init.Items
import net.minecraft.inventory.SlotFurnaceFuel
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntityFurnace
import org.jglrxavpok.moarboats.api.BoatModule
import org.jglrxavpok.moarboats.api.IControllable

class EngineModuleInventory(boat: IControllable, module: BoatModule): BaseModuleInventory(1, "testEngine", boat, module) {
    override fun id2key(id: Int): String? = when(id) {
        0 -> "fuelTime"
        1 -> "fuelTotalTime"
        else -> null
    }

    override fun isItemValidForSlot(index: Int, stack: ItemStack): Boolean {
        if(index == 0) {
            val itemstack = list[0]
            return TileEntityFurnace.isItemFuel(stack) || SlotFurnaceFuel.isBucket(stack) && itemstack.item !== Items.BUCKET
        }
        return false
    }

    override fun getFieldCount(): Int {
        return 2
    }
}