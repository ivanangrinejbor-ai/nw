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

import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Root model of a reusable script module (.neoscript file).
 *
 * Contains ONLY the exported scripts plus the user variables / lists that are
 * referenced by those scripts. It deliberately does NOT contain scenes, other
 * objects, unrelated scripts, assets or project settings.
 */
public class NeoScriptFile implements Serializable {

	public static final int FORMAT_VERSION = 1;
	public static final int MIN_SUPPORTED_VERSION = 1;
	public static final int MAX_SUPPORTED_VERSION = 1;
	public static final String ROOT_ELEMENT = "neoscript";
	public static final String APPLICATION_NAME = "NeoCatroid";
	public static final String EXTENSION = ".neoscript";

	private static final long serialVersionUID = 1L;

	private int formatVersion = FORMAT_VERSION;
	private String applicationName = APPLICATION_NAME;
	private String generatorVersion = "";
	private long creationDate = 0;

	private List<Script> scripts = new ArrayList<>();
	private List<UserVariable> userVariables = new ArrayList<>();
	private List<UserList> userLists = new ArrayList<>();

	public NeoScriptFile() {
	}

	public int getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(int formatVersion) {
		this.formatVersion = formatVersion;
	}

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getGeneratorVersion() {
		return generatorVersion;
	}

	public void setGeneratorVersion(String generatorVersion) {
		this.generatorVersion = generatorVersion;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	public List<Script> getScripts() {
		return scripts;
	}

	public void setScripts(List<Script> scripts) {
		this.scripts = scripts;
	}

	public List<UserVariable> getUserVariables() {
		return userVariables;
	}

	public void setUserVariables(List<UserVariable> userVariables) {
		this.userVariables = userVariables;
	}

	public List<UserList> getUserLists() {
		return userLists;
	}

	public void setUserLists(List<UserList> userLists) {
		this.userLists = userLists;
	}
}
