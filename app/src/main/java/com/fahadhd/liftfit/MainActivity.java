package com.fahadhd.liftfit;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.fahadhd.liftfit.R;
import com.fahadhd.liftfit.exercises.ExerciseActivity;
import com.fahadhd.liftfit.exercises.TimerService;
import com.fahadhd.liftfit.sessions.Session;
import com.fahadhd.liftfit.sessions.SessionsFragment;
import com.fahadhd.liftfit.data.TrackerDAO;
import com.fahadhd.liftfit.utilities.Constants;
import com.fahadhd.liftfit.utilities.Utility;


import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.GregorianCalendar;


public class MainActivity extends AppCompatActivity {

    SessionsFragment sessionsFragment;
    ArrayList<Session> sessions;
    //Database action object to query sqlite database tracker.db
    TrackerDAO dao;

    public static final String ADD_TASK = "add_session";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Title is already in place
        if(getSupportActionBar() != null){
            getSupportActionBar().setTitle("");
        }


        TrackerApplication application  = (TrackerApplication)this.getApplication();
        dao = application.getDatabase();
        sessions = application.getSessions();

        sessionsFragment =  ((SessionsFragment)getSupportFragmentManager()
                .findFragmentById(R.id.sessions_fragment));

        FloatingActionButton addTask = (FloatingActionButton) findViewById(R.id.session_button);
        assert addTask != null;
        addTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Adds a new session in a background thread
                new AddSession().execute();
            }
        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        sessionsFragment.restartAdapter();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Adds a session to database in a background thread.
    //Goes into exercise activity when finished.
    public class AddSession extends AsyncTask<String,Void,Session>{

        @Override
        protected Session doInBackground(String... params) {
            SimpleDateFormat fmt = new SimpleDateFormat("MMM dd");
            GregorianCalendar calendar = new GregorianCalendar();
            fmt.setCalendar(calendar);
            String dateFormatted = fmt.format(calendar.getTime());
            SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            int user_weight = Integer.parseInt(shared_pref.getString(getString
                    (R.string.pref_user_weight_key),getString
                    (R.string.pref_default_user_weight)));

            Session session = dao.addSession(dateFormatted,user_weight);
            dao.addNotes("",session.getSessionId());
            sessions.add(0,session);

            return session;
        }

        @Override
        protected void onPostExecute(Session session) {
            Intent exercise = new Intent(MainActivity.this, ExerciseActivity.class).
                    putExtra(ADD_TASK, session);
            startActivity(exercise);
        }
    }


    @Override
    protected void onDestroy() {
        if(Utility.isMyServiceRunning(TimerService.class,this)) {
            Intent timerIntent = new Intent(this, TimerService.class);
            timerIntent.setAction(Constants.ACTION.STOP_FOREGROUND_ACTION);
            startService(timerIntent);
        }
        super.onDestroy();
    }
}
