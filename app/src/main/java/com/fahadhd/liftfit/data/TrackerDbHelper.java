package com.fahadhd.liftfit.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;


public class TrackerDbHelper extends SQLiteOpenHelper{
    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "tracker.db";

    public static final String TAG = TrackerDbHelper.class.getSimpleName();


    public TrackerDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.v(TAG,"onCreate was called");
        // Create a table to hold each session of exercise.
        final String SQL_CREATE_SESSIONS_TABLE = "CREATE TABLE " + SessionEntry.TABLE_NAME + " (" +
                SessionEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SessionEntry.COLUMN_DATE + " TEXT NOT NULL, " +
                SessionEntry.COLUMN_USER_WEIGHT + " INTEGER NOT NULL, " +
                SessionEntry.COLUMN_TEMPLATE_NAME + " TEXT NOT NULL DEFAULT 'None'" +
                " );";

        //Each sessions can have 1-5 workouts which are stored in the workouts_table
        final String SQL_CREATE_WORKOUTS_TABLE = "CREATE TABLE " + WorkoutEntry.TABLE_NAME + " (" +
                WorkoutEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                WorkoutEntry.COLUMN_SES_KEY + " INTEGER NOT NULL, " +
                WorkoutEntry.COLUMN_NAME + " TEXT NOT NULL, " +
                WorkoutEntry.COLUMN_WEIGHT + " INTEGER NOT NULL," +
                WorkoutEntry.COLUMN_MAX_SETS + " INTEGER NOT NULL, " +
                WorkoutEntry.COLUMN_MAX_REPS + " INTEGER NOT NULL, " +
                // Set up the session_id column as a foreign key to session table.
                " FOREIGN KEY (" + WorkoutEntry.COLUMN_SES_KEY + ") REFERENCES " +
                SessionEntry.TABLE_NAME + " (" + SessionEntry._ID + ") ON DELETE CASCADE);";

        //Each workout has a set number of sets and the current number of reps
        final String SQL_CREATE_SETS_TABLE = "CREATE TABLE " + SetEntry.TABLE_NAME + " (" +
                SetEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                SetEntry.COLUMN_WORK_KEY + " INTEGER NOT NULL, " +
                SetEntry.COLUMN_CURR_REP + " INTEGER NOT NULL DEFAULT 0, " +
                SetEntry.COLUMN_SET_NUM + " INTEGER NOT NULL, " +
                // Set up the workout column as a foreign key to workout table.
                " FOREIGN KEY (" + SetEntry.COLUMN_WORK_KEY + ") REFERENCES " +
                WorkoutEntry.TABLE_NAME + " (" + WorkoutEntry._ID + ") ON DELETE CASCADE);";

        //Each workout has a set number of sets and the current number of reps
        // Create a table to hold each session of exercise.
        final String SQL_CREATE_TEMPLATES_TABLE = "CREATE TABLE " + TemplateEntry.TABLE_NAME + " (" +
                TemplateEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                TemplateEntry.COLUMN_TEMPLATE_NAME + " TEXT NOT NULL, " +
                TemplateEntry.COLUMN_WORKOUT_NAME + " TEXT NOT NULL, " +
                TemplateEntry.COLUMN_WEIGHT + " INTEGER NOT NULL," +
                TemplateEntry.COLUMN_MAX_SETS + " INTEGER NOT NULL, " +
                TemplateEntry.COLUMN_MAX_REPS + " INTEGER NOT NULL" +
                " );";

        final String SQL_CREATE_NOTES_TABLE = "CREATE TABLE " + NoteEntry.TABLE_NAME + " (" +
                NoteEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +

                NoteEntry.COLUMN_NOTE_CONTENT + " TEXT NOT NULL, " +
                NoteEntry.COLUMN_SES_KEY + " INTEGER NOT NULL, " +
                // Set up the workout column as a foreign key to workout table.
                " FOREIGN KEY (" + NoteEntry.COLUMN_SES_KEY + ") REFERENCES " +
                SessionEntry.TABLE_NAME + " (" + SessionEntry._ID + ") ON DELETE CASCADE);";

