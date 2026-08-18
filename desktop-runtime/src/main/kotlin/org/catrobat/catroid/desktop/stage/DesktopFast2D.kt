package org.catrobat.catroid.desktop.stage

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.Filter
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.World
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Fast2D-слой: сущности по строковому ID со своей box2d-физикой.
 * Зеркало Android FastTwoDManager + Fast2DRenderSystem (PPM = 100).
 */
class DesktopFast2DEntity {
    var x = 0f
    var y = 0f
    var rotation = 0f
    var scaleX = 1f
    var scaleY = 1f
    var z = 0f
    var textureName: String? = null
    var color = Color(1f, 1f, 1f, 1f)
    var velocityX = 0f
    var velocityY = 0f
    var angularVelocity = 0f
    var body: Body? = null
    var shapeType = "BOX"
    var density = 1f
    var friction = 0.5f
    var bounce = 0f
    var isDynamic = true
    var isSensor = false
    var groupIndex = 0
}

class DesktopFast2D {

    companion object {
        const val PPM = 100f
        private const val DEFAULT_HALF = 25f
    }

    val entities = mutableMapOf<String, DesktopFast2DEntity>()
    var cameraX = 0f
    var cameraY = 0f
    var cameraZoom = 1f

    /** Возвращает world-касание главной сцены (worldX, worldY) или null, если нет касания. */
    var touchProvider: (() -> Pair<Float, Float>?)? = null

    /** Резолвер текстур (engine.textures) для isTouched/рендера. */
    var textureResolver: ((String) -> Texture?)? = null

    private var world: World? = null

    init {
        world = World(Vector2(0f, -9.8f * PPM), true)
    }

    fun getOrCreate(id: String): DesktopFast2DEntity {
        return entities.getOrPut(id) { DesktopFast2DEntity() }
    }

    fun createEntity(id: String) {
        getOrCreate(id)
    }

    fun destroyEntity(id: String) {
        val e = entities.remove(id) ?: return
        val b = e.body
        if (b != null) {
            try {
                world?.destroyBody(b)
            } catch (_: Exception) {}
        }
    }

    fun setPosition(id: String, x: Float, y: Float) {
        val e = getOrCreate(id)
        e.x = x
        e.y = y
        val b = e.body
        if (b != null) {
            b.setTransform(x / PPM, y / PPM, b.angle)
            b.setAwake(true)
        }
    }

    fun setRotation(id: String, angle: Float) {
        val e = getOrCreate(id)
        e.rotation = angle
        val b = e.body
        if (b != null) {
            b.setTransform(b.position, Math.toRadians(angle.toDouble()).toFloat())
            b.setAwake(true)
        }
    }

    fun setScale(id: String, sx: Float, sy: Float) {
        val e = getOrCreate(id)
        e.scaleX = sx
        e.scaleY = sy
        rebuildFixture(e)
    }

    fun setZIndex(id: String, z: Float) {
        getOrCreate(id).z = z
    }

    fun setTexture(id: String, fileName: String) {
        val e = getOrCreate(id)
        e.textureName = fileName
        rebuildFixture(e)
    }

    fun setColor(id: String, r: Float, g: Float, b: Float, a: Float) {
        val e = getOrCreate(id)
        e.color.set(r / 255f, g / 255f, b / 255f, a / 100f)
    }

    fun setVelocity(id: String, vx: Float, vy: Float) {
        val e = getOrCreate(id)
        e.velocityX = vx
        e.velocityY = vy
    }

    fun setAngularVelocity(id: String, av: Float) {
        getOrCreate(id).angularVelocity = av
    }

