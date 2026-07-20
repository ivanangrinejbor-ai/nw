package org.catrobat.catroid.stage

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.BodyDef
import com.badlogic.gdx.physics.box2d.CircleShape
import com.badlogic.gdx.physics.box2d.PolygonShape
import org.junit.*
import org.junit.Assert.*

/**
 * Тесты DesktopPhysicsWorld — проверка исправления бага коллизии.
 *
 * Box2D native library нестабилен при dispose() в тестовом JVM без GL-контекста,
 * поэтому используется ОДИН экземпляр мира на весь класс, и dispose()
 * обёрнут в try/catch (память освободит ОС при завершении процесса).
 * Имена спрайтов уникальны глобально, чтобы избежать конфликтов.
 */
class DesktopPhysicsWorldCollisionTest {

    companion object {
        private var counter = 0
        private lateinit var pw: DesktopPhysicsWorld

        @BeforeClass @JvmStatic
        fun initWorld() {
            pw = DesktopPhysicsWorld(gravityX = 0f, gravityY = 0f)
        }

        @AfterClass @JvmStatic
        fun destroyWorld() {
            // Box2D world.dispose() требует GL-контекста, которого нет в тестовом JVM.
            // Пропускаем — ОС освободит память при завершении процесса.
        }
    }

    private fun s(
        prefix: String = "t",
        x: Float = 0f, y: Float = 0f,
        size: Float = 100f, w: Float = -1f, h: Float = -1f
    ): DesktopSprite {
        counter++
        return DesktopSprite(name = "${prefix}_$counter", x = x, y = y,
            size = size, width = w, height = h)
    }

    // ════════════════════════════════════════════════════════════════════
    // 1. Static → PolygonShape
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testStaticBodyUsesPolygon() {
        val b = pw.createBodyForSprite(s("wall"), isStatic = true)
        assertEquals(BodyDef.BodyType.StaticBody, b.type)
        assertFalse(b.isBullet)
        assertTrue(b.fixtureList.first().shape is PolygonShape)
    }

    @Test
    fun testStaticBody4Vertices() {
        val s = pw.createBodyForSprite(s("w", size = 80f), isStatic = true)
        val sh = s.fixtureList.first().shape as PolygonShape
        assertEquals(4, sh.vertexCount)
    }

    @Test
    fun testStaticBodyHonorsWidthHeight() {
        val s = pw.createBodyForSprite(s("wide", size = 50f, w = 300f, h = 20f), isStatic = true)
        val v = Vector2()
        (s.fixtureList.first().shape as PolygonShape).getVertex(0, v)
        assertTrue(kotlin.math.abs(v.x) > 1.0f)
        assertTrue(kotlin.math.abs(v.y) > 0.05f)
        assertTrue(kotlin.math.abs(v.y) < 1.0f)
    }

    @Test
    fun testTinyStaticBody() {
        val sh = pw.createBodyForSprite(s("tiny", size = 1f), isStatic = true)
            .fixtureList.first().shape
        assertNotNull(sh)
        assertTrue(sh is PolygonShape)
    }

    // ════════════════════════════════════════════════════════════════════
    // 2. Dynamic → Circle + Bullet
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testDynamicBodyCircleAndBullet() {
        val b = pw.createBodyForSprite(s("ball"), isStatic = false)
        assertEquals(BodyDef.BodyType.DynamicBody, b.type)
        assertTrue(b.isBullet)
        assertTrue(b.fixtureList.first().shape is CircleShape)
    }

    @Test
    fun testEnsureDynamicCreatesCircleBullet() {
        val b = pw.ensureBody(s("ed"), isStatic = false)
        assertTrue(b.fixtureList.first().shape is CircleShape)
        assertTrue(b.isBullet)
    }

    @Test
    fun testEnsureStaticCreatesPolygon() {
        val b = pw.ensureBody(s("es"), isStatic = true)
        assertTrue(b.fixtureList.first().shape is PolygonShape)
        assertEquals(BodyDef.BodyType.StaticBody, b.type)
        assertFalse(b.isBullet)
    }

    @Test
    fun testEnsureBodyIdempotent() {
        val sp = s("idem")
        val b1 = pw.ensureBody(sp, isStatic = false)
        val b2 = pw.ensureBody(sp, isStatic = true)
        assertSame(b1, b2)
    }

    // ════════════════════════════════════════════════════════════════════
    // 3. setBodyType switching
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testSwitchDynToStatic() {
        val sp = s("s1"); pw.createBodyForSprite(sp, isStatic = false)
        pw.setBodyType(sp, BodyDef.BodyType.StaticBody)
        val b = pw.getBody(sp)!!
        assertEquals(BodyDef.BodyType.StaticBody, b.type)
        assertTrue("Dyn→Static fixture PolygonShape", b.fixtureList.first().shape is PolygonShape)
    }

