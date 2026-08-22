package org.catrobat.catroid.content

import android.util.Log
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.math.Vector2
import org.catrobat.catroid.stage.StageActivity
import java.util.PriorityQueue

class PathNode(
    var x: Int, var y: Int,
    var g: Float = 0f,
    var h: Float = 0f,
    var parent: PathNode? = null
) {
    val f: Float get() = g + h

    fun set(x: Int, y: Int, g: Float = 0f, h: Float = 0f, parent: PathNode? = null) {
        this.x = x
        this.y = y
        this.g = g
        this.h = h
        this.parent = parent
    }

    fun reset() {
        parent = null
        g = 0f
        h = 0f
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PathNode) return false
        return x == other.x && y == other.y
    }

    override fun hashCode(): Int = 31 * x + y
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
    var enableDynamicReplanning: Boolean = false,
    var targetName: String? = null,
    var stopOnTouch: Boolean = false,
    var sizeCheckMode: Int = 0,
    var blockedPathAction: Int = 0,
    var pathStrategy: Int = 0,
    var rotateObject: Boolean = true
) {
    @Volatile var replanInFlight: Boolean = false
}

class PathfindingManager {

    companion object {
        private const val TAG = "PathfindingManager"
        private const val MAX_ITERATIONS = 200000
    }

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
    private val pendingObstacleNames = mutableSetOf<String>()
    private val followers = mutableMapOf<String, PathFollower>()
    private val pathNodePool = mutableListOf<PathNode>()
    private val maxPoolSize = 5000

    private data class ObstacleState(val x: Float, val y: Float, val w: Float, val h: Float)
    private val lastObstacleState = mutableMapOf<String, ObstacleState>()
    private var obstacleUpdateAccumulator = 0f
    private val obstacleUpdateInterval = 0.5f

    private val pathExecutor = java.util.concurrent.Executors.newSingleThreadExecutor { r ->
        Thread(r, "pathfinder").apply { isDaemon = true }
    }

    private fun runOnRenderThread(block: () -> Unit) {
        val app = com.badlogic.gdx.Gdx.app
        if (app != null) {
            app.postRunnable(block)
        } else {
            block()
        }
    }

    fun findPathToObjectAsync(
        fromSprite: String,
        targetSprite: String,
        sizeCheckMode: Int = 0,
        blockedPathAction: Int = 0,
        pathStrategy: Int = 0,
        onResult: (PathResult) -> Unit
    ) {
        val from = findSpriteByName(fromSprite)?.look
        val to = findSpriteByName(targetSprite)?.look
        if (from == null || to == null) {
            onResult(PathResult(emptyList(), false))
            return
        }
        val sx = from.xInUserInterfaceDimensionUnit
        val sy = from.yInUserInterfaceDimensionUnit
        val ex = to.xInUserInterfaceDimensionUnit
        val ey = to.yInUserInterfaceDimensionUnit
        val sw = from.widthInUserInterfaceDimensionUnit
        val sh = from.heightInUserInterfaceDimensionUnit
        pathExecutor.execute {
            val result = try {
                findPath(sx, sy, ex, ey, sizeCheckMode, sw, sh, blockedPathAction, pathStrategy)
            } catch (e: Exception) {
                Log.e(TAG, "Async pathfinding failed", e)
                PathResult(emptyList(), false)
            }
            runOnRenderThread { onResult(result) }
        }
    }

    @Synchronized
    fun createGrid(width: Int, height: Int, cellSize: Float, defaultWalkable: Boolean = true,
                   offsetX: Float = 0f, offsetY: Float = 0f) {
        val walkable = Array(width) { BooleanArray(height) { defaultWalkable } }
        navGrid = NavGrid(width, height, cellSize, walkable, offsetX, offsetY)
        for ((_, points) in obstacles) {
            for (pt in points) {
                markObstacle(pt.x, pt.y, false)
            }
        }
        val pending = pendingObstacleNames.toList()
        pendingObstacleNames.clear()
        for (name in pending) {
            addObstacle(name)
        }
    }

