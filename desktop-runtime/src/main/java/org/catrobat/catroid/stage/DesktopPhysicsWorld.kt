package org.catrobat.catroid.stage

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.utils.Array

/**
 * Десктопная интеграция Box2D.
 *
 * Создаёт мир с гравитацией, позволяет создавать динамические/статические тела
 * для спрайтов и шагает симуляцию. Используется как замена Android-физике
 * внутри [DesktopStageListener].
 *
 * ## Исправление бага коллизии статики под углом (2026-07)
 * Статические тела (стены, полы) теперь используют прямоугольный PolygonShape
 * вместо CircleShape — это предотвращает прохождение динамических объектов
 * сквозь стену при наклонном подходе (краевой эффект Box2D EdgeShape).
 * Динамические тела получают setBullet(true) для CCD (Continuous Collision
 * Detection), что устраняет tunneling при быстром движении.
 */
class DesktopPhysicsWorld(
    private var gravityX: Float = 0f,
    private var gravityY: Float = -9.8f
) {
    val world: World = World(Vector2(gravityX, gravityY), true)
    private val bodiesBySprite = mutableMapOf<DesktopSprite, Body>()
    private val customHitboxSprites = mutableSetOf<DesktopSprite>() // setHitbox() called explicitly
    private var accumulator = 0f
    private val timeStep = 1 / 60f
    private val maxSteps = 5

    /**
     * Создать Box2D-тело для спрайта.
     *
     * - **Static**: прямоугольный PolygonShape (ширина/высота спрайта либо size/100)
     * - **Dynamic**: CircleShape + setBullet(true) для CCD
     */
    fun createBodyForSprite(sprite: DesktopSprite, isStatic: Boolean = false): Body {
        val def = BodyDef().apply {
            type = if (isStatic) BodyDef.BodyType.StaticBody else BodyDef.BodyType.DynamicBody
            position.set(sprite.x, sprite.y)
            linearDamping = 0.5f
        }
        val body = world.createBody(def)
        if (isStatic) {
            // Static body → PolygonShape (прямоугольник), чтобы не было краевого
            // проскальзывания при наклонном ударе (BUG FIX 2026-07).
            val w = if (sprite.width > 0f) sprite.width / 100f else sprite.size / 100f
            val h = if (sprite.height > 0f) sprite.height / 100f else sprite.size / 100f
            val shape = PolygonShape()
            shape.setAsBox(maxOf(w, 0.5f) / 2f, maxOf(h, 0.5f) / 2f)
            body.createFixture(shape, 1f)
            shape.dispose()
        } else {
            // Dynamic body → CircleShape + CCD
            val shape = CircleShape().apply { radius = maxOf(sprite.size / 200f, 0.25f) }
            body.createFixture(shape, 1f)
            shape.dispose()
            body.setBullet(true) // CCD: предотвращает tunneling (BUG FIX 2026-07)
        }
        bodiesBySprite[sprite] = body
        return body
    }

    fun getBody(sprite: DesktopSprite): Body? = bodiesBySprite[sprite]

    /** Remove and destroy the physics body attached to a sprite (e.g. on clone delete). */
    fun removeBody(sprite: DesktopSprite) {
        val body = bodiesBySprite.remove(sprite)
        if (body != null) {
            world.destroyBody(body)
        }
    }

    fun hasBody(sprite: DesktopSprite): Boolean = bodiesBySprite.containsKey(sprite)

    /**
     * Создать тело, если его нет.
     * Если спрайт уже имеет явный hitbox (setHitbox), возвращает существующее тело
     * без изменения фикстур.
     */
    fun ensureBody(sprite: DesktopSprite, isStatic: Boolean = false): Body {
        return bodiesBySprite.getOrPut(sprite) { createBodyForSprite(sprite, isStatic) }
    }

    /** Установить гравитацию мира. */
    fun setGravity(x: Float, y: Float) {
        gravityX = x
        gravityY = y
        world.setGravity(Vector2(x, y))
    }

    /** Приложить силу к телу спрайта (в мировых координатах). */
    fun applyForce(sprite: DesktopSprite, fx: Float, fy: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyForceToCenter(fx, fy, true)
    }

    /** Приложить импульс к телу спрайта. */
    fun applyImpulse(sprite: DesktopSprite, ix: Float, iy: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyLinearImpulse(ix, iy, body.worldCenter.x, body.worldCenter.y, true)
    }

    /** Приложить вращающий момент. */
    fun applyTorque(sprite: DesktopSprite, torque: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyTorque(torque, true)
    }

    /** Приложить угловой импульс. */
    fun applyAngularImpulse(sprite: DesktopSprite, impulse: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.applyAngularImpulse(impulse, true)
    }

    /** Установить трение первого фикстура. */
    fun setFriction(sprite: DesktopSprite, friction: Float) {
        val body = bodiesBySprite[sprite] ?: return
        for (fixture in body.fixtureList) {
            fixture.friction = friction
        }
    }

    /** Установить restitution (упругость) первого фикстура. */
    fun setBounce(sprite: DesktopSprite, bounce: Float) {
        val body = bodiesBySprite[sprite] ?: return
        for (fixture in body.fixtureList) {
            fixture.restitution = bounce
        }
    }

    /** Изменить массу тела, пересчитывая плотность фикстур. */
    fun setMass(sprite: DesktopSprite, mass: Float) {
        val body = bodiesBySprite[sprite] ?: return
        if (mass <= 0f) return
        val massData = body.massData
        massData.mass = mass
        body.massData = massData
    }

    /** Установить линейное и угловое демпфирование. */
    fun setDamping(sprite: DesktopSprite, linear: Float, angular: Float) {
        val body = bodiesBySprite[sprite] ?: return
        body.linearDamping = linear
        body.angularDamping = angular
    }

    /**
     * Сменить тип тела (Static/Dynamic/Kinematic).
     *
     * При переключении в Static пересоздаёт фикстуру с PolygonShape (если не был
     * установлен явный hitbox), чтобы стены корректно отражали объекты под углом.
     * При переключении в Dynamic включает CCD (setBullet(true)).
     */
    fun setBodyType(sprite: DesktopSprite, type: BodyDef.BodyType) {
        val body = bodiesBySprite[sprite] ?: return
        val oldType = body.type
        body.type = type
        // Если тип изменился и не было явного hitbox — пересоздать фикстуру
        if (oldType != type && sprite !in customHitboxSprites) {
            for (f in body.fixtureList.toList()) {
                body.destroyFixture(f)
            }
            if (type == BodyDef.BodyType.StaticBody || type == BodyDef.BodyType.KinematicBody) {
                // Rectangle shape for static/kinematic
                val w = if (sprite.width > 0f) sprite.width / 100f else sprite.size / 100f
                val h = if (sprite.height > 0f) sprite.height / 100f else sprite.size / 100f
                val shape = PolygonShape()
                shape.setAsBox(maxOf(w, 0.5f) / 2f, maxOf(h, 0.5f) / 2f)
                body.createFixture(shape, 1f)
                shape.dispose()
            } else {
                // Circle + CCD for dynamic
                val shape = CircleShape().apply { radius = maxOf(sprite.size / 200f, 0.25f) }
                body.createFixture(shape, 1f)
                shape.dispose()
                body.setBullet(true)
            }
        } else if (type == BodyDef.BodyType.DynamicBody) {
            body.setBullet(true)
        }
    }

    /**
     * Заменить все фикстуры спрайта на прямоугольный PolygonShape.
     * Отмечает спрайт как имеющий явный hitbox, чтобы [createBodyForSprite]
     * не пересоздавал круглую форму при автоматическом ensureBody.
     */
    fun setHitbox(sprite: DesktopSprite, width: Float, height: Float) {
        val body = bodiesBySprite[sprite] ?: return
        customHitboxSprites.add(sprite)
        // Удалить старые фикстуры
        for (f in body.fixtureList.toList()) {
            body.destroyFixture(f)
        }
        val shape = PolygonShape()
        shape.setAsBox(maxOf(width, 0.5f) / 2f, maxOf(height, 0.5f) / 2f)
        body.createFixture(shape, 1f)
        shape.dispose()
        // Динамические тела должны сохранять CCD и после замены фикстуры
        if (body.type == BodyDef.BodyType.DynamicBody) {
            body.setBullet(true)
        }
    }

    /** Проверить, установлен ли явный hitbox для спрайта. */
    fun hasCustomHitbox(sprite: DesktopSprite): Boolean = sprite in customHitboxSprites

    /** Симулировать бросок луча (ray cast). Возвращает список {fixture, pointX, pointY, normalX, normalY, fraction}. */
    fun rayCast(startX: Float, startY: Float, endX: Float, endY: Float): List<RayCastResult> {
        val results = mutableListOf<RayCastResult>()
        world.rayCast(object : RayCastCallback {
            override fun reportRayFixture(fixture: Fixture, point: Vector2, normal: Vector2, fraction: Float): Float {
                results.add(RayCastResult(fixture, point.x, point.y, normal.x, normal.y, fraction))
                return 1f // continue
            }
        }, Vector2(startX, startY), Vector2(endX, endY))
        return results
    }

    /** Хранить джойнты по имени для совместного доступа. */
    private val jointsByName = mutableMapOf<String, Joint>()

    /** Создать джойнт (Distance, Revolute, Prismatic, Weld, etc.). */
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
        }
    }

    fun step(deltaSeconds: Float) {
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
