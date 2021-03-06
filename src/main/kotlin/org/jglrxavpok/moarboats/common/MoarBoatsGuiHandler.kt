package org.jglrxavpok.moarboats.common

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemMap
import net.minecraft.world.World
import net.minecraftforge.fml.common.network.IGuiHandler
import org.jglrxavpok.moarboats.client.gui.GuiPathEditor
import org.jglrxavpok.moarboats.common.entities.ModularBoatEntity
import org.jglrxavpok.moarboats.common.modules.HelmModule
import org.jglrxavpok.moarboats.common.state.EmptyMapData

object MoarBoatsGuiHandler: IGuiHandler {
    override fun getClientGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        return when(ID) {
            ModulesGui -> {
                val boatID = x
                val boat = world.getEntityByID(boatID) as? ModularBoatEntity ?: return null
                val module = boat.modules[y]
                module.createGui(player, boat)
            }
            PathEditor -> {
                val boatID = x
                val boat = world.getEntityByID(boatID) as? ModularBoatEntity ?: return null
                if(HelmModule in boat.modules) {
                    val inventory = boat.getInventory(HelmModule)
                    val stack = inventory.list[0]
                    val id = stack.itemDamage
                    if(stack.item is ItemMap) {
                        val mapData = HelmModule.mapDataCopyProperty[boat]
                        if(mapData != EmptyMapData) {
                            GuiPathEditor(player, boat, mapData, "map_$id")
                        } else {
                            null
                        }
                    } else {
                        null // NO MAP
                    }
                } else {
                    null // NO HELM
                }
            }
            else -> null
        }
    }

    override fun getServerGuiElement(ID: Int, player: EntityPlayer, world: World, x: Int, y: Int, z: Int): Any? {
        return when(ID) {
            ModulesGui -> {
                val boatID = x
                val boat = world.getEntityByID(boatID) as? ModularBoatEntity ?: return null
                val module = boat.modules[y]
                module.createContainer(player, boat)
            }
            PathEditor -> null
            else -> null
        }
    }

    val ModulesGui: Int = 0
    val PathEditor: Int = 1
}