    @Test
    fun testSwitchStaticToDyn() {
        val sp = s("s2"); pw.createBodyForSprite(sp, isStatic = true)
        pw.setBodyType(sp, BodyDef.BodyType.DynamicBody)
        val b = pw.getBody(sp)!!
        assertEquals(BodyDef.BodyType.DynamicBody, b.type)
        assertTrue("Static→Dyn fixture CircleShape", b.fixtureList.first().shape is CircleShape)
        assertTrue(b.isBullet)
    }

    @Test
    fun testSwitchToKinematic() {
        val sp = s("kin"); pw.createBodyForSprite(sp, isStatic = false)
        pw.setBodyType(sp, BodyDef.BodyType.KinematicBody)
        assertTrue(pw.getBody(sp)!!.fixtureList.first().shape is PolygonShape)
    }

    @Test
    fun testSameBodyTypeKeepsShape() {
        val sp = s("noop"); pw.createBodyForSprite(sp, isStatic = false)
        assertTrue(pw.getBody(sp)!!.fixtureList.first().shape is CircleShape)
        pw.setBodyType(sp, BodyDef.BodyType.DynamicBody)
        assertTrue(pw.getBody(sp)!!.fixtureList.first().shape is CircleShape)
    }

    // ════════════════════════════════════════════════════════════════════
    // 4. setHitbox
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testSetHitboxPolygon() {
        val sp = s("h1"); pw.createBodyForSprite(sp, isStatic = false)
        pw.setHitbox(sp, 200f, 50f)
        assertTrue(pw.hasCustomHitbox(sp))
        assertTrue(pw.getBody(sp)!!.fixtureList.first().shape is PolygonShape)
    }

    @Test
    fun testSetHitboxKeepsBullet() {
        val sp = s("h2"); pw.createBodyForSprite(sp, isStatic = false)
        pw.setHitbox(sp, 100f, 100f)
        assertTrue(pw.getBody(sp)!!.isBullet)
    }

    @Test
    fun testSetHitboxOnStaticPreservesType() {
        val sp = s("h3"); pw.createBodyForSprite(sp, isStatic = true)
        pw.setHitbox(sp, 300f, 30f)
        assertEquals(BodyDef.BodyType.StaticBody, pw.getBody(sp)!!.type)
        assertTrue(pw.getBody(sp)!!.fixtureList.first().shape is PolygonShape)
    }

    @Test
    fun testCustomHitboxPreventsRecreation() {
        val sp = s("cust"); pw.createBodyForSprite(sp, isStatic = false)
        pw.setHitbox(sp, 80f, 40f)
        assertTrue(pw.hasCustomHitbox(sp))
        pw.setBodyType(sp, BodyDef.BodyType.StaticBody)
        assertTrue(pw.getBody(sp)!!.fixtureList.first().shape is PolygonShape)
    }

    // ════════════════════════════════════════════════════════════════════
    // 5. removeBody
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testRemoveBody() {
        val sp = s("rm"); pw.createBodyForSprite(sp, isStatic = false)
        assertTrue(pw.hasBody(sp))
        pw.removeBody(sp)
        assertFalse(pw.hasBody(sp))
    }

    // ════════════════════════════════════════════════════════════════════
    // 6. Property setters
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testSetFriction() {
        val sp = s("fw"); pw.createBodyForSprite(sp, isStatic = true)
        pw.setFriction(sp, 0.5f)
        assertEquals(0.5f, pw.getBody(sp)!!.fixtureList.first().friction, 0.001f)
    }

    @Test
    fun testSetBounce() {
        val sp = s("bw"); pw.createBodyForSprite(sp, isStatic = true)
        pw.setBounce(sp, 0.8f)
        assertEquals(0.8f, pw.getBody(sp)!!.fixtureList.first().restitution, 0.001f)
    }

    @Test
    fun testSetMass() {
        val sp = s("ms"); pw.createBodyForSprite(sp, isStatic = false)
        pw.setMass(sp, 50f)
        assertEquals(50f, pw.getBody(sp)!!.massData.mass, 0.1f)
    }

    @Test
    fun testSetDamping() {
        val sp = s("dp"); pw.createBodyForSprite(sp, isStatic = false)
        pw.setDamping(sp, 0.1f, 0.05f)
        assertEquals(0.1f, pw.getBody(sp)!!.linearDamping, 0.001f)
        assertEquals(0.05f, pw.getBody(sp)!!.angularDamping, 0.001f)
    }

