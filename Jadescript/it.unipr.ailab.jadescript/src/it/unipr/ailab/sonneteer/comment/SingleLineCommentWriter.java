package it.unipr.ailab.sonneteer.comment;

import it.unipr.ailab.sonneteer.SourceCodeBuilder;

public class SingleLineCommentWriter implements CommentWriter {

    private final String comment;


    public SingleLineCommentWriter(String comment) {
        this.comment = comment;
    }


    @Override
    public void writeSonnet(SourceCodeBuilder s) {
        s.add("// ").line(comment);
    }


    public String getComment() {
        return comment;
    }

}
