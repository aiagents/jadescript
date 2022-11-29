package it.unipr.ailab.jadescript.semantics.expression.patternmatch;

import it.unipr.ailab.sonneteer.classmember.ClassMemberWriter;

import java.util.ArrayList;
import java.util.List;

//TODO better internal representation
public class PatternMatcher {
    private final List<ClassMemberWriter> classMemberWriters = new ArrayList<>();

    public void add(ClassMemberWriter c){
        this.classMemberWriters.add(c);
    }

    public List<ClassMemberWriter> getClassMemberWriters() {
        return classMemberWriters;
    }
}
