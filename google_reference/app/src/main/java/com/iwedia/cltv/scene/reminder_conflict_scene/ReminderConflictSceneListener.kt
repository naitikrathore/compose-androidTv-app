package com.iwedia.cltv.scene.reminder_conflict_scene

import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.TvChannel
import world.SceneListener

/**
 * Reminder conflict scene listener
 *
 * @author Shubham Kumar
 */

interface ReminderConflictSceneListener : SceneListener, TTSSetterInterface {
    fun onEventSelected()
    fun playChannel( channel: TvChannel )
}