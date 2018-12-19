package com.anondev.gaurav.camerablur;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class Dbhandler extends SQLiteOpenHelper{
    public static final int DATABASE_VERSION=1;
    public static final String DATABASE_NAME="indx.db";
    public static final String TABLE_NAME = "entry";

    public static final String ID="_id";

    public static final String COLUMN1_NAME_TITLE_TABLE1 = "Path";
    public static final String COLUMN2_NAME_TITLE_TABLE1 = "Map";




    public Dbhandler(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, DATABASE_NAME, factory, DATABASE_VERSION);

    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        String Query= "CREATE TABLE " + TABLE_NAME + " (" +
                ID + " INTEGER ," +
                COLUMN1_NAME_TITLE_TABLE1 +" TEXT,"+
                COLUMN2_NAME_TITLE_TABLE1 +" TEXT);";
        db.execSQL(Query);


    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }
    public void addRow(entry e)
    {
        int id=rCount();
        SQLiteDatabase db = getWritableDatabase();
        ContentValues x=new ContentValues();
        x.put(ID,id);
        x.put(COLUMN1_NAME_TITLE_TABLE1,e.getPath());
        x.put(COLUMN2_NAME_TITLE_TABLE1,e.getMap());
        db.insert(TABLE_NAME,null,x);

    }
    public entry getresult(int x){

        entry e=new entry();
        SQLiteDatabase db = getWritableDatabase();
        String query="SELECT * FROM "+TABLE_NAME+" WHERE "+ID+"="+x+";";
        Cursor c=db.rawQuery(query,null);
        c.moveToFirst();
        if(!c.isAfterLast())
        {
            if(c.getString(c.getColumnIndex(ID))!=null)
                e.set_id(c.getInt(c.getColumnIndex(ID)));
            if(c.getString(c.getColumnIndex(COLUMN1_NAME_TITLE_TABLE1))!=null)
                e.setPath(c.getString(c.getColumnIndex(COLUMN1_NAME_TITLE_TABLE1)));
            if(c.getString(c.getColumnIndex(COLUMN2_NAME_TITLE_TABLE1))!=null)
                e.setMap(c.getString(c.getColumnIndex(COLUMN2_NAME_TITLE_TABLE1)));
        }

        c.close();
        return e;
    }
    public int rCount() {
        String countQuery = "SELECT  * FROM " + TABLE_NAME;
        SQLiteDatabase db =getWritableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int cnt = cursor.getCount();
        cursor.close();
        return cnt;
    }
    public int set_newPath(String image,String BlurredImage){
        SQLiteDatabase db = getWritableDatabase();
        String Query="SELECT * FROM "+TABLE_NAME+" WHERE "+COLUMN1_NAME_TITLE_TABLE1+"='"+image+"'";
        Cursor c=db.rawQuery(Query,null);
        c.moveToFirst();
        if(!c.isAfterLast())
        {

            Query = "UPDATE " + TABLE_NAME + " SET " + COLUMN1_NAME_TITLE_TABLE1 + "='" + BlurredImage + "' WHERE " + COLUMN1_NAME_TITLE_TABLE1 + "='" + image + "';";
            db.execSQL(Query);
            return c.getInt(c.getColumnIndex(ID));
        }
        return -1;
    }

    public void move_entry(int from,int to){
        String Query;
        SQLiteDatabase db = getWritableDatabase();
        Query="UPDATE "+TABLE_NAME+" SET "+ID+"=-1 WHERE "+ID+"="+from+";";
        db.execSQL(Query);

        Query="UPDATE "+TABLE_NAME+" SET "+ID+"="+ID+"-1 WHERE "+ID+">"+from+";";
        db.execSQL(Query);

        Query="UPDATE "+TABLE_NAME+" SET "+ID+"="+ID+"+1 WHERE "+ID+">"+(to-1)+";";
        db.execSQL(Query);

        Query="UPDATE "+TABLE_NAME+" SET "+ID+"="+to+" WHERE "+ID+"="+(-1)+";";
        db.execSQL(Query);

    }


    public void DeleteEntry(int i){
        String Query;
        SQLiteDatabase db = getWritableDatabase();
        Query="DELETE FROM "+TABLE_NAME+" WHERE "+ID+"="+i+";";
        db.execSQL(Query);

        Query="UPDATE "+TABLE_NAME+" SET "+ID+"="+ID+"-1 WHERE "+ID+">"+i+";";
        db.execSQL(Query);

    }

}
