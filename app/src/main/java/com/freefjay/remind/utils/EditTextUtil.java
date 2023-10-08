package com.freefjay.remind.utils;

import android.text.Editable;
import android.text.TextWatcher;

import java.util.function.Consumer;

public class EditTextUtil {

    public static TextWatcher onChange(Consumer<CharSequence> onChange) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                onChange.accept(s);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        };
    }

}
