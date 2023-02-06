package it.unipr.ailab.jadescript.semantics;

import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import it.unipr.ailab.sonneteer.statement.BlockWriterElement;
import org.eclipse.emf.ecore.EObject;

public interface BlockElementAcceptor {

    WriterFactory w = WriterFactory.getInstance();

    void accept(BlockWriterElement element);


    /**
     * Creates a preliminary statement that declares a temporary variable.
     * Used to not evaluate expressions multiple times.
     * Please note that the name is uniquely associated to each (codeName,
     * eObject) pair.
     *
     * @param eObject            the eObject that is causing the creation of
     *                           the variable (used to generate a unique name).
     * @param type               type of the variable
     * @param codeName           name that will be included in the final
     *                           variable name
     * @param compiledExpression the expression used to initialize the variable
     * @return the final variable name
     */
    default String auxiliaryVariable(
        Maybe<? extends EObject> eObject,
        String type,
        String codeName,
        String compiledExpression
    ) {
        final Maybe<Integer> hash =
            Util.extractEObject(eObject).__(EObject::hashCode);

        final String finalName = "__auxvar_" + codeName + "_" + hash.orElse(0);
        accept(w.variable(
            type,
            finalName,
            w.expr(compiledExpression)
        ));
        return finalName;
    }

}
