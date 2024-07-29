package com.iwedia.cltv.scan_activity.entities;

import java.util.ArrayList;

/**
 * Dialog screen entity
 *
 * @author Dejan Nadj
 */
public class DialogEntity {

    /**
     * Dialog title
     */
    private String title;

    /**
     * Dialog options list
     */
    private ArrayList<String> options;

    /**
     * Dialog listener
     */
    private DialogListener dialogListener;

    /**
     * Dialog screen listener
     */
    public interface DialogListener {
        /**
         * On dialog option clicked
         *
         * @param position  option position in list
         */
        void onOptionClicked(int position);
    }

    /**
     * Constructor
     *
     * @param title     dialog title
     * @param options   dialog options
     * @param listener  dialog listener
     */
    public DialogEntity(String title, ArrayList<String> options, DialogListener listener) {
        this.title = title;
        this.options = options;
        this.dialogListener = listener;
    }

    /**
     * Get dialog title
     *
     * @return  dialog title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Get dialog options
     *
     * @return  dialog options
     */
    public ArrayList<String> getOptions() {
        return options;
    }

    /**
     * Dialog listener
     *
     * @return  dialog listener
     */
    public DialogListener getDialogListener() {
        return dialogListener;
    }
}
