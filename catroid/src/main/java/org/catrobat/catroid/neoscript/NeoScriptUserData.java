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

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.CompositeBrick;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.formulaeditor.UserVariable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class NeoScriptUserData {

	private NeoScriptUserData() {
	}

	public interface VariableResolver {
		UserVariable resolve(UserVariable variable);

		UserList resolve(UserList list);
	}

	public static void collect(org.catrobat.catroid.content.Script script, Collection<UserVariable> variables,
			Collection<UserList> lists) {
		VariableResolver collector = new VariableResolver() {
			@Override
			public UserVariable resolve(UserVariable variable) {
				if (variable != null) {
					variables.add(variable);
				}
				return variable;
			}

			@Override
			public UserList resolve(UserList list) {
				if (list != null) {
					lists.add(list);
				}
				return list;
			}
		};
		for (Brick brick : script.getBrickList()) {
			process(brick, collector, new java.util.HashSet<>());
		}
	}

	public static void relink(org.catrobat.catroid.content.Script script, Project project, Sprite sprite) {
		VariableResolver relinker = new VariableResolver() {
			@Override
			public UserVariable resolve(UserVariable variable) {
				if (variable == null) {
					return null;
				}
				UserVariable existing = sprite.getUserVariable(variable.getName());
				if (existing == null) {
					existing = project.getUserVariable(variable.getName());
				}
				if (existing == null) {
					existing = new UserVariable(variable.getName());
					if (variable.getValue() != null) {
						existing.setValue(variable.getValue());
					}
					if (!sprite.addUserVariable(existing)) {
						project.addUserVariable(existing);
					}
				}
				return existing;
			}

			@Override
			public UserList resolve(UserList list) {
				if (list == null) {
					return null;
				}
				UserList existing = sprite.getUserList(list.getName());
				if (existing == null) {
					existing = project.getUserList(list.getName());
				}
				if (existing == null) {
					existing = new UserList(list.getName());
					if (!sprite.addUserList(existing)) {
						project.addUserList(existing);
					}
				}
				return existing;
			}
		};
		for (Brick brick : script.getBrickList()) {
			process(brick, relinker, new java.util.HashSet<>());
		}
	}

	private static void process(Brick brick, VariableResolver resolver, Set<Object> visited) {
		if (brick == null || visited.contains(brick)) {
			return;
		}
		visited.add(brick);
		scanFields(brick, resolver, visited);
		if (brick instanceof CompositeBrick) {
			CompositeBrick composite = (CompositeBrick) brick;
			for (Brick nested : composite.getNestedBricks()) {
				process(nested, resolver, visited);
			}
			if (composite.hasSecondaryList()) {
				for (Brick nested : composite.getSecondaryNestedBricks()) {
					process(nested, resolver, visited);
				}
			}
		}
	}

	private static void scanFields(Object obj, VariableResolver resolver, Set<Object> visited) {
		Class<?> cls = obj.getClass();
		while (cls != null && cls != Object.class) {
			for (Field field : cls.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				Class<?> type = field.getType();
				if (type != UserVariable.class && type != UserList.class
						&& !Map.class.isAssignableFrom(type)) {
					continue;
				}
				field.setAccessible(true);
				try {
					Object value = field.get(obj);
					if (value instanceof UserVariable) {
						UserVariable replacement = resolver.resolve((UserVariable) value);
						if (replacement != value) {
							field.set(obj, replacement);
						}
					} else if (value instanceof UserList) {
						UserList replacement = resolver.resolve((UserList) value);
						if (replacement != value) {
							field.set(obj, replacement);
						}
					} else if (value instanceof Map) {
						@SuppressWarnings("unchecked")
						Map<Object, Object> map = (Map<Object, Object>) value;
						for (Map.Entry<Object, Object> entry : map.entrySet()) {
							Object entryValue = entry.getValue();
							if (entryValue instanceof UserVariable) {
								UserVariable replacement = resolver.resolve((UserVariable) entryValue);
								if (replacement != entryValue) {
									map.put(entry.getKey(), replacement);
								}
							} else if (entryValue instanceof UserList) {
								UserList replacement = resolver.resolve((UserList) entryValue);
								if (replacement != entryValue) {
									map.put(entry.getKey(), replacement);
								}
							} else if (entryValue instanceof Brick) {
								process((Brick) entryValue, resolver, visited);
							}
						}
					}
				} catch (IllegalAccessException e) {
					android.util.Log.w("NeoScriptUserData",
							"Cannot relink field " + field.getName() + " in " + obj.getClass().getSimpleName(), e);
				}
			}
			cls = cls.getSuperclass();
		}
	}
}
