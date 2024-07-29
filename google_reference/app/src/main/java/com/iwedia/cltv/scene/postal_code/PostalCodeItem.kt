package com.iwedia.cltv.scene.postal_code

class PostalCodeItem {

    companion object {
        /**
         * Item type CODE
         */
        val TYPE_CODE = 0

        /**
         * Empty value
         */
        val EMPTY_VALUE = " "
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
        type = TYPE_CODE
    }

    constructor(id: Int, type: Int, value: Int) {
        this.id = id
        this.type = type
        this.value = value
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