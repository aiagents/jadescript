package it.unipr.ailab.sonneteer;

import it.unipr.ailab.sonneteer.classmember.MethodWriter;
import it.unipr.ailab.sonneteer.comment.DocumentationCommentWriter;
import it.unipr.ailab.sonneteer.expression.SimpleExpressionWriter;
import it.unipr.ailab.sonneteer.qualifiers.Visibility;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.MethodCallStatementWriter;
import it.unipr.ailab.sonneteer.type.ClassDeclarationWriter;


public class TestMain {
    public static void main(String[] argv) {
        MethodWriter testMethod = new MethodWriter(Visibility.PUBLIC, false, false,
                "void", "testMethod"
        );
        testMethod.setBody(new BlockWriter().addStatement(
                new MethodCallStatementWriter("System.out.println", new SimpleExpressionWriter("\"Hello!\"")))
        );

        it.unipr.ailab.sonneteer.file.FileWriter fp = new it.unipr.ailab.sonneteer.file.FileWriter(
                "Person.java",
                "sonneteer.test",
                new ClassDeclarationWriter(Visibility.PUBLIC, false, false, "Person")
                        .addProperty("String", "name", false)
                        .addProperty("int", "age", false)
                        .addMember(testMethod)
        );


        fp.addComment(new DocumentationCommentWriter("Person.java content").addAuthor("Giuseppe Petrosino"));
        SourceCodeBuilder ssb = new SourceCodeBuilder("");
        fp.writeSonnet(ssb);
        System.out.println(ssb);
    }
}
