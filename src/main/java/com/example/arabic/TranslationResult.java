/*
 * ArabicTranslator - Minecraft Paper Plugin
 * Copyright (c) 2026 Warasugi
 * 
 * Licensed under the MIT License.
 */

package com.example.arabic;

public class TranslationResult {
    private final String arabic;
    private final String romanization;

    public TranslationResult(String arabic, String romanization) {
        this.arabic = arabic;
        this.romanization = romanization;
    }

    public String getArabic() {
        return arabic;
    }

    public String getRomanization() {
        return romanization;
    }
}
