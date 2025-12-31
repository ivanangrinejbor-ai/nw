package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import java.util.List;

public class CreateParticlesBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public CreateParticlesBrick() {
        addAllowedBrickField(BrickField.PARTICLE_ID, R.id.brick_particle_id_edit);
        addAllowedBrickField(BrickField.PARTICLE_LOOPING, R.id.brick_particle_looping_edit);
        addAllowedBrickField(BrickField.PARTICLE_DURATION, R.id.brick_particle_duration_edit);
        addAllowedBrickField(BrickField.PARTICLE_LIFETIME, R.id.brick_particle_lifetime_edit);
        addAllowedBrickField(BrickField.PARTICLE_SPEED, R.id.brick_particle_speed_edit);
        addAllowedBrickField(BrickField.PARTICLE_SIZE, R.id.brick_particle_size_edit);
        addAllowedBrickField(BrickField.PARTICLE_END_SIZE, R.id.brick_particle_end_size_edit);
        addAllowedBrickField(BrickField.PARTICLE_GRAVITY, R.id.brick_particle_gravity_edit);
        addAllowedBrickField(BrickField.PARTICLE_MAX_COUNT, R.id.brick_particle_max_count_edit);
        addAllowedBrickField(BrickField.PARTICLE_RATE, R.id.brick_particle_rate_edit);
        addAllowedBrickField(BrickField.PARTICLE_ANGLE, R.id.brick_particle_angle_edit);
        addAllowedBrickField(BrickField.PARTICLE_RADIUS, R.id.brick_particle_radius_edit);
        addAllowedBrickField(BrickField.PARTICLE_START_COLOR, R.id.brick_particle_start_color_edit);
        addAllowedBrickField(BrickField.PARTICLE_END_COLOR, R.id.brick_particle_end_color_edit);
        addAllowedBrickField(BrickField.PARTICLE_TEXTURE, R.id.brick_particle_texture_edit);
        addAllowedBrickField(BrickField.PARTICLE_ADDITIVE, R.id.brick_particle_additive_edit);
        addAllowedBrickField(BrickField.PARTICLE_ROTATION, R.id.brick_particle_rotation_edit);
        addAllowedBrickField(BrickField.PARTICLE_ROTATION_SPEED, R.id.brick_particle_rotation_speed_edit);
        addAllowedBrickField(BrickField.PARTICLE_POS_X, R.id.brick_particle_pos_x_edit);
        addAllowedBrickField(BrickField.PARTICLE_POS_Y, R.id.brick_particle_pos_y_edit);
        addAllowedBrickField(BrickField.PARTICLE_POS_Z, R.id.brick_particle_pos_z_edit);
        addAllowedBrickField(BrickField.PARTICLE_ROT_PITCH, R.id.brick_particle_rot_pitch_edit);
        addAllowedBrickField(BrickField.PARTICLE_ROT_YAW, R.id.brick_particle_rot_yaw_edit);
        addAllowedBrickField(BrickField.PARTICLE_ROT_ROLL, R.id.brick_particle_rot_roll_edit);
    }


    public CreateParticlesBrick(String particleId) {
        this();
        setFormulaWithBrickField(BrickField.PARTICLE_ID, new Formula(particleId));
        setFormulaWithBrickField(BrickField.PARTICLE_POS_X, new Formula(0));
        setFormulaWithBrickField(BrickField.PARTICLE_POS_Y, new Formula(0));
        setFormulaWithBrickField(BrickField.PARTICLE_POS_Z, new Formula(0));
        setFormulaWithBrickField(BrickField.PARTICLE_ROT_PITCH, new Formula(0));
        setFormulaWithBrickField(BrickField.PARTICLE_ROT_YAW, new Formula(0));
        setFormulaWithBrickField(BrickField.PARTICLE_ROT_ROLL, new Formula(0));
        setFormulaWithBrickField(BrickField.PARTICLE_LOOPING, new Formula(1));
        setFormulaWithBrickField(BrickField.PARTICLE_DURATION, new Formula(5.0));
        setFormulaWithBrickField(BrickField.PARTICLE_LIFETIME, new Formula(1.5));
        setFormulaWithBrickField(BrickField.PARTICLE_SPEED, new Formula(2.0));
        setFormulaWithBrickField(BrickField.PARTICLE_SIZE, new Formula(0.5));
        setFormulaWithBrickField(BrickField.PARTICLE_END_SIZE, new Formula(2.0));
        setFormulaWithBrickField(BrickField.PARTICLE_GRAVITY, new Formula(-0.2));
        setFormulaWithBrickField(BrickField.PARTICLE_MAX_COUNT, new Formula(500));
        setFormulaWithBrickField(BrickField.PARTICLE_RATE, new Formula(40));
        setFormulaWithBrickField(BrickField.PARTICLE_ANGLE, new Formula(15));
        setFormulaWithBrickField(BrickField.PARTICLE_RADIUS, new Formula(0.2));
        setFormulaWithBrickField(BrickField.PARTICLE_START_COLOR, new Formula("#FFFF8800"));
        setFormulaWithBrickField(BrickField.PARTICLE_END_COLOR, new Formula("#66FF0000"));
        setFormulaWithBrickField(BrickField.PARTICLE_TEXTURE, new Formula(""));
        setFormulaWithBrickField(BrickField.PARTICLE_ADDITIVE, new Formula(1));
        setFormulaWithBrickField(BrickField.PARTICLE_ROTATION, new Formula(0));
        setFormulaWithBrickField(BrickField.PARTICLE_ROTATION_SPEED, new Formula(180));
    }

    public CreateParticlesBrick(Formula particleId, Formula positionX, Formula positionY, Formula positionZ,
                                Formula rotationPitch, Formula rotationYaw, Formula rotationRoll,
                                Formula looping, Formula duration, Formula startLifetime, Formula startSpeed,
                                Formula startSize, Formula endSize, Formula gravityModifier, Formula maxParticles,
                                Formula emissionRate, Formula coneAngle, Formula coneRadius,
                                Formula startColor, Formula endColor, Formula texturePath, Formula isAdditive,
                                Formula startRotation, Formula rotationOverLifetime) {
        this();
        setFormulaWithBrickField(BrickField.PARTICLE_ID, particleId);
        setFormulaWithBrickField(BrickField.PARTICLE_POS_X, positionX);
        setFormulaWithBrickField(BrickField.PARTICLE_POS_Y, positionY);
        setFormulaWithBrickField(BrickField.PARTICLE_POS_Z, positionZ);
        setFormulaWithBrickField(BrickField.PARTICLE_ROT_PITCH, rotationPitch);
        setFormulaWithBrickField(BrickField.PARTICLE_ROT_YAW, rotationYaw);
        setFormulaWithBrickField(BrickField.PARTICLE_ROT_ROLL, rotationRoll);
        setFormulaWithBrickField(BrickField.PARTICLE_LOOPING, looping);
        setFormulaWithBrickField(BrickField.PARTICLE_DURATION, duration);
        setFormulaWithBrickField(BrickField.PARTICLE_LIFETIME, startLifetime);
        setFormulaWithBrickField(BrickField.PARTICLE_SPEED, startSpeed);
        setFormulaWithBrickField(BrickField.PARTICLE_SIZE, startSize);
        setFormulaWithBrickField(BrickField.PARTICLE_END_SIZE, endSize);
        setFormulaWithBrickField(BrickField.PARTICLE_GRAVITY, gravityModifier);
        setFormulaWithBrickField(BrickField.PARTICLE_MAX_COUNT, maxParticles);
        setFormulaWithBrickField(BrickField.PARTICLE_RATE, emissionRate);
        setFormulaWithBrickField(BrickField.PARTICLE_ANGLE, coneAngle);
        setFormulaWithBrickField(BrickField.PARTICLE_RADIUS, coneRadius);
        setFormulaWithBrickField(BrickField.PARTICLE_START_COLOR, startColor);
        setFormulaWithBrickField(BrickField.PARTICLE_END_COLOR, endColor);
        setFormulaWithBrickField(BrickField.PARTICLE_TEXTURE, texturePath);
        setFormulaWithBrickField(BrickField.PARTICLE_ADDITIVE, isAdditive);
        setFormulaWithBrickField(BrickField.PARTICLE_ROTATION, startRotation);
        setFormulaWithBrickField(BrickField.PARTICLE_ROTATION_SPEED, rotationOverLifetime);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_create_particles;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createParticlesAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.PARTICLE_ID),
                getFormulaWithBrickField(BrickField.PARTICLE_LOOPING),
                getFormulaWithBrickField(BrickField.PARTICLE_DURATION),
                getFormulaWithBrickField(BrickField.PARTICLE_LIFETIME),
                getFormulaWithBrickField(BrickField.PARTICLE_SPEED),
                getFormulaWithBrickField(BrickField.PARTICLE_SIZE),
                getFormulaWithBrickField(BrickField.PARTICLE_GRAVITY),
                getFormulaWithBrickField(BrickField.PARTICLE_MAX_COUNT),
                getFormulaWithBrickField(BrickField.PARTICLE_RATE),
                getFormulaWithBrickField(BrickField.PARTICLE_ANGLE),
                getFormulaWithBrickField(BrickField.PARTICLE_RADIUS),
                getFormulaWithBrickField(BrickField.PARTICLE_START_COLOR),
                getFormulaWithBrickField(BrickField.PARTICLE_END_COLOR),
                getFormulaWithBrickField(BrickField.PARTICLE_END_SIZE),
                getFormulaWithBrickField(BrickField.PARTICLE_TEXTURE),
                getFormulaWithBrickField(BrickField.PARTICLE_ADDITIVE),
                getFormulaWithBrickField(BrickField.PARTICLE_ROTATION),
                getFormulaWithBrickField(BrickField.PARTICLE_ROTATION_SPEED),
                getFormulaWithBrickField(BrickField.PARTICLE_POS_X),
                getFormulaWithBrickField(BrickField.PARTICLE_POS_Y),
                getFormulaWithBrickField(BrickField.PARTICLE_POS_Z),
                getFormulaWithBrickField(BrickField.PARTICLE_ROT_PITCH),
                getFormulaWithBrickField(BrickField.PARTICLE_ROT_YAW),
                getFormulaWithBrickField(BrickField.PARTICLE_ROT_ROLL)
        ));
    }
}