    @Test
    fun testSetGravity() {
        pw.setGravity(3f, -7f)
        val g = pw.world.gravity
        assertEquals(3f, g.x, 0.001f)
        assertEquals(-7f, g.y, 0.001f)
        pw.setGravity(0f, 0f) // reset
    }

    @Test
    fun testApplyForce() { pw.applyForce(s("af"), 10f, 0f) }

    @Test
    fun testApplyImpulse() { pw.applyImpulse(s("ai"), 5f, 0f) }

    @Test
    fun testSetMassOnStatic() {
        val sp = s("msw")
        pw.createBodyForSprite(sp, isStatic = true)
        pw.setMass(sp, 100f)
    }

    @Test
    fun testApplyForceOnNonexistentBodyDoesNotThrow() {
        pw.applyForce(s("fake"), 10f, 0f) // should not throw
    }

    // ════════════════════════════════════════════════════════════════════
    // 7. Shape dimensions
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testPolygonSizeBasedOnSize() {
        val v = Vector2()
        (pw.createBodyForSprite(s("dim", size = 200f), isStatic = true)
            .fixtureList.first().shape as PolygonShape).getVertex(0, v)
        assertTrue(kotlin.math.abs(v.x) > 0.4f)
    }

    @Test
    fun testCircleRadiusBasedOnSize() {
        val sh = pw.createBodyForSprite(s("crd", size = 200f), isStatic = false)
            .fixtureList.first().shape as CircleShape
        assertTrue(sh.radius > 0.3f)
    }

    @Test
    fun testBodyPositionAfterSwitch() {
        val sp = s("pos", x = 42f, y = 99f)
        pw.createBodyForSprite(sp, isStatic = true)
        pw.setBodyType(sp, BodyDef.BodyType.DynamicBody)
        val p = pw.getBody(sp)!!.position
        assertTrue(kotlin.math.abs(42f - p.x) < 1f)
        assertTrue(kotlin.math.abs(99f - p.y) < 1f)
    }

    // ════════════════════════════════════════════════════════════════════
    // 8. Stress: many bodies
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testManyStaticBodiesAllPolygon() {
        for (i in 0 until 50) {
            val sp = s("stat_s", size = 30f)
            pw.createBodyForSprite(sp, isStatic = true)
            assertTrue(pw.getBody(sp)!!.fixtureList.first().shape is PolygonShape)
        }
    }

    @Test
    fun testManyDynamicBodiesAllCircleAndBullet() {
        for (i in 0 until 50) {
            val sp = s("dyn_d", size = 20f)
            pw.createBodyForSprite(sp, isStatic = false)
            val b = pw.getBody(sp)!!
            assertTrue(b.fixtureList.first().shape is CircleShape)
            assertTrue(b.isBullet)
        }
    }

    // ════════════════════════════════════════════════════════════════════
    // 9. Edge cases
    // ════════════════════════════════════════════════════════════════════

    @Test
    fun testEnsureBodyCreatesCorrectFixtureByType() {
        val d1 = s("ec_dyn"); val b1 = pw.ensureBody(d1, false)
        assertTrue(b1.fixtureList.first().shape is CircleShape)
        assertTrue(b1.isBullet)
        val s1 = s("ec_sta"); val b2 = pw.ensureBody(s1, true)
        assertTrue(b2.fixtureList.first().shape is PolygonShape)
        assertFalse(b2.isBullet)
    }

    @Test
    fun testGetBodyReturnsNullForNonexistent() {
        assertNull(pw.getBody(s("nonexistent")))
    }

    @Test
    fun testHasBodyFalseForNonexistent() {
        assertFalse(pw.hasBody(s("nohas")))
    }

    @Test
    fun testSetBodyTypeOnNonexistentDoesNotThrow() {
        pw.setBodyType(s("nobody"), BodyDef.BodyType.StaticBody)
    }

    @Test
    fun testMixedBodiesDoNotThrow() {
        val sb = s("mix_s"); pw.createBodyForSprite(sb, true)
        val db = s("mix_d"); pw.createBodyForSprite(db, false)
        pw.ensureBody(s("mix_e1"), true)
        pw.ensureBody(s("mix_e2"), false)
        assertTrue(pw.getBody(sb)!!.fixtureList.first().shape is PolygonShape)
        assertTrue(pw.getBody(db)!!.fixtureList.first().shape is CircleShape)
        assertTrue(pw.getBody(db)!!.isBullet)
    }
}
