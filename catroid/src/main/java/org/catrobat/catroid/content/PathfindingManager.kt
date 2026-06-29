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

interface PathCallback {
    fun onWaypointReached(index: Int, position: Vector2)
    fun onPathCompleted(spriteName: String)
    fun onPathBlocked(spriteName: String, reason: String)
}

data class PathFollower(
    val spriteName: String,
    var waypoints: List<Vector2> = emptyList(),
    var currentIndex: Int = 0,
    var state: FollowState = FollowState.IDLE,
    var speed: Float = 100f,
    var callback: PathCallback? = null,
    var rotationSpeed: Float = 360f,
    var arrivalDistance: Float = 2f,
    var enableDynamicReplanning: Boolean = false
)

class PathfindingManager {

    data class NavGrid(
        val width: Int,
        val height: Int,
        val cellSize: Float,
        val walkable: Array<BooleanArray>,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is NavGrid) return false
            return width == other.width && height == other.height &&
                    cellSize == other.cellSize && walkable.contentDeepEquals(other.walkable) &&
                    offsetX == other.offsetX && offsetY == other.offsetY
        }
        override fun hashCode(): Int {
            var result = width
            result = 31 * result + height
            result = 31 * result + cellSize.hashCode()
            result = 31 * result + walkable.contentDeepHashCode()
            result = 31 * result + offsetX.hashCode()
            result = 31 * result + offsetY.hashCode()
            return result
        }
    }

    var navGrid: NavGrid? = null
    private val obstacles = mutableMapOf<String, MutableList<Vector2>>()
    private val followers = mutableMapOf<String, PathFollower>()
    private var avoidColorHex: String? = null

    fun createGrid(width: Int, height: Int, cellSize: Float, defaultWalkable: Boolean = true,
                   offsetX: Float = 0f, offsetY: Float = 0f) {
        val walkable = Array(width) { BooleanArray(height) { defaultWalkable } }
        navGrid = NavGrid(width, height, cellSize, walkable, offsetX, offsetY)
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
        val cx = kotlin.math.floor((worldX - grid.offsetX) / grid.cellSize).toInt()
        val cy = kotlin.math.floor((worldY - grid.offsetY) / grid.cellSize).toInt()
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
        return navGrid?.walkable?.get(cell.first)?.get(cell.second) ?: false
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
        val x = look.xInUserInterfaceDimensionUnit
        val y = look.yInUserInterfaceDimensionUnit
        val w = (look.widthInUserInterfaceDimensionUnit / 2f).coerceAtLeast(1f)
        val h = (look.heightInUserInterfaceDimensionUnit / 2f).coerceAtLeast(1f)
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
        val sx = kotlin.math.floor((startX - grid.offsetX) / cs).toInt().coerceIn(0, grid.width - 1)
        val sy = kotlin.math.floor((startY - grid.offsetY) / cs).toInt().coerceIn(0, grid.height - 1)
        val ex = kotlin.math.floor((endX - grid.offsetX) / cs).toInt().coerceIn(0, grid.width - 1)
        val ey = kotlin.math.floor((endY - grid.offsetY) / cs).toInt().coerceIn(0, grid.height - 1)

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
            val current = openSet.poll() ?: break
            val key = current.x to current.y
            if (key in closedSet) continue
            closedSet.add(key)

            if (current.x == ex && current.y == ey) {
                return PathResult(retracePath(current).map { Vector2(it.x * cs + cs / 2 + grid.offsetX, it.y * cs + cs / 2 + grid.offsetY) }, true)
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
        return findPath(from.xInUserInterfaceDimensionUnit, from.yInUserInterfaceDimensionUnit,
            to.xInUserInterfaceDimensionUnit, to.yInUserInterfaceDimensionUnit)
    }

    fun smoothPath(path: List<Vector2>): List<Vector2> {
        if (path.size <= 2) return path
        
        val smoothed = mutableListOf<Vector2>()
        smoothed.add(path.first())
        
        var currentIndex = 0
        while (currentIndex < path.size - 1) {
            var farthestVisible = currentIndex + 1
            
            for (i in path.size - 1 downTo currentIndex + 2) {
                if (hasLineOfSight(path[currentIndex], path[i])) {
                    farthestVisible = i
                    break
                }
            }
            
            smoothed.add(path[farthestVisible])
            currentIndex = farthestVisible
        }
        
        return smoothed
    }
    
    private fun hasLineOfSight(from: Vector2, to: Vector2): Boolean {
        val grid = navGrid ?: return false
        val distance = from.dst(to)
        val steps = (distance / (grid.cellSize * 0.5f)).toInt().coerceAtLeast(1)
        
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = from.x + (to.x - from.x) * t
            val y = from.y + (to.y - from.y) * t
            
            if (!isWalkable(x, y)) {
                return false
            }
        }
        
        return true
    }

    fun updateObstaclesDynamic() {
        val currentObstacles = obstacles.keys.toList()
        for (name in currentObstacles) {
            removeObstacle(name)
            addObstacle(name)
        }
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
                follower.callback?.onPathCompleted(name)
                continue
            }
            val target = follower.waypoints[follower.currentIndex]

            var sprite: Sprite? = null
            for (s in stageListener.spritesFromStage) {
                if (s.name == name) { sprite = s; break }
            }
            val look = sprite?.look ?: continue

            val currentX = look.xInUserInterfaceDimensionUnit
            val currentY = look.yInUserInterfaceDimensionUnit
            val dx = target.x - currentX
            val dy = target.y - currentY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

            if (dist < follower.arrivalDistance) {
                follower.callback?.onWaypointReached(follower.currentIndex, target)
                follower.currentIndex++
                
                if (follower.enableDynamicReplanning && follower.currentIndex < follower.waypoints.size) {
                    val nextTarget = follower.waypoints[follower.currentIndex]
                    if (!isWalkable(nextTarget.x, nextTarget.y)) {
                        val replanResult = findPath(currentX, currentY, 
                            follower.waypoints.last().x, follower.waypoints.last().y)
                        if (replanResult.found) {
                            follower.waypoints = smoothPath(replanResult.points)
                            follower.currentIndex = 0
                        } else {
                            follower.callback?.onPathBlocked(name, "Path blocked, no alternative found")
                            follower.state = FollowState.IDLE
                        }
                    }
                }
                continue
            }

            val step = follower.speed * delta
            val ratio = (step / dist).coerceAtMost(1f)
            val newX = currentX + dx * ratio
            val newY = currentY + dy * ratio

            look.setPositionInUserInterfaceDimensionUnit(newX, newY)
            
            val targetAngle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
            val currentAngle = look.getMotionDirectionInUserInterfaceDimensionUnit()
            val angleDiff = ((targetAngle - currentAngle + 540f) % 360f) - 180f
            val maxRotation = follower.rotationSpeed * delta
            val rotation = angleDiff.coerceIn(-maxRotation, maxRotation)
            look.setMotionDirectionInUserInterfaceDimensionUnit(currentAngle + rotation)
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

    fun resize(width: Int, height: Int) {
        val grid = navGrid ?: return
        if (width == grid.width && height == grid.height) return
        navGrid = null
        createGrid(width, height, grid.cellSize, true, grid.offsetX, grid.offsetY)
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

    fun findPathWithSmoothing(startX: Float, startY: Float, endX: Float, endY: Float): PathResult {
        val result = findPath(startX, startY, endX, endY)
        return if (result.found) {
            PathResult(smoothPath(result.points), true)
        } else {
            result
        }
    }

    fun findPathToObjectWithSmoothing(fromSprite: String, targetSprite: String): PathResult {
        val result = findPathToObject(fromSprite, targetSprite)
        return if (result.found) {
            PathResult(smoothPath(result.points), true)
        } else {
            result
        }
    }

    fun setPathForFollowerWithSmoothing(spriteName: String, path: List<Vector2>) {
        val smoothedPath = smoothPath(path)
        setPathForFollower(spriteName, smoothedPath)
    }

    fun getGridInfo(): String {
        val grid = navGrid ?: return "No grid created"
        return "Grid: ${grid.width}x${grid.height}, cellSize: ${grid.cellSize}, offset: (${grid.offsetX}, ${grid.offsetY})"
    }

    fun getObstacleCount(): Int = obstacles.size

    fun getFollowerCount(): Int = followers.size

    fun getFollowerInfo(spriteName: String): String {
        val follower = followers[spriteName] ?: return "No follower for $spriteName"
        return "Follower: $spriteName, state: ${follower.state}, waypoints: ${follower.waypoints.size}, " +
                "current: ${follower.currentIndex}, speed: ${follower.speed}"
    }

    fun isPathWalkable(path: List<Vector2>): Boolean {
        for (point in path) {
            if (!isWalkable(point.x, point.y)) {
                return false
            }
        }
        return true
    }

    fun getNearestWalkablePoint(worldX: Float, worldY: Float, searchRadius: Float = 100f): Vector2? {
        val grid = navGrid ?: return null
        val step = grid.cellSize
        
        if (isWalkable(worldX, worldY)) {
            return Vector2(worldX, worldY)
        }
        
        var radius = step
        while (radius <= searchRadius) {
            for (angle in 0 until 360 step 45) {
                val rad = Math.toRadians(angle.toDouble())
                val testX = worldX + (kotlin.math.cos(rad) * radius).toFloat()
                val testY = worldY + (kotlin.math.sin(rad) * radius).toFloat()
                
                if (isWalkable(testX, testY)) {
                    return Vector2(testX, testY)
                }
            }
            radius += step
        }
        
        return null
    }

    fun debugPrintGrid() {
        val grid = navGrid ?: run {
            println("No grid created")
            return
        }
        
        println("Pathfinding Grid (${grid.width}x${grid.height}):")
        println("Cell size: ${grid.cellSize}, Offset: (${grid.offsetX}, ${grid.offsetY})")
        println("Obstacles: ${obstacles.size}, Followers: ${followers.size}")
        
        for (y in 0 until grid.height) {
            val row = StringBuilder()
            for (x in 0 until grid.width) {
                row.append(if (grid.walkable[x][y]) "." else "#")
            }
            println(row.toString())
        }
    }

    fun getWalkableAreaPercentage(): Float {
        val grid = navGrid ?: return 0f
        var walkableCount = 0
        val totalCells = grid.width * grid.height
        
        for (x in 0 until grid.width) {
            for (y in 0 until grid.height) {
                if (grid.walkable[x][y]) {
                    walkableCount++
                }
            }
        }
        
        return (walkableCount.toFloat() / totalCells) * 100f
    }

    fun clearAllFollowers() {
        followers.clear()
    }

    fun removeFollower(spriteName: String) {
        followers.remove(spriteName)
    }

    fun setFollowerCallback(spriteName: String, callback: PathCallback?) {
        followers[spriteName]?.callback = callback
    }

    fun setFollowerSpeed(spriteName: String, speed: Float) {
        followers[spriteName]?.speed = speed
    }

    fun setFollowerRotationSpeed(spriteName: String, rotationSpeed: Float) {
        followers[spriteName]?.rotationSpeed = rotationSpeed
    }

    fun setFollowerArrivalDistance(spriteName: String, distance: Float) {
        followers[spriteName]?.arrivalDistance = distance
    }

    fun enableDynamicReplanning(spriteName: String, enabled: Boolean) {
        followers[spriteName]?.enableDynamicReplanning = enabled
    }
}
