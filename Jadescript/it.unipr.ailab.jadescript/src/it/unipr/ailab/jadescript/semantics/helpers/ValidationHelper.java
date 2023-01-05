package it.unipr.ailab.jadescript.semantics.helpers;

import com.google.common.collect.HashMultimap;
import it.unipr.ailab.jadescript.jadescript.FormalParameter;
import it.unipr.ailab.jadescript.jadescript.JadescriptPackage;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.semantics.InterceptAcceptor;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.context.MightUseAgentReference;
import it.unipr.ailab.jadescript.semantics.context.clashing.CallableClashValidator;
import it.unipr.ailab.jadescript.semantics.context.clashing.DefinitionClash;
import it.unipr.ailab.jadescript.semantics.context.clashing.NameClashValidator;
import it.unipr.ailab.jadescript.semantics.context.search.SearchLocation;
import it.unipr.ailab.jadescript.semantics.context.symbol.CallableSymbol;
import it.unipr.ailab.jadescript.semantics.context.symbol.Property;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.ListType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.MapType;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.SetType;
import it.unipr.ailab.jadescript.semantics.utils.Util;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.WriterFactory;
import jadescript.lang.Performative;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.common.types.JvmVisibility;
import org.eclipse.xtext.util.Strings;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.unipr.ailab.maybe.Maybe.iterate;
import static it.unipr.ailab.maybe.Maybe.safeDo;

/**
 * Created on 21/02/2019.
 */
public class ValidationHelper implements SemanticsConsts {

    //TODO ensure to always use Utile.extractEObject

    protected static final WriterFactory w = WriterFactory.getInstance();

    private final SemanticsModule module;

    public ValidationHelper(SemanticsModule module) {
        this.module = module;
    }


    public static boolean isReservedName(Maybe<String> name) {
        String toBeChecked = name.orElse("");
        switch (toBeChecked) {
            // jadescriptsupport internal java type names
            case "Agent":
            case "SimpleBehaviour":
            case "CyclicBehaviour":
            case "Ontology":


                // java keywords
            case "abstract":
            case "continue":
            case "for":
            case "new":
            case "switch":
            case "assert":
            case "default":
            case "goto":
            case "package":
            case "synchronized":
            case "boolean":
            case "do":
            case "if":
            case "private":
            case "this":
            case "break":
            case "double":
            case "implements":
            case "protected":
            case "throw":
            case "byte":
            case "else":
            case "import":
            case "public":
            case "throws":
            case "case":
            case "enum":
            case "instanceof":
            case "return":
            case "transient":
            case "catch":
            case "extends":
            case "int":
            case "short":
            case "try":
            case "char":
            case "final":
            case "interface":
            case "static":
            case "void":
            case "class":
            case "finally":
            case "long":
            case "strictfp":
            case "volatile":
            case "const":
            case "float":
            case "native":
            case "super":
            case "while":
                return true;
            default: {
                return toBeChecked.startsWith("_");
            }

        }
    }


    public boolean isAccessible(
            JvmTypeReference containingType,
            JvmVisibility visibilityOfMember,
            JvmTypeReference contextOfAccess
    ) {
        switch (visibilityOfMember) {
            case DEFAULT: //assuming this is package private:
                //return true only if contextOfAccess is in the same package of containingType
                return TypeHelper.extractPackageName(containingType).equals(TypeHelper.extractPackageName(contextOfAccess));
            case PRIVATE:
                //return true only if containingType == contextOfAccess
                return TypeHelper.typeReferenceEquals(containingType, contextOfAccess);
            case PROTECTED:
                //return true only if contextOfAccess isAssignable to containingType
                return module.get(TypeHelper.class).isAssignable(containingType, contextOfAccess);
            case PUBLIC:
                //always return true
                return true;
        }
        return false;
    }


    public void validateFormalParameter(Maybe<FormalParameter> formalParameter, ValidationMessageAcceptor acceptor) {
        assertNotReservedName(formalParameter.__(FormalParameter::getName), formalParameter,
                JadescriptPackage.eINSTANCE.getFormalParameter_Name(), acceptor
        );
    }


