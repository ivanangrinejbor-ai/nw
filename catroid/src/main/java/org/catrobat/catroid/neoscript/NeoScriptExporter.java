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

import org.catrobat.catroid.BuildConfig;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds a {@link NeoScriptFile} from a set of selected scripts, collecting the
 * user variables / lists that those scripts reference so the module is
 * self-describing.
 */
public final class NeoScriptExporter {

	private NeoScriptExporter() {
	}

	public static NeoScriptFile buildFromScripts(List<Script> scripts, Project project, Sprite sprite) {
		NeoScriptFile file = new NeoScriptFile();
		file.setApplicationName(NeoScriptFile.APPLICATION_NAME);
		file.setGeneratorVersion(BuildConfig.VERSION_NAME != null ? BuildConfig.VERSION_NAME : "");
		file.setCreationDate(System.currentTimeMillis());

		Set<UserVariable> variables = new HashSet<>();
		Set<UserList> lists = new HashSet<>();
		for (Script script : scripts) {
			file.getScripts().add(script);
			NeoScriptUserData.collect(script, variables, lists);
		}
		file.getUserVariables().addAll(variables);
		file.getUserLists().addAll(lists);
		return file;
	}
}
