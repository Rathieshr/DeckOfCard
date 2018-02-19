package com.rathiesh.deckofcards.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.rathiesh.deckofcards.R;
import com.rathiesh.deckofcards.adapter.RankRecyclerListAdapter;
import com.rathiesh.deckofcards.adapter.SuitRecyclerListAdapter;
import com.rathiesh.deckofcards.base.BaseActivity;
import com.rathiesh.deckofcards.businesslogiclayer.RankModel;
import com.rathiesh.deckofcards.delegates.CardDeckViewResponseInterface;
import com.rathiesh.deckofcards.viewModel.CardDeckViewModel;

import java.util.Vector;
/**
 * Created by rathiesh on 16/02/18.
 * This activity is the main activity which holds the layout of recycler view and corresponding adapter initializations
 */
 public class CardDeck extends BaseActivity implements CardDeckViewResponseInterface {

    private Context mContext;
    private CardDeckViewModel cardDeckViewModel;  // Model which holds all the details
    private RecyclerView recyclerView;            // recycler view declaration
    private Toolbar toolbar;                      // Toolbar initialization
    private boolean isSuit = true;                // boolean to know suit list or rank list.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card_deck);  // corresponding layout assigned to activity to setcontentview
        mContext = this;                              // Obtaining current activity instance through context
        initialization();                             // initialization method - all view initialization goes here
        setListeners();                               // set listeners and actions are performed in this method.
        cardDeckViewModel = new CardDeckViewModel(mContext,this); // ViewModel Intialization
        cardDeckViewModel.createSuitRankModel();      // Interface call to create the card model
    }

    /**
     * Set Listeners and actions are done in this methods
     */
    private void setListeners() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    /**
     * Adding Suit adapter to the recycler view
     * Model is passed by obtaining from base view model
     * Interface is intialized to make call from adapter and implementation is done here
     */
    private void setSuitAdapters() {
        SuitRecyclerListAdapter suitRecyclerListAdapter = new SuitRecyclerListAdapter(mContext,cardDeckViewModel.getDeckOfCardsModel(),this);
        recyclerView.setAdapter(suitRecyclerListAdapter);
    }

    /**
     * All view initialization and properties sets are done here.
     */
    private void initialization() {
        toolbar = findViewById(R.id.toolbar);      // toolbar is initialised
        toolbar.setTitle(R.string.app_name);       // setting title in toolbar
        if(isSuit)                                 // setting subtitle in toolbar
            toolbar.setSubtitle("Suit");
        else
            toolbar.setSubtitle("Rank");
        setSupportActionBar(toolbar);              // Setting toolbar
        recyclerView = findViewById(R.id.recyclerview);     //intializing recyclerview
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext)); // setting recycler view properties
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setHasFixedSize(true);
    }

    /**
     * Interface implementation
     * Called on model created and values are set
     */
    @Override
    public void onModelCreated() {
        setSuitAdapters();  // on all set Suit adapter is called and is set to recycler view.
    }

    /**
     * Interface implementation from suit adapter
     * called on clicking the item of suit list item.
     * @param rankModelVector - on clicking the child the corresponding rank vector is obtained
     *                        this is used to create the rank recyclerview.
     */
    @Override
    public void onSuitItemClick(Vector<RankModel> rankModelVector) {
        isSuit = false;                          // set to false , since on click it will redirect to rank and its not suit.
        toolbar.setSubtitle("Rank");             // change subtitle of the toolbar
        recyclerView.removeAllViewsInLayout();   // Since same recycler view is reused . the view are cleared first.
        RankRecyclerListAdapter rankRecyclerListAdapter = new RankRecyclerListAdapter(mContext,rankModelVector); // rank list adapter is initailized
        recyclerView.setAdapter(rankRecyclerListAdapter);         //The rank adapter is set to recyclerview
    }

    /**
     * check which list is currently shown and action is performed
     * on clicking back
     * if rank list previous suit list is shown.
     * else
     * if suit list then application is finished.
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow of the toolbar.
        if (item.getItemId() == android.R.id.home) {
            if(!isSuit){
                boolean isSuitlocal = true;
                isSuit = isSuitlocal;
                toolbar.setSubtitle("Suit"); 
                recyclerView.removeAllViewsInLayout();    // Since same recycler view is reused . the view are cleared first.
                cardDeckViewModel.createSuitRankModel();  // Interface call to create the card model
                setSuitAdapters();                        // on all set Suit adapter is called and is set to recycler view.
            }
            else{
                finish(); // close this activity and return to preview activity (if there is any)
            }
        }

        return super.onOptionsItemSelected(item);
    }
}
