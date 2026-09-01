/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.content.bricks;

import android.media.MediaMetadataRetriever;
import android.view.View;
import android.widget.TextView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.common.SoundInfo;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class PlaySoundAndWaitBrick extends PlaySoundBrick {

	private static final long serialVersionUID = 1L;

	public PlaySoundAndWaitBrick() {
	}

	@Override
	protected void onViewCreated(View prototypeView) {
		((TextView) view.findViewById(R.id.brick_play_sound_text_view))
				.setText(R.string.brick_play_sound_and_wait);
	}

	@Override
	public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
		SoundInfo resolvedSound = sound;
		if (resolvedSound != null && (resolvedSound.getFile() == null || !sprite.getSoundList().contains(resolvedSound))) {
			for (SoundInfo s : sprite.getSoundList()) {
				if (s.equals(resolvedSound) || (s.getName() != null && s.getName().equals(resolvedSound.getName()))
						|| (s.fileName != null && s.fileName.equals(resolvedSound.fileName))) {
					resolvedSound = s;
					break;
				}
			}
		}
		if (resolvedSound == null || resolvedSound.getFile() == null) {
			return;
		}
		sequence.addAction(sprite.getActionFactory().createPlaySoundAction(sprite, resolvedSound));
		sequence.addAction(sprite.getActionFactory().createWaitForSoundAction(sprite, sequence,
				new Formula(getDurationOfSound(resolvedSound)), resolvedSound.getFile().getAbsolutePath()));
	}

	private float getDurationOfSound(SoundInfo soundInfo) {
		if (soundInfo == null || soundInfo.getFile() == null) {
			return 0f;
		}
		MediaMetadataRetriever metadataRetriever = new MediaMetadataRetriever();
		try {
			metadataRetriever.setDataSource(soundInfo.getFile().getAbsolutePath());
			String durationStr = metadataRetriever
					.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
			if (durationStr != null) {
				return Integer.parseInt(durationStr) / 1000.0f;
			}
		} catch (Exception ignored) {
		} finally {
			try {
				metadataRetriever.release();
			} catch (Exception ignored) {
			}
		}
		return 0f;
	}
}
