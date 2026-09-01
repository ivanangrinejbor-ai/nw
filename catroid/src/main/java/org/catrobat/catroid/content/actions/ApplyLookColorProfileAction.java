package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.common.LookColorProfile;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Sprite;

public class ApplyLookColorProfileAction extends TemporalAction {
	private Sprite sprite;
	private LookData targetLook;

	@Override
	protected void update(float percent) {
		if (sprite == null || targetLook == null) {
			return;
		}
		LookColorProfile profile = sprite.getLookColorProfile();
		if (profile == null || targetLook.getPixmap() == null) {
			return;
		}
		TextureRegion oldRegion = targetLook.getTextureRegion();
		com.badlogic.gdx.graphics.Pixmap source = targetLook.getPixmap();
		com.badlogic.gdx.graphics.Pixmap result = profile.colorize(source);
		targetLook.setPixmap(result);
		targetLook.setTextureRegion(new TextureRegion(new Texture(result)));
		if (oldRegion != null) {
			oldRegion.getTexture().dispose();
		}
		source.dispose();
		if (sprite.look.getLookData() == targetLook) {
			sprite.look.refreshTextures(true);
		}
	}

	public void setSprite(Sprite sprite) {
		this.sprite = sprite;
	}

	public void setTargetLook(LookData targetLook) {
		this.targetLook = targetLook;
	}
}
