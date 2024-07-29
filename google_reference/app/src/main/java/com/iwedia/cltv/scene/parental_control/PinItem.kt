package com.iwedia.cltv.scene.parental_control

class PinItem {

    companion object{
        /**
         * Mask value
         */
        val MASK_VALUE = "*"

        /**
         * Empty value
         */
        val EMPTY_VALUE = " "

        /**
         * Item type password
         */
        val TYPE_PASSWORD = 0

        /**
         * Item type generic
         */
        val TYPE_GENERIC = 1
    }


    /**
     * Item id
     */
    private var id = 0

    /**
     * Type
     */
    private var type = 0

    /**
     * Value
     */
    private var value = 0

    /**
     * Is focused
     */
    private var isFocused = false

    /**
     * Constructor
     *
     * @param id Id
     */
   constructor(id: Int) {
        this.id = id
        type = TYPE_PASSWORD
    }

   constructor(id: Int, type: Int) {
        this.id = id
        this.type = type
    }

    /**
     * Get id
     *
     * @return Id
     */
    fun getId(): Int {
        return id
    }

    /**
     * Get value
     *
     * @return Value
     */
    fun getValue(): Int {
        return value
    }

    /**
     * Set value
     *
     * @param value Value
     */
    fun setValue(value: Int) {
        this.value = value
    }

    /**
     * Is focused
     *
     * @return Is focused
     */
    fun isFocused(): Boolean {
        return isFocused
    }

    /**
     * Set focused
     *
     * @param focused Is focused
     */
    fun setFocused(focused: Boolean) {
        isFocused = focused
    }

    /**
     * Get type
     * @return Type
     */
    fun getType(): Int {
        return type
    }

    /**
     * Set type
     * @param type Type
     */
    fun setType(type: Int) {
        this.type = type
    }
}