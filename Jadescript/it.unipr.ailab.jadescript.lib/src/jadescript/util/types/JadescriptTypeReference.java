package jadescript.util.types;

public class JadescriptTypeReference {
    private final JadescriptBuiltinTypeAtom base;
    private final JadescriptTypeReference arg1;
    private final JadescriptTypeReference arg2;
    private final int arity;

    public JadescriptTypeReference(JadescriptBuiltinTypeAtom base, JadescriptTypeReference arg1, JadescriptTypeReference arg2) {
        this.base = base;
        this.arg1 = arg1;
        this.arg2 = arg2;
        arity = 2;
    }

    public JadescriptTypeReference(JadescriptBuiltinTypeAtom base, JadescriptTypeReference arg1) {
        this.base = base;
        this.arg1 = arg1;
        this.arg2 = null;
        arity = 1;
    }

    public JadescriptTypeReference(JadescriptBuiltinTypeAtom base) {
        this.base = base;
        this.arg1 = null;
        this.arg2 = null;
        arity = 0;
    }

    public JadescriptBuiltinTypeAtom getBase() {
        return base;
    }

    public JadescriptTypeReference getArg1() {
        return arg1;
    }

    public JadescriptTypeReference getArg2() {
        return arg2;
    }

    public int getArity() {
        return arity;
    }
}
