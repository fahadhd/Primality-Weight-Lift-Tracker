package com.example.fahadhd.bodybuildingtracker.exercises;


import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import com.example.fahadhd.bodybuildingtracker.MainActivity;
import com.example.fahadhd.bodybuildingtracker.R;
import com.example.fahadhd.bodybuildingtracker.TrackerApplication;
import com.example.fahadhd.bodybuildingtracker.data.TrackerDAO;
import com.example.fahadhd.bodybuildingtracker.sessions.Session;
import com.example.fahadhd.bodybuildingtracker.utilities.Constants;

import java.util.ArrayList;

public class ExerciseActivity extends AppCompatActivity implements WorkoutDialog.Communicator {
    ExercisesFragment exercisesFragment;
    public static final String TAG = MainActivity.class.getSimpleName();
    TrackerDAO dao;
    public static long sessionID;
    ArrayList<Session> sessions;
    //ArrayList<Workout> workouts;
    String name;
    TrackerApplication application;
    /***********Snackbar variables**************/
    TimerService mTimerService;
    Intent timerIntent;
    View mySnackView; TextView timerView, snackbarText;
    Snackbar mySnackBar;
    long currentTime = 0L;
    boolean mServiceBound = false, durationUpdated = false, snackBarOn = false;
    boolean isServiceOn = false;
    /******************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercises);

        application  = (TrackerApplication)this.getApplication();
        dao = application.getDatabase();
        sessions = application.getSessions();
        exercisesFragment = (ExercisesFragment) getSupportFragmentManager().
                findFragmentById(R.id.exercises_fragment);
        sessionID = exercisesFragment.sessionID;
        mySnackView = getLayoutInflater().inflate(R.layout.my_snackbar, null);
        timerView = (TextView) mySnackView.findViewById(R.id.timer);

        /**Creates a new exercise loader if one doesn't exist or refreshes the data if one does exist.**/
        if (this.getSupportLoaderManager().getLoader(R.id.exercise_loader_id) == null) {
            this.getSupportLoaderManager().initLoader(R.id.exercise_loader_id, null, exercisesFragment);
        } else {
            this.getSupportLoaderManager().restartLoader(R.id.exercise_loader_id, null, exercisesFragment);
        }

