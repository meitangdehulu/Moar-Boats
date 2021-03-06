package org.jglrxavpok.moarboats.common.modules

import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.nbt.NBTTagList
import net.minecraft.util.EnumHand
import net.minecraft.util.ResourceLocation
import net.minecraft.util.math.MathHelper
import net.minecraft.world.storage.MapData
import net.minecraftforge.common.util.Constants
import org.jglrxavpok.moarboats.MoarBoats
import org.jglrxavpok.moarboats.client.gui.GuiHelmModule
import org.jglrxavpok.moarboats.common.containers.ContainerHelmModule
import org.jglrxavpok.moarboats.common.items.HelmItem
import org.jglrxavpok.moarboats.common.network.C2MapRequest
import org.jglrxavpok.moarboats.extensions.toDegrees
import org.jglrxavpok.moarboats.api.BoatModule
import org.jglrxavpok.moarboats.api.IControllable
import org.jglrxavpok.moarboats.common.containers.ContainerBase
import org.jglrxavpok.moarboats.common.modules.HelmModule.getInventory
import org.jglrxavpok.moarboats.common.state.*

object HelmModule: BoatModule(), BlockReason {
    override val id: ResourceLocation = ResourceLocation(MoarBoats.ModID, "helm")
    override val usesInventory = true
    override val moduleSpot = Spot.Navigation
    override val hopperPriority = 0

    private val Epsilon = 0.1
    val MaxDistanceToWaypoint = 1.5
    val MaxDistanceToWaypointSquared = MaxDistanceToWaypoint*MaxDistanceToWaypoint

    // State names
    val waypointsProperty = NBTListBoatProperty("waypoints", Constants.NBT.TAG_COMPOUND)
    val currentWaypointProperty = IntBoatProperty("currentWaypoint")
    val rotationAngleProperty = FloatBoatProperty("rotationAngle")
    val xCenterProperty = IntBoatProperty("xCenter")
    val zCenterProperty = IntBoatProperty("zCenter")
    val mapDataCopyProperty = MapDataProperty("internalMapData")
    val loopingProperty = BooleanBoatProperty("looping")

    val MapUpdatePeriod = 20*5 // every 5 second

    val StripeLength = 64

    override fun onInteract(from: IControllable, player: EntityPlayer, hand: EnumHand, sneaking: Boolean): Boolean {
        return false
    }

    override fun onInit(to: IControllable, fromItem: ItemStack?) {
        super.onInit(to, fromItem)
        if(to.worldRef.isRemote) {
            val stack = to.getInventory().getStackInSlot(0)
            if(!stack.isEmpty && stack.item is ItemMap) {
                val id = stack.itemDamage
                MoarBoats.network.sendToServer(C2MapRequest("map_$id", to.entityID, this.id))
            }
        }
    }

    override fun controlBoat(from: IControllable) {
        if(!from.inLiquid())
            return
        val waypoints = waypointsProperty[from]
        if(waypoints.tagCount() != 0) {
            val currentWaypoint = currentWaypointProperty[from] % waypoints.tagCount()
            val current = waypoints[currentWaypoint] as NBTTagCompound
            val nextX = current.getInteger("x")
            val nextZ = current.getInteger("z")

            val dx = from.positionX - nextX
            val dz = from.positionZ - nextZ
            val nextWaypoint = (currentWaypoint+1) % waypoints.tagCount()

            if(!loopingProperty[from] && currentWaypoint > nextWaypoint) {
                if(dx*dx+dz*dz < MaxDistanceToWaypointSquared) { // close to the last waypoint
                    from.blockMovement(this)
                    return
                }
            }

            val targetAngle = Math.atan2(dz, dx).toDegrees() + 90f
            val yaw = from.yaw
            if(MathHelper.wrapDegrees(targetAngle - yaw) > Epsilon) {
                from.turnRight()
            } else if(MathHelper.wrapDegrees(targetAngle - yaw) < -Epsilon) {
                from.turnLeft()
            }
            rotationAngleProperty[from] = MathHelper.wrapDegrees(targetAngle-yaw).toFloat()
        }
    }

    override fun update(from: IControllable) {
        val inventory = from.getInventory()
        val stack = inventory.getStackInSlot(0)
        val item = stack.item
        val waypoints = waypointsProperty[from]
        if(waypoints.tagCount() != 0) {
            val currentWaypoint = currentWaypointProperty[from] % waypoints.tagCount()
            val nextWaypoint = (currentWaypoint+1) % waypoints.tagCount()
            if(loopingProperty[from] || currentWaypoint <= nextWaypoint) { // next one is further from the start of the list
                val current = waypoints[currentWaypoint] as NBTTagCompound
                val currentX = current.getInteger("x")
                val currentZ = current.getInteger("z")
                val dx = currentX - from.positionX
                val dz = currentZ - from.positionZ
                if(dx*dx+dz*dz < MaxDistanceToWaypointSquared) {
                    currentWaypointProperty[from] = nextWaypoint
                }
            }
        }

        if(stack.isEmpty || item !is ItemMap) {
            receiveMapData(from, EmptyMapData)
            waypointsProperty[from] = NBTTagList() // reset waypoints
            return
        }
        val mapdata = mapDataCopyProperty[from]
        if (!from.worldRef.isRemote) {
            xCenterProperty[from] = mapdata.xCenter
            zCenterProperty[from] = mapdata.zCenter
        } else if(mapdata == EmptyMapData || from.correspondingEntity.ticksExisted % MapUpdatePeriod == 0) {
            val id = stack.itemDamage
            MoarBoats.network.sendToServer(C2MapRequest("map_$id", from.entityID, this.id))
        }
    }

    override fun onAddition(to: IControllable) {
        if(!to.worldRef.isRemote) {
            xCenterProperty[to] = 0
            zCenterProperty[to] = 0
            waypointsProperty[to] = NBTTagList()
        }
    }

    override fun createContainer(player: EntityPlayer, boat: IControllable): ContainerBase {
        return ContainerHelmModule(player.inventory, this, boat)
    }

    override fun createGui(player: EntityPlayer, boat: IControllable): GuiScreen {
        return GuiHelmModule(player.inventory, this, boat)
    }

    fun addWaypoint(boat: IControllable, blockX: Int, blockZ: Int, renderX: Int, renderZ: Int) {
        val waypointsData = waypointsProperty[boat]
        val waypointNBT = NBTTagCompound()
        waypointNBT.setInteger("x", blockX)
        waypointNBT.setInteger("z", blockZ)
        waypointNBT.setInteger("renderX", renderX)
        waypointNBT.setInteger("renderZ", renderZ)
        waypointsData.appendTag(waypointNBT)
        waypointsProperty[boat] = waypointsData
    }

    override fun dropItemsOnDeath(boat: IControllable, killedByPlayerInCreative: Boolean) {
        if(!killedByPlayerInCreative)
            boat.correspondingEntity.dropItem(HelmItem, 1)
    }

    fun removeLastWaypoint(boat: IControllable) {
        val waypointsData = waypointsProperty[boat]
        if(waypointsData.tagCount() > 0) {
            waypointsData.removeTag(waypointsData.tagCount()-1)
        }
    }

    fun receiveMapData(boat: IControllable, data: MapData) {
        mapDataCopyProperty[boat] = data
    }

    fun removeWaypoint(boat: IControllable, index: Int) {
        val waypointsData = waypointsProperty[boat]
        waypointsData.removeTag(index)
    }
}