    fun makePhysicsBody(id: String, isDynamic: Boolean, shape: String, density: Float, friction: Float, bounce: Float) {
        val e = getOrCreate(id)
        val b = e.body
        if (b != null) {
            try {
                world?.destroyBody(b)
            } catch (_: Exception) {}
        }
        e.isDynamic = isDynamic
        e.shapeType = shape
        e.density = density
        e.friction = friction
        e.bounce = bounce

        val bodyDef = BodyDef()
        bodyDef.type = if (isDynamic) BodyDef.BodyType.DynamicBody else BodyDef.BodyType.StaticBody
        bodyDef.position.set(e.x / PPM, e.y / PPM)
        bodyDef.angle = Math.toRadians(e.rotation.toDouble()).toFloat()
        bodyDef.awake = true
        bodyDef.allowSleep = true

        val body = world?.createBody(bodyDef) ?: return
        e.body = body
        rebuildFixture(e)
    }

    private fun fixtureSize(e: DesktopFast2DEntity, texture: Texture?): Pair<Float, Float> {
        var w = DEFAULT_HALF
        var h = DEFAULT_HALF
        if (texture != null) {
            w = abs(texture.width * e.scaleX) / 2f
            h = abs(texture.height * e.scaleY) / 2f
        }
        if (w < 0.1f) w = 0.1f
        if (h < 0.1f) h = 0.1f
        return w to h
    }

    private fun rebuildFixture(e: DesktopFast2DEntity) {
        val body = e.body ?: return
        val fixtures = body.fixtureList
        while (fixtures.size > 0) {
            body.destroyFixture(fixtures.first())
        }

        val (w, h) = fixtureSize(e, null)
        val shape = if ("CIRCLE".equals(e.shapeType, ignoreCase = true)) {
            CircleShape().apply { radius = max(w, h) / PPM }
        } else {
            PolygonShape().apply { setAsBox(w / PPM, h / PPM) }
        }

        val fdef = FixtureDef()
        fdef.shape = shape
        fdef.density = e.density
        fdef.friction = e.friction
        fdef.restitution = e.bounce
        val fixture = body.createFixture(fdef)
        fixture.isSensor = e.isSensor
        val filter = Filter()
        filter.groupIndex = e.groupIndex.toShort()
        fixture.filterData = filter
        shape.dispose()

        if (e.isDynamic) {
            body.resetMassData()
        }
        body.setAwake(true)
    }

    fun applyForce(id: String, fx: Float, fy: Float) {
        val e = entities[id] ?: return
        e.body?.applyForceToCenter(Vector2(fx, fy), true)
    }

    fun applyImpulse(id: String, ix: Float, iy: Float) {
        val e = entities[id] ?: return
        val b = e.body ?: return
        val center = b.worldCenter
        b.applyLinearImpulse(ix, iy, center.x, center.y, true)
    }

    fun setPhysicsVelocity(id: String, vx: Float, vy: Float) {
        entities[id]?.body?.setLinearVelocity(vx, vy)
    }

    fun setCollisionFilter(id: String, sensor: Boolean, groupIndex: Int) {
        val e = getOrCreate(id)
        e.isSensor = sensor
        e.groupIndex = groupIndex
        val b = e.body ?: return
        for (fixture in b.fixtureList) {
            fixture.isSensor = sensor
            val filter = fixture.filterData
            filter.groupIndex = groupIndex.toShort()
            fixture.filterData = filter
        }
        b.setAwake(true)
    }

    fun setCamera(x: Float, y: Float, zoom: Float) {
        cameraX = x
        cameraY = y
        cameraZoom = if (zoom <= 0f) 1f else zoom
    }

    fun setGravity(gx: Float, gy: Float) {
        world?.gravity?.set(gx, gy)
    }

