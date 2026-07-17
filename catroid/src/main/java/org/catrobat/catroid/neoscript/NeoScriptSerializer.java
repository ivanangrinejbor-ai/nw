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

import com.google.common.io.Files;
import com.thoughtworks.xstream.XStream;

import org.catrobat.catroid.io.StorageOperations;
import org.catrobat.catroid.io.XstreamSerializer;

import java.io.File;
import java.nio.charset.Charset;

/**
 * Serializes and deserializes {@link NeoScriptFile} objects reusing the existing
 * Catroid XStream configuration (so bricks, formulas, scripts and user data are
 * handled by the same converters that the project serializer uses). Adds file
 * format versioning and validation on top.
 */
public final class NeoScriptSerializer {

	private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n";

	private NeoScriptSerializer() {
	}

	private static XStream configuredXStream() {
		// Ensure the shared XStream is fully configured (project + scene aliases,
		// converters, security allow-list) before we add our own alias.
		XstreamSerializer.getInstance();
		XStream xstream = XstreamSerializer.getInstance().getXstream();
		xstream.alias(NeoScriptFile.ROOT_ELEMENT, NeoScriptFile.class);
		xstream.processAnnotations(NeoScriptFile.class);
		return xstream;
	}

	public static String serializeToString(NeoScriptFile neoScriptFile) {
		return XML_HEADER + configuredXStream().toXML(neoScriptFile);
	}

	public static void serializeToFile(NeoScriptFile neoScriptFile, File targetFile) throws Exception {
		StorageOperations.writeToFile(targetFile, serializeToString(neoScriptFile));
	}

	public static NeoScriptFile deserializeFromString(String xml) throws NeoScriptException {
		if (xml == null || xml.trim().isEmpty()) {
			throw new NeoScriptException("File is empty");
		}
		if (xml.indexOf("<" + NeoScriptFile.ROOT_ELEMENT) < 0) {
			throw new NeoScriptException("Wrong file type: missing " + NeoScriptFile.ROOT_ELEMENT + " root element");
		}
		try {
			Object result = configuredXStream().fromXML(xml);
			if (!(result instanceof NeoScriptFile)) {
				throw new NeoScriptException("Invalid .neoscript file content");
			}
			NeoScriptFile neoScriptFile = (NeoScriptFile) result;
			validate(neoScriptFile);
			return neoScriptFile;
		} catch (NeoScriptException e) {
			throw e;
		} catch (Exception e) {
			throw new NeoScriptException("Corrupted data: " + e.getMessage(), e);
		}
	}

	public static NeoScriptFile deserializeFromFile(File file) throws NeoScriptException {
		if (file == null || !file.exists()) {
			throw new NeoScriptException("File does not exist");
		}
		if (!file.getName().toLowerCase().endsWith(NeoScriptFile.EXTENSION)) {
			throw new NeoScriptException("Wrong file extension (expected " + NeoScriptFile.EXTENSION + ")");
		}
		try {
			String xml = Files.asCharSource(file, Charset.forName("UTF-8")).read();
			return deserializeFromString(xml);
		} catch (NeoScriptException e) {
			throw e;
		} catch (Exception e) {
			throw new NeoScriptException("Cannot read file: " + e.getMessage(), e);
		}
	}

	private static void validate(NeoScriptFile neoScriptFile) throws NeoScriptException {
		int version = neoScriptFile.getFormatVersion();
		if (version < NeoScriptFile.MIN_SUPPORTED_VERSION) {
			throw new NeoScriptException("Old incompatible version: " + version);
		}
		if (version > NeoScriptFile.MAX_SUPPORTED_VERSION) {
			throw new NeoScriptException("Future unsupported version: " + version);
		}
		if (neoScriptFile.getScripts() == null || neoScriptFile.getScripts().isEmpty()) {
			throw new NeoScriptException("No scripts found in file");
		}
	}
}