    /**
     * Validator-assertion that checks that the type referred by {@code x} is referrable (i.e., a type reference
     * to this type is allowed to be created in Jadescript user code).
     *
     * @param input    {@link EObject} used to link the eventual error marker to the portion of code
     * @param message  custom message for the eventual error marker
     * @param x        type reference
     * @param acceptor acceptor for the eventual error marker
     */
    public boolean assertTypeReferable(
            Maybe<? extends EObject> input,
            String message,
            IJadescriptType x,
            ValidationMessageAcceptor acceptor
    ) {
        return assertion(x.isReferrable(), "InvalidType", message, input, acceptor);
    }

    public boolean assertPropertiesOfTypeAccessible(
            Maybe<? extends EObject> input,
            String message,
            IJadescriptType x,
            ValidationMessageAcceptor acceptor
    ) {

        if (x instanceof MapType || x instanceof ListType) {
            return VALID;
        }
        return assertion(x.haveProperties(), "InvalidType", message, input, acceptor);
    }

    public void validateDuplicateParameters(
            ValidationMessageAcceptor acceptor,
            Maybe<EList<FormalParameter>> parameters
    ) {
        // checks duplicate formal parameters
        HashMultimap<String, FormalParameter> multiMap = HashMultimap.create();
        for (Maybe<FormalParameter> parameter : iterate(parameters)) {
            safeDo(parameter, parameter.__(FormalParameter::getName),
                    /* NULLSAFE REGION */(parameterSafe, nameSafe) -> {
                        // this portion of code is done only if parameter and name
                        // are != null (and everything in the dotchains that generated them is !=null
                        // too)

                        multiMap.put(nameSafe, parameterSafe);

                    }/* END NULLSAFE REGION - (parameterSafe, nameSafe) */
            );
        }

        for (Map.Entry<String, Collection<FormalParameter>> entry : multiMap.asMap().entrySet()) {
            Collection<FormalParameter> duplicates = entry.getValue();

            if (duplicates.size() > 1) {
                for (FormalParameter d : duplicates) {
                    acceptor.acceptError("Duplicate formal parameter '" + entry.getKey() + "'", d, null,
                            ValidationMessageAcceptor.INSIGNIFICANT_INDEX, ISSUE_DUPLICATE_ELEMENT
                    );
                }
            }
        }
    }

    public void validateIndexType(
            IJadescriptType collectionType,
            Maybe<RValueExpression> indexExpression,
            InterceptAcceptor subValidations
    ) {
        module.get(RValueExpressionSemantics.class).validate(indexExpression, , subValidations);

        assertion(
                !(collectionType instanceof SetType),
                "InvalidKeyType",
                "Unexpected key/index specification in sets.",
                indexExpression,
                subValidations
        );

        if (!subValidations.thereAreErrors()) {

            if (collectionType instanceof MapType) {
                assertExpectedType(
                        ((MapType) collectionType).getKeyType(),
                        module.get(RValueExpressionSemantics.class).inferType(indexExpression, ),
                        "InvalidKeyType",
                        indexExpression,
                        subValidations
                );
            } else {
                assertExpectedType(
                        Integer.class,
                        module.get(RValueExpressionSemantics.class).inferType(indexExpression, ),
                        "InvalidIndexType",
                        indexExpression,
                        subValidations
                );
            }
        }
    }

    public void validateMethodCompatibility(
            CallableSymbol toBeAdded,
            Maybe<? extends EObject> refEObject,
            ValidationMessageAcceptor acceptor
    ) {
        final List<DefinitionClash> clashes = module.get(ContextManager.class).currentContext()
                .actAs(CallableClashValidator.class)
                .flatMap(ccv -> ccv.checkCallableClash(module, toBeAdded))
                .filter(dc -> !dc.getAlreadyPresentSymbol().sourceLocation()
                        .equals(dc.getToBeAddedSymbol().sourceLocation())
                ).filter(Util.dinstinctBy(dc -> Util.tuple(
                        dc.getAlreadyPresentSymbol().getSignature(),
                        dc.getAlreadyPresentSymbol().sourceLocation()
                )))
                .collect(Collectors.toList());
        assertion(
                clashes.isEmpty(),
                "ClashingDeclaration",
                "Cannot declare operation '" + toBeAdded.name() + "', clashes found.\n" +
                        DefinitionClash.clashListToString(clashes),
                refEObject,
                acceptor
        );
    }

