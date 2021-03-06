package org.jglrxavpok.moarboats.common.containers

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.player.InventoryPlayer
import net.minecraft.inventory.Container
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemStack

abstract class ContainerBase(val playerInventory: InventoryPlayer): Container() {

    protected fun addPlayerSlots(isLarge: Boolean) {
        val yOffset = if(isLarge) 3 * 18 +2 else 0
        for (i in 0..2) {
            for (j in 0..8) {
                this.addSlotToContainer(Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18 + yOffset))
            }
        }

        for (k in 0..8) {
            this.addSlotToContainer(Slot(playerInventory, k, 8 + k * 18, 142 + yOffset))
        }
    }

    override fun canInteractWith(playerIn: EntityPlayer): Boolean {
        return true
    }

    override fun transferStackInSlot(playerIn: EntityPlayer, index: Int): ItemStack {
        var itemstack = ItemStack.EMPTY
        val slot = this.inventorySlots[index]

        if (slot != null && slot.hasStack) {
            val itemstack1 = slot.stack
            itemstack = itemstack1.copy()

            if (index in 0..26) {
                if (!this.mergeItemStack(itemstack1, 26, 36, false)) {
                    return ItemStack.EMPTY
                }
            } else if (index in 27..35 && !this.mergeItemStack(itemstack1, 0, 36, false)) {
                return ItemStack.EMPTY
            }

            if (itemstack1.isEmpty) {
                slot.putStack(ItemStack.EMPTY)
            } else {
                slot.onSlotChanged()
            }

            if (itemstack1.count == itemstack.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(playerIn, itemstack1)
        }

        return itemstack
    }
}