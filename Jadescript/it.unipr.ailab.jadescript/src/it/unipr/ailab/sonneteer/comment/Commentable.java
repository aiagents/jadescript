package it.unipr.ailab.sonneteer.comment;

import java.util.ArrayList;
import java.util.List;

public class Commentable {

    protected final List<CommentWriter> comments = new ArrayList<>();

    public void addComment(CommentWriter comment){
        comments.add(comment);
    }

    public void addComment(String comment){
        String[] lines = comment.split("\n");
        if(lines.length > 1) {
            MultilineCommentWriter m = new MultilineCommentWriter(lines[0]);
            for(int i = 1; i < lines.length; ++i){
                m.addLine(lines[i]);
            }
            addComment(m);
        }else{
            addComment(new SingleLineCommentWriter(lines[0]));
        }
    }

    public List<CommentWriter> getComments(){
        return comments;
    }

}