    public void validateFieldCompatibility(
            Maybe<String> fieldName,
            IJadescriptType fieldType,
            Maybe<? extends EObject> refEObject,
            SearchLocation currentLocation,
            ValidationMessageAcceptor acceptor
    ) {
        fieldName.safeDo(fieldNameSafe -> {
            final List<DefinitionClash> clashes = module.get(ContextManager.class).currentContext()
                    .actAs(NameClashValidator.class)
                    .flatMap(ncv -> ncv.checkNameClash(
                            fieldNameSafe,
                            new Property(fieldNameSafe, fieldType, false,
                                    currentLocation
                            )
                    ))
                    .filter(dc -> !dc.getAlreadyPresentSymbol().sourceLocation()
                            .equals(dc.getToBeAddedSymbol().sourceLocation())
                    ).filter(Util.dinstinctBy(dc -> Util.tuple(
                            dc.getAlreadyPresentSymbol().getSignature(),
                            dc.getAlreadyPresentSymbol().sourceLocation()
                    )))
                    .collect(Collectors.toList());
            assertion(
                    clashes.isEmpty(),
                    "ClashingDeclaration",
                    "Cannot declare property with name '" + fieldNameSafe + "', clashes found.\n" +
                            DefinitionClash.clashListToString(clashes),
                    refEObject,
                    acceptor
            );
        });
    }

    public boolean assertSupportedPerformative(
            Maybe<String> performative,
            Maybe<? extends EObject> eobject,
            ValidationMessageAcceptor acceptor
    ) {
        if (performative.isPresent()) {
            String perf = performative.toNullable();
            return assertion(
                    Stream.of(
                                    Performative.INFORM_REF,
                                    Performative.PROPAGATE,
                                    Performative.QUERY_REF,
                                    Performative.PROXY,
                                    Performative.SUBSCRIBE
                            ).map(Performative.nameByPerformative::get)
                            .noneMatch(perf::equals),
                    "UnsupportedPerformative",
                    "Performative '" + perf + "' is currently unsupported in Jadescript.",
                    eobject,
                    acceptor
            );
        }
        return VALID;
    }

    public boolean assertExpectedType(
            IJadescriptType expected,
            IJadescriptType actual,
            String issueCode,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        expected = expected.postResolve();
        return assertion(
                expected.isAssignableFrom(actual),
                issueCode,
                "Invalid type; found: '" + actual.getJadescriptName() +
                        "'; expected: '" + expected.getJadescriptName() + "' or subtype.",
                object,
                acceptor
        );
    }


    private String multiInvalidTypeMessage(
            List<IJadescriptType> expected,
            IJadescriptType actual
    ) {
        StringBuilder sb = new StringBuilder("Invalid type; found: '");
        sb.append(actual.getJadescriptName()).append("'; expected: ");
        for (int i = 0; i < expected.size(); i++) {
            final IJadescriptType e = expected.get(i);
            if (i != 0) {
                sb.append(", ");
            }
            sb.append("'").append(e.getJadescriptName()).append("'");
        }
        sb.append(", or subtypes.");
        return sb.toString();
    }

    public boolean assertExpectedTypes(
            List<IJadescriptType> expected,
            IJadescriptType actual,
            String issueCode,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        if (expected == null || expected.isEmpty()) {
            return VALID;
        }
        boolean b = false;
        for (IJadescriptType e : expected) {
            e = e.postResolve();
            b = b || e.isAssignableFrom(actual);
        }
        return assertion(
                b,
                issueCode,
                multiInvalidTypeMessage(expected, actual),
                object,
                acceptor
        );
    }