    fun isTouched(id: String, pointer: Int = 0): Boolean {
        if (pointer != 0) return false
        val e = entities[id] ?: return false
        val touch = touchProvider?.invoke() ?: return false
        // world-касание главной камеры -> координаты fast2d
        val fx = touch.first / cameraZoom + cameraX
        val fy = touch.second / cameraZoom + cameraY

        var dx = fx - e.x
        var dy = fy - e.y
        if (e.rotation != 0f) {
            val rad = Math.toRadians((-e.rotation).toDouble())
            val c = Math.cos(rad)
            val s = Math.sin(rad)
            val rx = dx * c - dy * s
            val ry = dx * s + dy * c
            dx = rx.toFloat()
            dy = ry.toFloat()
        }
        val tex = e.textureName?.let { textureResolver?.invoke(it) }
        val halfW = if (tex != null) abs(e.scaleX) * (tex.width / 2f) else DEFAULT_HALF
        val halfH = if (tex != null) abs(e.scaleY) * (tex.height / 2f) else DEFAULT_HALF
        return dx >= -halfW && dx <= halfW && dy >= -halfH && dy <= halfH
    }

    fun getX(id: String) = entities[id]?.x ?: 0f
    fun getY(id: String) = entities[id]?.y ?: 0f
    fun getRotation(id: String) = entities[id]?.rotation ?: 0f
    fun getScaleX(id: String) = entities[id]?.scaleX ?: 1f
    fun getScaleY(id: String) = entities[id]?.scaleY ?: 1f
    fun getTextureName(id: String) = entities[id]?.textureName ?: ""
    fun getColorR(id: String) = (entities[id]?.color?.r ?: 1f) * 255f
    fun getColorG(id: String) = (entities[id]?.color?.g ?: 1f) * 255f
    fun getColorB(id: String) = (entities[id]?.color?.b ?: 1f) * 255f
    fun getAlpha(id: String) = (entities[id]?.color?.a ?: 1f) * 100f
    fun getCamX() = cameraX
    fun getCamY() = cameraY
    fun getCamZoom() = cameraZoom

    fun step(dt: Float) {
        val w = world ?: return
        if (dt <= 0f) return

        for (e in entities.values) {
            val b = e.body
            if (b == null) {
                e.x += e.velocityX * dt
                e.y += e.velocityY * dt
                e.rotation += e.angularVelocity * dt
            }
        }

        w.step(dt, 6, 2)

        for (e in entities.values) {
            val b = e.body ?: continue
            e.x = b.position.x * PPM
            e.y = b.position.y * PPM
            e.rotation = Math.toDegrees(b.angle.toDouble()).toFloat()
        }
    }

    fun clear() {
        val w = world ?: return
        for (e in entities.values) {
            val b = e.body
            if (b != null) {
                try {
                    w.destroyBody(b)
                } catch (_: Exception) {}
            }
        }
        entities.clear()
        cameraX = 0f
        cameraY = 0f
        cameraZoom = 1f
    }

    fun render(batch: SpriteBatch, textures: Map<String, Texture>, virtualWidth: Float, virtualHeight: Float) {
        if (entities.isEmpty()) return
        batch.begin()
        val white = Color.WHITE
        for (e in entities.values.sortedBy { it.z }) {
            val texName = e.textureName ?: continue
            val texture = resolveTexture(textures, texName) ?: continue
            val w = texture.width * abs(e.scaleX)
            val h = texture.height * abs(e.scaleY)
            if (w < 1f || h < 1f) continue
            val sx = virtualWidth / 2f + (e.x - cameraX) * cameraZoom
            val sy = virtualHeight / 2f + (e.y - cameraY) * cameraZoom
            batch.setColor(e.color)
            batch.draw(
                texture,
                sx - w / 2f, sy - h / 2f,
                w / 2f, h / 2f,
                w, h,
                1f, 1f,
                e.rotation,
                0, 0,
                texture.width, texture.height,
                false, false
            )
        }
        batch.setColor(white)
        batch.end()
    }

    private fun resolveTexture(textures: Map<String, Texture>, name: String): Texture? {
        textures[name]?.let { return it }
        return textures.entries.firstOrNull { it.key.endsWith(name) || name.endsWith(it.key) }?.value
    }

    fun dispose() {
        clear()
        world?.dispose()
        world = null
    }
}