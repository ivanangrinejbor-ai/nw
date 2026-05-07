package org.catrobat.catroid.raptor

import com.danvexteam.lunoscript_annotations.LunoClass
import java.util.concurrent.ConcurrentHashMap

@LunoClass
class VoxelManager {
    enum class VoxelShape {
        CUBE, CROSS,
        FLOOR, CEILING,
        WALL_NORTH, WALL_SOUTH, WALL_EAST, WALL_WEST
    }

    data class BlockConfig(
        val shape: VoxelShape = VoxelShape.CUBE,
        val texX: Int = 0,
        val texY: Int = 0,
        val isTransparent: Boolean = false,
        val isPhysical: Boolean = true
    )

    companion object {
        private val worlds = ConcurrentHashMap<String, VoxelBuffer>()
        private val blockRegistry = ConcurrentHashMap<Short, BlockConfig>()

        fun createWorld(id: String, x: Int, y: Int, z: Int, wx: Int, wy: Int, wz: Int) {
            worlds[id] = VoxelBuffer(x, y, z, wx, wy, wz)
        }

        fun getBuffer(id: String): VoxelBuffer? = worlds[id]

        fun configureBlock(id: Short, shapeInt: Int, texX: Int, texY: Int) {
            val shape = VoxelShape.values().getOrElse(shapeInt) { VoxelShape.CUBE }
            blockRegistry[id] = BlockConfig(shape, texX, texY)
        }

        @JvmStatic
        fun getConfig(id: Short): BlockConfig = blockRegistry[id] ?: BlockConfig()

        fun setBlock(id: String, x: Int, y: Int, z: Int, type: Short, data: Byte = 0) {
            worlds[id]?.let {
                it.set(x, y, z, type)
                it.setData(x, y, z, data)
            }
        }

        @JvmStatic
        fun setPhysical(id: Short, phys: Boolean) {
            val old = blockRegistry[id] ?: BlockConfig()
            blockRegistry[id] = old.copy(isPhysical = phys)
        }

        @JvmStatic
        fun getBlockAbsolute(x: Int, y: Int, z: Int): Short {
            for (b in worlds.values) {
                if (x >= b.worldX && x < b.worldX + b.sizeX &&
                    y >= b.worldY && y < b.worldY + b.sizeY &&
                    z >= b.worldZ && z < b.worldZ + b.sizeZ) {
                    return b.get(x - b.worldX, y - b.worldY, z - b.worldZ)
                }
            }
            return 0
        }

        fun getBlockInfo(worldId: String, x: Int, y: Int, z: Int): String {
            val buffer = worlds[worldId] ?: return "0\n0"
            val type = buffer.get(x, y, z)
            val data = buffer.getData(x, y, z)
            return "$type\n$data"
        }

        fun loadFromString(id: String, data: String, delimX: String, delimY: String, delimZ: String) {
            val buffer = worlds[id] ?: return

            val planes = data.split(delimY)
            for (y in 0 until minOf(planes.size, buffer.sizeY)) {
                val rows = planes[y].split(delimZ)
                for (z in 0 until minOf(rows.size, buffer.sizeZ)) {
                    val blocks = rows[z].split(delimX)
                    for (x in 0 until minOf(blocks.size, buffer.sizeX)) {
                        val type = blocks[x].toShortOrNull() ?: 0
                        buffer.typeData[x + (y * buffer.sizeX) + (z * buffer.sizeX * buffer.sizeY)] = type
                    }
                }
            }
        }

        fun deleteWorld(id: String) {
            worlds.remove(id)
        }

        fun setTransparent(id: Short, isTransp: Boolean) {
            val current = blockRegistry[id] ?: BlockConfig()
            blockRegistry[id] = current.copy(isTransparent = isTransp)
        }
    }

    class VoxelBuffer(val sizeX: Int, val sizeY: Int, val sizeZ: Int, val worldX: Int, val worldY: Int, val worldZ: Int) {
        val typeData = ShortArray(sizeX * sizeY * sizeZ)
        val metaData = ByteArray(sizeX * sizeY * sizeZ)

        fun set(x: Int, y: Int, z: Int, type: Short) {
            if (x in 0 until sizeX && y in 0 until sizeY && z in 0 until sizeZ) {
                typeData[x + (y * sizeX) + (z * sizeX * sizeY)] = type
            }
        }
        fun get(x: Int, y: Int, z: Int): Short {
            return if (x in 0 until sizeX && y in 0 until sizeY && z in 0 until sizeZ) typeData[x + (y * sizeX) + (z * sizeX * sizeY)] else 0
        }

        fun setData(x: Int, y: Int, z: Int, data: Byte) {
            if (x in 0 until sizeX && y in 0 until sizeY && z in 0 until sizeZ) {
                metaData[x + (y * sizeX) + (z * sizeX * sizeY)] = data
            }
        }

        fun getData(x: Int, y: Int, z: Int): Byte {
            return if (x in 0 until sizeX && y in 0 until sizeY && z in 0 until sizeZ) {
                metaData[x + (y * sizeX) + (z * sizeX * sizeY)]
            } else 0
        }
    }
}