    @Synchronized
    fun deleteGrid() {
        navGrid = null
        followers.clear()
        obstacles.clear()
    }

    private fun worldToCell(worldX: Float, worldY: Float): Pair<Int, Int>? {
        if (worldX.isNaN() || worldY.isNaN()) return null
        val grid = navGrid ?: return null
        val cx = kotlin.math.floor((worldX - grid.offsetX) / grid.cellSize).toInt()
        val cy = kotlin.math.floor((worldY - grid.offsetY) / grid.cellSize).toInt()
        if (cx in 0 until grid.width && cy in 0 until grid.height) {
            return cx to cy
        }
        return null
    }

    private fun snapshotGrid(): NavGrid? {
        val grid = navGrid ?: return null
        val walkable = Array(grid.width) { i -> grid.walkable[i].clone() }
        return NavGrid(grid.width, grid.height, grid.cellSize, walkable, grid.offsetX, grid.offsetY)
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

    @Synchronized
    fun addObstacle(name: String) {
        if (obstacles.containsKey(name)) return
        val grid = navGrid
        if (grid == null) {
            pendingObstacleNames.add(name)
            return
        }
        val sprite = findSpriteByName(name) ?: return
        val look = sprite.look ?: return
        val x = look.xInUserInterfaceDimensionUnit
        val y = look.yInUserInterfaceDimensionUnit
        val w = (look.widthInUserInterfaceDimensionUnit / 2f).coerceAtLeast(1f)
        val h = (look.heightInUserInterfaceDimensionUnit / 2f).coerceAtLeast(1f)
        val points = mutableListOf<Vector2>()
        val step = grid.cellSize

        val pixmap: Pixmap? = try {
            look.lookData?.getPixmap()
        } catch (e: Exception) {
            null
        }
        val pixW = pixmap?.width ?: 0
        val pixH = pixmap?.height ?: 0
        val spriteW = look.widthInUserInterfaceDimensionUnit.coerceAtLeast(1f)
        val spriteH = look.heightInUserInterfaceDimensionUnit.coerceAtLeast(1f)

        var iterations = 0
        val maxIterations = 100000
        var wx = x - w
        while (wx <= x + w) {
            var wy = y - h
            while (wy <= y + h) {
                if (++iterations > maxIterations) break
                var isOpaque = true

                if (pixmap != null && pixW > 1 && pixH > 1) {
                    val relX = (wx - (x - w)) / (spriteW)
                    val relY = (wy - (y - h)) / (spriteH)
                    val px = (relX * pixW).toInt().coerceIn(0, pixW - 1)
                    val py = ((1f - relY) * pixH).toInt().coerceIn(0, pixH - 1)
                    val pixel = pixmap.getPixel(px, py)
                    val alpha = pixel and 0xFF
                    isOpaque = alpha > 10
                }

                if (isOpaque) {
                    val pt = Vector2(wx, wy)
                    points.add(pt)
                    markObstacle(pt.x, pt.y, false)
                }
                wy += step
            }
            if (iterations > maxIterations) break
            wx += step
        }
        obstacles[name] = points
    }

    @Synchronized
    fun removeObstacle(name: String) {
        val points = obstacles.remove(name) ?: return
        for (pt in points) {
            markObstacle(pt.x, pt.y, true)
        }
    }

    fun isCellWalkableForSize(cx: Int, cy: Int, grid: NavGrid, sizeCheckMode: Int, spriteWidth: Float, spriteHeight: Float): Boolean {
        if (cx !in 0 until grid.width || cy !in 0 until grid.height) return false
        if (!grid.walkable[cx][cy]) return false
        if (sizeCheckMode == 0) return true
        
        val cellsW = kotlin.math.ceil(spriteWidth / grid.cellSize).toInt().coerceAtLeast(1)
        val cellsH = kotlin.math.ceil(spriteHeight / grid.cellSize).toInt().coerceAtLeast(1)
        val halfW = cellsW / 2
        val halfH = cellsH / 2
        
        for (dx in -halfW..halfW) {
            for (dy in -halfH..halfH) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx !in 0 until grid.width || ny !in 0 until grid.height || !grid.walkable[nx][ny]) {
                    return false
                }
            }
        }
        return true
    }

    @Synchronized
    private fun obtainNode(x: Int, y: Int, g: Float = 0f, h: Float = 0f, parent: PathNode? = null): PathNode {
        val node = if (pathNodePool.isNotEmpty()) pathNodePool.removeAt(pathNodePool.lastIndex) else PathNode(0, 0)
        node.set(x, y, g, h, parent)
        return node
    }

    @Synchronized
    private fun freeNode(node: PathNode) {
        if (pathNodePool.size < maxPoolSize) {
            node.reset()
            pathNodePool.add(node)
        }
    }

    fun findPath(
        startX: Float, startY: Float, endX: Float, endY: Float,
        sizeCheckMode: Int = 0, spriteWidth: Float = 0f, spriteHeight: Float = 0f,
        blockedPathAction: Int = 0, pathStrategy: Int = 0
    ): PathResult {
        val grid = snapshotGrid() ?: return PathResult(emptyList(), false)
        val cs = grid.cellSize
        var sx = kotlin.math.floor((startX - grid.offsetX) / cs).toInt().coerceIn(0, grid.width - 1)
        var sy = kotlin.math.floor((startY - grid.offsetY) / cs).toInt().coerceIn(0, grid.height - 1)
        var ex = kotlin.math.floor((endX - grid.offsetX) / cs).toInt().coerceIn(0, grid.width - 1)
        var ey = kotlin.math.floor((endY - grid.offsetY) / cs).toInt().coerceIn(0, grid.height - 1)

        if (!isCellWalkableForSize(sx, sy, grid, sizeCheckMode, spriteWidth, spriteHeight)) {
            val altStart = findNearestWalkableCell(sx, sy, grid, sizeCheckMode = sizeCheckMode, spriteWidth = spriteWidth, spriteHeight = spriteHeight)
            if (altStart != null) {
                sx = altStart.first
                sy = altStart.second
            } else {
                return PathResult(emptyList(), false)
            }
        }
        if (!isCellWalkableForSize(ex, ey, grid, sizeCheckMode, spriteWidth, spriteHeight)) {
            val altEnd = findNearestWalkableCell(ex, ey, grid, sizeCheckMode = sizeCheckMode, spriteWidth = spriteWidth, spriteHeight = spriteHeight)
            if (altEnd != null) {
                ex = altEnd.first
                ey = altEnd.second
            } else {
                return PathResult(emptyList(), false)
            }
        }

        val openSet = PriorityQueue<PathNode>(compareBy { it.f })
        val closedSet = mutableSetOf<Pair<Int, Int>>()
        val bestG = mutableMapOf<Pair<Int, Int>, Float>()
        val startKey = sx to sy
        val startNode = obtainNode(sx, sy)
        startNode.h = heuristic(sx, sy, ex, ey)
        openSet.add(startNode)
        bestG[startKey] = 0f

        var closestNode: PathNode? = startNode
        var minH = startNode.h
        var iterations = 0

        val dirs = arrayOf(
            0 to 1, 1 to 0, 0 to -1, -1 to 0,
            1 to 1, 1 to -1, -1 to 1, -1 to -1
        )

        while (openSet.isNotEmpty()) {
            if (++iterations > MAX_ITERATIONS) {
                Log.w(TAG, "A* exceeded $MAX_ITERATIONS iterations, returning best partial path")
                break
            }
            val current = openSet.poll() ?: break
            val key = current.x to current.y
            if (key in closedSet) continue
            closedSet.add(key)

            if (current.h < minH) {
                minH = current.h
                closestNode = current
            }

            if (current.x == ex && current.y == ey) {
                val result = PathResult(retracePath(current).map { Vector2(it.x * cs + cs / 2 + grid.offsetX, it.y * cs + cs / 2 + grid.offsetY) }, true)
                while (openSet.isNotEmpty()) { freeNode(openSet.poll()) }
                return result
            }

            for ((dx, dy) in dirs) {
                val nx = current.x + dx
                val ny = current.y + dy
                if (nx !in 0 until grid.width || ny !in 0 until grid.height) continue
                if (!isCellWalkableForSize(nx, ny, grid, sizeCheckMode, spriteWidth, spriteHeight)) continue
                if (nx to ny in closedSet) continue

                if (dx != 0 && dy != 0) {
                    if (!isCellWalkableForSize(current.x + dx, current.y, grid, sizeCheckMode, spriteWidth, spriteHeight) ||
                        !isCellWalkableForSize(current.x, current.y + dy, grid, sizeCheckMode, spriteWidth, spriteHeight)) {
                        continue
                    }
                }

                var moveCost = if (dx != 0 && dy != 0) 1.414f else 1f
                if (pathStrategy != 0) {
                    val penalty = proximityPenalty(nx, ny, grid)
                    moveCost += (if (pathStrategy == 2) 1.5f else 0.15f) * penalty
                }
                val g = current.g + moveCost
                val nKey = nx to ny
                val prevBest = bestG[nKey]
                if (prevBest != null && g >= prevBest) continue

                val h = heuristic(nx, ny, ex, ey)
                bestG[nKey] = g
                openSet.add(obtainNode(nx, ny, g, h, current))
            }
        }

        if ((blockedPathAction == 1 || iterations > MAX_ITERATIONS) && closestNode != null && closestNode != startNode) {
            val result = PathResult(retracePath(closestNode).map { Vector2(it.x * cs + cs / 2 + grid.offsetX, it.y * cs + cs / 2 + grid.offsetY) }, false)
            while (openSet.isNotEmpty()) { freeNode(openSet.poll()) }
            return result
        }

        while (openSet.isNotEmpty()) { freeNode(openSet.poll()) }
        return PathResult(emptyList(), false)
    }

    private fun heuristic(ax: Int, ay: Int, bx: Int, by: Int): Float {
        val dx = kotlin.math.abs(ax - bx).toFloat()
        val dy = kotlin.math.abs(ay - by).toFloat()
        return (dx + dy) + (1.414f - 2f) * minOf(dx, dy)
    }

    private fun proximityPenalty(cx: Int, cy: Int, grid: NavGrid): Float {
        var blocked = 0
        var count = 0
        for (dx in -1..1) {
            for (dy in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 0 until grid.width && ny in 0 until grid.height) {
                    if (!grid.walkable[nx][ny]) blocked++
                    count++
                }
            }
        }
        return if (count == 0) 0f else blocked.toFloat() / count
    }

    private fun findNearestWalkableCell(
        cx: Int, cy: Int, grid: NavGrid,
        maxRadius: Int = kotlin.math.max(grid.width, grid.height) / 4,
        sizeCheckMode: Int = 0, spriteWidth: Float = 0f, spriteHeight: Float = 0f
    ): Pair<Int, Int>? {
        if (isCellWalkableForSize(cx, cy, grid, sizeCheckMode, spriteWidth, spriteHeight)) {
            return cx to cy
        }
        val effectiveRadius = maxRadius.coerceIn(10, 100)
        for (r in 1..effectiveRadius) {
            for (dx in -r..r) {
                for (dy in -r..r) {
                    if (kotlin.math.abs(dx) != r && kotlin.math.abs(dy) != r) continue
                    val nx = cx + dx
                    val ny = cy + dy
                    if (isCellWalkableForSize(nx, ny, grid, sizeCheckMode, spriteWidth, spriteHeight)) {
                        return nx to ny
                    }
                }
            }
        }
        return null
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

    fun setPathForFollowerWithTarget(spriteName: String, path: List<Vector2>, targetSpriteName: String) {
        val follower = followers.getOrPut(spriteName) { PathFollower(spriteName) }
        val targetSprite = findSpriteByName(targetSpriteName)
        val targetLook = targetSprite?.look
        if (targetLook != null) {
            val finalPoint = Vector2(targetLook.xInUserInterfaceDimensionUnit, targetLook.yInUserInterfaceDimensionUnit)
            val points = path.toMutableList()
            if (points.isEmpty() || points.last().dst(finalPoint) > 1f) {
                val followerLook = findSpriteByName(spriteName)?.look
                val sw = followerLook?.widthInUserInterfaceDimensionUnit ?: 0f
                val sh = followerLook?.heightInUserInterfaceDimensionUnit ?: 0f
                val from = if (points.isNotEmpty()) points.last()
                           else Vector2(followerLook?.xInUserInterfaceDimensionUnit ?: finalPoint.x,
                                        followerLook?.yInUserInterfaceDimensionUnit ?: finalPoint.y)
                if (sw <= 0f || sh <= 0f || hasLineOfSight(from, finalPoint, snapshotGrid() ?: return, follower.sizeCheckMode, sw, sh)) {
                    points.add(finalPoint)
                }
            }
            follower.waypoints = points
        } else {
            follower.waypoints = path
        }
        follower.currentIndex = 0
        follower.state = if (follower.waypoints.isNotEmpty()) FollowState.FOLLOWING else FollowState.REACHED
    }

    fun setFollowerTarget(spriteName: String, targetName: String?) {
        followers.getOrPut(spriteName) { PathFollower(spriteName) }.targetName = targetName
    }

    fun setFollowerStopOnTouch(spriteName: String, enabled: Boolean) {
        followers.getOrPut(spriteName) { PathFollower(spriteName) }.stopOnTouch = enabled
    }

    fun setFollowerSizeCheckMode(spriteName: String, sizeCheckMode: Int) {
        followers.getOrPut(spriteName) { PathFollower(spriteName) }.sizeCheckMode = sizeCheckMode
    }

    fun setFollowerBlockedPathAction(spriteName: String, action: Int) {
        followers.getOrPut(spriteName) { PathFollower(spriteName) }.blockedPathAction = action
    }

    fun setFollowerRotateObject(spriteName: String, enabled: Boolean) {
        followers.getOrPut(spriteName) { PathFollower(spriteName) }.rotateObject = enabled
    }

    fun setFollowerDynamicReplanning(spriteName: String, enabled: Boolean) {
        followers.getOrPut(spriteName) { PathFollower(spriteName) }.enableDynamicReplanning = enabled
    }

    fun setFollowerPathStrategy(spriteName: String, strategy: Int) {
        followers.getOrPut(spriteName) { PathFollower(spriteName) }.pathStrategy = strategy
    }

    fun smoothPath(
        path: List<Vector2>,
        sizeCheckMode: Int = 0,
        spriteWidth: Float = 0f,
        spriteHeight: Float = 0f
    ): List<Vector2> {
        if (path.size <= 2) return path

        val grid = snapshotGrid() ?: return path
        
        val smoothed = mutableListOf<Vector2>()
        smoothed.add(path.first())
        
        var currentIndex = 0
        while (currentIndex < path.size - 1) {
            var farthestVisible = currentIndex + 1
            
            val maxCheck = kotlin.math.min(path.size - 1, currentIndex + 32)
            for (i in maxCheck downTo currentIndex + 2) {
                if (hasLineOfSight(path[currentIndex], path[i], grid, sizeCheckMode, spriteWidth, spriteHeight)) {
                    farthestVisible = i
                    break
                }
            }
            
            if (farthestVisible == currentIndex + 1) {
                smoothed.add(path[++currentIndex])
                continue
            }
            
            smoothed.add(path[farthestVisible])
            currentIndex = farthestVisible
        }
        
        return smoothed
    }
    
    private fun hasLineOfSight(
        from: Vector2,
        to: Vector2,
        grid: NavGrid,
        sizeCheckMode: Int = 0,
        spriteWidth: Float = 0f,
        spriteHeight: Float = 0f
    ): Boolean {
        val distance = from.dst(to)
        val steps = (distance / (grid.cellSize * 0.5f)).toInt().coerceAtLeast(1)
        
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = from.x + (to.x - from.x) * t
            val y = from.y + (to.y - from.y) * t
            
            val cell = worldToCell(x, y) ?: return false
            if (!isCellWalkableForSize(cell.first, cell.second, grid, sizeCheckMode, spriteWidth, spriteHeight)) {
                return false
            }
        }
        
        return true
    }

    fun updateObstaclesDynamic() {
        val currentObstacles = obstacles.keys.toList()
        val currentSet = currentObstacles.toHashSet()
        val iterator = lastObstacleState.keys.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() !in currentSet) iterator.remove()
        }
        for (name in currentObstacles) {
            val sprite = findSpriteByName(name)
            val look = sprite?.look
            if (look == null) {
                lastObstacleState.remove(name)
                removeObstacle(name)
                continue
            }
            val x = look.xInUserInterfaceDimensionUnit
            val y = look.yInUserInterfaceDimensionUnit
            val w = look.widthInUserInterfaceDimensionUnit
            val h = look.heightInUserInterfaceDimensionUnit
            val prev = lastObstacleState[name]
            if (prev != null && prev.x == x && prev.y == y && prev.w == w && prev.h == h) continue
            lastObstacleState[name] = ObstacleState(x, y, w, h)
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

    @Synchronized
    fun update(delta: Float) {
        var followerActive = false
        for (f in followers.values) {
            if (f.state == FollowState.FOLLOWING) {
                followerActive = true
                break
            }
        }
        if (!followerActive && obstacles.isEmpty()) return

        val stageListener = StageActivity.getActiveStageListener() ?: return
        val stageSprites = stageListener.spritesFromStage.toList()
        val spritesByName = HashMap<String, Sprite>(stageSprites.size * 2)
        for (s in stageSprites) {
            spritesByName[s.name] = s
        }

        obstacleUpdateAccumulator += delta
        if (obstacleUpdateAccumulator >= obstacleUpdateInterval) {
            obstacleUpdateAccumulator = 0f
            updateObstaclesDynamic()
        }

        for ((name, follower) in followers) {
            if (follower.state != FollowState.FOLLOWING) continue

            val look = spritesByName[name]?.look

            if (follower.currentIndex >= follower.waypoints.size) {
                if (look != null && follower.enableDynamicReplanning && follower.targetName != null && !follower.replanInFlight) {
                    val tLook = findSpriteByName(follower.targetName!!)?.look
                    if (tLook != null) {
                        val tPos = Vector2(tLook.xInUserInterfaceDimensionUnit, tLook.yInUserInterfaceDimensionUnit)
                        val from = Vector2(look.xInUserInterfaceDimensionUnit, look.yInUserInterfaceDimensionUnit)
                        val sw = look.widthInUserInterfaceDimensionUnit
                        val sh = look.heightInUserInterfaceDimensionUnit
                        if (!hasLineOfSight(from, tPos, snapshotGrid() ?: return, follower.sizeCheckMode, sw, sh)) {
                            follower.replanInFlight = true
                            val scm = follower.sizeCheckMode
                            val bpa = follower.blockedPathAction
                            val pst = follower.pathStrategy
                            pathExecutor.execute {
                                val replanResult = try {
                                    findPath(from.x, from.y, tPos.x, tPos.y, scm, sw, sh, bpa, pst)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Async target replan failed", e)
                                    PathResult(emptyList(), false)
                                }
                                val newWaypoints = if (replanResult.found || (bpa == 1 && replanResult.points.isNotEmpty())) {
                                    var wp = smoothPath(replanResult.points, scm, sw, sh)
                                    if (wp.isNotEmpty() && wp.last().dst(tPos) > 1f
                                        && hasLineOfSight(wp.last(), tPos, snapshotGrid() ?: return@execute, scm, sw, sh)) {
                                        wp = wp.toMutableList().also { it.add(tPos) }
                                    }
                                    wp
                                } else {
                                    null
                                }
                                runOnRenderThread {
                                    follower.replanInFlight = false
                                    val stalePath = newWaypoints != null && newWaypoints.isNotEmpty()
                                            && follower.waypoints.isNotEmpty()
                                            && follower.waypoints.last().dst(newWaypoints.last()) < 1f
                                    if (newWaypoints != null && newWaypoints.isNotEmpty() && !stalePath) {
                                        follower.waypoints = newWaypoints
                                        follower.currentIndex = 0
                                    } else {
                                        follower.state = FollowState.REACHED
                                        follower.callback?.onPathCompleted(name)
                                    }
                                }
                            }
                            continue
                        }
                    }
                }
                follower.state = FollowState.REACHED
                follower.callback?.onPathCompleted(name)
                continue
            }
            val target = follower.waypoints[follower.currentIndex]
            if (look == null) continue

            if (follower.stopOnTouch && follower.targetName != null) {
                val targetSprite = findSpriteByName(follower.targetName!!)
                val targetLook = targetSprite?.look
                if (targetLook != null) {
                    val sx = look.xInUserInterfaceDimensionUnit
                    val sy = look.yInUserInterfaceDimensionUnit
                    val tx = targetLook.xInUserInterfaceDimensionUnit
                    val ty = targetLook.yInUserInterfaceDimensionUnit
                    val sw = look.widthInUserInterfaceDimensionUnit / 2f
                    val sh = look.heightInUserInterfaceDimensionUnit / 2f
                    val tw = targetLook.widthInUserInterfaceDimensionUnit / 2f
                    val th = targetLook.heightInUserInterfaceDimensionUnit / 2f
                    if (kotlin.math.abs(sx - tx) < sw + tw && kotlin.math.abs(sy - ty) < sh + th) {
                        follower.state = FollowState.REACHED
                        follower.callback?.onPathCompleted(name)
                        continue
                    }
                }
            }

            val currentX = look.xInUserInterfaceDimensionUnit
            val currentY = look.yInUserInterfaceDimensionUnit

            val grid = navGrid ?: continue
            val targetCell = worldToCell(target.x, target.y)

            if (targetCell == null || !isCellWalkableForSize(targetCell.first, targetCell.second, grid, follower.sizeCheckMode, look.widthInUserInterfaceDimensionUnit, look.heightInUserInterfaceDimensionUnit)) {
                if (!follower.replanInFlight) {
                    follower.replanInFlight = true
                    val finalTarget = if (follower.targetName != null) {
                        val tSprite = findSpriteByName(follower.targetName!!)
                        val tLook = tSprite?.look
                        if (tLook != null) Vector2(tLook.xInUserInterfaceDimensionUnit, tLook.yInUserInterfaceDimensionUnit) else follower.waypoints.lastOrNull()
                    } else follower.waypoints.lastOrNull()
                    val endX = finalTarget?.x ?: target.x
                    val endY = finalTarget?.y ?: target.y
                    val scm = follower.sizeCheckMode
                    val bpa = follower.blockedPathAction
                    val pst = follower.pathStrategy
                    val sw = look.widthInUserInterfaceDimensionUnit
                    val sh = look.heightInUserInterfaceDimensionUnit
                    pathExecutor.execute {
                        val replanResult = try {
                            findPath(currentX, currentY, endX, endY, scm, sw, sh, bpa, pst)
                        } catch (e: Exception) {
                            Log.e(TAG, "Async replan failed", e)
                            PathResult(emptyList(), false)
                        }
                        val newWaypoints = if (replanResult.found || (bpa == 1 && replanResult.points.isNotEmpty())) {
                            var wp = smoothPath(replanResult.points, scm, sw, sh)
                            if (finalTarget != null && wp.isNotEmpty() && wp.last().dst(finalTarget) > 1f
                                && hasLineOfSight(wp.last(), finalTarget, snapshotGrid() ?: return@execute, scm, sw, sh)) {
                                wp = wp.toMutableList().also { it.add(finalTarget) }
                            }
                            wp
                        } else {
                            null
                        }
                        runOnRenderThread {
                            follower.replanInFlight = false
                            val stalePath = newWaypoints != null && newWaypoints.isNotEmpty()
                                    && follower.waypoints.isNotEmpty()
                                    && follower.waypoints.last().dst(newWaypoints.last()) < 1f
                            if (newWaypoints != null && newWaypoints.isNotEmpty() && !stalePath) {
                                follower.waypoints = newWaypoints
                                follower.currentIndex = 0
                            } else {
                                follower.callback?.onPathBlocked(name, "Path blocked, no alternative found")
                                follower.state = FollowState.IDLE
                            }
                        }
                    }
                }
                continue
            }

            val dx = target.x - currentX
            val dy = target.y - currentY
            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

            if (dist < follower.arrivalDistance) {
                follower.callback?.onWaypointReached(follower.currentIndex, target)
                follower.currentIndex++
                
                if (follower.enableDynamicReplanning && follower.currentIndex < follower.waypoints.size) {
                    val nextTarget = follower.waypoints[follower.currentIndex]
                    val nextCell = worldToCell(nextTarget.x, nextTarget.y)
                    if (nextCell == null || !isCellWalkableForSize(nextCell.first, nextCell.second, grid, follower.sizeCheckMode, look.widthInUserInterfaceDimensionUnit, look.heightInUserInterfaceDimensionUnit)) {
                        if (!follower.replanInFlight) {
                            follower.replanInFlight = true
                            val endX = follower.waypoints.last().x
                            val endY = follower.waypoints.last().y
                            val scm = follower.sizeCheckMode
                            val bpa = follower.blockedPathAction
                            val pst = follower.pathStrategy
                            val sw = look.widthInUserInterfaceDimensionUnit
                            val sh = look.heightInUserInterfaceDimensionUnit
                            pathExecutor.execute {
                                val replanResult = try {
                                    findPath(currentX, currentY, endX, endY, scm, sw, sh, bpa, pst)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Async dynamic replan failed", e)
                                    PathResult(emptyList(), false)
                                }
                                val newWaypoints = if (replanResult.found || (bpa == 1 && replanResult.points.isNotEmpty())) {
                                    smoothPath(replanResult.points, scm, sw, sh)
                                } else {
                                    null
                                }
                                runOnRenderThread {
                                    follower.replanInFlight = false
                                    val stalePath = newWaypoints != null && newWaypoints.isNotEmpty()
                                            && follower.waypoints.isNotEmpty()
                                            && follower.waypoints.last().dst(newWaypoints.last()) < 1f
                                    if (newWaypoints != null && newWaypoints.isNotEmpty() && !stalePath) {
                                        follower.waypoints = newWaypoints
                                        follower.currentIndex = 0
                                    } else {
                                        follower.callback?.onPathBlocked(name, "Path blocked, no alternative found")
                                        follower.state = FollowState.IDLE
                                    }
                                }
                            }
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
            
            if (follower.rotateObject) {
                val targetAngle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                val currentAngle = look.getMotionDirectionInUserInterfaceDimensionUnit()
                val angleDiff = ((targetAngle - currentAngle + 540f) % 360f) - 180f
                val maxRotation = follower.rotationSpeed * delta
                val rotation = angleDiff.coerceIn(-maxRotation, maxRotation)
                look.setMotionDirectionInUserInterfaceDimensionUnit(currentAngle + rotation)
            }
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

    @Synchronized
    fun clearScene() {
        navGrid = null
        obstacles.clear()
        followers.clear()
        lastObstacleState.clear()
        obstacleUpdateAccumulator = 0f
    }

    fun dispose() {
        clearScene()
    }

    fun findPathWithSmoothing(
        startX: Float, startY: Float, endX: Float, endY: Float,
        sizeCheckMode: Int = 0, spriteWidth: Float = 0f, spriteHeight: Float = 0f
    ): PathResult {
        val result = findPath(startX, startY, endX, endY, sizeCheckMode, spriteWidth, spriteHeight)
        if (result.found && result.points.isNotEmpty()) {
            val finalPoint = Vector2(endX, endY)
            val points = result.points.toMutableList()
            if (points.last().dst(finalPoint) > 1f) {
                points.add(finalPoint)
            }
            return PathResult(smoothPath(points, sizeCheckMode, spriteWidth, spriteHeight), true)
        }
        return result
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
