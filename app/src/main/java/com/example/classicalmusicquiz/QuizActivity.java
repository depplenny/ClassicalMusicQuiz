package com.example.classicalmusicquiz;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

import java.util.ArrayList;

public class QuizActivity extends AppCompatActivity {
    // Debug tag.
    private static final String TAG = "xz";
    // Fields anout quiz.
    private static final int CORRECT_ANSWER_DELAY_MILLIS = 3000;
    private static final String REMAINING_SONGS_KEY = "remaining_songs";
    private int[] mButtonIDs = {R.id.buttonA, R.id.buttonB, R.id.buttonC, R.id.buttonD};
    private ArrayList<Integer> mRemainingSampleIDs;
    private ArrayList<Integer> mAnswerSampleIDs;
    private int mAnswerSampleID;
    private int mCurrentScore;
    private int mHighScore;
    private Button[] mButtons;
    private MyOnClickListener myOnCLickListener;
    // Fields about media player.
    private SimpleExoPlayer mPlayer;
    private PlayerView mPlayerView;
    private PlaybackStateListener playbackStateListener;
    private Sample answerSample;
    /**
     * Inflate quiz activity views, find the PlayerView, create
     * PlaybackStateListener and MyOnClickListener, initialize quiz, and initialze SimpleExoPlayer
     * when the activity is launched.
     *
     * @param savedInstanceState this help restore to a previous state of activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        mPlayerView = findViewById(R.id.playerView);
        playbackStateListener = new PlaybackStateListener();
        myOnCLickListener = new MyOnClickListener();
        initializeQuiz();
        initializePlayer(Uri.parse(answerSample.getUri()));
    }
    /**
     * Release the player when the activity is destroyed.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();
    }
    /**
     * Helper method that initializes quiz.
     */
    private void initializeQuiz() {
        boolean isNewGame = !getIntent().hasExtra(REMAINING_SONGS_KEY);
        // If it's a new game, set the current score to 0 and load all samples.
        if (isNewGame) {
            QuizUtils.setCurrentScore(this, 0);
            mRemainingSampleIDs = Sample.getAllSampleIDs(this);
        // Otherwise, get the remaining song samples from the Intent.
        } else {
            mRemainingSampleIDs = getIntent().getIntegerArrayListExtra(REMAINING_SONGS_KEY);
        }
        // Get current and highest scores.
        mCurrentScore = QuizUtils.getCurrentScore(this);
        mHighScore = QuizUtils.getHighScore(this);
        // Generate a question with four possible answers and get the correct answer.
        mAnswerSampleIDs = QuizUtils.generateQuestion(mRemainingSampleIDs);
        mAnswerSampleID = QuizUtils.getCorrectAnswerID(mAnswerSampleIDs);
        // Load the question mark as the background image until the user answers the question.
        mPlayerView.setDefaultArtwork(ContextCompat.getDrawable(this, R.drawable.question_mark));
        // If there is only one answer left, end the game.
        if (mAnswerSampleIDs.size() < 2) {
            QuizUtils.endGame(this);
            finish();
        }
        // Initialize the buttons with the composers names.
        mButtons = initializeButtons(mAnswerSampleIDs);
        answerSample = Sample.getSampleByID(this, mAnswerSampleID);

        if (answerSample == null) {
            Toast.makeText(this, getString(R.string.sample_not_found_error),
                    Toast.LENGTH_SHORT).show();
        }
    }
    /**
     * Initializes the button to the correct views, and sets the text to the composers names,
     * and set's the OnClick listener to the buttons.
     *
     * @param answerSampleIDs The IDs of the possible answers to the question.
     * @return The Array of initialized buttons.
     */
    private Button[] initializeButtons(ArrayList<Integer> answerSampleIDs) {
        Button[] buttons = new Button[mButtonIDs.length];
        for (int i = 0; i < answerSampleIDs.size(); i++) {
            Button currentButton = findViewById(mButtonIDs[i]);
            Sample currentSample = Sample.getSampleByID(this, answerSampleIDs.get(i));
            buttons[i] = currentButton;
            currentButton.setOnClickListener(myOnCLickListener);
            if (currentSample != null) {
                currentButton.setText(currentSample.getComposer());
            }
        }
        return buttons;
    }
    /**
     * Helper method that initializes SimpleExoPlayer.
     *
     * @param mediaUri The URI of the sample to play.
     */
    private void initializePlayer(Uri mediaUri) {
      if (mPlayer == null) {
        // Create a meadia source from Uri
        MediaSource mediaSource = buildMediaSource(mediaUri);
        // Create a SimpleExoPlayer
        mPlayer = new SimpleExoPlayer.Builder(this).build();
        // Bind it to PlayerView
        mPlayerView.setPlayer(mPlayer);
        // Add PlaybackStateListener to the player
        mPlayer.addListener(playbackStateListener);
        // Prepare the SimpleExoPlayer
        mPlayer.prepare(mediaSource);
        // Play media source
        mPlayer.setPlayWhenReady(true);
      }
    }
    /**
     * Helper method to build MediaSource.
     *
     * @param uri The URI of the sample to play.
     */
    private MediaSource buildMediaSource(Uri uri) {
       // Create a media source factory
       DataSource.Factory dataSourceFactory =
               new DefaultDataSourceFactory(this, Util.getUserAgent(this, "ClassicalMusicQuiz"));
       ProgressiveMediaSource.Factory progressiveMediaSourceFactory =
               new ProgressiveMediaSource.Factory(dataSourceFactory);
       // Create a media source using the supplied URI
       MediaSource mediaSource = progressiveMediaSourceFactory.createMediaSource(uri);

       return mediaSource;
    }
    /**
     * Helper method to release SimpleExoPlayer.
     */
    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.removeListener(playbackStateListener);
            mPlayer.release();
            mPlayer = null;
        }
    }
    /**
     * Inner class that implements Player.EventListener.
     */
    class PlaybackStateListener implements Player.EventListener {
        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            String stateString;
            switch (playbackState) {
                case ExoPlayer.STATE_IDLE:
                    stateString = "ExoPlayer.STATE_IDLE      -";
                    break;
                case ExoPlayer.STATE_BUFFERING:
                    stateString = "ExoPlayer.STATE_BUFFERING -";
                    break;
                case ExoPlayer.STATE_READY:
                    stateString = "ExoPlayer.STATE_READY     -";
                    break;
                case ExoPlayer.STATE_ENDED:
                    stateString = "ExoPlayer.STATE_ENDED     -";
                    break;
                default:
                    stateString = "UNKNOWN_STATE             -";
                    break;
            }
            Log.d(TAG, "changed state to " + stateString + " playWhenReady: " + playWhenReady);
        }
    }
    /**
     * Inner class that implements View.OnClickListener.
     */
    class MyOnClickListener implements View.OnClickListener {
        /**
         * The OnClick method for all of the answer buttons. The method uses the index of the button
         * in button array to to get the ID of the sample from the array of question IDs. It also
         * toggles the UI to show the correct answer.
         *
         * @param v The button that was clicked.
         */
        @Override
        public void onClick(View v) {
            // Show the correct answer.
            showCorrectAnswer();
            // Get the button that was pressed.
            Button pressedButton = (Button) v;
            // Get the index of the pressed button
            int userAnswerIndex = -1;
            for (int i = 0; i < mButtons.length; i++) {
                if (pressedButton.getId() == mButtonIDs[i]) {
                    userAnswerIndex = i;
                }
            }
            // Get the ID of the sample that the user selected.
            int userAnswerSampleID = mAnswerSampleIDs.get(userAnswerIndex);
            // If the user is correct, increase there score and update high score.
            if (QuizUtils.userCorrect(mAnswerSampleID, userAnswerSampleID)) {
                mCurrentScore++;
                QuizUtils.setCurrentScore(QuizActivity.this, mCurrentScore);
                if (mCurrentScore > mHighScore) {
                    mHighScore = mCurrentScore;
                    QuizUtils.setHighScore(QuizActivity.this, mHighScore);
                }
            }
            // Remove the answer sample from the list of all samples, so it doesn't get asked again.
            mRemainingSampleIDs.remove(Integer.valueOf(mAnswerSampleID));
            // Wait some time so the user can see the correct answer, then go to the next question.
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mPlayer.stop();
                    Intent nextQuestionIntent = new Intent(QuizActivity.this, QuizActivity.class);
                    nextQuestionIntent.putExtra(REMAINING_SONGS_KEY, mRemainingSampleIDs);
                    finish();
                    startActivity(nextQuestionIntent);
                }
            }, CORRECT_ANSWER_DELAY_MILLIS);
        }
        /**
         * Disables the buttons and changes the background colors and player art to
         * show the correct answer.
         */
        private void showCorrectAnswer() {
            mPlayerView.setDefaultArtwork(Sample.getComposerArtBySampleID(QuizActivity.this, mAnswerSampleID));
            for (int i = 0; i < mAnswerSampleIDs.size(); i++) {
                int buttonSampleID = mAnswerSampleIDs.get(i);
                mButtons[i].setEnabled(false);
                if (buttonSampleID == mAnswerSampleID) {
                    mButtons[i].getBackground().setColorFilter(ContextCompat.getColor(QuizActivity.this, android.R.color.holo_green_light),
                        PorterDuff.Mode.MULTIPLY);
                } else {
                    mButtons[i].getBackground().setColorFilter(ContextCompat.getColor(QuizActivity.this, android.R.color.holo_red_light),
                        PorterDuff.Mode.MULTIPLY);
                }
                mButtons[i].setTextColor(Color.WHITE);
            }
        }
    }
}
