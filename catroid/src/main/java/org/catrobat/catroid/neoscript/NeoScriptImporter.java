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
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;

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

		Set<String> existingSignatures = new HashSet<>();
		for (Script script : targetSprite.getScriptList()) {
			existingSignatures.add(scriptSignature(script));
		}

		try {
			for (Script original : file.getScripts()) {
				Script clone = original.clone();
				NeoScriptUserData.relink(clone, project, targetSprite);
				clone.setParents();

				String signature = scriptSignature(clone);

				if (strategy == ImportStrategy.APPEND_ALL) {
					targetSprite.addScript(clone);
					existingSignatures.add(signature);
					result.added.add(clone);
				} else {
					boolean duplicate = existingSignatures.contains(signature);
					if (duplicate) {
						if (strategy == ImportStrategy.REPLACE_DUPLICATES) {
							removeScriptBySignature(targetSprite, signature);
							existingSignatures.remove(signature);
							targetSprite.addScript(clone);
							existingSignatures.add(signature);
							result.replaced.add(clone);
							result.added.add(clone);
						} else {
							result.skipped.add(clone);
						}
					} else {
						targetSprite.addScript(clone);
						existingSignatures.add(signature);
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
	 * Combines the script type (class simple name) with its trigger / metadata.
	 */
	public static String scriptSignature(Script script) {
		StringBuilder signature = new StringBuilder(script.getClass().getSimpleName());
		if (script instanceof BroadcastScript) {
			signature.append('#').append(((BroadcastScript) script).getBroadcastMessage());
		}
		return signature.toString();
	}
}
