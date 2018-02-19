package com.rathiesh.deckofcards.base;

import android.content.Intent;

import com.rathiesh.deckofcards.R;
import com.rathiesh.deckofcards.activities.CardDeck;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by rathiesh on 18/02/18.
 */
@RunWith(RobolectricTestRunner.class)
public class SplashScreenTest {
    private SplashScreen splashScreenActivity;
    @Before
    public void setUp() throws Exception {
        splashScreenActivity = Robolectric.buildActivity(SplashScreen.class)
                .create()
                .resume()
                .get();
    }

    @After
    public void tearDown() throws Exception {
    }
    @Test
    public void checkActivityNotNull() throws Exception {
        assertNotNull(splashScreenActivity);
    }
    @Test
    public void shouldHaveCorrectAppName() throws Exception {
        String hello = splashScreenActivity.getResources().getString(R.string.app_name);
        assertThat(hello, equalTo("Deck Of Cards"));
    }
    @Test
    public void splashScreenDelay() throws InterruptedException{
        assertFalse(splashScreenActivity.isFinishing());
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        ShadowApplication instance = ShadowApplication.getInstance();
        Intent nextStartedActivity = instance.getNextStartedActivity();
        assertNotNull(nextStartedActivity);
        String className = nextStartedActivity.getComponent().getClassName();
        assertThat(className, is(CardDeck.class.getName()));
        assertTrue(splashScreenActivity.isFinishing());

    }

}