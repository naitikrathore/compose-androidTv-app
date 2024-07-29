package com.iwedia.ui.neon.media

import android.app.SearchManager
import android.database.Cursor
import android.provider.BaseColumns

/**
 * Column names that are expected from the Google Assistant to
 * exist in the cursor object returned.
 */
enum class Column(val columnName: String, val columnType: Int) {
  rowId(BaseColumns._ID, Cursor.FIELD_TYPE_INTEGER),
  title(SearchManager.SUGGEST_COLUMN_TEXT_1, Cursor.FIELD_TYPE_STRING),
  description(SearchManager.SUGGEST_COLUMN_TEXT_2, Cursor.FIELD_TYPE_STRING),
  icon(SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE, Cursor.FIELD_TYPE_STRING),
  dataType(SearchManager.SUGGEST_COLUMN_CONTENT_TYPE, Cursor.FIELD_TYPE_STRING),
  videoWidth(SearchManager.SUGGEST_COLUMN_VIDEO_WIDTH, Cursor.FIELD_TYPE_STRING),
  videoHeight(SearchManager.SUGGEST_COLUMN_VIDEO_HEIGHT, Cursor.FIELD_TYPE_STRING),
  productionYear(SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR, Cursor.FIELD_TYPE_STRING),
  columnDuration(SearchManager.SUGGEST_COLUMN_DURATION, Cursor.FIELD_TYPE_INTEGER),
  intentExtraData(SearchManager.SUGGEST_COLUMN_INTENT_EXTRA_DATA, Cursor.FIELD_TYPE_STRING);

  companion object {
    @JvmStatic
    fun columnNames() = arrayOf(
        rowId.columnName,
        title.columnName,
        description.columnName,
        icon.columnName,
        dataType.columnName,
        videoWidth.columnName,
        videoHeight.columnName,
        productionYear.columnName,
        columnDuration.columnName,
        intentExtraData.columnName
    )

    @JvmStatic
    fun columnTypes() = arrayOf(
        rowId.columnType,
        title.columnType,
        description.columnType,
        icon.columnType,
        dataType.columnType,
        videoWidth.columnType,
        videoHeight.columnType,
        productionYear.columnType,
        columnDuration.columnType,
        intentExtraData.columnType
    )
  }
}