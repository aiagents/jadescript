package it.unipr.ailab.jadescript.semantics.helpers;

import it.unipr.ailab.jadescript.jadescript.CodeBlock;
import it.unipr.ailab.jadescript.jadescript.RValueExpression;
import it.unipr.ailab.jadescript.jadescript.TypeExpression;
import it.unipr.ailab.jadescript.jvmmodel.JadescriptCompilerUtils;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.block.BlockSemantics;
import it.unipr.ailab.jadescript.semantics.context.symbol.UserVariable;
import it.unipr.ailab.jadescript.semantics.expression.RValueExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.expression.TypeExpressionSemantics;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.EmptyCreatable;
import it.unipr.ailab.jadescript.semantics.jadescripttypes.IJadescriptType;
import it.unipr.ailab.maybe.Maybe;
import it.unipr.ailab.sonneteer.SourceCodeBuilder;
import it.unipr.ailab.sonneteer.expression.ExpressionWriter;
import it.unipr.ailab.sonneteer.statement.BlockWriter;
import it.unipr.ailab.sonneteer.statement.LocalVarBindingProvider;
import it.unipr.ailab.sonneteer.statement.StatementWriter;
import it.unipr.ailab.sonneteer.statement.VariableDeclarationWriter;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtend2.lib.StringConcatenationClient;
import org.eclipse.xtext.common.types.JvmExecutable;
import org.eclipse.xtext.common.types.JvmField;
import org.eclipse.xtext.common.types.JvmTypeReference;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.eclipse.xtext.xbase.typesystem.references.LightweightTypeReference;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static it.unipr.ailab.maybe.Maybe.of;

public class CompilationHelper implements IQualifiedNameProvider {

    private final SemanticsModule module;

    public CompilationHelper(SemanticsModule module) {
        this.module = module;
    }

    private final static LateVarBindingContext bindingContext = new LateVarBindingContext();

    public static final LocalVarBindingProvider userBlockLocalVars = new LocalVarBindingProvider() {
        @Override
        public VariableDeclarationWriter bindDeclaration(
                String chosenType,
                String varName,
                ExpressionWriter nullableInitExpression
        ) {
            final Optional<UserVariable> variable = bindingContext.findVariable(varName);
            if (variable.isPresent()) {
                return variable.get().bindDeclaration(nullableInitExpression);
            } else {
                return DEFAULT_VAR_BINDING_PROVIDER.bindDeclaration(
                        chosenType,
                        varName,
                        nullableInitExpression
                );
            }
        }

        @Override
        public StatementWriter bindWrite(String varName, ExpressionWriter expression) {
            final Optional<UserVariable> variable = bindingContext.findVariable(varName);
            if (variable.isPresent()) {
                if (variable.get().isCapturedInAClosure()) {
                    return variable.get().bindWriteInClosure(expression);
                } else {
                    return variable.get().bindWrite(expression);
                }
            } else {
                return DEFAULT_VAR_BINDING_PROVIDER.bindWrite(varName, expression);
            }
        }

        @Override
        public String bindRead(String varName) {
            final Optional<UserVariable> variable = bindingContext.findVariable(varName);
            if (variable.isPresent()) {
                if (variable.get().isCapturedInAClosure()) {
                    return variable.get().bindReadInClosure();
                } else {
                    return variable.get().bindRead();
                }
            } else {
                return DEFAULT_VAR_BINDING_PROVIDER.bindRead(varName);
            }
        }


    };

    @NotNull
    public static String extractOntologyVarName(JvmTypeReference jvmTypeReference) {
        return SemanticsConsts.ONTOLOGY_VAR_NAME + "__" +
                jvmTypeReference.getQualifiedName('.').replaceAll("\\.", "_");
    }

    public static String extractOntologyVarName(IJadescriptType usedOntologyType) {
        return extractOntologyVarName(usedOntologyType.asJvmTypeReference());
    }


    public void createAndSetBody(
            JvmExecutable container,
            Consumer<SourceCodeBuilder> initializer
    ) {
        module.get(JvmTypesBuilder.class).setBody(container, new StringConcatenationClient() {
            @Override
            protected void appendTo(TargetStringConcatenation target) {
                SourceCodeBuilder sourceCodeBuilder = new SourceCodeBuilder("");
                initializer.accept(sourceCodeBuilder);
                target.append(sourceCodeBuilder);
                target.newLineIfNotEmpty();
            }
        });
    }

    public void createAndSetInitializer(
            JvmField field,
            Consumer<SourceCodeBuilder> init
    ) {
        module.get(JvmTypesBuilder.class).setInitializer(field, new StringConcatenationClient() {
            @Override
            protected void appendTo(TargetStringConcatenation target) {
                SourceCodeBuilder sourceCodeBuilder = new SourceCodeBuilder("");
                init.accept(sourceCodeBuilder);
                target.append(sourceCodeBuilder);
            }
        });
    }

