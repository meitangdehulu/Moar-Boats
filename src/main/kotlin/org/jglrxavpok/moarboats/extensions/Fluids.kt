package org.jglrxavpok.moarboats.extensions

import net.minecraft.block.BlockLiquid
import net.minecraft.block.state.IBlockState
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fluids.BlockFluidBase
import net.minecraftforge.fluids.IFluidBlock

object Fluids {

    fun getLiquidHeight(blockstate: IBlockState, world: World, pos: BlockPos) = when(blockstate.block) {
        is BlockLiquid -> BlockLiquid.getLiquidHeight(blockstate, world, pos)
        is BlockFluidBase -> {
            pos.y + getBlockLiquidHeight(blockstate, world, pos)
        }
        else -> error("Unknown liquid type $blockstate (${blockstate.block}) in MoarBoats")
    }

    fun getLiquidLocalLevel(blockstate: IBlockState) = when(blockstate.block) {
        is BlockLiquid -> blockstate.getValue(BlockLiquid.LEVEL)
        is BlockFluidBase -> blockstate.getValue(BlockFluidBase.LEVEL)
        else -> error("Unknown liquid type $blockstate (${blockstate.block}) in MoarBoats")
    }

    fun getBlockLiquidHeight(blockstate: IBlockState, world: World, pos: BlockPos): Float {
        val level = getLiquidLocalLevel(blockstate)
        val blockUp = world.getBlockState(pos.up())
        return if(blockUp.material.isLiquid || blockUp.block is IFluidBlock)
            1.0f
        else
            1.0f - BlockLiquid.getLiquidHeightPercent(level)
    }

    fun isUsualLiquidBlock(blockstate: IBlockState) = blockstate.block is BlockFluidBase || blockstate.block is BlockLiquid
}