package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SetMaterialBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SetMaterialBrick() {
        addAllowedBrickField(BrickField.OBJECT_NAME, R.id.brick_set_material_object_edit);
        addAllowedBrickField(BrickField.MATERIAL_R, R.id.brick_set_material_r_edit);
        addAllowedBrickField(BrickField.MATERIAL_G, R.id.brick_set_material_g_edit);
        addAllowedBrickField(BrickField.MATERIAL_B, R.id.brick_set_material_b_edit);
        addAllowedBrickField(BrickField.MATERIAL_A, R.id.brick_set_material_a_edit);
        addAllowedBrickField(BrickField.MATERIAL_METALLIC, R.id.brick_set_material_metallic_edit);
        addAllowedBrickField(BrickField.MATERIAL_ROUGHNESS, R.id.brick_set_material_roughness_edit);
        addAllowedBrickField(BrickField.MATERIAL_TEXTURE_COLOR, R.id.brick_set_material_texture_color_edit);
        addAllowedBrickField(BrickField.MATERIAL_TEXTURE_NORMAL, R.id.brick_set_material_texture_normal_edit);
        addAllowedBrickField(BrickField.MATERIAL_TEXTURE_MR, R.id.brick_set_material_texture_mr_edit);
    }

    public SetMaterialBrick(String objectName) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_NAME, new Formula(objectName));
    }


    public SetMaterialBrick(Formula objectName, Formula r, Formula g, Formula b, Formula a, Formula metallic, Formula roughness,
                            Formula colorTexture, Formula normalTexture, Formula mrTexture) {
        this();
        setFormulaWithBrickField(BrickField.OBJECT_NAME, objectName);
        setFormulaWithBrickField(BrickField.MATERIAL_R, r);
        setFormulaWithBrickField(BrickField.MATERIAL_G, g);
        setFormulaWithBrickField(BrickField.MATERIAL_B, b);
        setFormulaWithBrickField(BrickField.MATERIAL_A, a);
        setFormulaWithBrickField(BrickField.MATERIAL_METALLIC, metallic);
        setFormulaWithBrickField(BrickField.MATERIAL_ROUGHNESS, roughness);
        setFormulaWithBrickField(BrickField.MATERIAL_TEXTURE_COLOR, colorTexture);
        setFormulaWithBrickField(BrickField.MATERIAL_TEXTURE_NORMAL, normalTexture);
        setFormulaWithBrickField(BrickField.MATERIAL_TEXTURE_MR, mrTexture);
    }

    public SetMaterialBrick(String objectName, Double r, Double g, Double b, Double a, Double metallic, Double roughness,
                            String colorTexture, String normalTexture, String mrTexture) {
        this(
                new Formula(objectName),
                r != null ? new Formula(r) : null,
                g != null ? new Formula(g) : null,
                b != null ? new Formula(b) : null,
                a != null ? new Formula(a) : null,
                metallic != null ? new Formula(metallic) : null,
                roughness != null ? new Formula(roughness) : null,
                colorTexture != null ? new Formula(colorTexture) : null,
                normalTexture != null ? new Formula(normalTexture) : null,
                mrTexture != null ? new Formula(mrTexture) : null
        );
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_set_material;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createSetMaterialAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.OBJECT_NAME),
                getFormulaWithBrickField(BrickField.MATERIAL_R),
                getFormulaWithBrickField(BrickField.MATERIAL_G),
                getFormulaWithBrickField(BrickField.MATERIAL_B),
                getFormulaWithBrickField(BrickField.MATERIAL_A),
                getFormulaWithBrickField(BrickField.MATERIAL_METALLIC),
                getFormulaWithBrickField(BrickField.MATERIAL_ROUGHNESS),
                getFormulaWithBrickField(BrickField.MATERIAL_TEXTURE_COLOR),
                getFormulaWithBrickField(BrickField.MATERIAL_TEXTURE_NORMAL),
                getFormulaWithBrickField(BrickField.MATERIAL_TEXTURE_MR)
        ));
    }
}