package vn.tdtu.student.todo;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;

public class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.ViewHolder> {
    private Context context;
    private ArrayList<Note> notes;
    private String username;
    private DatabaseReference mDatabase;
    private Database database;

    public NoteAdapter(Context context, String username ,ArrayList<Note> notes) {
        this.context = context;
        this.notes = notes;
        this.username = username;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        database = new Database(context, username + ".sqlite", null, 1);
        database.QueryData("create table if not exists note(Id char(100) primary key, content varchar(100) ) ");

        mDatabase = FirebaseDatabase.getInstance().getReference().child(username);
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_note, parent , false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Note note = notes.get(position);

        holder.content.setText(note.getContent());

        holder.checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if(isChecked) {
                holder.content.setPaintFlags(holder.content.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.content.setTypeface(null, Typeface.NORMAL);
            }
            else{
                holder.content.setPaintFlags(holder.content.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                holder.content.setTypeface(null, Typeface.BOLD);
            }
        });

        holder.itemView.setOnLongClickListener(view -> {
            PopupMenu popup = new PopupMenu(context , holder.itemView);
            popup.inflate(R.menu.menu_note_item);
            popup.setOnMenuItemClickListener(menuItem -> {
                switch(menuItem.getItemId()){
                    case R.id.btn_edit_note:
                        editNote(note, position);
                        break;
                    case R.id.btn_delete_item:
                        ConfirmDeleteDialog(note, position);
                        break;
                    default:
                        return true;
                }
                return false;
            });
            popup.show();
            return false;
        });

        holder.itemView.setOnClickListener(view -> editNote(note, position));
    }


    private void editNote(Note note , int position) {
        final Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCancelable(false);

        dialog.setContentView(R.layout.dialog_edit_item);

        TextInputEditText content = (TextInputEditText) dialog.findViewById(R.id.text_layout_content_update);
        content.setText(note.getContent());

        Button btn_edit = (Button) dialog.findViewById(R.id.btn_content_update);
        Button btn_cancel = (Button) dialog.findViewById(R.id.btn_cancel_update);

        btn_edit.setOnClickListener(v -> {

            String text = content.getText().toString();
            note.setContent(text);

            //update local
            database.QueryData("update note set content = '" + text + "' where Id = '"  + note.getId() + "'");

            //update firebase
            HashMap hashMap = new HashMap();
            hashMap.put("content", text);
            mDatabase.child(note.getId()).updateChildren(hashMap).addOnCompleteListener(task -> {
                if(task.isComplete()) {
                    Toast.makeText(context, "Update success", Toast.LENGTH_SHORT).show();
                }
                else
                    Toast.makeText(context, "Some thing wrong", Toast.LENGTH_SHORT).show();
            });

            notifyItemChanged(position);
            dialog.dismiss();
        });

        btn_cancel.setOnClickListener(v -> {
            dialog.dismiss();
        });

        final Window window = dialog.getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);

        dialog.show();
    }

    private void ConfirmDeleteDialog(Note note, int position){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete")
                .setMessage("Are you sure you want to delete " + note.getContent() + " ?")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    deleteNote( note, position);
                })
                .create();
        builder.show();
    }

    private void deleteNote(Note note, int position) {
        notes.remove(note);

        //delete local
        database.QueryData("delete from note where Id = '" + note.getId() + "'" );
        Toast.makeText(context, "Remove success", Toast.LENGTH_SHORT).show();

        //delete firebase
        mDatabase.child(note.getId()).removeValue().addOnCompleteListener(task -> {
            if(task.isComplete()) {
                Toast.makeText(context, "Remove success", Toast.LENGTH_SHORT).show();
                Log.e("TAG", "deleteNote: " +  mDatabase.child(note.getId()) );
            }
            else
                Toast.makeText(context, "Something wrong", Toast.LENGTH_SHORT).show();
        });


        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        TextView content;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            content = itemView.findViewById(R.id.content_note);
            checkBox = itemView.findViewById(R.id.checkbox_note);

        }
    }

}