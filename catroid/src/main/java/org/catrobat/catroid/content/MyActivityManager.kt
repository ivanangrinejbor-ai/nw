package org.catrobat.catroid.content

import android.app.Activity

class MyActivityManager {
    companion object {
        var base_activity: Activity? = null
        var project_activity: Activity? = null
        var sprite_activity: Activity? = null
        var stage_activity: Activity? = null
        var actor_activity: Activity? = null

        fun clearActivity(activity: Activity) {
            if (base_activity === activity) base_activity = null
            if (project_activity === activity) project_activity = null
            if (sprite_activity === activity) sprite_activity = null
            if (stage_activity === activity) stage_activity = null
            if (actor_activity === activity) actor_activity = null
        }
    }
}