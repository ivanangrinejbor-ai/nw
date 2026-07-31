package org.catrobat.catroid.ui.fragment

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.danvexteam.lunoscript_annotations.LunoClass
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.content.bricks.UserDefinedBrickV2
import org.catrobat.catroid.content.bricks.UserDefinedReceiverBrickV2
import org.catrobat.catroid.utils.ToastUtil
import org.koin.android.ext.android.inject
import java.util.ArrayList

@LunoClass
class AddUserDefinedBrickV2Fragment : Fragment() {
    companion object {
        const val TAG: String = "add_user_defined_brick_v2_fragment"
        fun newInstance(addBrickListener: AddBrickFragment.OnAddBrickListener): AddUserDefinedBrickV2Fragment {
            val fragment = AddUserDefinedBrickV2Fragment()
            fragment.addBrickListener = addBrickListener
            return fragment
        }
    }

    private var addBrickListener: AddBrickFragment.OnAddBrickListener? = null
    private val projectManager: ProjectManager by inject()

    private lateinit var editBlockName: EditText
    private lateinit var parametersContainer: LinearLayout
    private lateinit var buttonAddParameter: Button
    private var confirmItem: MenuItem? = null

    private val parameterNamesList = ArrayList<String>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_user_defined_brick_v2, container, false)
        editBlockName = view.findViewById(R.id.edit_block_name)
        parametersContainer = view.findViewById(R.id.parameters_container)
        buttonAddParameter = view.findViewById(R.id.button_add_parameter)

        buttonAddParameter.setOnClickListener {
            if (parameterNamesList.size >= 5) {
                ToastUtil.showError(context, "Maximum 5 parameters are allowed in V2 blocks!")
            } else {
                parameterNamesList.add("")
                renderParameters()
            }
        }

        val activity = activity as AppCompatActivity
        val actionBar = activity.supportActionBar
        actionBar?.setTitle("Create V2 Block")
        setHasOptionsMenu(true)

        renderParameters()

        return view
    }

    private fun renderParameters() {
        parametersContainer.removeAllViews()
        for (i in 0 until parameterNamesList.size) {
            val paramView = layoutInflater.inflate(R.layout.item_user_brick_v2_param, parametersContainer, false)
            val textIndex = paramView.findViewById<TextView>(R.id.text_param_index)
            val editName = paramView.findViewById<EditText>(R.id.edit_param_name)
            val btnDelete = paramView.findViewById<ImageButton>(R.id.btn_delete_param)

            textIndex.text = "$${i + 1}:"
            editName.setText(parameterNamesList[i])

            editName.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    parameterNamesList[i] = editName.text.toString().trim()
                }
            }

            btnDelete.setOnClickListener {
                saveCurrentParamTexts()
                parameterNamesList.removeAt(i)
                renderParameters()
            }

            parametersContainer.addView(paramView)
        }
    }

    private fun saveCurrentParamTexts() {
        for (i in 0 until parametersContainer.childCount) {
            val child = parametersContainer.getChildAt(i)
            val editName = child.findViewById<EditText>(R.id.edit_param_name)
            if (i < parameterNamesList.size) {
                parameterNamesList[i] = editName.text.toString().trim()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_confirm_userdefined, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        for (index in 0 until menu.size()) {
            menu.getItem(index).isVisible = false
        }
        confirmItem = menu.findItem(R.id.confirm)
        confirmItem?.isVisible = true
        confirmItem?.isEnabled = true
        super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.confirm) {
            saveCurrentParamTexts()
            val blockName = editBlockName.text.toString().trim()
            if (blockName.isEmpty()) {
                ToastUtil.showErrorWithColor(context, R.string.brick_user_defined_already_exists, Color.RED)
                return true
            }

            val cleanedParams = ArrayList<String>()
            for (p in parameterNamesList) {
                if (p.isNotEmpty()) {
                    cleanedParams.add(p)
                }
            }

            val userBrick = UserDefinedBrickV2(blockName, cleanedParams)
            val currentSprite = projectManager.currentSprite

            currentSprite.addUserDefinedBrick(userBrick)
            addUserDefinedScriptToScript(userBrick)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addUserDefinedScriptToScript(brickToAddScript: UserDefinedBrickV2) {
        val scriptBrick = UserDefinedReceiverBrickV2(brickToAddScript)
        addBrickListener?.addBrick(scriptBrick)

        val fragmentManager = parentFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val categoryFragment = fragmentManager.findFragmentByTag(
            BrickCategoryFragment.BRICK_CATEGORY_FRAGMENT_TAG
        )
        if (categoryFragment != null) {
            fragmentTransaction.remove(categoryFragment)
            fragmentManager.popBackStack()
        }

        val userBrickListFragment = fragmentManager.findFragmentByTag(
            UserDefinedBrickListFragment.USER_DEFINED_BRICK_LIST_FRAGMENT_TAG
        )
        if (userBrickListFragment != null) {
            fragmentTransaction.remove(userBrickListFragment)
            fragmentManager.popBackStack()
        }
        fragmentTransaction.commit()
    }
}