    public boolean assertExpectedType(
            IJadescriptType expected,
            IJadescriptType actual,
            String issueCode,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            ValidationMessageAcceptor acceptor
    ) {
        expected = expected.postResolve();
        return assertion(
                expected.isAssignableFrom(actual),
                issueCode,
                "invalid type; found: '" + actual.getJadescriptName() +
                        "'; expected: '" + expected.getJadescriptName() + "' or subtype",
                object,
                feature,
                acceptor
        );

    }

    public boolean assertExpectedType(
            IJadescriptType expected,
            IJadescriptType actual,
            String issueCode,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ) {
        expected = expected.postResolve();
        return assertion(
                expected.isAssignableFrom(actual),
                issueCode,
                "invalid type; found: '" + actual.getJadescriptName() +
                        "'; expected: '" + expected.getJadescriptName() + "' or subtype",
                object,
                feature,
                index,
                acceptor
        );
    }

    public boolean assertExpectedType(
            Class<?> expected,
            IJadescriptType actual,
            String issueCode,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        Objects.requireNonNull(expected);
        final IJadescriptType expectedDescriptor = module.get(TypeHelper.class).jtFromClass(expected);
        return assertion(
                expectedDescriptor.isAssignableFrom(actual),
                issueCode,
                "invalid type; found: '" + actual.getJadescriptName() +
                        "'; expected: '" + expectedDescriptor.getJadescriptName() + "' or subtype",
                object,
                acceptor
        );
    }


    public boolean assertExpectedType(
            Class<?> expected,
            IJadescriptType actual,
            String issueCode,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            ValidationMessageAcceptor acceptor
    ) {
        Objects.requireNonNull(expected);
        final IJadescriptType expectedDescriptor = module.get(TypeHelper.class).jtFromClass(expected);
        return assertion(
                expectedDescriptor.isAssignableFrom(actual),
                issueCode,
                "invalid type; found: '" + actual.getJadescriptName() +
                        "'; expected: '" + expectedDescriptor.getJadescriptName() + "' or subtype",
                object,
                feature,
                acceptor
        );
    }

    public boolean assertExpectedType(
            Class<?> expected,
            IJadescriptType actual,
            String issueCode,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ) {
        Objects.requireNonNull(expected);
        final IJadescriptType expectedDescriptor = module.get(TypeHelper.class).jtFromClass(expected);
        return assertion(
                expectedDescriptor.isAssignableFrom(actual),
                issueCode,
                "invalid type; found: '" + actual.getJadescriptName() +
                        "'; expected: '" + expectedDescriptor.getJadescriptName() + "' or subtype",
                object,
                feature,
                index,
                acceptor
        );
    }


    public void assertValueExpected(
            Maybe<?> value,
            String name,
            Maybe<? extends EObject> expressionContainer,
            ValidationMessageAcceptor acceptor
    ) {
        assertion(
                value != null && value.isPresent(),
                "Missing" + Strings.toFirstUpper(name) + "Value",
                "Missing mandatory expression for " + name,
                expressionContainer,
                acceptor
        );
    }

