package com.atsumeru.web.importer.listener;

public interface OnImportCallback {
    void onProgressChanged(int count, int total);
}
