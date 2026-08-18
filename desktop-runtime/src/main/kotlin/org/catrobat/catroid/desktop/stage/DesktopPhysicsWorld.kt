package org.catrobat.catroid.desktop.stage

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.Contact
import com.badlogic.gdx.physics.box2d.ContactListener
import com.badlogic.gdx.physics.box2d.Fixture
import com.badlogic.gdx.physics.box2d.FixtureDef
import com.badlogic.gdx.physics.box2d.Joint
import com.badlogic.gdx.physics.box2d.PolygonShape
import com.badlogic.gdx.physics.box2d.RayCastCallback
import com.badlogic.gdx.physics.box2d.World
import com.badlogic.gdx.physics.box2d.joints.DistanceJointDef
import com.badlogic.gdx.physics.box2d.joints.GearJointDef
import com.badlogic.gdx.physics.box2d.joints.PrismaticJointDef
import com.badlogic.gdx.physics.box2d.joints.PulleyJointDef
import com.badlogic.gdx.physics.box2d.joints.RevoluteJointDef
import com.badlogic.gdx.physics.box2d.joints.WeldJointDef
import java.util.concurrent.ConcurrentHashMap

/**
 * Простой box2d-мир для спрайтов.
 * Масштаб: 100 px = 1 метр.
 * Типы тел: NONE (нет тела), DYNAMIC, FIXED (static, без гравитации).
 */
