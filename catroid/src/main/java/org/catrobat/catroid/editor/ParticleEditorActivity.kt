package org.catrobat.catroid.editor

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.badlogic.gdx.Gdx
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.R
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.raptor.GameObject
import org.catrobat.catroid.raptor.ParticleComponent
import org.catrobat.catroid.raptor.ParticleCurvePoint
import org.catrobat.catroid.raptor.ParticleSystem3DComponent
import org.catrobat.catroid.raptor.SceneManager
import org.catrobat.catroid.raptor.ThreeDManager
import java.util.Locale

class ParticleEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_GAME_OBJECT_ID = "game_object_id"
        const val EXTRA_SPRITE_NAME = "sprite_name"
        const val EXTRA_USE_UI2 = "use_ui2"

        fun launch(context: Context, gameObjectId: String, useUi2: Boolean = false) {
            val intent = Intent(context, ParticleEditorActivity::class.java)
            intent.putExtra(EXTRA_GAME_OBJECT_ID, gameObjectId)
            intent.putExtra(EXTRA_USE_UI2, useUi2)
            context.startActivity(intent)
        }

        fun launchBySpriteName(context: Context, spriteName: String, useUi2: Boolean = false) {
            val intent = Intent(context, ParticleEditorActivity::class.java)
            intent.putExtra(EXTRA_SPRITE_NAME, spriteName)
            intent.putExtra(EXTRA_USE_UI2, useUi2)
            context.startActivity(intent)
        }
    }

    private lateinit var scrollView: ScrollView
    private lateinit var mainLayout: LinearLayout
    private var gameObject: GameObject? = null
    private var particleSystem3D: ParticleSystem3DComponent? = null
    private var legacyParticle: ParticleComponent? = null
    private var isUi2 = false

    private val updateHandler = Handler(Looper.getMainLooper())
    private var pendingUpdate: Runnable? = null

    private val density by lazy { resources.displayMetrics.density }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isUi2 = intent.getBooleanExtra(EXTRA_USE_UI2, false)

        if (isUi2) {
            setTheme(R.style.Catroid)
        }

        val gameObjectId = intent.getStringExtra(EXTRA_GAME_OBJECT_ID)
        val spriteName = intent.getStringExtra(EXTRA_SPRITE_NAME)

        if (gameObjectId == null && spriteName == null) {
            Toast.makeText(this, "No object ID or sprite name provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val sceneManager = SceneManager.getInstance()
        gameObject = if (gameObjectId != null) {
            sceneManager?.findGameObject(gameObjectId)
        } else {
            sceneManager?.findGameObject(spriteName)
        }

        if (gameObject == null) {
            val msg = if (sceneManager == null) {
                "Scene Manager not available. Open 3D editor first."
            } else {
                "Object not found for: ${gameObjectId ?: spriteName}"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        particleSystem3D = gameObject!!.getComponent(ParticleSystem3DComponent::class.java)
        legacyParticle = gameObject!!.getComponent(ParticleComponent::class.java)

        if (particleSystem3D == null && legacyParticle == null) {
            Toast.makeText(this, "No particle component found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        buildEditor()
    }

    private fun setupUI() {
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (12 * density).toInt()
            setPadding(pad, pad, pad, pad)
        }

        scrollView.addView(mainLayout)
        setContentView(scrollView)

        supportActionBar?.apply {
            title = if (particleSystem3D != null) "Particle System 3D" else "Particle Effect"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onDestroy() {
        updateHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private fun buildEditor() {
        mainLayout.removeAllViews()

        if (particleSystem3D != null) {
            build3DEditor(particleSystem3D!!)
        } else if (legacyParticle != null) {
            buildLegacyEditor(legacyParticle!!)
        }
    }

    private fun build3DEditor(ps: ParticleSystem3DComponent) {
        addSectionHeader("Main")

        addCheckbox("Looping", ps.looping) { ps.looping = it; updatePS3D() }
        addCheckbox("Prewarm", ps.prewarm) { ps.prewarm = it; updatePS3D() }
        addFloatInput("Duration", ps.duration) { ps.duration = it; updatePS3D() }
        addFloatInput("Max Particles", ps.maxParticles.toFloat()) { ps.maxParticles = it.toInt(); updatePS3D() }

        addMinMaxCurveEditor("Start Lifetime", ps.startLifetime)
        addMinMaxCurveEditor("Start Speed", ps.startSpeed)
        addMinMaxCurveEditor("Start Size", ps.startSize)
        addMinMaxCurveEditor("Gravity Modifier", ps.gravityModifier)

        addSpinnerEnum("Simulation Space", ParticleSystem3DComponent.SimulationSpace.values(),
            ps.simulationSpace.ordinal) { ps.simulationSpace = it; updatePS3DImmediate() }

        addModuleSection("Emission", ps.emission.enabled, { ps.emission.enabled = it; updatePS3D() }) {
            addMinMaxCurveEditor("Rate Over Time", ps.emission.rateOverTime)
            addMinMaxCurveEditor("Rate Over Distance", ps.emission.rateOverDistance)

            addSectionHeader("Bursts")
            for (i in ps.emission.bursts.indices) {
                val burst = ps.emission.bursts[i]
                val idx = i
                addHorizontalRow { row ->
                    addSmallFloatInput(row, "Time", burst.time) { burst.time = it; updatePS3D() }
                    addSmallFloatInput(row, "Count", burst.count.constantMax) {
                        burst.count = ParticleSystem3DComponent.MinMaxCurve(it)
                        updatePS3D()
                    }
                    addSmallFloatInput(row, "Prob", burst.probability) { burst.probability = it; updatePS3D() }
                    addButton(row, "X", Color.RED) {
                        ps.emission.bursts.removeAt(idx)
                        updatePS3D()
                        buildEditor()
                    }
                }
            }
            addButton(mainLayout, "+ Burst", Color.WHITE) {
                ps.emission.bursts.add(ParticleSystem3DComponent.Burst(0f, 30f))
                updatePS3D()
                buildEditor()
            }
        }

        addModuleSection("Shape", ps.shape.enabled, { ps.shape.enabled = it; updatePS3D() }) {
            addSpinnerEnum("Shape Type", ParticleSystem3DComponent.ShapeType.values(),
                ps.shape.type.ordinal) { ps.shape.type = it; updatePS3D(); buildEditor() }

            when (ps.shape.type) {
                ParticleSystem3DComponent.ShapeType.CONE -> {
                    addFloatInput("Angle", ps.shape.coneAngle) { ps.shape.coneAngle = it; updatePS3D() }
                    addFloatInput("Radius", ps.shape.coneRadius) { ps.shape.coneRadius = it; updatePS3D() }
                    addFloatInput("Length", ps.shape.coneLength) { ps.shape.coneLength = it; updatePS3D() }
                }
                ParticleSystem3DComponent.ShapeType.SPHERE,
                ParticleSystem3DComponent.ShapeType.HEMISPHERE -> {
                    addFloatInput("Radius", ps.shape.sphereRadius) { ps.shape.sphereRadius = it; updatePS3D() }
                }
                ParticleSystem3DComponent.ShapeType.BOX -> {
                    addFloatInput("X", ps.shape.boxSize.x) { ps.shape.boxSize.x = it; updatePS3D() }
                    addFloatInput("Y", ps.shape.boxSize.y) { ps.shape.boxSize.y = it; updatePS3D() }
                    addFloatInput("Z", ps.shape.boxSize.z) { ps.shape.boxSize.z = it; updatePS3D() }
                }
                ParticleSystem3DComponent.ShapeType.CIRCLE -> {
                    addFloatInput("Radius", ps.shape.circleRadius) { ps.shape.circleRadius = it; updatePS3D() }
                    addFloatInput("Arc", ps.shape.circleArc) { ps.shape.circleArc = it; updatePS3D() }
                }
                ParticleSystem3DComponent.ShapeType.EDGE -> {
                    addFloatInput("Length", ps.shape.edgeLength) { ps.shape.edgeLength = it; updatePS3D() }
                }
                else -> {}
            }

            addSpinnerEnum("Emit From", ParticleSystem3DComponent.EmitFrom.values(),
                ps.shape.emitFrom.ordinal) { ps.shape.emitFrom = it; updatePS3D() }
        }

        addModuleSection("Velocity Over Lifetime", ps.velocityOverLifetime.enabled,
            { ps.velocityOverLifetime.enabled = it; updatePS3D() }) {
            addMinMaxCurveEditor("Linear X", ps.velocityOverLifetime.x)
            addMinMaxCurveEditor("Linear Y", ps.velocityOverLifetime.y)
            addMinMaxCurveEditor("Linear Z", ps.velocityOverLifetime.z)
            addMinMaxCurveEditor("Orbital Y", ps.velocityOverLifetime.orbitalY)
            addMinMaxCurveEditor("Radial", ps.velocityOverLifetime.radial)
            addMinMaxCurveEditor("Speed Modifier", ps.velocityOverLifetime.speedModifier)
        }

        addModuleSection("Force Over Lifetime", ps.forceOverLifetime.enabled,
            { ps.forceOverLifetime.enabled = it; updatePS3D() }) {
            addMinMaxCurveEditor("Force X", ps.forceOverLifetime.x)
            addMinMaxCurveEditor("Force Y", ps.forceOverLifetime.y)
            addMinMaxCurveEditor("Force Z", ps.forceOverLifetime.z)
        }

        addModuleSection("Color Over Lifetime", ps.colorOverLifetime.enabled,
            { ps.colorOverLifetime.enabled = it; updatePS3D() }) {
            addMinMaxGradientEditor("Color", ps.colorOverLifetime.color)
        }

        addModuleSection("Size Over Lifetime", ps.sizeOverLifetime.enabled,
            { ps.sizeOverLifetime.enabled = it; updatePS3D() }) {
            addCheckbox("Separate Axes", ps.sizeOverLifetime.separateAxes) {
                ps.sizeOverLifetime.separateAxes = it; updatePS3D(); buildEditor()
            }
            if (ps.sizeOverLifetime.separateAxes) {
                addMinMaxCurveEditor("X", ps.sizeOverLifetime.sizeX)
                addMinMaxCurveEditor("Y", ps.sizeOverLifetime.sizeY)
                addMinMaxCurveEditor("Z", ps.sizeOverLifetime.sizeZ)
            } else {
                addMinMaxCurveEditor("Size", ps.sizeOverLifetime.size)
            }
        }

        addModuleSection("Rotation Over Lifetime", ps.rotationOverLifetime.enabled,
            { ps.rotationOverLifetime.enabled = it; updatePS3D() }) {
            addCheckbox("Separate Axes", ps.rotationOverLifetime.separateAxes) {
                ps.rotationOverLifetime.separateAxes = it; updatePS3D(); buildEditor()
            }
            if (ps.rotationOverLifetime.separateAxes) {
                addMinMaxCurveEditor("X", ps.rotationOverLifetime.angularVelocityX)
                addMinMaxCurveEditor("Y", ps.rotationOverLifetime.angularVelocityY)
                addMinMaxCurveEditor("Z", ps.rotationOverLifetime.angularVelocityZ)
            } else {
                addMinMaxCurveEditor("Angular Velocity", ps.rotationOverLifetime.angularVelocity)
            }
        }

        addModuleSection("Noise", ps.noise.enabled, { ps.noise.enabled = it; updatePS3D() }) {
            addFloatInput("Strength", ps.noise.strength) { ps.noise.strength = it; updatePS3D() }
            addFloatInput("Frequency", ps.noise.frequency) { ps.noise.frequency = it; updatePS3D() }
            addFloatInput("Octaves", ps.noise.octaves.toFloat()) { ps.noise.octaves = Math.max(1, it.toInt()); updatePS3D() }
            addFloatInput("Scroll Speed", ps.noise.scrollSpeed) { ps.noise.scrollSpeed = it; updatePS3D() }
            addCheckbox("Damping", ps.noise.damping) { ps.noise.damping = it; updatePS3D() }
            addCheckbox("Separate Axes", ps.noise.separateAxes) {
                ps.noise.separateAxes = it; updatePS3D(); buildEditor()
            }
            if (ps.noise.separateAxes) {
                addFloatInput("X", ps.noise.strengthX) { ps.noise.strengthX = it; updatePS3D() }
                addFloatInput("Y", ps.noise.strengthY) { ps.noise.strengthY = it; updatePS3D() }
                addFloatInput("Z", ps.noise.strengthZ) { ps.noise.strengthZ = it; updatePS3D() }
            }
        }

        addModuleSection("Collision", ps.collision.enabled, { ps.collision.enabled = it; updatePS3D() }) {
            addSpinnerEnum("Mode", ParticleSystem3DComponent.CollisionMode.values(),
                ps.collision.mode.ordinal) { ps.collision.mode = it; updatePS3D(); buildEditor() }
            addFloatInput("Bounce", ps.collision.bounce) { ps.collision.bounce = it; updatePS3D() }
            addFloatInput("Dampen", ps.collision.dampen) { ps.collision.dampen = it; updatePS3D() }
            addFloatInput("Lifetime Loss", ps.collision.lifetimeLoss) { ps.collision.lifetimeLoss = it; updatePS3D() }
            addFloatInput("Min Kill Speed", ps.collision.minKillSpeed) { ps.collision.minKillSpeed = it; updatePS3D() }
            addFloatInput("Radius Scale", ps.collision.radiusScale) { ps.collision.radiusScale = it; updatePS3D() }
            addSpinnerEnum("Quality", ParticleSystem3DComponent.CollisionQuality.values(),
                ps.collision.quality.ordinal) { ps.collision.quality = it; updatePS3D() }

            if (ps.collision.mode == ParticleSystem3DComponent.CollisionMode.PLANES) {
                addSectionHeader("Collision Planes")
                for (i in ps.collision.planes.indices) {
                    val plane = ps.collision.planes[i]
                    val idx = i
                    addHorizontalRow { row ->
                        addSmallFloatInput(row, "Point Y", plane.point.y) { plane.point.y = it; updatePS3D() }
                        addSmallFloatInput(row, "Normal Y", plane.normal.y) { plane.normal.y = it; updatePS3D() }
                        addButton(row, "X", Color.RED) {
                            ps.collision.planes.removeAt(idx)
                            updatePS3D()
                            buildEditor()
                        }
                    }
                }
                addButton(mainLayout, "+ Plane", Color.WHITE) {
                    ps.collision.planes.add(ParticleSystem3DComponent.CollisionPlane())
                    updatePS3D()
                    buildEditor()
                }
            }
        }

        addModuleSection("Sub Emitters", ps.subEmitters.enabled,
            { ps.subEmitters.enabled = it; updatePS3D() }) {
            for (i in ps.subEmitters.entries.indices) {
                val entry = ps.subEmitters.entries[i]
                val idx = i

                addSpinnerEnum("Trigger", ParticleSystem3DComponent.SubEmitterTrigger.values(),
                    entry.trigger.ordinal) { entry.trigger = it; updatePS3D() }

                addHorizontalRow { row ->
                    addButton(row, entry.subEmitterObjectId ?: "Select...", Color.WHITE) {
                        showSubEmitterPicker(entry)
                    }
                    addSmallFloatInput(row, "Prob", entry.probability) { entry.probability = it; updatePS3D() }
                    addSmallFloatInput(row, "Count", entry.emitCount.toFloat()) {
                        entry.emitCount = Math.max(0, it.toInt()); updatePS3D()
                    }
                    addButton(row, "X", Color.RED) {
                        ps.subEmitters.entries.removeAt(idx)
                        updatePS3D()
                        buildEditor()
                    }
                }
            }
            addButton(mainLayout, "+ Sub Emitter", Color.WHITE) {
                ps.subEmitters.entries.add(ParticleSystem3DComponent.SubEmitterEntry())
                updatePS3D()
                buildEditor()
            }
        }

        addModuleSection("Trails", ps.trails.enabled, { ps.trails.enabled = it; updatePS3D() }) {
            addFloatInput("Ratio", ps.trails.ratio) { ps.trails.ratio = it; updatePS3D() }
            addFloatInput("Lifetime", ps.trails.lifetime) { ps.trails.lifetime = it; updatePS3D() }
            addFloatInput("Min Vertex Dist", ps.trails.minimumVertexDistance) { ps.trails.minimumVertexDistance = it; updatePS3D() }
            addCheckbox("World Space", ps.trails.worldSpace) { ps.trails.worldSpace = it; updatePS3D() }
            addCheckbox("Die With Particles", ps.trails.dieWithParticles) { ps.trails.dieWithParticles = it; updatePS3D() }
            addCheckbox("Inherit Color", ps.trails.inheritParticleColor) { ps.trails.inheritParticleColor = it; updatePS3D() }
            addMinMaxCurveEditor("Width Over Trail", ps.trails.widthOverTrail)
        }

        addModuleSection("Texture Sheet Animation", ps.textureSheetAnimation.enabled,
            { ps.textureSheetAnimation.enabled = it; updatePS3D() }) {
            addFloatInput("Tiles X", ps.textureSheetAnimation.tilesX.toFloat()) { ps.textureSheetAnimation.tilesX = Math.max(1, it.toInt()); updatePS3D() }
            addFloatInput("Tiles Y", ps.textureSheetAnimation.tilesY.toFloat()) { ps.textureSheetAnimation.tilesY = Math.max(1, it.toInt()); updatePS3D() }
            addFloatInput("Cycles", ps.textureSheetAnimation.cycles.toFloat()) { ps.textureSheetAnimation.cycles = Math.max(1, it.toInt()); updatePS3D() }
            addMinMaxCurveEditor("Frame Over Time", ps.textureSheetAnimation.frameOverTime)
        }

        addSectionHeader("Renderer")

        val supportedModes = arrayOf(
            ParticleSystem3DComponent.RenderMode.BILLBOARD,
            ParticleSystem3DComponent.RenderMode.STRETCHED_BILLBOARD,
            ParticleSystem3DComponent.RenderMode.HORIZONTAL_BILLBOARD,
            ParticleSystem3DComponent.RenderMode.VERTICAL_BILLBOARD,
            ParticleSystem3DComponent.RenderMode.MESH
        )
        var currentModeIdx = 0
        for (i in supportedModes.indices) {
            if (supportedModes[i] == ps.renderer.renderMode) { currentModeIdx = i; break }
        }
        addSpinnerEnum("Render Mode", supportedModes, currentModeIdx) {
            ps.renderer.renderMode = it; updatePS3DImmediate(); buildEditor()
        }

        if (ps.renderer.renderMode == ParticleSystem3DComponent.RenderMode.STRETCHED_BILLBOARD) {
            addFloatInput("Length Scale", ps.renderer.lengthScale) { ps.renderer.lengthScale = it; updatePS3D() }
            addFloatInput("Speed Scale", ps.renderer.speedScale) { ps.renderer.speedScale = it; updatePS3D() }
        }

        if (ps.renderer.renderMode == ParticleSystem3DComponent.RenderMode.MESH) {
            val meshTypes = arrayOf(
                ParticleSystem3DComponent.MeshType.CUBE,
                ParticleSystem3DComponent.MeshType.SPHERE_LOW,
                ParticleSystem3DComponent.MeshType.CYLINDER_LOW,
                ParticleSystem3DComponent.MeshType.CUSTOM
            )
            var currentMeshIdx = 0
            for (i in meshTypes.indices) {
                if (meshTypes[i] == ps.renderer.meshType) { currentMeshIdx = i; break }
            }
            addSpinnerEnum("Mesh Shape", meshTypes, currentMeshIdx) {
                ps.renderer.meshType = it; updatePS3DImmediate(); buildEditor()
            }

            if (ps.renderer.meshType == ParticleSystem3DComponent.MeshType.CUSTOM) {
                addHorizontalRow { row ->
                    addLabel(row, ps.renderer.meshPath ?: "None")
                    addButton(row, "Set .glb", Color.WHITE) { showMeshPicker(ps) }
                }
            }

            addCheckbox("Align To Velocity", ps.renderer.alignToVelocity) { ps.renderer.alignToVelocity = it; updatePS3DImmediate() }
        }

        addCheckbox("Additive Blend", ps.renderer.isAdditive) { ps.renderer.isAdditive = it; updatePS3DImmediate() }

        addHorizontalRow { row ->
            addLabel(row, ps.renderer.texturePath ?: "Default")
            addButton(row, "Set", Color.WHITE) { showTexturePicker(ps) }
            addButton(row, "Clear", Color.RED) {
                ps.renderer.texturePath = null; updatePS3DImmediate(); buildEditor()
            }
        }
    }

    private fun buildLegacyEditor(p: ParticleComponent) {
        addSectionHeader("Legacy Particle Effect")

        p.migrateOldDataIfNeeded()

        val basicView = layoutInflater.inflate(R.layout.inspector_particle, mainLayout, false)
        setWhiteTextToAllChildren(basicView as ViewGroup)
        mainLayout.addView(basicView)

        hideOldFields(basicView)

        val loopingCheck = basicView.findViewById<CheckBox>(R.id.p_looping)
        loopingCheck?.isChecked = p.looping
        loopingCheck?.setOnCheckedChangeListener { _, isChecked -> p.looping = isChecked; updateParticles() }

        setupFloatParam(basicView, R.id.p_duration, "Duration", p.duration) { p.duration = it; updateParticles() }
        setupFloatParam(basicView, R.id.p_start_lifetime, "Start Lifetime", p.startLifetime) { p.startLifetime = it; updateParticles() }
        setupFloatParam(basicView, R.id.p_max_particles, "Max Particles", p.maxParticles.toFloat()) { p.maxParticles = it.toInt(); updateParticles() }
        setupFloatParam(basicView, R.id.p_emission_rate, "Emission Rate", p.emissionRate) { p.emissionRate = it; updateParticles() }

        addSectionHeader("Spawn Shape")
        addSpinnerEnum("Shape", ParticleComponent.SpawnShape.values(),
            p.spawnShape.ordinal) { p.spawnShape = it; updateParticles() }
        addFloatInput("Size X", p.spawnSize.x) { p.spawnSize.x = it; updateParticles() }
        addFloatInput("Size Y", p.spawnSize.y) { p.spawnSize.y = it; updateParticles() }
        addFloatInput("Size Z", p.spawnSize.z) { p.spawnSize.z = it; updateParticles() }
        addCheckbox("Spawn on Surface Only", p.spawnOnSurface) { p.spawnOnSurface = it; updateParticles() }

        addSectionHeader("Appearance")
        addFloatInput("Base Size", p.baseSize) { p.baseSize = it; updateParticles() }

        addSectionHeader("Graphs")
        addFloatGraphEditor("Size Over Lifetime", p.sizeGraph, 0f, 3f)
        addFloatGraphEditor("Speed Along Shape", p.speedGraph, 0f, 10f)
        addFloatGraphEditor("Gravity Y", p.gravityGraph, -10f, 10f)
        addFloatGraphEditor("Vortex", p.vortexGraph, -10f, 10f)
        addFloatGraphEditor("Turbulence", p.turbulenceGraph, 0f, 10f)
        addFloatGraphEditor("Rotation (deg)", p.rotationGraph, -180f, 180f)
        addColorGraphEditor("Color Over Lifetime", p.colorGraph)
    }

    private fun hideOldFields(view: View) {
        val ids = intArrayOf(
            R.id.p_start_speed, R.id.p_start_size, R.id.p_gravity,
            R.id.p_end_size, R.id.p_start_rotation, R.id.p_rotation_over_lifetime, R.id.p_cone_radius
        )
        for (id in ids) {
            val v = view.findViewById<View>(id)
            if (v != null) v.visibility = View.GONE
        }
        val colorStart = view.findViewById<View>(R.id.p_start_color)
        if (colorStart != null && colorStart.parent is View) {
            (colorStart.parent as View).visibility = View.GONE
        }
    }

    private fun setupFloatParam(view: View, id: Int, label: String, value: Float, onChange: (Float) -> Unit) {
        val includedLayout = view.findViewById<View>(id) ?: return
        val editText = includedLayout.findViewById<EditText>(R.id.edit_param_value) ?: return
        editText.setText(String.format(Locale.US, "%.2f", value))
        addDelayedTextListener(editText) {
            try { onChange(it.toFloat()) } catch (_: Exception) {}
        }
    }

    private fun addSectionHeader(title: String) {
        val tv = TextView(this).apply {
            text = title
            setTextColor(if (isUi2) 0xFF00D4FF.toInt() else Color.WHITE)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, (20 * density).toInt(), 0, (8 * density).toInt())
        }
        mainLayout.addView(tv)
    }

    private fun addCheckbox(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val cb = CheckBox(this).apply {
            text = label
            setTextColor(if (isUi2) 0xFF00D4FF.toInt() else Color.WHITE)
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked -> onChange(isChecked) }
        }
        mainLayout.addView(cb)
    }

    private fun addFloatInput(label: String, value: Float, onChange: (Float) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
            gravity = Gravity.CENTER_VERTICAL
        }

        val tv = TextView(this).apply {
            text = label
            setTextColor(if (isUi2) 0xFF94A3B8.toInt() else Color.LTGRAY)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(tv)

        val et = EditText(this).apply {
            setText(String.format(Locale.US, "%.2f", value))
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isUi2) 0xFF1E293B.toInt() else 0xFF333333.toInt())
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams((100 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        addDelayedTextListener(et) {
            try { onChange(it.toFloat()) } catch (_: Exception) {}
        }
        row.addView(et)

        mainLayout.addView(row)
    }

    private fun addMinMaxCurveEditor(label: String, curve: ParticleSystem3DComponent.MinMaxCurve) {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val labelTv = TextView(this).apply {
            text = label
            setTextColor(if (isUi2) 0xFF94A3B8.toInt() else Color.LTGRAY)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(labelTv)

        val modes = arrayOf("Constant", "Random 2 Const", "Curve", "Random 2 Curves")
        val modeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@ParticleEditorActivity,
                android.R.layout.simple_spinner_dropdown_item, modes)
            setSelection(curve.mode.ordinal)
        }
        headerRow.addView(modeSpinner)
        wrapper.addView(headerRow)

        val contentArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrapper.addView(contentArea)

        fun rebuildContent() {
            contentArea.removeAllViews()
            when (curve.mode) {
                ParticleSystem3DComponent.CurveMode.CONSTANT -> {
                    addFloatInputDirect(contentArea, "Value", curve.constantMax) {
                        curve.constantMax = it; curve.constantMin = it; updatePS3D()
                    }
                }
                ParticleSystem3DComponent.CurveMode.RANDOM_BETWEEN_TWO_CONSTANTS -> {
                    addHorizontalRow(contentArea) { row ->
                        addSmallFloatInput(row, "Min", curve.constantMin) { curve.constantMin = it; updatePS3D() }
                        addSmallFloatInput(row, "Max", curve.constantMax) { curve.constantMax = it; updatePS3D() }
                    }
                }
                ParticleSystem3DComponent.CurveMode.CURVE -> {
                    addFloatInputDirect(contentArea, "Multiplier", curve.multiplier) { curve.multiplier = it; updatePS3D() }
                    buildCurveGraph(contentArea, curve.curve)
                }
                ParticleSystem3DComponent.CurveMode.RANDOM_BETWEEN_TWO_CURVES -> {
                    addFloatInputDirect(contentArea, "Multiplier", curve.multiplier) { curve.multiplier = it; updatePS3D() }
                    addLabelDirect(contentArea, "Max Curve:")
                    buildCurveGraph(contentArea, curve.curve)
                    addLabelDirect(contentArea, "Min Curve:")
                    buildCurveGraph(contentArea, curve.curveMin)
                }
            }
        }

        rebuildContent()

        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var firstCall = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (firstCall) { firstCall = false; return }
                val newMode = ParticleSystem3DComponent.CurveMode.values()[pos]
                if (newMode != curve.mode) {
                    curve.mode = newMode
                    if ((newMode == ParticleSystem3DComponent.CurveMode.CURVE ||
                                newMode == ParticleSystem3DComponent.CurveMode.RANDOM_BETWEEN_TWO_CURVES) &&
                        curve.curve.isEmpty()
                    ) {
                        curve.curve.add(ParticleCurvePoint(0f, curve.constantMax))
                        curve.curve.add(ParticleCurvePoint(1f, curve.constantMax))
                    }
                    if (newMode == ParticleSystem3DComponent.CurveMode.RANDOM_BETWEEN_TWO_CURVES &&
                        curve.curveMin.isEmpty()
                    ) {
                        curve.curveMin.add(ParticleCurvePoint(0f, curve.constantMin))
                        curve.curveMin.add(ParticleCurvePoint(1f, curve.constantMin))
                    }
                    updatePS3DImmediate()
                    rebuildContent()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        mainLayout.addView(wrapper)
    }

    private fun addMinMaxGradientEditor(label: String, gradient: ParticleSystem3DComponent.MinMaxGradient) {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val labelTv = TextView(this).apply {
            text = label
            setTextColor(if (isUi2) 0xFF94A3B8.toInt() else Color.LTGRAY)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(labelTv)

        val modes = arrayOf("Color", "Random 2 Colors", "Gradient", "Random 2 Gradients")
        val modeSpinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@ParticleEditorActivity,
                android.R.layout.simple_spinner_dropdown_item, modes)
            setSelection(gradient.mode.ordinal)
        }
        headerRow.addView(modeSpinner)
        wrapper.addView(headerRow)

        val contentArea = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        wrapper.addView(contentArea)

        fun rebuildContent() {
            contentArea.removeAllViews()
            when (gradient.mode) {
                ParticleSystem3DComponent.GradientMode.COLOR -> {
                    addColorParamDirect(contentArea, "Color", gradient.colorMax) { c ->
                        gradient.colorMax.set(c); gradient.colorMin.set(c); updatePS3D()
                    }
                }
                ParticleSystem3DComponent.GradientMode.RANDOM_BETWEEN_TWO_COLORS -> {
                    addColorParamDirect(contentArea, "Color Min", gradient.colorMin) { gradient.colorMin.set(it); updatePS3D() }
                    addColorParamDirect(contentArea, "Color Max", gradient.colorMax) { gradient.colorMax.set(it); updatePS3D() }
                }
                ParticleSystem3DComponent.GradientMode.GRADIENT -> {
                    buildGradientEditor(contentArea, gradient.gradient)
                }
                ParticleSystem3DComponent.GradientMode.RANDOM_BETWEEN_TWO_GRADIENTS -> {
                    addLabelDirect(contentArea, "Max Gradient:")
                    buildGradientEditor(contentArea, gradient.gradient)
                    addLabelDirect(contentArea, "Min Gradient:")
                    buildGradientEditor(contentArea, gradient.gradientMin)
                }
            }
        }

        rebuildContent()

        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var firstCall = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (firstCall) { firstCall = false; return }
                val newMode = ParticleSystem3DComponent.GradientMode.values()[pos]
                if (newMode != gradient.mode) {
                    gradient.mode = newMode
                    if ((newMode == ParticleSystem3DComponent.GradientMode.GRADIENT ||
                                newMode == ParticleSystem3DComponent.GradientMode.RANDOM_BETWEEN_TWO_GRADIENTS) &&
                        gradient.gradient.isEmpty()
                    ) {
                        gradient.gradient.add(ParticleCurvePoint(0f, com.badlogic.gdx.graphics.Color(gradient.colorMax)))
                        gradient.gradient.add(ParticleCurvePoint(1f, com.badlogic.gdx.graphics.Color(gradient.colorMax)))
                    }
                    if (newMode == ParticleSystem3DComponent.GradientMode.RANDOM_BETWEEN_TWO_GRADIENTS &&
                        gradient.gradientMin.isEmpty()
                    ) {
                        gradient.gradientMin.add(ParticleCurvePoint(0f, com.badlogic.gdx.graphics.Color(gradient.colorMin)))
                        gradient.gradientMin.add(ParticleCurvePoint(1f, com.badlogic.gdx.graphics.Color(gradient.colorMin)))
                    }
                    updatePS3D()
                    rebuildContent()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        mainLayout.addView(wrapper)
    }

    private fun buildCurveGraph(container: LinearLayout, curvePoints: MutableList<ParticleCurvePoint<Float>>) {
        var computedMin = 0f
        var computedMax = 1f
        for (p in curvePoints) {
            if (p.value < computedMin) computedMin = p.value
            if (p.value > computedMax) computedMax = p.value
        }
        val range = (computedMax - computedMin).coerceAtLeast(0.1f)
        computedMin -= range * 0.1f
        computedMax += range * 0.1f

        val graphView = CurveEditorView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (120 * density).toInt()
            ).apply { setMargins(0, (4 * density).toInt(), 0, (4 * density).toInt()) }
            setData(curvePoints, computedMin, computedMax) { updatePS3D() }
        }
        container.addView(graphView)

        addHorizontalRow(container) { row ->
            addButton(row, "+ Point", Color.WHITE) {
                val newTime = if (curvePoints.isEmpty()) 0.5f
                else Math.min(1f, curvePoints.last().time + 0.2f)
                val newVal = if (curvePoints.isEmpty()) 1f else curvePoints.last().value
                curvePoints.add(ParticleCurvePoint(newTime, newVal))
                curvePoints.sortBy { it.time }
                graphView.setData(curvePoints, computedMin, computedMax) { updatePS3D() }
                updatePS3D()
            }
            addButton(row, "Del Sel", Color.WHITE) {
                if (graphView.deleteSelectedPoint()) updatePS3D()
            }
            addButton(row, "Clear", Color.parseColor("#FF8A80")) {
                curvePoints.clear()
                graphView.setData(curvePoints, 0f, 1f) { updatePS3D() }
                updatePS3D()
            }
        }
    }

    private fun buildGradientEditor(container: LinearLayout, gradientPoints: MutableList<ParticleCurvePoint<com.badlogic.gdx.graphics.Color>>) {
        val listLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        container.addView(listLayout)

        fun refreshList() {
            listLayout.removeAllViews()
            gradientPoints.sortBy { it.time }

            for (point in gradientPoints) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }

                val timeEdit = EditText(this).apply {
                    setText(String.format(Locale.US, "%.2f", point.time))
                    setTextColor(Color.WHITE)
                    setBackgroundColor(if (isUi2) 0xFF1E293B.toInt() else 0xFF333333.toInt())
                    textSize = 11f
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.3f)
                }
                addDelayedTextListener(timeEdit) {
                    try { point.time = it.toFloat().coerceIn(0f, 1f); updatePS3D() } catch (_: Exception) {}
                }
                row.addView(timeEdit)

                val gdxCol = point.value
                val androidColor = Color.argb(
                    (gdxCol.a * 255).toInt(),
                    (gdxCol.r * 255).toInt(),
                    (gdxCol.g * 255).toInt(),
                    (gdxCol.b * 255).toInt()
                )
                val colorBtn = Button(this).apply {
                    setBackgroundColor(androidColor)
                    layoutParams = LinearLayout.LayoutParams(
                        (48 * density).toInt(), (32 * density).toInt()
                    )
                    setOnClickListener {
                        val self = this
                        ColorPickerDialogBuilder.with(this@ParticleEditorActivity)
                            .setTitle("Pick Color")
                            .initialColor(androidColor)
                            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                            .density(12)
                            .showAlphaSlider(true)
                            .setPositiveButton("OK") { _, col, _ ->
                                point.value.set(
                                    Color.red(col) / 255f,
                                    Color.green(col) / 255f,
                                    Color.blue(col) / 255f,
                                    Color.alpha(col) / 255f
                                )
                                self.setBackgroundColor(col)
                                updatePS3D()
                            }
                            .setNegativeButton("Cancel", null)
                            .build()
                            .show()
                    }
                }
                row.addView(colorBtn)

                val alphaLabel = TextView(this).apply {
                    text = String.format(Locale.US, " a:%.0f%%", gdxCol.a * 100)
                    setTextColor(Color.GRAY)
                    textSize = 10f
                }
                row.addView(alphaLabel)

                val delBtn = ImageButton(this).apply {
                    setImageResource(android.R.drawable.ic_delete)
                    setBackgroundColor(Color.TRANSPARENT)
                    setOnClickListener {
                        gradientPoints.remove(point)
                        updatePS3D()
                        refreshList()
                    }
                }
                row.addView(delBtn)

                listLayout.addView(row)
            }
        }

        refreshList()

        addButton(container, "+ Add Color Key", Color.WHITE) {
            val newTime = if (gradientPoints.isEmpty()) 0f
            else Math.min(1f, gradientPoints.last().time + 0.25f)
            val lastCol = if (gradientPoints.isEmpty()) com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 1f)
            else com.badlogic.gdx.graphics.Color(gradientPoints.last().value)
            gradientPoints.add(ParticleCurvePoint(newTime, lastCol))
            updatePS3D()
            refreshList()
        }
    }

    private fun addModuleSection(title: String, enabled: Boolean, onToggle: (Boolean) -> Unit, contentBuilder: () -> Unit) {
        val section = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (12 * density).toInt(), 0, (4 * density).toInt())
        }

        val enableCb = CheckBox(this).apply { isChecked = enabled }
        headerRow.addView(enableCb)

        val titleTv = TextView(this).apply {
            text = "$title ${if (enabled) "▼" else "▶"}"
            setTextColor(if (enabled) Color.WHITE else Color.GRAY)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding((8 * density).toInt(), 0, 0, 0)
        }
        headerRow.addView(titleTv)
        section.addView(headerRow)

        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((16 * density).toInt(), 0, 0, 0)
            visibility = if (enabled) View.VISIBLE else View.GONE
        }
        section.addView(contentContainer)

        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1)
            setBackgroundColor(0x30FFFFFF)
        }
        section.addView(divider)

        if (enabled) {
            contentContainer.addView(LinearLayout(this@ParticleEditorActivity).also {
                mainLayoutForModule = it
            })
            contentBuilder()
            mainLayoutForModule = null
        }

        enableCb.setOnCheckedChangeListener { _, isChecked ->
            onToggle(isChecked)
            titleTv.text = "$title ${if (isChecked) "▼" else "▶"}"
            titleTv.setTextColor(if (isChecked) Color.WHITE else Color.GRAY)
            contentContainer.removeAllViews()
            if (isChecked) {
                contentContainer.addView(LinearLayout(this@ParticleEditorActivity).also {
                    mainLayoutForModule = it
                })
                contentBuilder()
                mainLayoutForModule = null
            }
            contentContainer.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        titleTv.setOnClickListener {
            if (enableCb.isChecked) {
                val visible = contentContainer.visibility == View.VISIBLE
                contentContainer.visibility = if (visible) View.GONE else View.VISIBLE
                titleTv.text = "$title ${if (visible) "▶" else "▼"}"
            }
        }

        mainLayout.addView(section)
    }

    private var mainLayoutForModule: LinearLayout? = null
    private fun targetLayout(): LinearLayout = mainLayoutForModule ?: mainLayout

    private fun <T : Enum<T>> addSpinnerEnum(label: String, values: Array<T>, selectedIdx: Int, onChange: (T) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val enumValues = values as Array<Enum<*>>

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
        }

        val tv = TextView(this).apply {
            text = "$label: "
            setTextColor(if (isUi2) 0xFF94A3B8.toInt() else Color.LTGRAY)
            textSize = 12f
        }
        row.addView(tv)

        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@ParticleEditorActivity,
                android.R.layout.simple_spinner_dropdown_item, values.map { it.name }.toTypedArray())
            setSelection(selectedIdx.coerceAtMost(values.size - 1))
        }
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var firstCall = true
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
                if (firstCall) { firstCall = false; return }
                @Suppress("UNCHECKED_CAST")
                onChange(enumValues[pos] as T)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        spinner.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(spinner)

        targetLayout().addView(row)
    }

    private fun addHorizontalRow(parent: LinearLayout = targetLayout(), builder: (LinearLayout) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
        }
        builder(row)
        parent.addView(row)
    }

    private fun addLabel(parent: LinearLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        parent.addView(tv)
    }

    private fun addLabelDirect(parent: LinearLayout, text: String) {
        val tv = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 11f
        }
        parent.addView(tv)
    }

    private fun addButton(parent: LinearLayout, text: String, color: Int, onClick: () -> Unit) {
        val btn = Button(this, null, 0, android.R.style.Widget_Material_Button_Small).apply {
            this.text = text
            setTextColor(color)
            setOnClickListener { onClick() }
            isAllCaps = false
        }
        parent.addView(btn)
    }

    private fun addSmallFloatInput(parent: LinearLayout, label: String, value: Float, onChange: (Float) -> Unit) {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((4 * density).toInt(), 0, (4 * density).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val tv = TextView(this).apply {
            text = label
            setTextColor(Color.GRAY)
            textSize = 10f
        }
        col.addView(tv)

        val et = EditText(this).apply {
            setText(String.format(Locale.US, "%.2f", value))
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isUi2) 0xFF1E293B.toInt() else 0xFF333333.toInt())
            textSize = 12f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }
        addDelayedTextListener(et) {
            try { onChange(it.toFloat()) } catch (_: Exception) {}
        }
        col.addView(et)
        parent.addView(col)
    }

    private fun addFloatInputDirect(parent: LinearLayout, label: String, value: Float, onChange: (Float) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
        }

        val tv = TextView(this).apply {
            text = label
            setTextColor(if (isUi2) 0xFF94A3B8.toInt() else Color.LTGRAY)
            textSize = 12f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        row.addView(tv)

        val et = EditText(this).apply {
            setText(String.format(Locale.US, "%.2f", value))
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isUi2) 0xFF1E293B.toInt() else 0xFF333333.toInt())
            textSize = 13f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams((100 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        addDelayedTextListener(et) {
            try { onChange(it.toFloat()) } catch (_: Exception) {}
        }
        row.addView(et)
        parent.addView(row)
    }

    private fun addColorParamDirect(parent: LinearLayout, label: String, initialColor: com.badlogic.gdx.graphics.Color, onChange: (com.badlogic.gdx.graphics.Color) -> Unit) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, (4 * density).toInt(), 0, (4 * density).toInt())
        }

        val tv = TextView(this).apply {
            text = "$label: "
            setTextColor(if (isUi2) 0xFF94A3B8.toInt() else Color.LTGRAY)
            textSize = 12f
        }
        row.addView(tv)

        val androidColor = Color.argb(
            (initialColor.a * 255).toInt(),
            (initialColor.r * 255).toInt(),
            (initialColor.g * 255).toInt(),
            (initialColor.b * 255).toInt()
        )

        val colorBtn = Button(this).apply {
            setBackgroundColor(androidColor)
            layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (32 * density).toInt())
            setOnClickListener {
                val self = this
                ColorPickerDialogBuilder.with(this@ParticleEditorActivity)
                    .setTitle("Pick $label")
                    .initialColor(androidColor)
                    .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                    .density(12)
                    .showAlphaSlider(true)
                    .setPositiveButton("OK") { _, col, _ ->
                        self.setBackgroundColor(col)
                        onChange(com.badlogic.gdx.graphics.Color(
                            Color.red(col) / 255f,
                            Color.green(col) / 255f,
                            Color.blue(col) / 255f,
                            Color.alpha(col) / 255f
                        ))
                    }
                    .setNegativeButton("Cancel", null)
                    .build()
                    .show()
            }
        }
        row.addView(colorBtn)
        parent.addView(row)
    }

    private fun addFloatGraphEditor(title: String, graph: MutableList<ParticleCurvePoint<Float>>, defaultMin: Float, defaultMax: Float) {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val headerTv = TextView(this).apply {
            text = title
            setTextColor(if (isUi2) 0xFF00D4FF.toInt() else Color.WHITE)
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(headerTv)

        val minLabel = TextView(this).apply { text = "Min:"; setTextColor(Color.GRAY); textSize = 10f }
        headerRow.addView(minLabel)
        val minEdit = EditText(this).apply {
            setText(String.format(Locale.US, "%.1f", defaultMin))
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isUi2) 0xFF1E293B.toInt() else 0xFF333333.toInt())
            textSize = 11f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams((60 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        headerRow.addView(minEdit)

        val maxLabel = TextView(this).apply { text = "Max:"; setTextColor(Color.GRAY); textSize = 10f }
        headerRow.addView(maxLabel)
        val maxEdit = EditText(this).apply {
            setText(String.format(Locale.US, "%.1f", defaultMax))
            setTextColor(Color.WHITE)
            setBackgroundColor(if (isUi2) 0xFF1E293B.toInt() else 0xFF333333.toInt())
            textSize = 11f
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            layoutParams = LinearLayout.LayoutParams((60 * density).toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
        headerRow.addView(maxEdit)

        wrapper.addView(headerRow)

        val graphView = CurveEditorView(this)
        graphView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (150 * density).toInt()
        )

        val updateRange = Runnable {
            try {
                val min = minEdit.text.toString().toFloat()
                val max = maxEdit.text.toString().toFloat()
                graphView.setRange(min, if (min >= max) min + 0.1f else max)
            } catch (_: Exception) {}
        }
        addDelayedTextListener(minEdit) { updateRange.run() }
        addDelayedTextListener(maxEdit) { updateRange.run() }

        graphView.setData(graph, defaultMin, defaultMax) { updateParticles() }
        wrapper.addView(graphView)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }
        addButton(btnRow, "Clear", Color.parseColor("#FF8A80")) {
            graph.clear()
            val curMin = try { minEdit.text.toString().toFloat() } catch (_: Exception) { 0f }
            val curMax = try { maxEdit.text.toString().toFloat() } catch (_: Exception) { 1f }
            graphView.setData(graph, curMin, curMax) { updateParticles() }
            updateParticles()
        }
        addButton(btnRow, "Del Sel", Color.WHITE) {
            if (!graphView.deleteSelectedPoint()) {
                Toast.makeText(this, "Select a point on graph first", Toast.LENGTH_SHORT).show()
            } else {
                updateParticles()
            }
        }
        addButton(btnRow, "+ Point", Color.WHITE) {
            val curMin = try { minEdit.text.toString().toFloat() } catch (_: Exception) { 0f }
            val curMax = try { maxEdit.text.toString().toFloat() } catch (_: Exception) { 1f }
            val newTime = if (graph.isEmpty()) 0.5f else Math.min(1f, graph.last().time + 0.2f)
            val newVal = if (graph.isEmpty()) (curMin + curMax) / 2f else graph.last().value
            graph.add(ParticleCurvePoint(newTime, newVal))
            graphView.setData(graph, curMin, curMax) { updateParticles() }
            updateParticles()
        }
        wrapper.addView(btnRow)

        mainLayout.addView(wrapper)
    }

    private fun addColorGraphEditor(title: String, graph: MutableList<ParticleCurvePoint<com.badlogic.gdx.graphics.Color>>) {
        val wrapper = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, (8 * density).toInt(), 0, (8 * density).toInt())
        }

        val headerTv = TextView(this).apply {
            text = title
            setTextColor(if (isUi2) 0xFF00D4FF.toInt() else Color.WHITE)
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        wrapper.addView(headerTv)

        val preview = GradientPreviewView(this, graph)
        preview.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            (48 * density).toInt()
        )
        wrapper.addView(preview)

        for (i in graph.indices) {
            val point = graph[i]
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
            }

            val gdxCol = point.value
            val androidColor = Color.argb(
                (gdxCol.a * 255).toInt(), (gdxCol.r * 255).toInt(),
                (gdxCol.g * 255).toInt(), (gdxCol.b * 255).toInt()
            )
            val colorBtn = Button(this).apply {
                setBackgroundColor(androidColor)
                layoutParams = LinearLayout.LayoutParams((48 * density).toInt(), (32 * density).toInt())
                setOnClickListener {
                    val self = this
                    ColorPickerDialogBuilder.with(this@ParticleEditorActivity)
                        .setTitle("Pick Color")
                        .initialColor(androidColor)
                        .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                        .density(12)
                        .showAlphaSlider(true)
                        .setPositiveButton("OK") { _, col, _ ->
                            point.value.set(Color.red(col) / 255f, Color.green(col) / 255f,
                                Color.blue(col) / 255f, Color.alpha(col) / 255f)
                            self.setBackgroundColor(col)
                            preview.setPoints(graph)
                            updateParticles()
                        }
                        .setNegativeButton("Cancel", null)
                        .build()
                        .show()
                }
            }
            row.addView(colorBtn)

            val timeLabel = TextView(this).apply {
                text = String.format(Locale.US, " t:%.2f", point.time)
                setTextColor(Color.GRAY); textSize = 10f
            }
            row.addView(timeLabel)

            val delBtn = ImageButton(this).apply {
                setImageResource(android.R.drawable.ic_delete)
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener {
                    graph.removeAt(i)
                    preview.setPoints(graph)
                    updateParticles()
                    buildEditor()
                }
            }
            row.addView(delBtn)
            wrapper.addView(row)
        }

        addButton(wrapper, "+ Add Color Key", Color.WHITE) {
            val newTime = if (graph.isEmpty()) 0f else Math.min(1f, graph.last().time + 0.25f)
            val lastCol = if (graph.isEmpty()) com.badlogic.gdx.graphics.Color(1f, 1f, 1f, 1f)
            else com.badlogic.gdx.graphics.Color(graph.last().value)
            graph.add(ParticleCurvePoint(newTime, lastCol))
            preview.setPoints(graph)
            updateParticles()
            buildEditor()
        }

        mainLayout.addView(wrapper)
    }

    private fun addDelayedTextListener(editText: EditText, updater: (String) -> Unit) {
        var lastText = editText.text.toString()
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                if (text != lastText && editText.hasFocus()) {
                    lastText = text
                    updater(text)
                }
            }
        })
    }

    private fun updatePS3D() {
        if (pendingUpdate != null) updateHandler.removeCallbacks(pendingUpdate!!)
        pendingUpdate = Runnable {
            val go = gameObject ?: return@Runnable
            val ps = particleSystem3D ?: return@Runnable
            val threeDManager = SceneManager.getInstance()?.engine ?: return@Runnable
            Gdx.app.postRunnable {
                threeDManager.updateParticleEffect3D(go.id, ps, go.transform.worldTransform)
            }
        }
        updateHandler.postDelayed(pendingUpdate!!, 300)
    }

    private fun updatePS3DImmediate() {
        val go = gameObject ?: return
        val ps = particleSystem3D ?: return
        val threeDManager = SceneManager.getInstance()?.engine ?: return
        Gdx.app.postRunnable {
            threeDManager.updateParticleEffect3D(go.id, ps, go.transform.worldTransform)
        }
    }

    private fun updateParticles() {
        val go = gameObject ?: return
        val p = legacyParticle ?: return
        val threeDManager = SceneManager.getInstance()?.engine ?: return
        Gdx.app.postRunnable {
            threeDManager.updateParticleEffect(go.id, p, go.transform.worldTransform)
        }
    }

    private fun showSubEmitterPicker(entry: ParticleSystem3DComponent.SubEmitterEntry) {
        val sceneManager = SceneManager.getInstance() ?: return
        val candidates = mutableListOf<String>()
        for (obj in sceneManager.allGameObjects.values) {
            if (obj.hasComponent(ParticleSystem3DComponent::class.java) && obj.id != gameObject?.id) {
                candidates.add(obj.id)
            }
        }
        if (candidates.isEmpty()) {
            Toast.makeText(this, "No other objects with Particle System 3D found", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Select Sub Emitter")
            .setItems(candidates.toTypedArray()) { _, which ->
                entry.subEmitterObjectId = candidates[which]
                updatePS3DImmediate()
                buildEditor()
            }
            .show()
    }

    private fun showMeshPicker(ps: ParticleSystem3DComponent) {
        val projectFilesDir = ProjectManager.getInstance().currentProject?.filesDir ?: return
        val modelFiles = projectFilesDir.listFiles()
            ?.filter { it.name.lowercase().let { n -> n.endsWith(".glb") || n.endsWith(".gltf") || n.endsWith(".obj") } }
            ?.map { it.name }
            ?.toTypedArray()
            ?: emptyArray()

        if (modelFiles.isEmpty()) {
            Toast.makeText(this, "No 3D models found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Mesh")
            .setItems(modelFiles) { _, which ->
                ps.renderer.meshPath = modelFiles[which]
                updatePS3DImmediate()
                buildEditor()
            }
            .show()
    }

    private fun showTexturePicker(ps: ParticleSystem3DComponent) {
        val projectFilesDir = ProjectManager.getInstance().currentProject?.filesDir ?: return
        val texFiles = projectFilesDir.listFiles()
            ?.filter { it.name.lowercase().let { n -> n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") } }
            ?.map { it.name }
            ?.toTypedArray()
            ?: emptyArray()

        if (texFiles.isEmpty()) {
            Toast.makeText(this, "No texture files found", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Select Texture")
            .setItems(texFiles) { _, which ->
                ps.renderer.texturePath = texFiles[which]
                updatePS3DImmediate()
                buildEditor()
            }
            .show()
    }

    private fun setWhiteTextToAllChildren(vg: ViewGroup) {
        for (i in 0 until vg.childCount) {
            val child = vg.getChildAt(i)
            if (child is TextView) {
                child.setTextColor(if (isUi2) 0xFF00D4FF.toInt() else 0xFFFFFFFF.toInt())
            } else if (child is ViewGroup) {
                setWhiteTextToAllChildren(child)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