        db.execSQL(SQL_CREATE_SESSIONS_TABLE);
        db.execSQL(SQL_CREATE_WORKOUTS_TABLE);
        db.execSQL(SQL_CREATE_SETS_TABLE);
        db.execSQL(SQL_CREATE_TEMPLATES_TABLE);
        db.execSQL(SQL_CREATE_NOTES_TABLE);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        // Used to enable cascading delete
        db.execSQL("PRAGMA foreign_keys = ON;");
        super.onOpen(db);
    }

    //TODO: Change method to retain data when a new version comes out.
    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        Log.w(TAG,
                "Upgrading the database from version " + oldVersion + " to "+ newVersion);
        // Delete's and recreate's all tables when a new version comes out.
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SessionEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + WorkoutEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SetEntry.TABLE_NAME);
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TemplateEntry.TABLE_NAME);
        onCreate(sqLiteDatabase);
    }


    /* Inner class that defines the table contents of the sessions table */
    public static final class SessionEntry implements BaseColumns {

        public static final String TABLE_NAME = "sessions";

        //Current date stored as a string
        public static final String COLUMN_DATE = "date";

        //Current users weight in lbs stored as an integer
        public static final String COLUMN_USER_WEIGHT = "user_weight";

        //Current template a session is using. Empty string means no template is used
        public static final String COLUMN_TEMPLATE_NAME= "template_name";
    }

    /* Inner class that defines the table contents of each workout a session includes */
    public static final class WorkoutEntry implements BaseColumns {

        public static final String TABLE_NAME = "workouts";

        //Foreign key reference to sessions to keep track of each sessions workouts
        public static final String COLUMN_SES_KEY = "session_id";

        //The name of the routine of the exercise - stored as a string
        public static final String COLUMN_NAME = "name";

        //Current weight of a routine - stored as an int
        public static final String COLUMN_WEIGHT = "weight";

        //Maximum number of sets a workout can do - stored as a int.
        public static final String COLUMN_MAX_SETS = "max_sets";

        //Maximum number of reps a workout can do - stored as a int.
        public static final String COLUMN_MAX_REPS = "max_reps";

    }

    /* Inner class that defines the table contents of a single set in a workout includes */
    public static final class SetEntry implements BaseColumns {

        public static final String TABLE_NAME = "sets";
        //Foreign key reference to a specific workout to keep track of its sets/reps.
        public static final String COLUMN_WORK_KEY = "workout_id";

        //Current number of reps a set currently has
        public static final String COLUMN_CURR_REP = "curr_rep";

        /*The order number of the single set in a workout*/

        public static final String COLUMN_SET_NUM = "set_num";

    }

    /* Inner class that defines the table contents of a user templates */
    public static final class TemplateEntry implements BaseColumns {
        public static final String TABLE_NAME = "templates";

        //The name of the routine of the exercise - stored as a string
        public static final String COLUMN_WORKOUT_NAME = "name";

        //Current weight of a routine - stored as an int
        public static final String COLUMN_WEIGHT = "weight";

        //Maximum number of sets a workout can do - stored as a int.
        public static final String COLUMN_MAX_SETS = "max_sets";

        //Maximum number of reps a workout can do - stored as a int.
        public static final String COLUMN_MAX_REPS = "max_reps";

        //Determines what template the row is referring to
        public static final String COLUMN_TEMPLATE_NAME = "template_name";

    }

    /* Inner class that defines the table contents the notes for each session */
    public static final class NoteEntry implements BaseColumns {
        public static final String TABLE_NAME = "notes";

        public static final String COLUMN_NOTE_CONTENT = "note_content";

        public static final String COLUMN_SES_KEY = "session_id";
    }

}
