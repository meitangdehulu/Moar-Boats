package org.jglrxavpok.moarboats.common.network

import io.netty.buffer.ByteBuf
import net.minecraft.item.ItemMap
import net.minecraftforge.fml.common.network.simpleimpl.IMessage
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext
import org.jglrxavpok.moarboats.common.containers.ContainerHelmModule
import org.jglrxavpok.moarboats.common.modules.HelmModule

@Deprecated(message = "Replace with C12AddWaypoint or C12RemoveWaypoint")
class C1MapClick(): IMessage {

    var pixelX: Double = 0.0
    var pixelY: Double = 0.0
    var mapAreaSize: Double = 0.0
    var button: Int = 0

    constructor(pixelX: Double, pixelY: Double, mapAreaSize: Double, mouseButton: Int): this() {
        this.pixelX = pixelX
        this.pixelY = pixelY
        this.mapAreaSize = mapAreaSize
        this.button = mouseButton
    }

    override fun fromBytes(buf: ByteBuf) {
        mapAreaSize = buf.readDouble()
        pixelX = buf.readDouble()
        pixelY = buf.readDouble()
        button = buf.readInt()
    }

    override fun toBytes(buf: ByteBuf) {
        buf.writeDouble(mapAreaSize)
        buf.writeDouble(pixelX)
        buf.writeDouble(pixelY)
        buf.writeInt(button)
    }

    object Handler: IMessageHandler<C1MapClick, IMessage> {
        override fun onMessage(message: C1MapClick, ctx: MessageContext): IMessage? {
            val player = ctx.serverHandler.player
            val container = player.openContainer
            if(container !is ContainerHelmModule) {
                error("Invalid container, expected ContainerHelmModule, got $container")
            }
            val stack = container.getSlot(0).stack
            val item = stack.item as? ItemMap ?: error("Got click while there was no map!")
            val boat = container.boat
            val mapdata = item.getMapData(stack, boat.worldRef)!!
            val helm = container.helm as HelmModule
            val mapScale = (1 shl mapdata.scale.toInt()).toFloat()
            val pixelsToMap = 128f/message.mapAreaSize

            when(message.button) {
                0 -> helm.addWaypoint(boat,
                        pixel2map(message.pixelX, mapdata.xCenter, message.mapAreaSize, mapScale),
                        pixel2map(message.pixelY, mapdata.zCenter, message.mapAreaSize, mapScale),
                        (message.pixelX * pixelsToMap).toInt(), (message.pixelY * pixelsToMap).toInt())

                1 -> helm.removeLastWaypoint(boat)
            }


            return null
        }

        private fun pixel2map(pixel: Double, center: Int, mapAreaSize: Double, mapScale: Float): Int {
            val pixelsToMap = 128f/mapAreaSize
            return Math.floor((center / mapScale + (pixel - mapAreaSize /2) * pixelsToMap) * mapScale).toInt()
        }
    }
}