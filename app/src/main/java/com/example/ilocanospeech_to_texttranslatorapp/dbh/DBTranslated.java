package com.example.ilocanospeech_to_texttranslatorapp.dbh;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.ilocanospeech_to_texttranslatorapp.model.RecyclerModel;

import java.util.ArrayList;
import java.util.List;

public class DBTranslated extends SQLiteOpenHelper {
    private static final String DB_NAME = "translateddb";
    private static final int DB_VERSION = 1;
    private static final String TABLE_NAME = "tbl_translated";
    private static final String ID_COL = "id";
    private static final String ENG_TEXT = "english_text";
    private static final String ILO_TEXT = "ilocano_text";
    private static final String SAVED_TIME = "timestamp";

    public DBTranslated(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String query = "CREATE TABLE " + TABLE_NAME + " (" + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        ENG_TEXT + " TEXT," + ILO_TEXT + " TEXT," + SAVED_TIME + " TEXT)";

        sqLiteDatabase.execSQL(query);
    }

    public void addTranslatedText(String englishText, String ilocanoText, String timeStamp) {
        SQLiteDatabase sqLiteDatabase = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(ENG_TEXT, englishText);
        values.put(ILO_TEXT, ilocanoText);
        values.put(SAVED_TIME, timeStamp);

        sqLiteDatabase.insert(TABLE_NAME, null, values);
        sqLiteDatabase.close();
    }

    public List<RecyclerModel> getAllTranslated() {
        List<RecyclerModel> translations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_NAME + " ORDER BY id DESC LIMIT 20", null);

        if(cursor.moveToFirst()){
            do {
                @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex(ID_COL));
                @SuppressLint("Range") String timestamp = cursor.getString(cursor.getColumnIndex(SAVED_TIME));
                @SuppressLint("Range") String englishText = cursor.getString(cursor.getColumnIndex(ENG_TEXT));
                @SuppressLint("Range") String ilocanoText = cursor.getString(cursor.getColumnIndex(ILO_TEXT));

                translations.add(new RecyclerModel(id, timestamp, englishText, ilocanoText));
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return translations;
    }

    public void deleteTranslationbyID(int id){
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("tbl_translated", "id=?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int getHistoryNum(){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_NAME, null);
        int c = 0;
        if (cursor.moveToFirst()){
            c = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return c;
    }
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }
}