    public List<String> adaptAndCompileRValueList(
            List<String> compiledArgs,
            List<IJadescriptType> argTypes,
            List<IJadescriptType> destinationTypes
    ) {
        List<String> result = new ArrayList<>();
        final TypeHelper typeHelper = module.get(TypeHelper.class);
        for (int i = 0; i < Math.min(compiledArgs.size(), Math.min(argTypes.size(), destinationTypes.size())); i++) {
            result.add(
                    typeHelper.compileWithEventualImplicitConversions(
                            compiledArgs.get(i),
                            argTypes.get(i),
                            destinationTypes.get(i)
                    )
            );
        }
        return result;
    }

    public List<String> adaptAndCompileRValueList(
            List<? extends RValueExpression> rvals,
            List<IJadescriptType> destinationTypes
    ) {
        List<String> compiledArgs = rvals.stream()
                .map(Maybe::of)
                .map(x -> x.__(xx -> (RValueExpression) xx))
                .map(x -> module.get(RValueExpressionSemantics.class).compile(x))
                .map(x -> x.orElse(""))
                .collect(Collectors.toList());
        List<IJadescriptType> argTypes = rvals.stream()
                .map(Maybe::of)
                .map(x -> x.__(xx -> (RValueExpression) xx))
                .map(x -> module.get(RValueExpressionSemantics.class).inferType(x))
                .collect(Collectors.toList());
        return adaptAndCompileRValueList(compiledArgs, argTypes, destinationTypes);
    }

    public String compileRValueList(
            List<? extends RValueExpression> rvals,
            List<IJadescriptType> destinationTypes
    ) {
        if (rvals == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rvals.size(); i++) {
            Maybe<RValueExpression> expr = of(rvals.get(i));
            if (destinationTypes != null) {
                IJadescriptType destType = destinationTypes.get(i);
                IJadescriptType argType = module.get(RValueExpressionSemantics.class).inferType(expr);
                if (module.get(TypeHelper.class).implicitConversionCanOccur(argType, destType)) {
                    sb.append(module.get(TypeHelper.class).compileImplicitConversion(
                            module.get(RValueExpressionSemantics.class).compile(expr).orElse(""),
                            argType,
                            destType
                    ));
                } else {
                    sb.append(module.get(RValueExpressionSemantics.class).compile(expr).orElse(""));
                }
            } else {
                sb.append(module.get(RValueExpressionSemantics.class).compile(expr).orElse(""));
            }
            if (i != rvals.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public String compileRValueList(List<RValueExpression> rvals) {
        return compileRValueList(rvals, null);
    }


    public LateVarBindingContext lateBindingContext() {
        return bindingContext;
    }

    public SourceCodeBuilder compileBlockToNewSCB(Maybe<CodeBlock> cb) {
        SourceCodeBuilder ssb = new SourceCodeBuilder("");
        if (cb != null) {
            final BlockWriter compiledBlock = module.get(BlockSemantics.class).compile(cb);
            compiledBlock.setBindingProvider(userBlockLocalVars);
            compiledBlock.writeSonnet(ssb);
        }
        bindingContext.clear();
        return ssb;
    }

    public String compileEmptyConstructorCall(
            Maybe<TypeExpression> typeExpr
    ) {
        return compileEmptyConstructorCall(
                module.get(TypeExpressionSemantics.class).toJadescriptType(typeExpr)
        );
    }

    public static String compileEmptyConstructorCall(
            final IJadescriptType jadescriptType
    ) {
        if (jadescriptType instanceof EmptyCreatable) {
            return ((EmptyCreatable) jadescriptType).compileNewEmptyInstance();
        } else {
            return "null";
        }
    }

    public LightweightTypeReference toLightweightTypeReference(IJadescriptType type, EObject container) {
        return module.get(JadescriptCompilerUtils.class)
                .toLightweightTypeReference(type.asJvmTypeReference(), container);
    }


    @Override
    public QualifiedName getFullyQualifiedName(EObject eObject) {
        if (eObject == null)
            //noinspection ReturnOfNull
            return null;
        return module.get(IQualifiedNameProvider.class).getFullyQualifiedName(eObject);
    }

    @SuppressWarnings("unused")
    public QualifiedName apply(EObject arg0) {
        return module.get(IQualifiedNameProvider.class).getFullyQualifiedName(arg0);
    }

    public static class LateVarBindingContext {
        private final Map<String, UserVariable> map = new HashMap<>();


        public synchronized Optional<UserVariable> findVariable(String varCode) {
            return Optional.ofNullable(map.get(varCode));
        }

        public synchronized void pushVariable(UserVariable variable) {
            map.put("V" + Integer.toHexString(variable.hashCode()), variable);
        }

        public synchronized void clear() {
            map.clear();
        }

        public synchronized void forEach(BiConsumer<String, UserVariable> action) {
            map.forEach(action);
        }
    }
}