        /**Button in charge of adding workouts to the list_view. First displays a dialog for input**/
        ImageButton addWorkout = (ImageButton) findViewById(R.id.btn_add_workout);
        if (addWorkout != null) {
            addWorkout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    WorkoutDialog dialog = new WorkoutDialog();
                    dialog.show(getFragmentManager(), "WorkoutDialog");
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        timerIntent = new Intent(this, TimerService.class);
        currentTime = 0L;
        //Binds to an existing running service
        if (isMyServiceRunning(TimerService.class)) {
            isServiceOn = true;
            Log.v(TAG,"Bound to service");
            bindService(timerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            registerTimerReceiver();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_exercises, menu);
        return true;
    }



    @Override
    protected void onStop() {
        super.onStop();
        if(isServiceOn && mServiceBound) {
            unBindTimerService();
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    public void getWorkoutInfo(String name, int weight, int max_sets, int max_reps) {
        this.name = name;
        new AddWorkoutTask().execute(weight, max_sets, max_reps);
    }

    @Override
    public void updateWorkoutInfo(Workout workout, String name, int weight, int maxSet, int maxRep) {
        Workout updatedWorkout =new Workout(workout.getSessionID(),workout.getWorkoutID(),
                name,weight,maxSet,maxRep,new ArrayList<Set>());
        if(!workout.equals(updatedWorkout)){
//        Log.v(TAG, "position:"+exercisesFragment.position + "workouts "+sessions.get(exercisesFragment.position).getWorkouts().size()+""
//        +"order number is "+ workout.getOrderNum());
            new UpdateWorkout().execute(workout, updatedWorkout);
        }
    }

    @Override
    public void deleteWorkoutInfo(Workout workout) {
        new DeleteWorkout().execute(workout);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                startActivity(new Intent(getApplicationContext(),MainActivity.class));
                return true;
        }
        return (super.onOptionsItemSelected(menuItem));
    }


    /*************** Adds workout data in background thread *****************/
    public class AddWorkoutTask extends AsyncTask<Integer, Void, Void> {

        @Override
        protected Void doInBackground(Integer... params) {
            int weight = params[0];
            int max_sets = params[1];
            int max_reps = params[2];
            addWorkoutTask(name, weight, max_sets, max_reps);
            //Restart loader so it updates the new data to the list.
            ExerciseActivity.this.getSupportLoaderManager().restartLoader(R.id.exercise_loader_id, null, exercisesFragment);
            return null;
        }

        public void addWorkoutTask(String name, int weight, int max_sets, int max_reps) {
            int position = exercisesFragment.position;
            ArrayList<Workout> workouts = sessions.get(position).workouts;
            Workout workout = dao.addWorkout(sessionID, workouts.size() + 1, name, weight, max_sets, max_reps);
            ArrayList<Set> sets = dao.addSets(workout.getWorkoutID(), max_sets);
            workout.sets = sets;
            sessions.get(exercisesFragment.position).getWorkouts().add(workout);
        }
    }


    /***************** Updates a workout in background thread *************/

    public class UpdateWorkout extends AsyncTask<Workout,Void,Void>{

        @Override
        protected Void doInBackground(Workout... params) {
            ArrayList<Workout> workouts = sessions.get(exercisesFragment.position).getWorkouts();
            dao.db.beginTransaction();
            Workout oldWorkout = params[0];
            Workout updatedWorkout = params[1];
            dao.updateWorkout(oldWorkout,updatedWorkout);
            updatedWorkout.sets = dao.addSets(updatedWorkout.getWorkoutID(),updatedWorkout.getMaxSets());
            dao.db.setTransactionSuccessful();
            dao.db.endTransaction();

            for(int i = 0; i< workouts.size(); i++){
                if(workouts.get(i).getWorkoutID() == updatedWorkout.getWorkoutID()){
                    workouts.set(i,updatedWorkout);
                }
            }
            ExerciseActivity.this.getSupportLoaderManager().restartLoader(R.id.exercise_loader_id, null, exercisesFragment);
            return null;
        }
    }
    //TODO: these async tasks can be merged into one and that loop can be made into a helper method
    //also instead of looping most of the time the workoutid is the same as its index in the array
    //so check that first to speed things up.
    public class DeleteWorkout extends AsyncTask<Workout,Void,Void>{

        @Override
        protected Void doInBackground(Workout... params) {
            dao.deleteWorkout(params[0].getWorkoutID());
            ArrayList<Workout> workouts = sessions.get(exercisesFragment.position).getWorkouts();
            for(int i = 0; i < workouts.size(); i++){
                if(workouts.get(i).getWorkoutID() == params[0].getWorkoutID()){
                    workouts.remove(i);
                }
            }
            ExerciseActivity.this.getSupportLoaderManager().restartLoader(R.id.exercise_loader_id, null, exercisesFragment);
            return null;
        }
    }


    /****************************** Timer Snackbar ****************************************/

    /****************** BroadCast Receiver in charge of snackbar counter *****************/

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(mTimerService != null) {
                //Unbinds from service if its a service for another workout
                if (mTimerService.getSessionID() != sessionID) {
                    unBindTimerService();
                    unregisterReceiver(broadcastReceiver);
                    return;
                }
                if (intent.getAction().equals(Constants.TIMER.TIMER_OFF) ||
                        (snackBarOn && mySnackBar!= null && !mySnackBar.isShown())) {
                    stopTimerService();
                    return;
                }
                if (!snackBarOn) {
                    if (mySnackBar == null) {
                        mySnackBar = initCustomSnackbar(mTimerService.getMessage());
                    }
                    mySnackBar.setDuration(600000);
                    mySnackBar.show();
                    snackBarOn = true;
                }
                updateTimerUI();
            }
        }
    };

