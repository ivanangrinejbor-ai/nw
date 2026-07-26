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
package org.catrobat.catroid.ui.recyclerview.fragment

import android.content.Intent
import android.util.Log
import android.view.Menu
import android.view.View
import androidx.annotation.PluralsRes
import androidx.appcompat.app.AppCompatActivity
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.common.Constants
import org.catrobat.catroid.common.SharedPreferenceKeys
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.utils.LockUtils
import org.catrobat.catroid.io.XstreamSerializer
import org.catrobat.catroid.io.asynctask.ProjectLoader.ProjectLoadListener
import org.catrobat.catroid.io.asynctask.loadProject
import org.catrobat.catroid.ui.UiUtils
import org.catrobat.catroid.ui.controller.BackpackListManager
import org.catrobat.catroid.ui.recyclerview.adapter.SceneAdapter
import org.catrobat.catroid.ui.recyclerview.adapter.multiselection.MultiSelectionManager
import org.catrobat.catroid.ui.recyclerview.backpack.BackpackActivity
import org.catrobat.catroid.ui.recyclerview.controller.SceneController
import org.catrobat.catroid.utils.ToastUtil
import org.koin.android.ext.android.inject
import java.io.IOException

class SceneListFragment : RecyclerViewFragment<Scene?>(),
    ProjectLoadListener {

    private val sceneController = SceneController()
    private val projectManager: ProjectManager by inject()

    override fun onResume() {
        super.onResume()
        val currentProject = projectManager.currentProject
        val hasMultipleScenes = currentProject.sceneList.size > 1 || currentProject.hasGlobalScene()
        if (!hasMultipleScenes) {
            projectManager.currentlyEditedScene = currentProject.defaultScene
            switchToSpriteListFragment()
        }
        projectManager.currentlyEditedScene = currentProject.defaultScene
        (requireActivity() as AppCompatActivity).supportActionBar?.title = currentProject.name
    }

    private fun switchToSpriteListFragment() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SpriteListFragment(), SpriteListFragment.TAG)
            .commit()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.new_group).isVisible = false
        menu.findItem(R.id.new_scene).isVisible = false
    }

    override fun initializeAdapter() {
        sharedPreferenceDetailsKey = SharedPreferenceKeys.SHOW_DETAILS_SCENES_PREFERENCE_KEY
        val scenes = ArrayList<Scene?>()
        // Global scene first (if exists)
        val project = projectManager.currentProject
        if (project.hasGlobalScene()) {
            scenes.add(project.globalScene)
        }
        scenes.addAll(project.sceneList)
        adapter = SceneAdapter(scenes)
        onAdapterReady()
    }

    override fun packItems(selectedItems: List<Scene?>) {
        setShowProgressBar(true)
        var packedItemCnt = 0
        for (item in selectedItems) {
            try {
                BackpackListManager.getInstance().scenes.add(sceneController.pack(item))
                BackpackListManager.getInstance().saveBackpack()
                packedItemCnt++
            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
        if (packedItemCnt > 0) {
            ToastUtil.showSuccess(
                activity, resources.getQuantityString(
                    R.plurals.packed_scenes,
                    packedItemCnt,
                    packedItemCnt
                )
            )
            switchToBackpack()
        }
        finishActionMode()
    }

    override fun isBackpackEmpty() = BackpackListManager.getInstance().scenes.isEmpty()

    override fun switchToBackpack() {
        val workspace = activity?.findViewById<View>(R.id.workspace_layout) as? org.catrobat.catroid.ui.workspace.WorkspaceLayout
        if (workspace != null && workspace.visibility == View.VISIBLE) {
            workspace.openWindow("BackpackScenes", "Рюкзак: Сцены") { org.catrobat.catroid.ui.recyclerview.backpack.BackpackSceneFragment() }
            return
        }
        val intent = Intent(activity, BackpackActivity::class.java)
        intent.putExtra(BackpackActivity.EXTRA_FRAGMENT_POSITION, BackpackActivity.FRAGMENT_SCENES)
        startActivity(intent)
    }

    override fun copyItems(selectedItems: List<Scene?>) {
        setShowProgressBar(true)
        var copiedItemCnt = 0
        for (item in selectedItems) {
            if (item != null && item.isGlobalScene) {
                // Глобальная сцена не копируется (единственная на проект)
                continue
            }
            try {
                adapter.add(sceneController.copy(item, projectManager.currentProject))
                copiedItemCnt++
            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
        }
        if (copiedItemCnt > 0) {
            ToastUtil.showSuccess(
                activity, resources.getQuantityString(
                    R.plurals.copied_scenes,
                    copiedItemCnt,
                    copiedItemCnt
                )
            )
        }
        finishActionMode()
    }

    @PluralsRes
    override fun getDeleteAlertTitleId() = R.plurals.delete_scenes

    override fun deleteItems(selectedItems: List<Scene?>) {
        val locked = selectedItems.filterNotNull().flatMap { LockUtils.getLockedBricks(it) }
        if (locked.isEmpty()) {
            performDelete(selectedItems)
        } else {
            LockUtils.requestPassword(requireContext(), R.string.brick_context_dialog_delete_brick) { pw ->
                if (LockUtils.verify(locked, pw)) {
                    performDelete(selectedItems)
                } else {
                    ToastUtil.showError(requireContext(), R.string.brick_wrong_password)
                }
            }
        }
    }

    private fun performDelete(selectedItems: List<Scene?>) {
        setShowProgressBar(true)
        var deletedItemsCount = 0
        for (item in selectedItems) {
            try {
                if (item != null && item.isGlobalScene) {
                    // Глобальная сцена вне sceneList — удаляем через project ссылку
                    projectManager.currentProject.setGlobalScene(null)
                    if (item.directory != null && item.directory.exists()) {
                        org.catrobat.catroid.io.StorageOperations.deleteDir(item.directory)
                    }
                } else {
                    sceneController.delete(item)
                }
            } catch (e: IOException) {
                Log.e(TAG, Log.getStackTraceString(e))
            }
            adapter.remove(item)
            deletedItemsCount++
        }
        ToastUtil.showSuccess(
            activity, resources.getQuantityString(
                R.plurals.deleted_scenes,
                deletedItemsCount,
                deletedItemsCount
            )
        )
        finishActionMode()
        val currentProject = projectManager.currentProject
        // Only regular scenes matter for the "empty" check — globalScene is separate
        if (currentProject.sceneList.isEmpty()) {
            createEmptySceneWithDefaultName()
        }
        if (currentProject.sceneList.size < 2) {
            projectManager.currentlyEditedScene = currentProject.defaultScene
            switchToSpriteListFragment()
        }
    }

    private fun createEmptySceneWithDefaultName() {
        setShowProgressBar(true)
        val currentProject = projectManager.currentProject
        // Create a fresh default scene with just a background sprite.
        // Legacy "isGlobal" sprites are migrated to this new scene;
        // the dedicated globalScene (project.globalScene) is left untouched.
        val legacyGlobalSprites = currentProject.sceneList
            .flatMap { it.getSpriteList() }
            .filter { it.isGlobal() }
            .toList()
        for (sprite in legacyGlobalSprites) {
            currentProject.sceneList.forEach { it.removeSprite(sprite) }
        }
        val scene = Scene(getString(R.string.default_scene_name), currentProject)
        val backgroundSprite = Sprite(getString(R.string.background))
        backgroundSprite.look.zIndex = Constants.Z_INDEX_BACKGROUND
        scene.addSprite(backgroundSprite)
        for (sprite in legacyGlobalSprites) {
            scene.addSprite(sprite)
        }
        adapter.add(scene)
        currentProject.addScene(scene)
        setShowProgressBar(false)
    }

    override fun getRenameDialogTitle() = R.string.rename_scene_dialog

    override fun getRenameDialogHint() = R.string.scene_name_label

    override fun renameItem(item: Scene?, name: String) {
        if (item?.name != name) {
            if (sceneController.rename(item, name)) {
                val currentProject = projectManager.currentProject
                XstreamSerializer.getInstance().saveProject(currentProject)
                loadProject(currentProject.directory, requireContext().applicationContext)
                initializeAdapter()
            } else {
                ToastUtil.showError(activity, R.string.error_rename_scene)
            }
        }
        finishActionMode()
    }

    override fun onItemClick(item: Scene?, selectionManager: MultiSelectionManager?) {
        when (actionModeType) {
            RENAME -> {
                super.onItemClick(item, null)
                return
            }
            NONE -> {
                projectManager.currentlyEditedScene = item

                val workspace = activity?.findViewById<View>(R.id.workspace_layout) as? org.catrobat.catroid.ui.workspace.WorkspaceLayout
                if (workspace != null && workspace.visibility == View.VISIBLE) {
                    workspace.openWindow(SpriteListFragment.TAG, "Спрайты") { SpriteListFragment() }

                    workspace.removeWindow(SceneListFragment.TAG, force = false)

                    val sprites = requireActivity().supportFragmentManager.findFragmentByTag(SpriteListFragment.TAG) as? SpriteListFragment
                    sprites?.initializeAdapter()
                    return
                }

                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SpriteListFragment(), SpriteListFragment.TAG)
                    .addToBackStack(SpriteListFragment.TAG)
                    .commit()
            }
            else -> super.onItemClick(item, selectionManager)
        }
    }

    override fun onSettingsClick(item: Scene?, view: View) {
        val itemList = mutableListOf<Scene?>()
        itemList.add(item)

        val hiddenOptionMenuIds = intArrayOf(
            R.id.new_group,
            R.id.new_scene,
            R.id.show_details,
            R.id.project_options,
            R.id.project_files,
            R.id.project_libs,
            R.id.editor3d,
            R.id.edit,
            R.id.from_local,
            R.id.from_library
        )
        val popupMenu = UiUtils.createSettingsPopUpMenu(view, requireContext(), R.menu
            .menu_project_activity, hiddenOptionMenuIds)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.backpack -> packItems(itemList)
                R.id.copy -> copyItems(itemList)
                R.id.rename -> showRenameDialog(item)
                R.id.delete -> {
                    if (item != null && item.isGlobalScene) {
                        showDeleteGlobalSceneDialog(item)
                    } else {
                        deleteItems(itemList)
                    }
                }
                R.id.scene_transition -> showTransitionDialog(item)
                else -> {
                }
            }
            true
        }
        // Глобальная сцена: копирование/рюкзак не поддерживаются (она вне sceneList)
        if (item != null && item.isGlobalScene) {
            popupMenu.menu.findItem(R.id.backpack)?.isVisible = false
            popupMenu.menu.findItem(R.id.copy)?.isVisible = false
            popupMenu.menu.findItem(R.id.scene_transition)?.isVisible = false
        }
        popupMenu.menu.findItem(R.id.backpack).setTitle(R.string.pack)
        popupMenu.show()
    }

    private fun showDeleteGlobalSceneDialog(scene: Scene) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(scene.name)
            .setMessage(R.string.delete_global_scene_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                val project = projectManager.currentProject
                project.setGlobalScene(null)
                try {
                    if (scene.directory != null && scene.directory.exists()) {
                        org.catrobat.catroid.io.StorageOperations.deleteDir(scene.directory)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Cannot delete global scene directory", e)
                }
                XstreamSerializer.getInstance().saveProject(project)
                initializeAdapter()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showTransitionDialog(scene: Scene?) {
        if (scene == null) return
        val context = requireContext()

        val layout = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(60, 20, 60, 0)
        }

        fun buildSection(
            headerRes: Int,
            options: Array<String>,
            selected: Int,
            duration: Float,
            onSeek: (Float) -> Unit
        ): android.widget.Spinner {
            val header = android.widget.TextView(context).apply {
                setText(headerRes)
                textSize = 16f
                setPadding(0, 16, 0, 4)
            }

            val spinner = android.widget.Spinner(context).apply {
                adapter = android.widget.ArrayAdapter(context, android.R.layout.simple_spinner_item, options).also {
                    it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                setSelection(selected.coerceIn(0, options.size - 1))
            }

            val valueText = android.widget.TextView(context).apply {
                text = String.format(java.util.Locale.US, "%.1f s", duration)
                setPadding(0, 0, 16, 0)
            }
            val seek = android.widget.SeekBar(context).apply {
                max = 50
                progress = (duration * 10).toInt().coerceIn(1, 50)
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(s: android.widget.SeekBar?, p: Int, fromUser: Boolean) {
                        val v = (p / 10f).coerceAtLeast(0.1f)
                        valueText.text = String.format(java.util.Locale.US, "%.1f s", v)
                        onSeek(v)
                    }
                    override fun onStartTrackingTouch(s: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(s: android.widget.SeekBar?) {}
                })
            }

            val durRow = android.widget.LinearLayout(context).apply { orientation = android.widget.LinearLayout.HORIZONTAL }
            val durLabel = android.widget.TextView(context).apply {
                setText(R.string.scene_transition_duration)
                setPadding(0, 0, 16, 0)
            }
            durRow.addView(durLabel)
            durRow.addView(valueText, android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            layout.addView(header)
            layout.addView(spinner)
            layout.addView(durRow)
            layout.addView(seek)
            return spinner
        }

        val startOptions = arrayOf(
            context.getString(R.string.scene_transition_none),
            context.getString(R.string.scene_transition_fade_in)
        )
        val exitOptions = arrayOf(
            context.getString(R.string.scene_transition_none),
            context.getString(R.string.scene_transition_fade_out)
        )

        var startType = scene.startTransitionType
        var startDur = scene.startTransitionDuration
        var exitType = scene.exitTransitionType
        var exitDur = scene.exitTransitionDuration

        val startSpinner = buildSection(
            R.string.scene_transition_start, startOptions, scene.startTransitionType, scene.startTransitionDuration
        ) { startDur = it }
        val exitSpinner = buildSection(
            R.string.scene_transition_exit, exitOptions, scene.exitTransitionType, scene.exitTransitionDuration
        ) { exitDur = it }

        androidx.appcompat.app.AlertDialog.Builder(context)
            .setTitle(R.string.scene_transition_title)
            .setView(layout)
            .setPositiveButton(R.string.ok) { _, _ ->
                scene.startTransitionType = startSpinner.selectedItemPosition
                scene.startTransitionDuration = startDur.coerceAtLeast(0.1f)
                scene.exitTransitionType = exitSpinner.selectedItemPosition
                scene.exitTransitionDuration = exitDur.coerceAtLeast(0.1f)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onLoadFinished(success: Boolean) {
        if (!success) {
            ToastUtil.showError(activity, R.string.error_load_project)
            return
        }
        val scenes = ArrayList<Scene?>()
        val project = projectManager.currentProject
        if (project.hasGlobalScene()) {
            scenes.add(project.globalScene)
        }
        scenes.addAll(project.sceneList)
        adapter.items = scenes
    }

    companion object {
        val TAG: String = SceneListFragment::class.java.simpleName
    }
}
