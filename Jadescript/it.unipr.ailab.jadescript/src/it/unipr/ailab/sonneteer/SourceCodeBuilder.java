package it.unipr.ailab.sonneteer;

public class SourceCodeBuilder {
    private final StringBuilder sb;
    private int indentationLevel = 0;
    private boolean freshLine = true;

    public SourceCodeBuilder(){
        this("");
    }

    public SourceCodeBuilder(String s){
        if(!s.isEmpty()) freshLine = false;
        this.sb = new StringBuilder(s);
    }

    public SourceCodeBuilder line(String s){
        if(freshLine) appendTabs();
        sb.append(s).append("\n");
        freshLine = true;
        return this;
    }

    public SourceCodeBuilder line(){
        if(freshLine) appendTabs();
        sb.append("\n");
        freshLine = true;
        return this;
    }

    public SourceCodeBuilder spaced(String s){
        if(freshLine) appendTabs();
        sb.append(s).append(" ");
        freshLine = false;
        return this;
    }

    public SourceCodeBuilder add(String s){
        if(freshLine) appendTabs();
        sb.append(s);
        freshLine = false;
        return this;
    }

    public SourceCodeBuilder add(SourceCodeBuilder scb){
        String[] lines = scb.toString().split("\n");
        for (String line : lines) {
            line(line);
        }
        return this;
    }

    public SourceCodeBuilder add(Writer w){
        w.writeSonnet(this);
        return this;
    }

    public SourceCodeBuilder indent(){
        indentationLevel++;
        return this;
    }

    public SourceCodeBuilder open(String opener){
        return this.line(opener).indent();
    }

    public SourceCodeBuilder dedent(){
        indentationLevel--;
        return this;
    }

    public SourceCodeBuilder close(String closer){
        return this.dedent().line(closer);
    }

    public SourceCodeBuilder closeAndOpen(String dedentedLine){
        return this.dedent().line(dedentedLine).indent();
    }

    public String toString(){
        return sb.toString();
    }

    private void appendTabs() {
        sb.append("\t".repeat(Math.max(0, indentationLevel)));
    }
}
