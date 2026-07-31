package org.catrobat.catroid.stage

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.utils.Array

class DesktopPhysicsWorld(
    private var gravityX: Float = 0f,
    private var gravityY: Float = -9.8f
) {
    val world: World = World(Vector2(gravityX, gravityY), true)
    private val bodiesBySprite = mutableMapOf<DesktopSprite, Body>()
    private val customHitboxSprites = mutableSetOf<DesktopSprite>()
    private var accumulator = 0f
    private val timeStep = 1 / 60f
    private val maxSteps = 5

    fun createBodyForSprite(sprite: DesktopSprite, isStatic: Boolean = false): Body {
        val def = BodyDef().apply {
            type = if (isStatic) BodyDef.BodyType.StaticBody else BodyDef.BodyType.DynamicBody
            position.set(sprite.x, sprite.y)
            angle = Math.toRadians((90.0 - sprite.direction).toDouble()).toFloat()
            linearDamping = 0.5f
        }
        val body = world.createBody(def)
        bodiesBySprite[sprite] = body
        if (applyCustomHitboxes(sprite)) {
            return body
        }
        if (isStatic) {
            val w = if (sprite.width > 0f) sprite.width / 100f else sprite.size / 100f
            val h = if (sprite.height > 0f) sprite.height / 100f else sprite.size / 100f
            val shape = PolygonShape()
            shape.setAsBox(maxOf(w, 0.5f) / 2f, maxOf(h, 0.5f) / 2f)
            body.createFixture(shape, 1f)
            shape.dispose()
        } else {
            val shape = CircleShape().apply { radius = maxOf(sprite.size / 200f, 0.25f) }
            body.createFixture(shape, 1f)
            shape.dispose()
            body.setBullet(true)
        }
        return body
    }

    fun applyCustomHitboxes(sprite: DesktopSprite, look: DesktopLook? = null): Boolean {
        val body = bodiesBySprite[sprite] ?: return false
        val sourceLook = look ?: sprite.currentLook() ?: return false
        val boxes = sourceLook.hitboxes
        if (boxes.isEmpty()) return false

        for (f in body.fixtureList.toList()) {
            body.destroyFixture(f)
        }

        val sx = sprite.size / 100f * sprite.scaleX
        val sy = sprite.size / 100f * sprite.scaleY
        for (hb in boxes) {
            if (hb.width <= 0f || hb.height <= 0f) continue
            val hw = hb.width / 2f
            val hh = hb.height / 2f
            val rad = Math.toRadians(hb.rotation.toDouble())
            val cos = Math.cos(rad).toFloat()
            val sin = Math.sin(rad).toFloat()
            val lx = floatArrayOf(-hw, hw, hw, -hw)
            val ly = floatArrayOf(-hh, -hh, hh, hh)
            val verts = Array(4) { i ->
                val rx = lx[i] * cos - ly[i] * sin
                val ry = lx[i] * sin + ly[i] * cos
                Vector2((hb.x + rx) * sx, -(hb.y + ry) * sy)
            }
            val shape = PolygonShape()
            shape.set(verts)
            body.createFixture(shape, 1f)
            shape.dispose()
        }

        customHitboxSprites.add(sprite)
        if (body.type == BodyDef.BodyType.DynamicBody) {
            body.setBullet(true)
        }
        return true
    }

    fun getBody(sprite: DesktopSprite): Body? = bodiesBySprite[sprite]

    fun removeBody(sprite: DesktopSprite) {
        val body = bodiesBySprite.remove(sprite)
        if (body != null) {
            world.destroyBody(body)
        }
    }

    fun hasBody(sprite: DesktopSprite): Boolean = bodiesBySprite.containsKey(sprite)

    fun ensureBody(sprite: DesktopSprite, isStatic: Boolean = false): Body {
        return bodiesBySprite.getOrPut(sprite) { createBodyForSprite(sprite, isStatic) }
    }

    fun setGravity(x: Float, y: Float) {
        gravityX = x
        gravityY = y
        world.setGravity(Vector2(x, y))
    }

    fun applyForce(sprite: DesktopSprite, fx: Float, fy: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyForceToCenter(fx, fy, true)
    }

    fun applyImpulse(sprite: DesktopSprite, ix: Float, iy: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyLinearImpulse(ix, iy, body.worldCenter.x, body.worldCenter.y, true)
    }

    fun applyTorque(sprite: DesktopSprite, torque: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyTorque(torque, true)
    }

    fun applyAngularImpulse(sprite: DesktopSprite, impulse: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyAngularImpulse(impulse, true)
    }

    fun setFriction(sprite: DesktopSprite, friction: Float) {
        val body = bodiesBySprite[sprite] ?: return
        for (fixture in body.fixtureList) {
            fixture.friction = friction
        }
    }

    fun setBounce(sprite: DesktopSprite, bounce: Float) {
        val body = bodiesBySprite[sprite] ?: return
        for (fixture in body.fixtureList) {
            fixture.restitution = bounce
        }
    }

    fun setMass(sprite: DesktopSprite, mass: Float) {
        val body = bodiesBySprite[sprite] ?: return
        if (mass <= 0f) return
        val massData = body.massData
        massData.mass = mass
        body.massData = massData
    }

    fun setDamping(sprite: DesktopSprite, linear: Float, angular: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.linearDamping = linear
        body.angularDamping = angular
    }

    fun setBodyType(sprite: DesktopSprite, type: BodyDef.BodyType) {
        val body = bodiesBySprite[sprite] ?: return
        val oldType = body.type
        body.type = type
        if (oldType != type && sprite !in customHitboxSprites) {
            for (f in body.fixtureList.toList()) {
                body.destroyFixture(f)
            }
            if (type == BodyDef.BodyType.StaticBody || type == BodyDef.BodyType.KinematicBody) {
                val w = if (sprite.width > 0f) sprite.width / 100f else sprite.size / 100f
                val h = if (sprite.height > 0f) sprite.height / 100f else sprite.size / 100f
                val shape = PolygonShape()
                shape.setAsBox(maxOf(w, 0.5f) / 2f, maxOf(h, 0.5f) / 2f)
                body.createFixture(shape, 1f)
                shape.dispose()
            } else {
                val shape = CircleShape().apply { radius = maxOf(sprite.size / 200f, 0.25f) }
                body.createFixture(shape, 1f)
                shape.dispose()
                body.setBullet(true)
            }
        } else if (type == BodyDef.BodyType.DynamicBody) {
            body.setBullet(true)
        }
    }

    fun setHitbox(sprite: DesktopSprite, width: Float, height: Float) {
        val body = bodiesBySprite[sprite] ?: return
        customHitboxSprites.add(sprite)
        for (f in body.fixtureList.toList()) {
            body.destroyFixture(f)
        }
        val shape = PolygonShape()
        shape.setAsBox(maxOf(width, 0.5f) / 2f, maxOf(height, 0.5f) / 2f)
        body.createFixture(shape, 1f)
        shape.dispose()
        if (body.type == BodyDef.BodyType.DynamicBody) {
            body.setBullet(true)
        }
    }

    fun hasCustomHitbox(sprite: DesktopSprite): Boolean = sprite in customHitboxSprites

    fun rayCast(startX: Float, startY: Float, endX: Float, endY: Float): List<RayCastResult> {
        val results = mutableListOf<RayCastResult>()
        world.rayCast(object : RayCastCallback {
            override fun reportRayFixture(fixture: Fixture, point: Vector2, normal: Vector2, fraction: Float): Float {
                results.add(RayCastResult(fixture, point.x, point.y, normal.x, normal.y, fraction))
                return 1f
            }
        }, Vector2(startX, startY), Vector2(endX, endY))
        return results
    }

    private val jointsByName = mutableMapOf<String, Joint>()

    fun addJoint(name: String, joint: Joint) {
        jointsByName[name] = joint
    }

    fun getJoint(name: String): Joint? = jointsByName[name]

    fun destroyJoint(name: String) {
        val joint = jointsByName.remove(name) ?: return
        world.destroyJoint(joint)
    }

    fun destroyJointByRef(joint: Joint) {
        jointsByName.entries.removeAll { it.value == joint }
        world.destroyJoint(joint)
    }

    fun syncSpritesFromPhysics() {
        for ((sprite, body) in bodiesBySprite) {
            sprite.x = body.position.x
            sprite.y = body.position.y
            sprite.direction = (90.0 - Math.toDegrees(body.angle.toDouble())).toFloat()
        }
    }

    fun step(deltaSeconds: Float) {
        if (bodiesBySprite.isEmpty()) return

        val hasDynamic = bodiesBySprite.values.any { it.type == BodyDef.BodyType.DynamicBody }
        if (!hasDynamic) {
            syncSpritesFromPhysics()
            return
        }

        accumulator += deltaSeconds
        var steps = 0
        while (accumulator >= timeStep && steps < maxSteps) {
            world.step(timeStep, 6, 2)
            accumulator -= timeStep
            steps++
        }
        syncSpritesFromPhysics()
    }

    fun dispose() {
        for (joint in jointsByName.values) {
            world.destroyJoint(joint)
        }
        jointsByName.clear()
        for (body in bodiesBySprite.values) {
            world.destroyBody(body)
        }
        bodiesBySprite.clear()
        world.dispose()
    }
}

data class RayCastResult(
    val fixture: Fixture,
    val pointX: Float,
    val pointY: Float,
    val normalX: Float,
    val normalY: Float,
    val fraction: Float
)