    /****************** Service to bind exercise activity with timer service ****************/
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TimerService.MyBinder myBinder = (TimerService.MyBinder) service;
            mTimerService = myBinder.getService();
            mServiceBound = true;
        }
    };

    /******** Returns a custom snackbar to be used for the timer between sets **************/
    public Snackbar initCustomSnackbar(String message){
        View exerciseView;
        exerciseView = findViewById(R.id.exercises_list_main);
        if(exerciseView == null) return  null;
        final Snackbar snackbar = Snackbar.make(exerciseView, message, Snackbar.LENGTH_LONG);

        /**** Customizing snackbar view with my own.*****/
        LayoutInflater inflater =  getLayoutInflater();
        mySnackView = inflater.inflate(R.layout.my_snackbar, null);
        timerView = (TextView) mySnackView.findViewById(R.id.timer);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) snackbar.getView();
        layout.setBackgroundColor(Color.GRAY);
        snackbarText = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
        snackbarText.setTextSize(14f);
        //layout basically works like a list where you can add views at the top and also remove them.
        layout.addView(mySnackView, 0);
        /*********************************************/

        /////Makes the action button width smaller.////
        snackbar.setActionTextColor(Color.WHITE);
        Button action = (Button) snackbar.getView().findViewById(android.support.design.R.id.snackbar_action);
        ViewGroup.LayoutParams params= action.getLayoutParams();
        params.width= 100;
        action.setLayoutParams(params);
        //////////////////////////////////////////////

        /**Dismisses snackbar and stop service when user presses the X button.**/
        snackbar.setAction(R.string.dismiss, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimerService();
            }
        });

        return snackbar;
    }


    /************************ Helper methods for snackbar timer start *********************************/
    //Receives the current timer from the timer service broadcast and updates the UI
    public boolean updateTimerUI(){
        if(!durationUpdated && mTimerService.isDurationReached() && mySnackBar != null){
            //settings snackbar to a different color when timer has reached its duration
            Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout) mySnackBar.getView();
            layout.setBackgroundColor(Color.BLUE);
            snackbarText.setText(mTimerService.getMessage());
            durationUpdated = true;
        }
        //Updating the timer in the snackbar
        this.currentTime = mTimerService.getTimer();
        int secs = (int) (currentTime / 1000);
        int minutes = secs / 60;
        timerView.setText(Integer.toString(minutes) + ":" + String.format("%02d", secs%60));
        return true;
    }
    public void startTimerService(String message){
        if(isServiceOn && mTimerService != null){
            mTimerService.resetTimer(message);
            durationUpdated = false;
            if(mySnackBar != null){
                mySnackBar.dismiss();
                snackBarOn = false;
            }
        }
        else {
            timerIntent.setAction(Constants.ACTION.START_FOREGROUND_ACTION);
            timerIntent.putExtra(Constants.GENERAL.SESSION_ID, sessionID);
            timerIntent.putExtra(Constants.TIMER.TIMER_MSG, message);
            startService(timerIntent);
            isServiceOn = true;
            Log.v(TAG, "Bound to service");
            bindService(timerIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
            registerTimerReceiver();
        }
        mySnackBar = initCustomSnackbar(message);
    }

    public void stopTimerService(){
        unBindTimerService();
        if(isServiceOn) {
            timerIntent.setAction(Constants.ACTION.STOP_FOREGROUND_ACTION);
            startService(timerIntent);
            unregisterReceiver(broadcastReceiver);
            isServiceOn = false;
        }
        if(mySnackBar != null){mySnackBar.dismiss(); snackBarOn = false;}
        durationUpdated = false;
    }


    public void registerTimerReceiver(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Constants.TIMER.TIMER_RUNNING);
        intentFilter.addAction(Constants.TIMER.TIMER_OFF);
        registerReceiver(broadcastReceiver, intentFilter);
    }


    public void unBindTimerService(){
        if(mServiceBound) {
            Log.v(TAG,"UnBounded from service");
            unbindService(mServiceConnection);
            mServiceBound = false;
        }
    }

    public  boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /*********************** Helper methods for snackbar timer end ***************************/
}
