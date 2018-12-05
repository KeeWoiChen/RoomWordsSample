package com.example.taruc.roomwordssample;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        RecyclerView recyclerView = findViewById(R.id.recyclerview);
        final WordListAdapter adapter = new WordListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Entity(tableName = "word_table")
    public class Word {

        @PrimaryKey
        @NonNull
        @ColumnInfo(name = "word")
        private String mWord;

        public Word(@NonNull String word) {this.mWord = word;}

        public String getWord(){return this.mWord;}
    }

    @Dao
    public interface WordDao {

        @Insert
        void insert(Word word);

        @Query("DELETE FROM word_table")
        void deleteAll();

        @Query("SELECT * from word_table ORDER BY word ASC")
        LiveData<List<Word>> getAllWords();
    }

    @Database(entities = {Word.class}, version = 1, exportSchema = false)
    public abstract class WordRoomDatabase extends RoomDatabase {

        public abstract WordDao wordDao();
        private static WordRoomDatabase INSTANCE;

        static WordRoomDatabase getDatabase(final Context context) {
            if (INSTANCE == null) {
                synchronized (WordRoomDatabase.class) {
                    if (INSTANCE == null) {
                        INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                WordRoomDatabase.class, "word_database")
                                // Wipes and rebuilds instead of migrating
                                // if no Migration object.
                                // Migration is not part of this practical.
                                .fallbackToDestructiveMigration()
                                .build();
                    }
                }
            }
            return INSTANCE;
        }
    }

    public class WordRepository {

        private WordDao mWordDao;
        private LiveData<List<Word>> mAllWords;

        WordRepository(Application application) {
            WordRoomDatabase db = WordRoomDatabase.getDatabase(application);
            mWordDao = db.wordDao();
            mAllWords = mWordDao.getAllWords();
        }

        LiveData<List<Word>> getAllWords() {
            return mAllWords;
        }

        public void insert (Word word) {
            new insertAsyncTask(mWordDao).execute(word);
        }

        private static class insertAsyncTask extends AsyncTask<Word, Void, Void> {

            private WordDao mAsyncTaskDao;

            insertAsyncTask(WordDao dao) {
                mAsyncTaskDao = dao;
            }

            @Override
            protected Void doInBackground(final Word... params) {
                mAsyncTaskDao.insert(params[0]);
                return null;
            }
        }
    }

    public class WordViewModel extends AndroidViewModel {

        private WordRepository mRepository;

        private LiveData<List<Word>> mAllWords;

        public WordViewModel (Application application) {
            super(application);
            mRepository = new WordRepository(application);
            mAllWords = mRepository.getAllWords();
        }

        LiveData<List<Word>> getAllWords() { return mAllWords; }

        public void insert(Word word) { mRepository.insert(word); }
    }

    public class WordListAdapter extends RecyclerView.Adapter<WordListAdapter.WordViewHolder> {

        private final LayoutInflater mInflater;
        private List<Word> mWords; // Cached copy of words

        WordListAdapter(Context context) { mInflater = LayoutInflater.from(context); }

        @Override
        public WordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = mInflater.inflate(R.layout.recyclerview_item, parent, false);
            return new WordViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(WordViewHolder holder, int position) {
            if (mWords != null) {
                Word current = mWords.get(position);
                holder.wordItemView.setText(current.getWord());
            } else {
                // Covers the case of data not being ready yet.
                holder.wordItemView.setText("No Word");
            }
        }

        void setWords(List<Word> words){
            mWords = words;
            notifyDataSetChanged();
        }

        // getItemCount() is called many times, and when it is first called,
        // mWords has not been updated (means initially, it's null, and we can't return null).
        @Override
        public int getItemCount() {
            if (mWords != null)
                return mWords.size();
            else return 0;
        }

        class WordViewHolder extends RecyclerView.ViewHolder {
            private final TextView wordItemView;

            private WordViewHolder(View itemView) {
                super(itemView);
                wordItemView = itemView.findViewById(R.id.textView);
            }
        }
    }
}
