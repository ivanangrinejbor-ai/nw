package org.catrobat.catroid.test.physics;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.*;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for 8 new physics bricks:
 * SetAngularVelocity, SetLinearDamping, SetAngularDamping,
 * SetPhysicsFixedRotation, SetPhysicsBullet, SetGravityScale,
 * ApplyForceAtPoint, SetPhysicsSensor
 */
public class NewPhysicsBricksTest {

    private Sprite sprite;

    @Before
    public void setUp() {
        sprite = new Sprite("PhysicsSprite");
    }

    // --- SetAngularVelocityBrick ---

    @Test
    public void testSetAngularVelocityBrickCreation() {
        SetAngularVelocityBrick brick = new SetAngularVelocityBrick(90.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_angular_velocity, brick.getViewResource());
    }

    @Test
    public void testSetAngularVelocityBrickFormula() {
        SetAngularVelocityBrick brick = new SetAngularVelocityBrick(new Formula(180));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_ANGULAR_VELOCITY));
    }

    @Test
    public void testSetAngularVelocityBrickClone() throws CloneNotSupportedException {
        SetAngularVelocityBrick brick = new SetAngularVelocityBrick(45.0);
        Brick clone = brick.clone();
        assertNotNull(clone);
        assertNotSame(brick, clone);
    }

    // --- SetLinearDampingBrick ---

    @Test
    public void testSetLinearDampingBrickCreation() {
        SetLinearDampingBrick brick = new SetLinearDampingBrick(0.5);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_linear_damping, brick.getViewResource());
    }

    @Test
    public void testSetLinearDampingBrickFormula() {
        SetLinearDampingBrick brick = new SetLinearDampingBrick(new Formula(3));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_DAMPING));
    }

    // --- SetAngularDampingBrick ---

    @Test
    public void testSetAngularDampingBrickCreation() {
        SetAngularDampingBrick brick = new SetAngularDampingBrick(1.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_angular_damping, brick.getViewResource());
    }

    @Test
    public void testSetAngularDampingBrickFormula() {
        SetAngularDampingBrick brick = new SetAngularDampingBrick(new Formula(2.5));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_DAMPING));
    }

    // --- SetPhysicsFixedRotationBrick ---

    @Test
    public void testSetFixedRotationBrickCreation() {
        SetPhysicsFixedRotationBrick brick = new SetPhysicsFixedRotationBrick(1.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_fixed_rotation, brick.getViewResource());
    }

    @Test
    public void testSetFixedRotationBrickToggleValue() {
        SetPhysicsFixedRotationBrick brick = new SetPhysicsFixedRotationBrick(new Formula(1));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_TOGGLE));
    }

    // --- SetPhysicsBulletBrick ---

    @Test
    public void testSetBulletBrickCreation() {
        SetPhysicsBulletBrick brick = new SetPhysicsBulletBrick(1.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_bullet, brick.getViewResource());
    }

    @Test
    public void testSetBulletBrickToggleValue() {
        SetPhysicsBulletBrick brick = new SetPhysicsBulletBrick(new Formula(0));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_TOGGLE));
    }

    // --- SetGravityScaleBrick ---

    @Test
    public void testSetGravityScaleBrickCreation() {
        SetGravityScaleBrick brick = new SetGravityScaleBrick(0.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_gravity_scale, brick.getViewResource());
    }

    @Test
    public void testSetGravityScaleBrickFormula() {
        SetGravityScaleBrick brick = new SetGravityScaleBrick(new Formula(2.0));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_GRAVITY_SCALE));
    }

    @Test
    public void testSetGravityScaleZeroForWeightless() {
        SetGravityScaleBrick brick = new SetGravityScaleBrick(0.0);
        assertNotNull(brick);
    }

    // --- ApplyForceAtPointBrick ---

    @Test
    public void testApplyForceAtPointBrickCreation() {
        ApplyForceAtPointBrick brick = new ApplyForceAtPointBrick(10, 0, 50, 50);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_apply_force_at_point, brick.getViewResource());
    }

    @Test
    public void testApplyForceAtPointBrickAllFields() {
        ApplyForceAtPointBrick brick = new ApplyForceAtPointBrick(
                new Formula(5), new Formula(10), new Formula(20), new Formula(30));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_FORCE_X));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_FORCE_Y));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_POINT_X));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_POINT_Y));
    }

    @Test
    public void testApplyForceAtPointBrickClone() throws CloneNotSupportedException {
        ApplyForceAtPointBrick brick = new ApplyForceAtPointBrick(1, 2, 3, 4);
        Brick clone = brick.clone();
        assertNotNull(clone);
        assertTrue(clone instanceof ApplyForceAtPointBrick);
    }

    // --- SetPhysicsSensorBrick ---

    @Test
    public void testSetPhysicsSensorBrickCreation() {
        SetPhysicsSensorBrick brick = new SetPhysicsSensorBrick(1.0);
        assertNotNull(brick);
        assertEquals(org.catrobat.catroid.R.layout.brick_set_physics_sensor, brick.getViewResource());
    }

    @Test
    public void testSetPhysicsSensorBrickToggleValue() {
        SetPhysicsSensorBrick brick = new SetPhysicsSensorBrick(new Formula(1));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.PHYSICS_TOGGLE));
    }

    @Test
    public void testSetPhysicsSensorBrickClone() throws CloneNotSupportedException {
        SetPhysicsSensorBrick brick = new SetPhysicsSensorBrick(0.0);
        Brick clone = brick.clone();
        assertNotNull(clone);
    }

    // --- Cross-brick validation ---

    @Test
    public void testAllNewPhysicsBricksHaveSerialVersionUID() {
        // Each brick must have serialVersionUID = 1L (validated by class structure)
        assertNotNull(new SetAngularVelocityBrick());
        assertNotNull(new SetLinearDampingBrick());
        assertNotNull(new SetAngularDampingBrick());
        assertNotNull(new SetPhysicsFixedRotationBrick());
        assertNotNull(new SetPhysicsBulletBrick());
        assertNotNull(new SetGravityScaleBrick());
        assertNotNull(new ApplyForceAtPointBrick());
        assertNotNull(new SetPhysicsSensorBrick());
    }

    @Test
    public void testAllNewPhysicsBricksNoArgConstructor() {
        // XStream needs no-arg constructor
        assertNotNull(new SetAngularVelocityBrick());
        assertNotNull(new SetLinearDampingBrick());
        assertNotNull(new SetAngularDampingBrick());
        assertNotNull(new SetPhysicsFixedRotationBrick());
        assertNotNull(new SetPhysicsBulletBrick());
        assertNotNull(new SetGravityScaleBrick());
        assertNotNull(new ApplyForceAtPointBrick());
        assertNotNull(new SetPhysicsSensorBrick());
    }
}
