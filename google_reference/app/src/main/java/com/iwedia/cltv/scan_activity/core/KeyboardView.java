package com.iwedia.cltv.scan_activity.core;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.iwedia.cltv.R;
import com.iwedia.cltv.ReferenceApplication;
import com.iwedia.cltv.TypeFaceProvider;
import com.iwedia.cltv.config.ConfigColorManager;
import com.iwedia.cltv.config.ConfigFontManager;
import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.utils.Utils;;

/**
 * Keyboard view
 *
 * @author Dejan Nadj
 */
public class KeyboardView extends RecyclerView {
    /**
     * Keyboard type
     */
    public enum Type {
        TEXT,
        NUMERIC;
    }

    /**
     * Type
     */
    private Type type;

    /**
     * Constructor
     *
     * @param context
     */
    public KeyboardView(@NonNull Context context) {
        this(context, null, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     */
    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor
     *
     * @param context
     * @param attrs
     * @param defStyle
     */
    public KeyboardView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(Type.TEXT);
    }

    /**
     * View initialization
     */
    private void init(final Type type) {
        this.type = type;
        setFocusable(true);
        setClickable(true);

        try {
            int color_context = Color.parseColor(ConfigColorManager.Companion.getColor("color_background"));
            Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context","" +color_context);
            setBackgroundColor(color_context);
        } catch(Exception ex) {
            Log.d(Constants.LogTag.CLTV_TAG + "Exception color_context", "" + ex);
        }
        if (type == Type.TEXT) {
            GridLayoutManager gridLayoutManager
                    = new GridLayoutManager(getContext(), 11, LinearLayoutManager.VERTICAL, false);
            setLayoutManager(gridLayoutManager);
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    if (((KeyboardAdapter) getAdapter()).getItemAtPosition(position).equals(KeyboardAdapter.SPACE_CHARACTER)) {
                        return 7;
                    }
                    return 1;
                }
            });
        } else {
            GridLayoutManager gridLayoutManager
                    = new GridLayoutManager(getContext(), 4, LinearLayoutManager.VERTICAL, false);
            setLayoutManager(gridLayoutManager);
        }

        setAdapter(new KeyboardAdapter(type));

        //Set view dimensions
        post(new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams params;
                final float scale = getResources().getDisplayMetrics().density;
                int itemWidth = (int) (420 * scale);
                int itemHeight = (int) (206.5 * scale);
                params = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
                int marginTop = type == Type.TEXT ? 0 : (int) (40 * scale);
                params.setMargins((int) (280 * scale), marginTop, 0, 0);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                setLayoutParams(params);
            }
        });
    }

    /**
     * Set keyboard numeric type
     */
    public void setNumericType() {
        init(Type.NUMERIC);
    }

    /**
     * Set keyboard edit text
     *
     * @param editText
     */
    public void setEditText(EditText editText) {
        ((KeyboardAdapter) getAdapter()).setEditText(editText);
        if (type == Type.TEXT) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
        } else {
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        }
    }

    /**
     * Keyboard view holder
     */
    protected class KeyboardViewHolder extends ViewHolder {

        /**
         * Keyboard button
         */
        private TextView button;

        /**
         * Root view
         */
        private View rootView;

        /**
         * Button start margin
         */
        private View startMarginView;

        public KeyboardViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView;
            startMarginView = itemView.findViewById(R.id.margin_start);
            button = (TextView) itemView.findViewById(R.id.button);
            button.setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
        }

        /**
         * Get root view
         *
         * @return root view
         */
        public View getRootView() {
            return rootView;
        }

        /**
         * Get start margin view
         *
         * @return start margin view
         */
        public View getStartMarginView() {
            return startMarginView;
        }

        /**
         * Get button view
         *
         * @return button view
         */
        public TextView getButton() {
            return button;
        }
    }

    /**
     * Keyboard adapter
     */
    protected class KeyboardAdapter extends RecyclerView.Adapter<KeyboardViewHolder> {

        /**
         * Keyboard type
         */
        private Type type = Type.TEXT;

        /**
         * Special unicode characters
         */
        protected static final String SPACE_CHARACTER = " ";
        protected static final String NUMBER_SPACE_CHARACTER = "\u23B5";
        protected static final String DELETE_CHARACTER = "\u232b";
        protected static final String UP_ARROW_CHARACTER = "\u21E7"/*"\u2191"*/;
        protected static final String LEFT_ARROW_CHARACTER = "\u25C0"/*"\u003C"*/;
        protected static final String RIGHT_ARROW_CHARACTER = "\u25B6"/*"\u003E"*/;
        protected static final String ENTER_CHARACTER = "NEXT";


        /**
         * Default buttons layout
         */
        protected String[] buttons = {
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", DELETE_CHARACTER,
                "q", "w", "e", "r", "t", "y", "u", "i", "o", "p", "@",
                "a", "s", "d", "f", "g", "h", "j", "k", "l", "_", "&",
                "z", "x", "c", "v", "b", "n", "m", ",", ".", "-", "?",
                "=/#", UP_ARROW_CHARACTER, SPACE_CHARACTER, LEFT_ARROW_CHARACTER, RIGHT_ARROW_CHARACTER
        };

        /**
         * Uppercase buttons layout
         */
        protected String[] buttonsUppercase = {
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", DELETE_CHARACTER,
                "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P", "@",
                "A", "S", "D", "F", "G", "H", "J", "K", "L", "%", "#",
                "Z", "X", "C", "V", "B", "N", "M", ";", ":", "/", "!",
                "=/#", UP_ARROW_CHARACTER, SPACE_CHARACTER, LEFT_ARROW_CHARACTER, RIGHT_ARROW_CHARACTER
        };

        /**
         * Symbols buttons layout
         */
        protected String[] symbols = {
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "0", DELETE_CHARACTER,
                "~", "'", "|", "\u2219", "+", "-", "\u00F7", "\u00D7", "\\", "=", "@",
                "\u20AC", "\u00A5", "$", "\u00A2", "^", "\u00B0", "\u00A9", "{", "}", "%", "#",
                "[", "]", "(", ")", "-", "<", ">", ";", ":", "/", "!",
                "ABC", "", SPACE_CHARACTER, LEFT_ARROW_CHARACTER, RIGHT_ARROW_CHARACTER
        };

        /**
         * Number buttons layout
         */
        protected String[] numberButtons = {
                "1", "2", "3", "-",
                "4", "5", "6", ".",
                "7", "8", "9", DELETE_CHARACTER,
                "+", "0", NUMBER_SPACE_CHARACTER, ENTER_CHARACTER
        };

        /**
         * Keyboard edit text where pressed buttons will be inserted
         */
        private EditText editText;

        /**
         * Edit text cursor position
         */
        private int cursorPosition = -1;

        /**
         * Is lowercase state
         */
        private boolean lowercase = true;

        /**
         * Symbols layout flag
         */
        private boolean symbolsLayout = false;

        /**
         * Blink flag
         */
        private boolean disableBlink;

        /**
         * Constructor
         *
         * @param type keyboard type
         */
        protected KeyboardAdapter(Type type) {
            this.type = type;
        }

        @NonNull
        @Override
        public KeyboardViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {

            final View view = LayoutInflater.from(getContext()).inflate(R.layout.keyboard_button_layout, viewGroup, false);
            final KeyboardViewHolder viewHolder = new KeyboardViewHolder(view);
            final Typeface typeface = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_regular"));
            final Typeface typefaceFocus = TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(), ConfigFontManager.Companion.getFont("font_bold"));

            viewHolder.getButton().setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Utils.Companion.clickAnimation(viewHolder.getRootView());

                    //Insert pressed button to the edit text
                    int position = viewHolder.getAdapterPosition();
                    if (getItemAtPosition(position).equals(UP_ARROW_CHARACTER)) {
                        lowercase = !lowercase;
                        notifyDataSetChanged();
                        post(new Runnable() {
                            @Override
                            public void run() {
                                getLayoutManager().findViewByPosition(45).requestFocus();
                            }
                        });
                    } else if (getItemAtPosition(position).equals("=/#")) {
                        symbolsLayout = true;
                        notifyDataSetChanged();
                        post(new Runnable() {
                            @Override
                            public void run() {
                                getLayoutManager().findViewByPosition(44).requestFocus();
                            }
                        });
                    } else if (getItemAtPosition(position).equals("ABC")) {
                        symbolsLayout = false;
                        notifyDataSetChanged();
                        post(new Runnable() {
                            @Override
                            public void run() {
                                getLayoutManager().findViewByPosition(44).requestFocus();
                            }
                        });
                    } else if (viewHolder.getButton().getText().equals(ENTER_CHARACTER)) {
                        setVisibility(GONE);
                        disableBlink = true;
                        editText.setPressed(false);
                    } else if (editText != null) {
                        insertText(position);
                    }
                }
            });
            //Button key listener
            viewHolder.getButton().setOnKeyListener(new OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    //Prevent focus lose for numeric keyboard
                    if (event.getAction() == KeyEvent.ACTION_DOWN && type == Type.NUMERIC) {
                        int position = viewHolder.getAdapterPosition();
                        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                            //Right edge
                            if (((position + 1) % 4) == 0) {
                                return true;
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                            //Left edge
                            if (position == 0 || (position % 4) == 0) {
                                return true;
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                            //Top edge
                            if (position >= 0 && position < 4) {
                                return true;
                            }
                        }
                        if (keyCode == KeyEvent.KEYCODE_BACK) {
                            setVisibility(GONE);
                            disableBlink = true;
                            editText.setPressed(false);
                            return true;
                        }
                    }

                    if (keyCode != KeyEvent.KEYCODE_DPAD_CENTER && keyCode != KeyEvent.KEYCODE_ENTER) {
                        if (editText.dispatchKeyEvent(event)) {
                            updateCursorPosition();
                        }
                    }

                    return false;
                }
            });
            //Handle button focus
            viewHolder.getButton().setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    int position = viewHolder.getAdapterPosition();
                    if (position == -1) {
                        return;
                    }
                    String item = getItemAtPosition(viewHolder.getAdapterPosition());
                    boolean isSpace = item.equals(SPACE_CHARACTER);
                    boolean isBackspace = item.equals(DELETE_CHARACTER);
                    boolean isUpperCase = item.equals(UP_ARROW_CHARACTER);
                    boolean isLeftArrow = item.equals(LEFT_ARROW_CHARACTER);
                    boolean isRightArrow = item.equals(RIGHT_ARROW_CHARACTER);
                    viewHolder.getStartMarginView().setVisibility(GONE);
                    if (hasFocus) {
                        if (isSpace) {
                            viewHolder.getButton().setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                            viewHolder.getRootView().setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                            setImage(viewHolder, true, R.drawable.space_black);
                        } else if (isBackspace) {
                            v.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                            setImage(viewHolder, true, R.drawable.backspace_black);
                        } else if (isUpperCase) {
                            v.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                            setImage(viewHolder, true, R.drawable.shift_black);
                        } else if (isLeftArrow) {
                            v.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                            setImage(viewHolder, true, R.drawable.left_black);
                        } else if (isRightArrow) {
                            v.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                            setImage(viewHolder, true, R.drawable.right_black);
                        } else {
                            v.setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                            viewHolder.getButton().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_color_background")));
                            viewHolder.getButton().setTypeface(typefaceFocus);
                        }
                    } else {
                        if (isSpace) {
                            viewHolder.getButton().setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                            viewHolder.getRootView().setBackgroundColor(getResources().getColor(R.color.transparent, null));
                            setImage(viewHolder, false, R.drawable.space_white);
                        } else if (isBackspace) {
                            v.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                            setImage(viewHolder, false, R.drawable.backspace_white);
                        } else if (isUpperCase) {
                            v.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                            setImage(viewHolder, false, R.drawable.shift_white);
                        } else if (isLeftArrow) {
                            v.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                            setImage(viewHolder, false, R.drawable.left_white);
                        } else if (isRightArrow) {
                            v.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                            setImage(viewHolder, false, R.drawable.right_white);
                        } else {
                            v.setBackgroundColor(getResources().getColor(R.color.transparent, null));
                            viewHolder.getButton().setTypeface(typeface);
                            viewHolder.getButton().setTextColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                        }
                    }
                }
            });
            return viewHolder;
        }

        /**
         * Set button image
         *
         * @param viewHolder keyboard view holder
         * @param hasFocus   has focus flag
         * @param imageId    button image res id
         */
        private void setImage(KeyboardViewHolder viewHolder, boolean hasFocus, int imageId) {
            viewHolder.getButton().setText("");
            viewHolder.getStartMarginView().setVisibility(VISIBLE);
            if(hasFocus){
                viewHolder.getStartMarginView().setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
            }else{
                viewHolder.getStartMarginView().setBackgroundColor(getResources().getColor(R.color.transparent, null));
            }
            viewHolder.getButton().setCompoundDrawablesWithIntrinsicBounds(imageId, 0, 0, 0);
        }

        /**
         * Init button image
         *
         * @param keyboardViewHolder keyboard view holder
         * @param width              button layout width
         * @param height             button layout height
         * @param marginWidth        margin layout width
         * @param imageId            button image id
         */
        private void initImage(KeyboardViewHolder keyboardViewHolder, int width, int height, int marginWidth, int imageId) {
            RelativeLayout.LayoutParams params;
            float scale = getResources().getDisplayMetrics().density;
            int itemWidth = (int) (width * scale);
            int itemHeight = (int) (height * scale);
            params = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
            params.setMargins(0, 0, 0, (int) (10 * scale));
            if (imageId == R.drawable.space_white) {
                params.addRule(RelativeLayout.CENTER_IN_PARENT);
            }
            params.addRule(RelativeLayout.RIGHT_OF, keyboardViewHolder.startMarginView.getId());
            keyboardViewHolder.getButton().setLayoutParams(params);

            RelativeLayout.LayoutParams marginParams;
            marginParams = new RelativeLayout.LayoutParams((int) (marginWidth * scale), itemHeight);
            keyboardViewHolder.getStartMarginView().setLayoutParams(marginParams);
            keyboardViewHolder.getButton().setText("");
            keyboardViewHolder.getStartMarginView().setVisibility(VISIBLE);
            keyboardViewHolder.getStartMarginView().setBackgroundColor(getResources().getColor(R.color.transparent, null));
            keyboardViewHolder.getButton().setCompoundDrawablesWithIntrinsicBounds(imageId, 0, 0, 0);
        }

        @Override
        public void onBindViewHolder(@NonNull KeyboardViewHolder keyboardViewHolder, int i) {
            if (type == Type.TEXT) {
                keyboardViewHolder.getButton().setClickable(true);
                keyboardViewHolder.getButton().setFocusable(true);
                if (symbolsLayout) {
                    keyboardViewHolder.getButton().setText(symbols[i]);
                    if (symbols[i].equals("")) {
                        keyboardViewHolder.getButton().setClickable(false);
                        keyboardViewHolder.getButton().setFocusable(false);
                    }
                } else if (lowercase) {
                    keyboardViewHolder.getButton().setText(buttons[i]);
                } else {
                    keyboardViewHolder.getButton().setText(buttonsUppercase[i]);
                }
            } else {
                keyboardViewHolder.getButton().setText(numberButtons[i]);
            }
            keyboardViewHolder.getButton().setTypeface(TypeFaceProvider.Companion.getTypeFace(ReferenceApplication.Companion.applicationContext(),ConfigFontManager.Companion.getFont("font_regular")));

            keyboardViewHolder.getButton().setGravity(Gravity.CENTER);
            keyboardViewHolder.getStartMarginView().setVisibility(GONE);
            //Init layout params
            if (type == Type.TEXT) {
                //Space is bigger than other buttons
                if (getItemAtPosition(i).equals(SPACE_CHARACTER)) {
                    //Set space root view params
                    RelativeLayout.LayoutParams params;
                    final float scale = getResources().getDisplayMetrics().density;
                    int itemWidth = (int) (35 * scale) * 7;
                    int itemHeight = (int) (15 * scale);
                    params = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
                    params.setMargins(0, 0, 0, (int) (10 * scale));
                    keyboardViewHolder.getRootView().setLayoutParams(params);

                    //Set space button view params
                    RelativeLayout.LayoutParams paramsButton;
                    itemWidth = (int) (30 * scale) * 7;
                    itemHeight = (int) (11 * scale);
                    paramsButton = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
                    paramsButton.addRule(RelativeLayout.CENTER_IN_PARENT);
                    keyboardViewHolder.getButton().setLayoutParams(paramsButton);
                    keyboardViewHolder.getButton().setGravity(Gravity.NO_GRAVITY);
                    keyboardViewHolder.getButton().setBackgroundColor(Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text")));
                    keyboardViewHolder.getRootView().setBackgroundColor(getResources().getColor(R.color.transparent, null));
                    keyboardViewHolder.getButton().setText("");
                    keyboardViewHolder.getButton().setCompoundDrawablesWithIntrinsicBounds(R.drawable.space_white, 0, 0, 0);
                } else {
                    keyboardViewHolder.getButton().setBackgroundColor(getResources().getColor(R.color.transparent, null));
                    keyboardViewHolder.getRootView().setBackgroundColor(getResources().getColor(R.color.transparent, null));
                    if (getItemAtPosition(i).equals(DELETE_CHARACTER)) {
                        initImage(keyboardViewHolder, 25, 25, 5, R.drawable.backspace_white);
                    } else if (getItemAtPosition(i).equals(UP_ARROW_CHARACTER)) {
                        initImage(keyboardViewHolder, 23, 25, 7, R.drawable.shift_white);
                    } else if (getItemAtPosition(i).equals(LEFT_ARROW_CHARACTER)) {
                        initImage(keyboardViewHolder, 19, 25, 11, R.drawable.left_white);
                    } else if (getItemAtPosition(i).equals(RIGHT_ARROW_CHARACTER)) {
                        initImage(keyboardViewHolder, 19, 25, 11, R.drawable.right_white);
                    } else {
                        RelativeLayout.LayoutParams params;
                        final float scale = getResources().getDisplayMetrics().density;
                        int itemWidth = (int) (30 * scale);
                        int itemHeight = (int) (25 * scale);
                        params = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
                        params.setMargins(0, 0, 0, (int) (10 * scale));
                        params.addRule(RelativeLayout.RIGHT_OF, keyboardViewHolder.startMarginView.getId());
                        keyboardViewHolder.getButton().setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
                        keyboardViewHolder.getButton().setLayoutParams(params);
                        if (getItemAtPosition(i).equals("=/#") || getItemAtPosition(i).equals("ABC")) {
                            keyboardViewHolder.getButton().setTextSize(10);
                        }
                    }
                }
            } else {
                if (getItemAtPosition(i).equals(DELETE_CHARACTER)) {
                    initImage(keyboardViewHolder, 55, 30, 35, R.drawable.backspace_white);
                } else {
                    RelativeLayout.LayoutParams params;
                    final float scale = getResources().getDisplayMetrics().density;
                    int itemWidth = (int) (90 * scale);
                    int itemHeight = (int) (30 * scale);
                    params = new RelativeLayout.LayoutParams(itemWidth, itemHeight);
                    params.setMargins(0, 0, 0, (int) (10 * scale));
                    keyboardViewHolder.getButton().setLayoutParams(params);
                    keyboardViewHolder.getButton().setBackgroundColor(getResources().getColor(R.color.transparent, null));
                    keyboardViewHolder.getRootView().setBackgroundColor(getResources().getColor(R.color.transparent, null));
                }
            }
        }

        @Override
        public int getItemCount() {
            if (type == Type.TEXT) {
                return 49;
            } else {
                return 16;
            }
        }

        /**
         * Get button text at the position
         *
         * @param position
         * @return button text
         */
        public String getItemAtPosition(int position) {
            if (type == Type.TEXT) {
                if (symbolsLayout) {
                    return symbols[position];
                }
                return lowercase ? buttons[position] : buttonsUppercase[position];
            } else {
                return numberButtons[position];
            }
        }

        /**
         * Set keyboard edit text
         *
         * @param editText
         */
        public void setEditText(EditText editText) {
            this.editText = editText;
            editText.setCursorVisible(true);
            editText.setSelection(editText.getText().length());
            cursorPosition = editText.getText().length();
            disableBlink = false;
            blink();
        }

        private void updateCursorPosition() {
            cursorPosition = editText.getText().length();
            editText.setSelection(cursorPosition);
        }

        /**
         * Insert text and update cursor position
         *
         * @param pos inserted text
         */
        public void insertText(int pos) {
            CharSequence insert = getItemAtPosition(pos).toString();

            String text = editText.getText().toString();
            cursorPosition = cursorPosition == -1 ? editText.getText().length() : cursorPosition;
            int selectedPosition = cursorPosition;
            if (insert.equals(DELETE_CHARACTER)) {
                if (text.isEmpty() || selectedPosition == 0) {
                    return;
                }
                CharSequence temp = text.subSequence(0, selectedPosition - 1);
                CharSequence temp2 = text.subSequence(selectedPosition, text.length());
                editText.setText(text.subSequence(0, text.length() - 1));
                editText.setText(temp.toString() + temp2.toString());
                int position = selectedPosition == 0 ? 0 : selectedPosition - 1;
                editText.setSelection(position);
                cursorPosition = position;
            } else if (insert.equals(LEFT_ARROW_CHARACTER)) {
                int position = selectedPosition == 0 ? 0 : selectedPosition - 1;
                editText.setSelection(position);
                cursorPosition = position;
            } else if (insert.equals(RIGHT_ARROW_CHARACTER)) {
                int position = selectedPosition == editText.getText().length() ? selectedPosition : selectedPosition + 1;
                editText.setSelection(position);
                cursorPosition = position;
            } else {
                if (insert.equals(SPACE_CHARACTER) || insert.equals(NUMBER_SPACE_CHARACTER)) {
                    insert = " ";
                }
                if (selectedPosition == text.length()) {
                    editText.setText(text + insert);
                    editText.setSelection(editText.getText().length());
                    cursorPosition = editText.getText().length();
                } else {
                    CharSequence temp = text.subSequence(0, selectedPosition);
                    CharSequence temp2 = text.subSequence(selectedPosition, text.length());
                    editText.setText(temp.toString() + insert + temp2.toString());
                    editText.setSelection(selectedPosition + 1);
                    cursorPosition = selectedPosition + 1;
                }
            }
        }

        /**
         * Start edit text cursor blinking
         */
        private void blink() {
            editText.setText(editText.getText());
            editText.setPressed(true);
            editText.setSelection(cursorPosition);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!disableBlink) {
                        blink();
                    }
                }
            }, 500);
        }

    }
}
