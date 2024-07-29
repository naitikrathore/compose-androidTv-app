// IStringTranslationListener.aidl
package com.cltv.mal.model.async;

// Declare any non-default types here with import statements
interface IStringTranslationListener {
    String getStringValue(in String stringId);
}