package com.iwedia.cltv.scan_activity.entities;

/**
 * Frontend type
 *
 * @author Veljko Ilkic
 */
public class FrontendType {

    /**
     * Available type
     */
    public enum Type {
        DVB_C, DVB_T, DVB_S, IP
    }

    /**
     * Current type
     */
    private Type type;

    /**
     * Name
     */
    private String name;

    /**
     * Constructor
     *
     * @param name name
     * @param type type
     */
    public FrontendType(String name, Type type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Get name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get type
     *
     * @return type
     */
    public Type getType() {
        return type;
    }
}
