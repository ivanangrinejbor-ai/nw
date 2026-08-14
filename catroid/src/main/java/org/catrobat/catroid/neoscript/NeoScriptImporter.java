/*
 * NeoCatroid
 * Copyright (C) 2026 The NeoCatroid Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package org.catrobat.catroid.neoscript;

import org.catrobat.catroid.content.BroadcastScript;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.RaspiInterruptScript;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.UserDefinedScript;
import org.catrobat.catroid.content.WhenBackgroundChangesScript;
import org.catrobat.catroid.content.WhenFirebaseChangedScript;
import org.catrobat.catroid.content.WhenGamepadButtonScript;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class NeoScriptImporter {

	private NeoScriptImporter() {
	}

	public enum ImportStrategy {
		SKIP_DUPLICATES,
		REPLACE_DUPLICATES,
		APPEND_ALL,
		REPLACE_ALL
	}

	public static final class ImportResult {
		public final List<Script> added = new ArrayList<>();
		public final List<Script> skipped = new ArrayList<>();
		public final List<Script> replaced = new ArrayList<>();
	}

	public static ImportResult importScripts(NeoScriptFile file, Project project, Sprite targetSprite,
			boolean overwrite) throws NeoScriptException {
		return importScripts(file, project, targetSprite,
				overwrite ? ImportStrategy.REPLACE_DUPLICATES : ImportStrategy.SKIP_DUPLICATES);
	}

	public static ImportResult importScripts(NeoScriptFile file, Project project, Sprite targetSprite,
			ImportStrategy strategy) throws NeoScriptException {
		if (file == null || file.getScripts() == null || file.getScripts().isEmpty()) {
			throw new NeoScriptException("No scripts to import");
		}

		ImportResult result = new ImportResult();

		if (strategy == ImportStrategy.REPLACE_ALL) {
			List<Script> imported = new ArrayList<>();
			try {
				for (Script original : file.getScripts()) {
					Script clone = original.clone();
					NeoScriptUserData.relink(clone, project, targetSprite);
					clone.setParents();
					imported.add(clone);
				}
			} catch (CloneNotSupportedException e) {
				throw new NeoScriptException("Failed to clone script: " + e.getMessage(), e);
			}
			targetSprite.getScriptList().clear();
			for (Script s : imported) {
				targetSprite.addScript(s);
				result.added.add(s);
			}
			return result;
		}

		Set<String> targetSignatures = new HashSet<>();
		for (Script script : targetSprite.getScriptList()) {
			String sig = scriptSignature(script);
			if (sig != null) {
				targetSignatures.add(sig);
			}
		}

		if (strategy == ImportStrategy.REPLACE_DUPLICATES) {
			Set<String> importedParameterizedSigs = new HashSet<>();
			for (Script s : file.getScripts()) {
				String sig = scriptSignature(s);
				if (sig != null) {
					importedParameterizedSigs.add(sig);
				}
			}
			List<Script> toRemove = new ArrayList<>();
			for (Script script : targetSprite.getScriptList()) {
				String sig = scriptSignature(script);
				if (sig != null && importedParameterizedSigs.contains(sig)) {
					toRemove.add(script);
				}
			}
			List<Script> cloned = new ArrayList<>();
			try {
				for (Script original : file.getScripts()) {
					Script clone = original.clone();
					NeoScriptUserData.relink(clone, project, targetSprite);
					clone.setParents();
					cloned.add(clone);
				}
			} catch (CloneNotSupportedException e) {
				throw new NeoScriptException("Failed to clone script: " + e.getMessage(), e);
			}
			targetSprite.getScriptList().removeAll(toRemove);
			for (Script s : cloned) {
				targetSprite.addScript(s);
				result.added.add(s);
				if (scriptSignature(s) != null) {
					result.replaced.add(s);
				}
			}
			return result;
		}

		try {
			for (Script original : file.getScripts()) {
				Script clone = original.clone();
				NeoScriptUserData.relink(clone, project, targetSprite);
				clone.setParents();

				String signature = scriptSignature(clone);

				if (strategy == ImportStrategy.APPEND_ALL) {
					targetSprite.addScript(clone);
					result.added.add(clone);
				} else if (strategy == ImportStrategy.SKIP_DUPLICATES) {
					boolean duplicate = signature != null && targetSignatures.contains(signature);
					if (duplicate) {
						result.skipped.add(clone);
					} else {
						targetSprite.addScript(clone);
						result.added.add(clone);
					}
				}
			}
		} catch (CloneNotSupportedException e) {
			throw new NeoScriptException("Failed to clone script: " + e.getMessage(), e);
		}
		return result;
	}

	private static void removeScriptBySignature(Sprite sprite, String signature) {
		List<Script> toRemove = new ArrayList<>();
		for (Script script : sprite.getScriptList()) {
			if (Objects.equals(scriptSignature(script), signature)) {
				toRemove.add(script);
			}
		}
		sprite.getScriptList().removeAll(toRemove);
	}

	public static String scriptSignature(Script script) {
		if (script instanceof BroadcastScript) {
			return "BroadcastScript#" + ((BroadcastScript) script).getBroadcastMessage();
		}
		if (script instanceof WhenBackgroundChangesScript) {
			org.catrobat.catroid.common.LookData look = ((WhenBackgroundChangesScript) script).getLook();
			return "WhenBackgroundChangesScript#" + (look != null ? look.getName() : "");
		}
		if (script instanceof WhenGamepadButtonScript) {
			return "WhenGamepadButtonScript#" + ((WhenGamepadButtonScript) script).getAction();
		}
		if (script instanceof WhenFirebaseChangedScript) {
			WhenFirebaseChangedScript fb = (WhenFirebaseChangedScript) script;
			String bucket = "";
			if (fb.getFormulaMap() != null && fb.getFormulaMap().containsKey(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_BUCKET)) {
				org.catrobat.catroid.formulaeditor.Formula f = fb.getFormulaMap().get(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_BUCKET);
				if (f != null) {
					bucket = f.getTrimmedFormulaString(null);
				}
			}
			String path = "";
			if (fb.getFormulaMap() != null && fb.getFormulaMap().containsKey(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_PATH)) {
				org.catrobat.catroid.formulaeditor.Formula f = fb.getFormulaMap().get(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_PATH);
				if (f != null) {
					path = f.getTrimmedFormulaString(null);
				}
			}
			return "WhenFirebaseChangedScript#" + bucket + "/" + path;
		}
		if (script instanceof RaspiInterruptScript) {
			RaspiInterruptScript raspi = (RaspiInterruptScript) script;
			return "RaspiInterruptScript#" + raspi.getPin() + "+" + raspi.getEventValue();
		}
		if (script instanceof UserDefinedScript) {
			UserDefinedScript uds = (UserDefinedScript) script;
			return "UserDefinedScript#" + uds.getUserDefinedBrickID();
		}
		return null;
	}
}
