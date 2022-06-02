package vn.tdtu.student.todo;

import android.accounts.Account;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import vn.tdtu.student.todo.databinding.ActivityLoginBinding;
import vn.tdtu.student.todo.databinding.ActivityMainBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    private final String[] check = {".", "#", "$", "[", "]"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        binding.btnLoginEmail.setOnClickListener(view12 -> {
            String username = binding.textInputEmail.getText().toString();
           if(!username.isEmpty() && checkContain(username)){
               Intent intent = new Intent(this, MainActivity.class);
               intent.putExtra("username", username);
               startActivity(intent);
           }
           else if(!checkContain(username)){
               binding.textLayoutEmail.setError("Your username must not contain '.', '#', '$', '[', or ']'");
               return;
           }
           else{
               binding.textLayoutEmail.setError("You must input your username");
               return;
           }
        });

    }

    private Boolean checkContain(String text){
        for (String s : check) {
            if(text.contains(s))
                return false;
        }
        return true;

    }
}
