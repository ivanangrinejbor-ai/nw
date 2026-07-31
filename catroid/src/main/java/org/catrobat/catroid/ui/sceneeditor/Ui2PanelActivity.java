/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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
package org.catrobat.catroid.ui.sceneeditor;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import org.catrobat.catroid.R;
import org.catrobat.catroid.ui.fragment.ProjectFiles2Fragment;
import org.catrobat.catroid.ui.fragment.ProjectOptionsFragment;

public class Ui2PanelActivity extends AppCompatActivity {

	public static final String EXTRA_PANEL = "extra_panel";
	public static final String PANEL_PROJECT_FILES = "project_files";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ui2_panel);

		Toolbar toolbar = findViewById(R.id.ui2_panel_toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}
		toolbar.setNavigationOnClickListener(v -> finish());

		if (savedInstanceState == null) {
			String panel = getIntent().getStringExtra(EXTRA_PANEL);
			Fragment fragment;
			if (PANEL_PROJECT_FILES.equals(panel)) {
				fragment = new ProjectFiles2Fragment();
				toolbar.setTitle("Файлы проекта 2.0");
			} else {
				fragment = new org.catrobat.catroid.ui.fragment.ProjectOptions2Fragment();
				toolbar.setTitle("Опции проекта 2.0");
			}
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragment_container, fragment, "ui2_panel_fragment")
					.commit();
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return true;
	}
}
