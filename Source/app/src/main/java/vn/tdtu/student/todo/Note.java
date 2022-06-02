package vn.tdtu.student.todo;

public class Note {
    public String id;
    public String content;

    public Note() {}

    public Note(String id , String content) {
        this.content = content;
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }


}
