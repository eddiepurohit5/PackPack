package com.packproof.app;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;

public class TripStore extends SQLiteOpenHelper {
 public TripStore(Context c) { super(c,"packproof.db",null,1); }
 public void onCreate(SQLiteDatabase db) { db.execSQL("CREATE TABLE trips (id INTEGER PRIMARY KEY, data TEXT NOT NULL)"); }
 public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion) { }
 public void save(JSONObject trip) throws JSONException { ContentValues v=new ContentValues(); v.put("id",trip.getLong("id")); v.put("data",trip.toString()); getWritableDatabase().insertWithOnConflict("trips",null,v,SQLiteDatabase.CONFLICT_REPLACE); }
 public JSONArray all() throws JSONException { JSONArray trips=new JSONArray(); try(Cursor c=getReadableDatabase().rawQuery("SELECT data FROM trips ORDER BY id DESC",null)) { while(c.moveToNext()) trips.put(new JSONObject(c.getString(0))); } return trips; }
 public void delete(long id) { getWritableDatabase().delete("trips","id=?",new String[]{String.valueOf(id)}); }
}