    public boolean assertNotReservedName(
            Maybe<String> name,
            Maybe<? extends EObject> input,
            EStructuralFeature feature,
            ValidationMessageAcceptor acceptor
    ) {
        return assertNotReservedName(
                name,
                input,
                feature,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public boolean assertNotReservedName(
            Maybe<String> name,
            Maybe<? extends EObject> input,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ) {
        if (name != null && name.isPresent()) {
            return assertion(!isReservedName(name), "ReservedName",
                    name + " is a reserved Jadescript name. Please use another one.",
                    input,
                    feature,
                    index,
                    acceptor
            );
        }
        return VALID;
    }

    public boolean emitError(
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        return emitError(issueCode, description, object, null, ValidationMessageAcceptor.INSIGNIFICANT_INDEX, acceptor);
    }

    public boolean emitError(
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ){
        return assertion(false, issueCode, description, object, feature, index, acceptor);
    }

    public void emitInfo(
        String issueCode,
        String description,
        Maybe<? extends EObject> object,
        ValidationMessageAcceptor acceptor
    ){
        final Maybe<? extends EObject> eobject = Util.extractEObject(object);
        if(eobject.isNothing()){
            return;
        }
        final EObject eObjectSafe = eobject.toNullable();
        acceptor.acceptInfo(
            description,
            eObjectSafe,
            null,
            ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
            issueCode
        );
    }

    public boolean assertion(
            boolean isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ) {

        if (!isTrue) {

            Util.extractEObject(object).safeDo(value -> {
                acceptor.acceptError(
                        description,
                        value,
                        feature,
                        index,
                        ISSUE_CODE_PREFIX + issueCode
                );
            });
            return INVALID;
        }

        return VALID;
    }

    public boolean assertion(
            boolean isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            ValidationMessageAcceptor acceptor
    ) {
        return assertion(
                isTrue,
                issueCode,
                description,
                object,
                feature,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public boolean assertion(
            boolean isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        return assertion(
                isTrue,
                issueCode,
                description,
                object,
                null,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public void assertionAndThen(
            boolean isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor,
            Runnable andThen
    ) {
        InterceptAcceptor ia = new InterceptAcceptor(acceptor);
        assertion(isTrue, issueCode, description, object, ia);
        if (!ia.thereAreErrors()) {
            andThen.run();
        }
    }

    public boolean assertion(
            Maybe<Boolean> isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ) {
        Optional<? extends EObject> eObject = object.toOpt();

        if (!isTrue.orElse(true) && eObject.isPresent()) {
            acceptor.acceptError(description, eObject.get(), feature, index, ISSUE_CODE_PREFIX + issueCode);
            return INVALID;
        } else {
            return VALID;
        }
    }

    public void assertion(
            Maybe<Boolean> isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            ValidationMessageAcceptor acceptor
    ) {
        assertion(
                isTrue,
                issueCode,
                description,
                object,
                feature,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public boolean assertion(
            Maybe<Boolean> isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        return assertion(
                isTrue,
                issueCode,
                description,
                object,
                null,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public void advice(
            boolean isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ) {
        Optional<? extends EObject> eObject = object.toOpt();

        if (!isTrue && eObject.isPresent()) {
            acceptor.acceptWarning(description, eObject.get(), feature, index, issueCode);
        }
    }

    public void advice(
            boolean isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            ValidationMessageAcceptor acceptor
    ) {
        advice(
                isTrue,
                issueCode,
                description,
                object,
                feature,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public void advice(
            boolean isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        advice(
                isTrue,
                issueCode,
                description,
                object,
                null,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public void advice(
            Maybe<Boolean> isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            int index,
            ValidationMessageAcceptor acceptor
    ) {
        Optional<? extends EObject> eObject = object.toOpt();

        if (!isTrue.orElse(true) && eObject.isPresent()) {
            acceptor.acceptWarning(description, eObject.get(), feature, index, issueCode);
        }
    }

    public void advice(
            Maybe<Boolean> isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            EStructuralFeature feature,
            ValidationMessageAcceptor acceptor
    ) {
        advice(
                isTrue,
                issueCode,
                description,
                object,
                feature,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }

    public void advice(
            Maybe<Boolean> isTrue,
            String issueCode,
            String description,
            Maybe<? extends EObject> object,
            ValidationMessageAcceptor acceptor
    ) {
        advice(
                isTrue,
                issueCode,
                description,
                object,
                null,
                ValidationMessageAcceptor.INSIGNIFICANT_INDEX,
                acceptor
        );
    }


    public boolean assertCanUseAgentReference(Maybe<? extends EObject> obj, ValidationMessageAcceptor acceptor) {
        return assertion(
                MightUseAgentReference.canUseAgentReference(module.get(ContextManager.class).currentContext()),
                "AgentNotAccessible",
                "Agent not accessible from this context.",
                obj,
                acceptor
        );
    }

}
