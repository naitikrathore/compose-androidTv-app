package com.iwedia.cltv.entities

import world.SceneData

/**
 * Dialog scene data
 *
 * @author Aleksandar Lazic
 */
class DialogSceneData constructor(
    previousSceneId: Int,
    previousSceneInstance: Int,
    vararg data: Any?
): SceneData(
    previousSceneId,
    previousSceneInstance,
    data
) {

    interface DialogClickListener{
        fun onPositiveButtonClicked()
        fun onNegativeButtonClicked()
    }

    /**
     * Dialog Type enum
     */
    enum class DialogType {
        BOOLEAN, TEXT, NUMBER, OPTIONS, YES_NO, SCHEDULER, SCHEDULED_REMINDER, INACTIVITY_TIMER
    }

    /**
     * Dialog type
     */
    var type: DialogType? = null

    /**
     * Title
     */
    var title: String? = null

    /**
     * Message
     */
    var message: String? = null

    /**
     * Image
     */
    var imageRes: Int = -1

    /**
     * Positive button text
     */
    var positiveButtonText: String? = null

    /**
     * Negative button text
     */
    var negativeButtonText: String? = null

    /**
     * subMessage
     */
    var subMessage: String? = null

    /**
     * Button click listener
     */
    var dialogClickListener: DialogClickListener?= null

    /**
     * Is back key enabled
     */
    var isBackEnabled: Boolean = true

    /**
     * Positive button enabled
     */
    var positiveButtonEnabled = true
}