package vn.tdtu.student.todo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.accounts.Account;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.SearchView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import vn.tdtu.student.todo.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private RecyclerView recyclerView;
    private NoteAdapter adapter;

    private Database database;

    private final ArrayList<Note> notes = new ArrayList<>();
    private final ArrayList<Note> origin = new ArrayList<>();

    private DatabaseReference mDatabase;
    private String username;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initView();

        binding.add.setOnClickListener(view -> {
            DialogAdd();
        });

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

    }

    private void initView() {
        //binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //intent
        Intent intent = this.getIntent();
        username = intent.getStringExtra("username");
        binding.titleBg.setText(String.format("What's up, %s! ", username));

        //recycle view
        adapter = new NoteAdapter(this, username ,notes);
        recyclerView = findViewById(R.id.recycleView_task);
        RecyclerView.LayoutManager manager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL,false);
        recyclerView.setLayoutManager(manager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        loadData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Type here to search");

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                notes.clear();
                for(int i = 0 ; i < origin.size(); i++){
                    String value = origin.get(i).getContent();
                    if(value.contains(text)) {
                        notes.add(origin.get(i));
                    }
                }

                adapter.notifyDataSetChanged();

                return false;
            }
        });


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                return true;
            case R.id.action_clear:
                ConfirmClearDialog();
                return true;
            case R.id.action_reload:
                checkEnoughDataFirebase();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void updateUI() {
        if (adapter.getItemCount() == 0) {
            binding.notice.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        }else {
            binding.notice.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void loadData() {
        //local - online database
        //local SQLite
        database = new Database(this, username + ".sqlite", null, 1);
        database.QueryData("create table if not exists note(Id char(100) primary key, content varchar(100) ) ");
        Cursor dataNote = database.GetData("select * from note");

        notes.clear();
        origin.clear();
        while(dataNote.moveToNext()){
            String id = dataNote.getString(0);
            String content = dataNote.getString(1);

            Note note = new Note(id, content);
            notes.add(note);
            origin.add(note);
            adapter.notifyDataSetChanged();
        }

        //online firebase
        mDatabase = FirebaseDatabase.getInstance().getReference().child(username);
        mDatabase.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                if(notes.size() == 0 && origin.size() == 0){
                    Note note = snapshot.getValue(Note.class);
                    note.setId(snapshot.getKey());

                    notes.add(note);
                    origin.add(note);
                    updateUI();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                Note note = snapshot.getValue(Note.class);
                note.setId(snapshot.getKey());
                for(int i = 0 ; i< notes.size(); i++ )
                    if(note.getId().equals(origin.get(i).getId()))
                        origin.remove(note);

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        if(isNetworkConnected()){
            Toast.makeText(this, "You are using online. Click reload in menu to update database", Toast.LENGTH_SHORT).show();
            checkEnoughDataFirebase();
        }else{
            Toast.makeText(this, "You are using offline", Toast.LENGTH_SHORT).show();
        }
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem reload = menu.findItem(R.id.action_reload);
        reload.setVisible(isNetworkConnected());
        return true;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    private void checkEnoughDataFirebase() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                long count = snapshot.getChildrenCount();

                if( count < notes.size()){
                    for(int i = 0; i < notes.size(); i++){
                        mDatabase.child(notes.get(i).getId()).setValue(notes.get(i));
                    }
                }else if(count > notes.size()){
                    for(int i = 0; i < notes.size(); i++){
                        mDatabase.removeValue();
                        mDatabase.child(notes.get(i).getId()).setValue(notes.get(i));
                    }
                }else if(count > 0 && notes.size() == 0){
                    mDatabase.removeValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Toast.makeText(this, "Update database done", Toast.LENGTH_SHORT).show();
    }

    //Function delete - add
    private void ConfirmClearDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear")
                .setMessage("Are you sure you want to clear all?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", (dialogInterface, i) -> {

                    notes.clear();

                    database.QueryData("delete from note");
                    mDatabase.removeValue();

                    Toast.makeText(MainActivity.this, "Clear all success", Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    updateUI();
                })
                .create();
        builder.show();
    }

    private void DialogAdd(){
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        dialog.setContentView(R.layout.dialog_add_item);

        TextInputEditText content = (TextInputEditText) dialog.findViewById(R.id.text_input_content_add);
        TextInputLayout layout = (TextInputLayout) dialog.findViewById(R.id.text_layout_content_add);

        Button btn_add = (Button) dialog.findViewById(R.id.btn_content_add);
        Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel_add);

        btn_add.setOnClickListener(v -> {
            String text = content.getText().toString();
            if(text.isEmpty()){
                layout.setError("You must input content");
            }else{
                addNewNote(text);
                dialog.dismiss();
            }
        });

        btn_cancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        final Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);

        dialog.show();
    }

    private void addNewNote(String content) {
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();

        Note note = new Note(id , content);
        notes.add(note);
        origin.add(note);

        database.QueryData("insert into note values('" + id + "', '" + content + "')");
        mDatabase.child(note.getId()).setValue(note);

        Toast.makeText(this, "Add success", Toast.LENGTH_SHORT).show();
        updateUI();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onPause() {
        super.onPause();

        Cursor dataCount = database.GetData("select count(*) from note");
        while(dataCount.moveToNext()){
            String count = dataCount.getString(0);
            if(Integer.parseInt(count) == 0){
                for(int i = 0; i < notes.size(); i++){
                    String id = notes.get(i).getId();
                    String content = notes.get(i).getContent();
                    database.QueryData("insert into note values('" + id + "', '" + content + "')");

                }
            }
        }

    }
}