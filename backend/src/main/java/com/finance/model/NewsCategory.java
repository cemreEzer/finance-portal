package com.finance.model;

/**
 * Haber kategorileri.
 * Doküman isterlerine uygun olarak: genel ekonomi, hisse, döviz, tahvil vb.
 */
public enum NewsCategory {

    GENEL_EKONOMI("Genel Ekonomi"),
    HISSE("Hisse Senedi"),
    DOVIZ("Döviz"),
    TAHVIL_BONO("Tahvil / Bono"),
    FON("Yatırım Fonları"),
    KRIPTO("Kripto Para"),
    EMTIA("Emtia"),
    DUNYA("Dünya Ekonomisi"),
    DIGER("Diğer");

    private final String displayName;

    NewsCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
