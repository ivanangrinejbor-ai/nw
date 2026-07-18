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
import java.util.Set;

/**
 * Merges the scripts of a {@link NeoScriptFile} into a target sprite.
 *
 * Responsibilities:
 * <ul>
 *   <li>Generate fresh internal ids (script id + every brick id) via {@link Script#clone()}.</li>
 *   <li>Re-link variable / list references to the host project / sprite (by name).</li>
 *   <li>Detect duplicate scripts using a stable signature (type + trigger) instead of
 *       memory reference.</li>
 *   <li>Honour the chosen {@link ImportStrategy} with explicit REPLACE_ALL (mode 1)
 *       and APPEND_ALL (mode 0) semantics for the AssignScriptsBrick surface.</li>
 * </ul>
 */
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
			// Atomic: clone + relink every imported script first. Only if ALL clones
			// succeed do we remove the existing scripts and add the new ones. Any
			// failure (CloneNotSupportedException) leaves the target sprite untouched.
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

		// Only scripts with a *parameterized* trigger (e.g. BroadcastScript+message)
		// can be meaningfully deduplicated. Scripts whose signature is null
		// (StartScript, WhenClonedScript, WhenTouchDownScript, WhenConditionScript, …)
		// are always unique — a sprite can have many of them doing different things.
		Set<String> targetSignatures = new HashSet<>();
		for (Script script : targetSprite.getScriptList()) {
			String sig = scriptSignature(script);
			if (sig != null) {
				targetSignatures.add(sig);
			}
		}

		if (strategy == ImportStrategy.REPLACE_DUPLICATES) {
			// Only collect parameterized-trigger scripts as candidates for removal.
			// Non-parameterized scripts in the incoming file are always added as-is.
			Set<String> importedParameterizedSigs = new HashSet<>();
			for (Script s : file.getScripts()) {
				String sig = scriptSignature(s);
				if (sig != null) {
					importedParameterizedSigs.add(sig);
				}
			}
			// BUG-NS-06/07/08 fix: atomic approach — clone all first, only then mutate.
			List<Script> toRemove = new ArrayList<>();
			for (Script script : targetSprite.getScriptList()) {
				String sig = scriptSignature(script);
				if (sig != null && importedParameterizedSigs.contains(sig)) {
					toRemove.add(script);
				}
			}
			// Clone + relink every incoming script atomically
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
			// All clones succeeded — atomically swap parameterized duplicates, add the rest
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
					// Always add, regardless of signature
					targetSprite.addScript(clone);
					result.added.add(clone);
				} else if (strategy == ImportStrategy.SKIP_DUPLICATES) {
					// null signature = non-parameterized = always unique, always add
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
			if (scriptSignature(script).equals(signature)) {
				toRemove.add(script);
			}
		}
		sprite.getScriptList().removeAll(toRemove);
	}

	/**
	 * Builds a stable, reference-free signature used for duplicate detection.
	 *
	 * <p>Returns a non-null string <em>only</em> for scripts whose trigger is parameterized
	 * (i.e. two scripts of the same type with the same parameter are genuinely duplicates).
	 * For non-parameterized scripts (StartScript, WhenClonedScript, WhenConditionScript,
	 * WhenTouchDownScript, WhenScript, etc.) returns {@code null}, meaning "always unique —
	 * never treat as a duplicate".
	 *
	 * <p>This prevents BUG-NS-07/08: losing multiple StartScript blocks that perform
	 * different actions just because they share the same class name.
	 */
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
			// Use the formula map's bucket+path as a stable key
			String bucket = fb.getFormulaMap().containsKey(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_BUCKET)
					? fb.getFormulaMap().get(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_BUCKET).getTrimmedFormulaString(null)
					: "";
			String path = fb.getFormulaMap().containsKey(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_PATH)
					? fb.getFormulaMap().get(org.catrobat.catroid.content.bricks.Brick.BrickField.FIREBASE_TRIGGER_PATH).getTrimmedFormulaString(null)
					: "";
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
		// All other script types (StartScript, WhenClonedScript, WhenConditionScript,
		// WhenTouchDownScript, WhenScript, WhenNfcScript, WhenMouseButton*, etc.)
		// have no unique trigger parameter → cannot be deduplicated → return null.
		return null;
	}
}