class DesktopPhysicsWorld(
    private val scale: Float = 100f,
    private val onCollision: (DesktopSpriteRuntime, DesktopSpriteRuntime) -> Unit = { _, _ -> }
) {

    enum class BodyType { NONE, DYNAMIC, FIXED }

    class PhysicsBody(
        val body: Body,
        val sprite: DesktopSpriteRuntime,
        var type: BodyType,
        var radius: Float,
        var halfW: Float,
        var halfH: Float,
        var isCircle: Boolean = true,
        var isSensor: Boolean = false,
        var ragdoll: Boolean = false
    )

    class RayResult {
        var hasHit = false
        var sprite: DesktopSpriteRuntime? = null
        var x = 0f
        var y = 0f
        var nx = 0f
        var ny = 0f
        var fraction = -1f
    }

    private var world: World? = null
    private val bodies = ConcurrentHashMap<DesktopSpriteRuntime, PhysicsBody>()
    private val joints = ConcurrentHashMap<String, Joint>()
    private val rayResults = ConcurrentHashMap<String, RayResult>()

    private var collisionsThisStep = mutableListOf<Pair<DesktopSpriteRuntime, DesktopSpriteRuntime>>()

    init {
        initWorld()
    }

    private fun initWorld() {
        if (world != null) return
        world = World(Vector2(0f, -9.8f * scale), true).also { w ->
            w.setContactListener(object : ContactListener {
                override fun beginContact(contact: Contact) {
                    val a = bodyOfFixture(contact.fixtureA)
                    val b = bodyOfFixture(contact.fixtureB)
                    if (a != null && b != null) {
                        collisionsThisStep += a to b
                    }
                }

                override fun endContact(contact: Contact) {}
                override fun preSolve(contact: Contact, oldManifold: com.badlogic.gdx.physics.box2d.Manifold) {}
                override fun postSolve(contact: Contact, contactImpulse: com.badlogic.gdx.physics.box2d.ContactImpulse) {}
            })
        }
    }

    private fun bodyOfFixture(fixture: Fixture): DesktopSpriteRuntime? {
        val body = fixture.body
        return bodies.entries.firstOrNull { it.value.body === body }?.key
    }

    fun isWorldActive(): Boolean = world != null

    fun step(dt: Float) {
        val w = world ?: return
        if (dt <= 0f) return
        w.step(dt, 6, 2)

        // sync: спрайт следует за телом
        for ((sprite, pb) in bodies) {
            val pos = pb.body.position
            sprite.x = pos.x / scale
            sprite.y = pos.y / scale
            sprite.rotation = Math.toDegrees(pb.body.angle.toDouble()).toFloat()
        }

        if (collisionsThisStep.isNotEmpty()) {
            val handled = mutableSetOf<Pair<String, String>>()
            for ((a, b) in collisionsThisStep) {
                if (a.isBackground || b.isBackground) continue
                val key = setKey(a.name, b.name)
                if (handled.contains(key)) continue
                handled.add(key)
                onCollision(a, b)
            }
        }
        collisionsThisStep.clear()
    }

    private fun setKey(a: String, b: String): Pair<String, String> {
        return if (a < b) a to b else b to a
    }

    fun ensureBody(sprite: DesktopSpriteRuntime, type: BodyType, width: Float, height: Float) {
        if (type == BodyType.NONE) {
            removeBody(sprite)
            return
        }
        initWorld()
        val existing = bodies[sprite]
        if (existing != null) {
            if (existing.type == type && !needsReshape(existing, width, height)) return
            destroyBody(existing)
        }
        createBody(sprite, type, width, height)
    }

    private fun needsReshape(pb: PhysicsBody, width: Float, height: Float): Boolean {
        val halfW = (width / 2f) / scale
        val halfH = (height / 2f) / scale
        if (pb.isCircle) {
            return abs(pb.radius - minOf(halfW, halfH)) > 0.01f
        }
        return abs(pb.halfW - halfW) > 0.01f || abs(pb.halfH - halfH) > 0.01f
    }

    fun recreateBody(sprite: DesktopSpriteRuntime) {
        val pb = bodies[sprite] ?: return
        val type = pb.type
        val w = sprite.widthPx
        val h = sprite.heightPx
        destroyBody(pb)
        createBody(sprite, type, w, h)
    }

    private fun createBody(sprite: DesktopSpriteRuntime, type: BodyType, width: Float, height: Float) {
        val w = world ?: return
        val wPx = if (width > 0.1f) width else sprite.widthPx
        val hPx = if (height > 0.1f) height else sprite.heightPx

        val bodyDef = BodyDef()
        bodyDef.type = when (type) {
            BodyType.DYNAMIC -> com.badlogic.gdx.physics.box2d.BodyDef.BodyType.DynamicBody
            else -> com.badlogic.gdx.physics.box2d.BodyDef.BodyType.StaticBody
        }
        bodyDef.position.set(sprite.x / scale, sprite.y / scale)
        bodyDef.angle = Math.toRadians(sprite.rotation.toDouble()).toFloat()
        bodyDef.fixedRotation = false

        val body = w.createBody(bodyDef)

        val shape = if (type == BodyType.FIXED) {
            PolygonShape().apply {
                setAsBox((wPx / 2f) / scale, (hPx / 2f) / scale)
            }
        } else {
            CircleShape().apply {
                radius = minOf(wPx, hPx) / 2f / scale
            }
        }

        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1f
            friction = 0.1f
            restitution = 0.1f
        }

        val fixture = body.createFixture(fixtureDef)
        fixture.setSensor(false)
        shape.dispose()

        val pb = PhysicsBody(
            body = body,
            sprite = sprite,
            type = type,
            radius = minOf(wPx, hPx) / 2f / scale,
            halfW = (wPx / 2f) / scale,
            halfH = (hPx / 2f) / scale,
            isCircle = type != BodyType.FIXED
        )
        pb.isSensor = fixture.isSensor
        bodies[sprite] = pb

        body.userData = pb

        if (type == BodyType.FIXED) {
            body.gravityScale = 0f
        }
    }

    private fun destroyBody(pb: PhysicsBody) {
        world?.destroyBody(pb.body)
    }

    fun removeBody(sprite: DesktopSpriteRuntime) {
        val pb = bodies.remove(sprite) ?: return
        destroyBody(pb)
    }

    fun clearAll() {
        val w = world ?: return
        for (j in joints.values) {
            try {
                w.destroyJoint(j)
            } catch (_: Exception) {}
        }
        joints.clear()
        for (pb in bodies.values) {
            try {
                w.destroyBody(pb.body)
            } catch (_: Exception) {}
        }
        bodies.clear()
    }

    fun dispose() {
        clearAll()
        world?.dispose()
        world = null
    }

    fun bodyOf(sprite: DesktopSpriteRuntime): Body? = bodies[sprite]?.body

    fun setType(sprite: DesktopSpriteRuntime, type: BodyType, width: Float, height: Float) {
        ensureBody(sprite, type, width, height)
    }

    fun setGravity(x: Float, y: Float) {
        world?.gravity?.set(x / scale, y / scale)
    }

    fun setVelocity(sprite: DesktopSpriteRuntime, vx: Float, vy: Float) {
        val body = ensureDynamic(sprite) ?: return
        body.linearVelocity.set(vx / scale, vy / scale)
    }

    fun applyForce(sprite: DesktopSpriteRuntime, fx: Float, fy: Float) {
        val body = ensureDynamic(sprite) ?: return
        body.applyForceToCenter(Vector2(fx / scale, fy / scale), true)
    }

    fun applyImpulse(sprite: DesktopSpriteRuntime, ix: Float, iy: Float) {
        val body = ensureDynamic(sprite) ?: return
        body.applyLinearImpulse(Vector2(ix / scale, iy / scale), body.worldCenter, true)
    }

    fun applyTorque(sprite: DesktopSpriteRuntime, torque: Float) {
        val body = ensureDynamic(sprite) ?: return
        body.applyTorque(torque, true)
    }

    fun applyAngularImpulse(sprite: DesktopSpriteRuntime, impulse: Float) {
        val body = ensureDynamic(sprite) ?: return
        body.applyAngularImpulse(impulse, true)
    }

    fun setAngularVelocity(sprite: DesktopSpriteRuntime, degreesPerSecond: Float) {
        val body = ensureDynamic(sprite) ?: return
        body.angularVelocity = Math.toRadians(degreesPerSecond.toDouble()).toFloat()
    }

    fun setMass(sprite: DesktopSpriteRuntime, mass: Float) {
        val body = bodyOf(sprite) ?: return
        val md = body.massData
        md.mass = mass
        body.setMassData(md)
    }

    fun setDamping(sprite: DesktopSpriteRuntime, damping: Float) {
        val body = bodyOf(sprite) ?: return
        body.linearDamping = damping
    }

    fun setLinearDamping(sprite: DesktopSpriteRuntime, damping: Float) {
        setDamping(sprite, damping)
    }

    fun setAngularDamping(sprite: DesktopSpriteRuntime, damping: Float) {
        bodyOf(sprite)?.angularDamping = damping
    }

    fun setBounce(sprite: DesktopSpriteRuntime, restitution: Float) {
        bodyOf(sprite)?.fixtureList?.forEach { it.restitution = restitution }
    }

    fun setFriction(sprite: DesktopSpriteRuntime, friction: Float) {
        bodyOf(sprite)?.fixtureList?.forEach { it.friction = friction }
    }

    fun setSensor(sprite: DesktopSpriteRuntime, sensor: Boolean) {
        bodyOf(sprite)?.fixtureList?.forEach { it.isSensor = sensor }
    }

    fun setBullet(sprite: DesktopSpriteRuntime, bullet: Boolean) {
        bodyOf(sprite)?.isBullet = bullet
    }

    fun setFixedRotation(sprite: DesktopSpriteRuntime, fixed: Boolean) {
        bodyOf(sprite)?.setFixedRotation(fixed)
    }

    fun setGravityScale(sprite: DesktopSpriteRuntime, gs: Float) {
        bodyOf(sprite)?.gravityScale = gs
    }

    fun setRagdoll(sprite: DesktopSpriteRuntime, ragdoll: Boolean) {
        bodies[sprite]?.ragdoll = ragdoll
    }

    fun syncSpriteToBody(sprite: DesktopSpriteRuntime) {
        val pb = bodies[sprite] ?: return
        pb.body.setTransform(sprite.x / scale, sprite.y / scale, Math.toRadians(sprite.rotation.toDouble()).toFloat())
        pb.body.setLinearVelocity(0f, 0f)
    }

    fun setHitbox(sprite: DesktopSpriteRuntime, width: Float, height: Float) {
        val pb = bodies[sprite] ?: return
        val wPx = width.coerceAtLeast(1f)
        val hPx = height.coerceAtLeast(1f)
        val oldFixture = pb.body.fixtureList.firstOrNull() ?: return
        pb.body.destroyFixture(oldFixture)

        val fixtureDef = FixtureDef()
        if (pb.isCircle) {
            val shape = CircleShape().apply { radius = minOf(wPx, hPx) / 2f / scale }
            fixtureDef.shape = shape
            pb.body.createFixture(fixtureDef)
            shape.dispose()
        } else {
            val shape = PolygonShape().apply { setAsBox((wPx / 2f) / scale, (hPx / 2f) / scale) }
            fixtureDef.shape = shape
            pb.body.createFixture(fixtureDef)
            shape.dispose()
        }
        pb.body.fixtureList.forEach { it.isSensor = pb.isSensor }
        pb.radius = minOf(wPx, hPx) / 2f / scale
        pb.halfW = (wPx / 2f) / scale
        pb.halfH = (hPx / 2f) / scale
    }

    private fun ensureDynamic(sprite: DesktopSpriteRuntime): Body? {
        var pb = bodies[sprite]
        if (pb == null) {
            val wPx = sprite.widthPx
            val hPx = sprite.heightPx
            createBody(sprite, BodyType.DYNAMIC, wPx, hPx)
            pb = bodies[sprite]
        }
        if (pb != null && pb.type == BodyType.FIXED) return null
        return pb?.body
    }

    // ---------- Joints (зеркало PhysicsWorld.create*Joint) ----------

    private fun jointBody(sprite: DesktopSpriteRuntime): Body? =
        bodyOf(sprite) ?: ensureDynamic(sprite)

    fun createRevoluteJoint(
        jointId: String,
        spriteA: DesktopSpriteRuntime,
        spriteB: DesktopSpriteRuntime,
        anchorX: Float,
        anchorY: Float
    ): Boolean {
        if (jointId.isEmpty() || joints.containsKey(jointId)) return false
        val bodyA = jointBody(spriteA) ?: return false
        val bodyB = jointBody(spriteB) ?: return false
        val def = RevoluteJointDef()
        def.initialize(bodyA, bodyB, Vector2(anchorX / scale, anchorY / scale))
        def.collideConnected = false
        joints[jointId] = world!!.createJoint(def)
        return true
    }

    fun createDistanceJoint(
        jointId: String,
        spriteA: DesktopSpriteRuntime,
        spriteB: DesktopSpriteRuntime,
        length: Float,
        frequency: Float,
        damping: Float
    ): Boolean {
        if (jointId.isEmpty() || joints.containsKey(jointId)) return false
        val bodyA = jointBody(spriteA) ?: return false
        val bodyB = jointBody(spriteB) ?: return false
        val def = DistanceJointDef()
        def.initialize(bodyA, bodyB, bodyA.worldCenter, bodyB.worldCenter)
        def.collideConnected = false
        if (length > 0f) def.length = length / scale
        if (frequency > 0f) def.frequencyHz = frequency
        if (damping > 0f) def.dampingRatio = damping
        joints[jointId] = world!!.createJoint(def)
        return true
    }

    fun createWeldJoint(
        jointId: String,
        spriteA: DesktopSpriteRuntime,
        spriteB: DesktopSpriteRuntime,
        anchorX: Float,
        anchorY: Float
    ): Boolean {
        if (jointId.isEmpty() || joints.containsKey(jointId)) return false
        val bodyA = jointBody(spriteA) ?: return false
        val bodyB = jointBody(spriteB) ?: return false
        val def = WeldJointDef()
        def.initialize(bodyA, bodyB, Vector2(anchorX / scale, anchorY / scale))
        def.collideConnected = false
        joints[jointId] = world!!.createJoint(def)
        return true
    }

    fun createPrismaticJoint(
        jointId: String,
        spriteA: DesktopSpriteRuntime,
        spriteB: DesktopSpriteRuntime,
        anchorX: Float,
        anchorY: Float,
        axisX: Float,
        axisY: Float
    ): Boolean {
        if (jointId.isEmpty() || joints.containsKey(jointId)) return false
        val bodyA = jointBody(spriteA) ?: return false
        val bodyB = jointBody(spriteB) ?: return false
        val def = PrismaticJointDef()
        def.initialize(
            bodyA, bodyB,
            Vector2(anchorX / scale, anchorY / scale),
            Vector2(axisX / scale, axisY / scale)
        )
        def.collideConnected = false
        joints[jointId] = world!!.createJoint(def)
        return true
    }

    fun createPulleyJoint(
        jointId: String,
        spriteA: DesktopSpriteRuntime,
        spriteB: DesktopSpriteRuntime,
        groundAnchorAX: Float, groundAnchorAY: Float,
        groundAnchorBX: Float, groundAnchorBY: Float,
        ratio: Float
    ): Boolean {
        if (jointId.isEmpty() || joints.containsKey(jointId)) return false
        val bodyA = jointBody(spriteA) ?: return false
        val bodyB = jointBody(spriteB) ?: return false
        val def = PulleyJointDef()
        def.initialize(
            bodyA, bodyB,
            Vector2(groundAnchorAX / scale, groundAnchorAY / scale),
            Vector2(groundAnchorBX / scale, groundAnchorBY / scale),
            bodyA.worldCenter, bodyB.worldCenter,
            ratio
        )
        def.collideConnected = false
        joints[jointId] = world!!.createJoint(def)
        return true
    }

    fun createGearJoint(jointId: String, jointAId: String, jointBId: String, ratio: Float): Boolean {
        if (jointId.isEmpty() || joints.containsKey(jointId)) return false
        val jointA = joints[jointAId] ?: return false
        val jointB = joints[jointBId] ?: return false

        val a1 = jointA.bodyA
        val a2 = jointA.bodyB
        val b1 = jointB.bodyA
        val b2 = jointB.bodyB
        val common: Body
        val uniqueA: Body
        val uniqueB: Body
        common = when {
            a1 === b1 || a1 === b2 -> a1
            a2 === b1 || a2 === b2 -> a2
            else -> return false
        }
        uniqueA = if (a1 === common) a2 else a1
        uniqueB = if (b1 === common) b2 else b1

        val def = GearJointDef()
        def.joint1 = jointA
        def.joint2 = jointB
        def.bodyA = uniqueA
        def.bodyB = uniqueB
        def.ratio = ratio
        joints[jointId] = world!!.createJoint(def)
        return true
    }

    fun destroyJoint(jointId: String) {
        val joint = joints.remove(jointId) ?: return
        try {
            world?.destroyJoint(joint)
        } catch (_: Exception) {}
    }

    fun hasJoint(jointId: String): Boolean = joints.containsKey(jointId)

    fun rayResult(rayId: String): RayResult? = rayResults[rayId]

    fun performRayCast(rayId: String, startX: Float, startY: Float, endX: Float, endY: Float) {
        val w = world ?: return
        if (startX == endX && startY == endY) return
        val callback = object : RayCastCallback {
            val result = RayResult()
            override fun reportRayFixture(
                fixture: Fixture,
                point: Vector2,
                normal: Vector2,
                fraction: Float
            ): Float {
                if (fraction < result.fraction || !result.hasHit) {
                    result.hasHit = true
                    result.sprite = (fixture.body.userData as? PhysicsBody)?.sprite
                    result.x = point.x * scale
                    result.y = point.y * scale
                    result.nx = normal.x
                    result.ny = normal.y
                    result.fraction = fraction
                }
                return fraction
            }
        }
        w.rayCast(callback, Vector2(startX / scale, startY / scale), Vector2(endX / scale, endY / scale))
        rayResults[rayId] = callback.result
    }

    private fun abs(v: Float) = if (v < 0f) -v else v
}