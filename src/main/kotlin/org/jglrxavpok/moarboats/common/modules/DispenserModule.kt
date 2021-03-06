package org.jglrxavpok.moarboats.common.modules

import net.minecraft.block.BlockDispenser
import net.minecraft.client.gui.GuiScreen
import net.minecraft.dispenser.IBehaviorDispenseItem
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.init.SoundEvents
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.jglrxavpok.moarboats.MoarBoats
import org.jglrxavpok.moarboats.api.BoatModule
import org.jglrxavpok.moarboats.api.IControllable
import org.jglrxavpok.moarboats.client.gui.GuiDispenserModule
import org.jglrxavpok.moarboats.common.containers.ContainerDispenserModule
import org.jglrxavpok.moarboats.common.state.BlockPosProperty
import org.jglrxavpok.moarboats.common.state.DoubleBoatProperty
import org.jglrxavpok.moarboats.extensions.Fluids
import org.jglrxavpok.moarboats.extensions.use

object DispenserModule: BoatModule() {
    override val id = ResourceLocation(MoarBoats.ModID, "dispenser")
    override val usesInventory = true
    override val moduleSpot = Spot.Storage

    val blockPeriodProperty = DoubleBoatProperty("period")
    val lastDispensePositionProperty = BlockPosProperty("lastFire")
    val BOAT_BEHIND = Vec3d(0.0, 0.0, 0.0625 * 25)

    // Row indices
    val TOP = 1
    val MIDDLE = 0
    val BOTTOM = -1

    override fun onInteract(from: IControllable, player: EntityPlayer, hand: EnumHand, sneaking: Boolean) = false

    override fun controlBoat(from: IControllable) { }

    override fun update(from: IControllable) {
        if(!from.inLiquid() || from.worldRef.isRemote)
            return
        lastDispensePositionProperty[from].use { pos ->
            val period = blockPeriodProperty[from]
            if(pos.distanceSq(from.positionX, from.positionY, from.positionZ) > period*period) {
                dispenseItem(BOTTOM, from)
                dispenseItem(MIDDLE, from)
                dispenseItem(TOP, from)
                val newPos = BlockPos.PooledMutableBlockPos.retain(from.positionX, from.positionY, from.positionZ)
                lastDispensePositionProperty[from] = newPos
                newPos.release()
            }
        }
    }

    private fun dispenseItem(row: Int, boat: IControllable) {
        val pos = boat.localToWorld(BOAT_BEHIND)
        val blockPos = BlockPos.PooledMutableBlockPos.retain(pos.x, pos.y+row + .75f, pos.z)
        val inventoryRowStart = (-row)*5 +5
        firstValidStack(inventoryRowStart, boat)?.let { (index, stack) ->
            val item = stack.item
            val world = boat.worldRef
            when(item) {
                is ItemBlock -> useItemBlock(item, world, stack, blockPos, boat)
                else -> {
                    val behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(item)
                    val resultingStack = behavior.dispense(boat, stack)
                    boat.getInventory().setInventorySlotContents(index, resultingStack)
                }
            }
            boat.getInventory().syncToClient()
        }
        blockPos.release()
    }

    private fun useItemBlock(item: ItemBlock, world: World, stack: ItemStack, blockPos: BlockPos.PooledMutableBlockPos, boat: IControllable) {
        val block = item.block
        val newState = block.getStateFromMeta(stack.metadata)
        if(world.isAirBlock(blockPos) || Fluids.isUsualLiquidBlock(world.getBlockState(blockPos))) {
            if(world.mayPlace(block, blockPos, false, EnumFacing.fromAngle(boat.yaw.toDouble()), boat.correspondingEntity)) {
                val succeeded = world.setBlockState(blockPos, newState, 11)
                if (succeeded) {
                    try {
                        val state = world.getBlockState(blockPos)
                        if (state.block === block) {
                            setTileEntityNBT(world, blockPos, stack)
                            block.onBlockPlacedBy(world, blockPos, state, null, stack)
                        }
                    } catch (npe: NullPointerException) {
                        // some blocks do not like at all being placed by a machine apparently (eg chests)
                    }
                    stack.shrink(1)
                }
            }
        }
    }

    // adapted from ItemBlock.java
    private fun setTileEntityNBT(worldIn: World, pos: BlockPos, stackIn: ItemStack) {
        val nbttagcompound = stackIn.getSubCompound("BlockEntityTag")

        if (nbttagcompound != null) {
            val tileentity = worldIn.getTileEntity(pos)

            if (tileentity != null) {
                val nbttagcompound1 = tileentity.writeToNBT(NBTTagCompound())
                val nbttagcompound2 = nbttagcompound1.copy()
                nbttagcompound1.merge(nbttagcompound)
                nbttagcompound1.setInteger("x", pos.x)
                nbttagcompound1.setInteger("y", pos.y)
                nbttagcompound1.setInteger("z", pos.z)

                if (nbttagcompound1 != nbttagcompound2) {
                    tileentity.readFromNBT(nbttagcompound1)
                    tileentity.markDirty()
                }
            }
        }
    }

    private fun firstValidStack(startIndex: Int, boat: IControllable): Pair<Int, ItemStack>? {
        val inv = boat.getInventory()
        return (0..4)
                .map { offset -> inv.getStackInSlot(startIndex+offset) }
                .filter { !it.isEmpty }
                .mapIndexed { index, itemStack -> Pair(startIndex+index, itemStack) }
                .firstOrNull { val item = it.second.item
                    item is ItemBlock
                            || BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(item) != IBehaviorDispenseItem.DEFAULT_BEHAVIOR
                }
    }

    override fun onAddition(to: IControllable) {
        blockPeriodProperty[to] = 1.0 // every block by default
    }

    fun changePeriod(boat: IControllable, period: Double) {
        blockPeriodProperty[boat] = period
    }

    override fun createContainer(player: EntityPlayer, boat: IControllable) = ContainerDispenserModule(player.inventory, this, boat)

    override fun createGui(player: EntityPlayer, boat: IControllable): GuiScreen {
        return GuiDispenserModule(player.inventory, this, boat)
    }

    override fun dropItemsOnDeath(boat: IControllable, killedByPlayerInCreative: Boolean) {
        if(!killedByPlayerInCreative)
            boat.correspondingEntity.dropItem(ItemBlock.getItemFromBlock(Blocks.DISPENSER), 1)
    }
}
