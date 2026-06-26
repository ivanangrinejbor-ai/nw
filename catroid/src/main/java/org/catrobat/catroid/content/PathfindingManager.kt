package org.catrobat.catroid.content

import com.badlogic.gdx.math.Vector2
import org.catrobat.catroid.stage.StageActivity
import java.util.PriorityQueue

data class PathNode(
    val x: Int, val y: Int,
    var g: Float = 0f,
    var h: Float = 0f,
    var parent: PathNode? = null
) {
    val f: Float get() = g + h
}

data class PathResult(
    val points: List<Vector2>,
    val found: Boolean
)

enum class FollowState { IDLE, FOLLOWING, PAUSED, REACHED }

data class PathFollower(
    val spriteName: String,
    var waypoints: List<Vector2> = emptyList(),
    var currentIndex: Int = 0,
    var state: FollowState = FollowState.IDLE,
    var speed: Float = 100f
)

class PathfindingManager {

    data class NavGrid(
        val width: Int,
        val height: Int,
        val cellSize: Float,
        val walkable: Array<BooleanArray>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NavGrid) return false
            return width == other.width && height == other.height &&
                    cellSize == other.cellSize && walkable.contentDeepEquals(other.walkable)
        }
        override fun hashCode(): Int {
            var result = width
            result = 31 * result + height
            result = 31 * result + cellSize.hashCode()
            result = 31 * result + walkable.contentDeepHashCode()
            return result
        }
    }

    var navGrid: NavGrid? = null
    private val obstacles = mutableMapOf<String, MutableList<Vector2>>()
    private val followers = mutableMapOf<String, PathFollower>()
    private var avoidColorHex: String? = null

    fun createGrid(width: Int, height: Int, cellSize: Float, defaultWalkable: Boolean = true) {
        val walkable = Array(width) { BooleanArray(height) { defaultWalkable } }
        navGrid = NavGrid(width, height, cellSize, walkable)
        for ((_, points) in obstacles) {
            for (pt in points) {
                markObstacle(pt.x, pt.y, false)
            }
        }
    }

    fun deleteGrid() {
        navGrid = null
        followers.clear()
    }

    fun rebuildGrid() {
        val grid = navGrid ?: return
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                grid.walkable[x][y] = true
            }
        }
        for ((_, points) in obstacles) {
            for (pt in points) {
                markObstacle(pt.x, pt.y, false)
            }
        }
    }

    private fun worldToCell(worldX: Float, worldY: Float): Pair<Int, Int>? {
        val grid = navGrid ?: return null
        val cx = kotlin.math.floor(worldX / grid.cellSize).toInt()
        val cy = kotlin.math.floor(worldY / grid.cellSize).toInt()
        if (cx in 0 until grid.width && cy in 0 until grid.height) {
            return cx to cy
        }
        return null
    }

    private fun markObstacle(worldX: Float, worldY: Float, walkable: Boolean) {
        val cell = worldToCell(worldX, worldY) ?: return
        navGrid?.walkable?.get(cell.first)?.set(cell.second, walkable)
    }

    fun isWalkable(worldX: Float, worldY: Float): Boolean {
        val cell = worldToCell(worldX, worldY) ?: return false
        return navGrid!!.walkable[cell.first][cell.second]
    }

    private fun findSpriteByName(name: String): Sprite? {
        val stageListener = StageActivity.getActiveStageListener() ?: return null
        for (sprite in stageListener.spritesFromStage) {
            if (sprite.name == name) return sprite
        }
        return null
    }

    fun addObstacle(name: String) {
        if (obstacles.containsKey(name)) return
        val sprite = findSpriteByName(name) ?: return
        val look = sprite.look ?: return
        val x = look.xInAbstractUserUnit
        val y = look.yInAbstractUserUnit
        val w = (look.widthInUserUnits / 2f).coerceAtLeast(1f)
        val h = (look.heightInUserUnits / 2f).coerceAtLeast(1f)
        val points = mutableListOf<Vector2>()
        val step = navGrid?.cellSize?.coerceAtMost(1f) ?: 1f
        var wx = x - w
        while (wx <= x + w) {
            var wy = y - h
            while (wy <= y + h) {
                val pt = Vector2(wx, wy)
                points.add(pt)
                markObstacle(pt.x, pt.y, false)
                wy += step
            }
            wx += step
        }
        obstacles[name] = points
    }

    fun removeObstacle(name: String) {
        val points = obstacles.remove(name) ?: return
        for (pt in points) {
            markObstacle(pt.x, pt.y, true)
        }
    }

    fun updateObstacles() {
        val names = obstacles.keys.toList()
        obstacles.clear()
        if (navGrid != null) rebuildGrid()
        for (name in names) {
            addObstacle(name)
        }
    }

    fun setAvoidColor(hexColor: String?) {
        avoidColorHex = hexColor
    }

    fun getAvoidColor(): String? = avoidColorHex

    fun createObstaclesFromBackground(): Int {
        val stageListener = StageActivity.getActiveStageListener() ?: return 0
        var count = 0
        for (sprite in stageListener.spritesFromStage) {
            if (sprite.name == "Background") continue
            if (obstacles.containsKey(sprite.name)) continue
            addObstacle(sprite.name)
            count++
        }
        return count
    }

    fun findPath(startX: Float, startY: Float, endX: Float, endY: Float): PathResult {
        val grid = navGrid ?: return PathResult(emptyList(), false)
        val cs = grid.cellSize
        val sx = kotlin.math.floor(startX / cs).toInt().coerceIn(0, grid.width - 1)
        val sy = kotlin.math.floor(startY / cs).toInt().coerceIn(0, grid.height - 1)
        val ex = kotlin.math.floor(endX / cs).toInt().coerceIn(0, grid.width - 1)
        val ey = kotlin.math.floor(endY / cs).toInt().coerceIn(0, grid.height - 1)

        if (!grid.walkable[sx][sy] || !grid.walkable[ex][ey]) {
            return PathResult(emptyList(), false)
        }

        val openSet = PriorityQueue<PathNode>(compareBy { it.f })
        val closedSet = mutableSetOf<Pair<Int, Int>>()
        val bestG = mutableMapOf<Pair<Int, Int>, Float>()
        val startKey = sx to sy
        val startNode = PathNode(sx, sy)
        startNode.h = heuristic(sx, sy, ex, ey)
        openSet.add(startNode)
        bestG[startKey] = 0f

        val dirs = arrayOf(
            0 to 1, 1 to 0, 0 to -1, -1 to 0,
            1 to 1, 1 to -1, -1 to 1, -1 to -1
        )

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()!!
            val key = current.x to current.y
            if (key in closedSet) continue
            closedSet.add(key)

            if (current.x == ex && current.y == ey) {
                return PathResult(retracePath(current).map { Vector2(it.x * cs + cs / 2, it.y * cs + cs / 2) }, true)
            }

            for ((dx, dy) in dirs) {
                val nx = current.x + dx
                val ny = current.y + dy
                if (nx !in 0 until grid.width || ny !in 0 until grid.height) continue
                if (!grid.walkable[nx][ny]) continue
                if (nx to ny in closedSet) continue

                val moveCost = if (dx != 0 && dy != 0) 1.414f else 1f
                val g = current.g + moveCost
                val nKey = nx to ny
                val prevBest = bestG[nKey]
                if (prevBest != null && g >= prevBest) continue

                val h = heuristic(nx, ny, ex, ey)
                bestG[nKey] = g
                openSet.add(PathNode(nx, ny, g, h, current))
            }
        }
        return PathResult(emptyList(), false)
    }

    private fun heuristic(ax: Int, ay: Int, bx: Int, by: Int): Float {
        val dx = (ax - bx).toFloat()
        val dy = (ay - by).toFloat()
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    private fun retracePath(node: PathNode): List<PathNode> {
        val path = mutableListOf<PathNode>()
        var current: PathNode? = node
        while (current != null) {
            path.add(current)
            current = current.parent
        }
        return path.reversed()
    }

    fun findPathToObject(fromSprite: String, targetSprite: String): PathResult {
        val from = findSpriteByName(fromSprite)?.look ?: return PathResult(emptyList(), false)
        val to = findSpriteByName(targetSprite)?.look ?: return PathResult(emptyList(), false)
        return findPath(from.xInAbstractUserUnit, from.yInAbstractUserUnit,
            to.xInAbstractUserUnit, to.yInAbstractUserUnit)
    }

    fun setPathForFollower(spriteName: String, path: List<Vector2>) {
        val follower = followers.getOrPut(spriteName) { PathFollower(spriteName) }
        follower.waypoints = path
        follower.currentIndex = 0
        follower.state = if (path.isNotEmpty()) FollowState.FOLLOWING else FollowState.REACHED
    }

    fun getFollower(spriteName: String): PathFollower? = followers[spriteName]

    fun startFollowing(spriteName: String, speed: Float = 100f) {
        val f = followers[spriteName] ?: return
        f.speed = speed
        f.state = FollowState.FOLLOWING
    }

    fun stopFollowing(spriteName: String) {
        val f = followers[spriteName] ?: return
        f.state = FollowState.IDLE
        f.waypoints = emptyList()
        f.currentIndex = 0
    }

    fun pauseFollowing(spriteName: String) {
        val f = followers[spriteName] ?: return
        if (f.state == FollowState.FOLLOWING) f.state = FollowState.PAUSED
    }

    fun resumeFollowing(spriteName: String) {
        val f = followers[spriteName] ?: return
        if (f.state == FollowState.PAUSED) f.state = FollowState.FOLLOWING
    }

    fun update(delta: Float) {
        val stageListener = StageActivity.getActiveStageListener() ?: return
        for ((name, follower) in followers) {
            if (follower.state != FollowState.FOLLOWING) continue
            if (follower.currentIndex >= follower.waypoints.size) {
                follower.state = FollowState.REACHED
                continue
            }
            val target = follower.waypoints[follower.currentIndex]

            var sprite: Sprite? = null
            for (s in stageListener.spritesFromStage) {
                if (s.name == name) { sprite = s; break }
            }
            val look = sprite?.look ?: continue

            val currentX = look.xInAbstractUserUnit
            val currentY = look.yInAbstractUserUnit
            val dx = target.x - currentX
            val dy = target.y - currentY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

            if (dist < 2f) {
                follower.currentIndex++
                continue
            }

            val step = follower.speed * delta
            val ratio = (step / dist).coerceAtMost(1f)
            val newX = currentX + dx * ratio
            val newY = currentY + dy * ratio

            look.setXYInAbstractUserUnit(newX, newY)
            val angleDeg = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            look.setDirectionInUserUnits(-angleDeg + 90f)
        }
    }

    fun getPathPointCount(spriteName: String): Int {
        return followers[spriteName]?.waypoints?.size ?: 0
    }

    fun getCurrentPathPoint(spriteName: String): Vector2? {
        val f = followers[spriteName] ?: return null
        if (f.currentIndex in f.waypoints.indices) {
            return f.waypoints[f.currentIndex]
        }
        return null
    }

    fun getNextPathPoint(spriteName: String): Vector2? {
        val f = followers[spriteName] ?: return null
        val next = f.currentIndex + 1
        if (next in f.waypoints.indices) {
            return f.waypoints[next]
        }
        return null
    }

    fun isEndReached(spriteName: String): Boolean {
        return followers[spriteName]?.state == FollowState.REACHED
    }

    fun clearScene() {
        navGrid = null
        obstacles.clear()
        followers.clear()
        avoidColorHex = null
    }

    fun dispose() {
        clearScene()
    }
}
