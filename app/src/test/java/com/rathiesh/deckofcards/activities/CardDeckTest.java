package com.rathiesh.deckofcards.activities;

import com.rathiesh.deckofcards.R;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

/**
 * Created by rathiesh on 19/02/18.
 */
@RunWith(RobolectricTestRunner.class)
public class CardDeckTest {

    private CardDeck cardDeckActivity;
    @Before
    public void setUp() throws Exception {
        cardDeckActivity = Robolectric.buildActivity(CardDeck.class)
                .create()
                .resume()
                .get();
    }

    @After
    public void tearDown() throws Exception {
    }
    @Test
    public void checkActivityNotNull() throws Exception {
        assertNotNull(cardDeckActivity);
    }
    @Test
    public void shouldHaveCorrectAppName() throws Exception {
        String hello = cardDeckActivity.getResources().getString(R.string.app_name);
        assertThat(hello, equalTo("Deck Of Cards"));
    }

}