/*
 * generated by Xtext 2.25.0
 */
package it.unipr.ailab.jadescript.validation;

import com.google.common.collect.HashMultimap;
import com.google.inject.Inject;
import it.unipr.ailab.jadescript.jadescript.*;
import it.unipr.ailab.jadescript.jvmmodel.JadescriptCompilerUtils;
import it.unipr.ailab.jadescript.semantics.SemanticsModule;
import it.unipr.ailab.jadescript.semantics.context.ContextManager;
import it.unipr.ailab.jadescript.semantics.helpers.ValidationHelper;
import it.unipr.ailab.jadescript.semantics.topelement.AgentDeclarationSemantics;
import it.unipr.ailab.jadescript.semantics.topelement.BehaviourTopLevelDeclarationSemantics;
import it.unipr.ailab.jadescript.semantics.topelement.GlobalOperationDeclarationSemantics;
import it.unipr.ailab.jadescript.semantics.topelement.OntologyDeclarationSemantics;
import it.unipr.ailab.maybe.Maybe;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.validation.Check;
import org.eclipse.xtext.validation.CheckType;
import org.eclipse.xtext.validation.ValidationMessageAcceptor;
import org.eclipse.xtext.xbase.XExpression;
import org.eclipse.xtext.xbase.jvmmodel.JvmAnnotationReferenceBuilder;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypeReferenceBuilder;
import org.eclipse.xtext.xbase.jvmmodel.JvmTypesBuilder;
import org.eclipse.xtext.xbase.lib.Exceptions;
import org.eclipse.xtext.xtype.XImportDeclaration;
import org.eclipse.xtext.xtype.XImportSection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;


/*Suppressing "unused" because methods with @Check annotation are invoked with
    reflection API
 */
@SuppressWarnings("unused")
public class JadescriptValidator extends AbstractJadescriptValidator {

    protected static final String ISSUE_CODE_PREFIX =
        "it.unipr.ailab.jadescript.";
    public static final String DUPLICATE_ELEMENT =
        (JadescriptValidator.ISSUE_CODE_PREFIX + "DuplicateElement");
    @Inject
    private JvmTypeReferenceBuilder.Factory typeRefBuilderFactory;
    @Inject
    private JvmTypesBuilder _jvmTypesBuilder;
    @Inject
    private JadescriptCompilerUtils _jadescriptCompilerUtils;
    @Inject
    private IQualifiedNameProvider _iQualifiedNameProvider;
    @Inject
    private JvmAnnotationReferenceBuilder _jvmAnnotationReferenceBuilder;


    public SemanticsModule createSemanticsModule(final Model model) {
        final JvmTypeReferenceBuilder _typeReferenceBuilder =
            this.typeRefBuilderFactory.create(
                model.eResource().getResourceSet());
        SemanticsModule module = new SemanticsModule(
            "Validation",
            this._jvmTypesBuilder,
            _typeReferenceBuilder,
            this._jvmAnnotationReferenceBuilder,
            this._iQualifiedNameProvider, this._jadescriptCompilerUtils
        );
        String moduleName;
        boolean _isWithModule = model.isWithModule();
        if (_isWithModule) {
            moduleName = model.getName();
        } else {
            moduleName = "";
        }
        final ContextManager contextManager = module.get(ContextManager.class);

        contextManager.enterModule(
            moduleName,
            Maybe.some(model)
        );

        contextManager.enterFile(
            model.eResource().getURI().toString(),
            model.eResource().getURI().trimFileExtension().lastSegment(),
            Maybe.toListOfMaybes(Maybe.some(model.getImportSection())
                .__(ImportSection::getImportSection)
                .__(XImportSection::getImportDeclarations))
        );
        return module;
    }


    @Override
    public void checkInnerExpressions(final XExpression expr) {
    }


    private String extractName(final TopElement te) {
        this._jadescriptCompilerUtils.setAnnotationReferenceBuilder(
            this._jvmAnnotationReferenceBuilder
        );
        if (te instanceof Agent) {
            return ((Agent) te).getName();
        }
        if (te instanceof Behaviour) {
            return ((Behaviour) te).getName();
        }
        if (te instanceof Ontology) {
            return ((Ontology) te).getName();
        }
        if (te instanceof GlobalFunctionOrProcedure) {
            return ((GlobalFunctionOrProcedure) te).getName();
        }
        return null;
    }


    @Check
    public void checkUniqueTopElements(final Model model) {

        this._jadescriptCompilerUtils.setAnnotationReferenceBuilder(
            this._jvmAnnotationReferenceBuilder
        );

        final HashMultimap<String, TopElement> multiMap = HashMultimap.create();
        EList<TopElement> _elements = model.getElements();

        for (final TopElement e : _elements) {
            if ((e instanceof GlobalFunctionOrProcedure)) {
                int parCount = ((GlobalFunctionOrProcedure) e)
                    .getParameters().size();
                String funcName = ((GlobalFunctionOrProcedure) e).getName();
                multiMap.put(((funcName + "#") + parCount), e);
            } else {
                multiMap.put(this.extractName(e), e);
            }
        }

        Set<Map.Entry<String, Collection<TopElement>>> _entrySet =
            multiMap.asMap().entrySet();
        for (Map.Entry<String, Collection<TopElement>> entry : _entrySet) {
            final Collection<TopElement> duplicates = entry.getValue();
            int size = duplicates.size();
            if (size > 1) {
                for (TopElement d : duplicates) {
                    this.error(
                        "Duplicate element '" + entry.getKey() + "' in module "
                            + "'" + model.getName() + "'",
                        d,
                        JadescriptPackage.eINSTANCE.getNamedElement_Name(),
                        JadescriptValidator.DUPLICATE_ELEMENT
                    );
                }
            }
        }
    }


    protected void addImportUnusedIssues(
        final Map<String, List<XImportDeclaration>> imports
    ) {
        // Overriden to do nothing.
    }


    @Override
    protected void checkIsFromCurrentlyCheckedResource(EObject object) {
//        overriden to make sure that objects with null resources are accepted
        if (object.eResource() != null) {
            super.checkIsFromCurrentlyCheckedResource(object);
        }
    }


    @Check(CheckType.FAST)
    public void validateModelFAST(final Model model) {
        try {
            this._jadescriptCompilerUtils.setAnnotationReferenceBuilder(
                this._jvmAnnotationReferenceBuilder
            );

            Maybe<String> maybeName = Maybe.some(model.getName());
            boolean _isReservedName =
                ValidationHelper.isReservedName(maybeName);

            if (_isReservedName) {
                this.error("Invalid module name: " + maybeName.orElse("(null)"),
                    model, null, ValidationMessageAcceptor.INSIGNIFICANT_INDEX
                );
            }


            HashMultimap<String, GlobalFunctionOrProcedure> functionsMap =
                HashMultimap.create();

            EList<TopElement> _elements = model.getElements();
            for (final TopElement element : _elements) {
                if ((element instanceof GlobalFunctionOrProcedure)) {
                    functionsMap.put(
                        ((GlobalFunctionOrProcedure) element).getName(),
                        ((GlobalFunctionOrProcedure) element)
                    );
                }
            }


            for (final String k : functionsMap.keySet()) {
                {
                    SemanticsModule module = createSemanticsModule(model);
                    GlobalOperationDeclarationSemantics gms = module.get(
                        GlobalOperationDeclarationSemantics.class);
                    for (final GlobalFunctionOrProcedure v :
                        functionsMap.get(k)) {
                        gms.addMethod(Maybe.some(v));
                    }

                    gms.validateOnEdit(gms.getOriginalMethod(k), this);

                }
            }
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.FAST)
    public void validateOntologyFAST(final Ontology ontology) {
        try {
            Model model = EcoreUtil2.getContainerOfType(ontology, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(OntologyDeclarationSemantics.class).validateOnEdit(
                Maybe.some(ontology), this
            );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.FAST)
    public void validateBehaviourFAST(final Behaviour behaviour) {
        try {
            Model model = EcoreUtil2.getContainerOfType(behaviour, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(BehaviourTopLevelDeclarationSemantics.class)
                .validateOnEdit(
                    Maybe.some(behaviour),
                    this
                );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.FAST)
    public void validateAgentFAST(final Agent agent) {
        try {
            Model model = EcoreUtil2.getContainerOfType(agent, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(AgentDeclarationSemantics.class).validateOnEdit(
                Maybe.some(agent),
                this
            );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.NORMAL)
    public void validateModelNORMAL(final Model model) {
        try {
            this._jadescriptCompilerUtils.setAnnotationReferenceBuilder(
                this._jvmAnnotationReferenceBuilder
            );

            Maybe<String> maybeName = Maybe.some(model.getName());
            boolean _isReservedName =
                ValidationHelper.isReservedName(maybeName);

            if (_isReservedName) {
                this.error("Invalid module name: " + maybeName.orElse("(null)"),
                    model, null, ValidationMessageAcceptor.INSIGNIFICANT_INDEX
                );
            }


            HashMultimap<String, GlobalFunctionOrProcedure> functionsMap =
                HashMultimap.create();

            EList<TopElement> _elements = model.getElements();
            for (final TopElement element : _elements) {
                if ((element instanceof GlobalFunctionOrProcedure)) {
                    functionsMap.put(
                        ((GlobalFunctionOrProcedure) element).getName(),
                        ((GlobalFunctionOrProcedure) element)
                    );
                }
            }


            for (final String k : functionsMap.keySet()) {
                {
                    SemanticsModule module = createSemanticsModule(model);
                    GlobalOperationDeclarationSemantics gms = module.get(
                        GlobalOperationDeclarationSemantics.class);
                    for (final GlobalFunctionOrProcedure v :
                        functionsMap.get(k)) {
                        gms.addMethod(Maybe.some(v));
                    }

                    gms.validateOnSave(gms.getOriginalMethod(k), this);

                }
            }
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.NORMAL)
    public void validateOntologyNORMAL(final Ontology ontology) {
        try {
            Model model = EcoreUtil2.getContainerOfType(ontology, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(OntologyDeclarationSemantics.class).validateOnSave(
                Maybe.some(ontology), this
            );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.NORMAL)
    public void validateBehaviourNORMAL(final Behaviour behaviour) {
        try {
            Model model = EcoreUtil2.getContainerOfType(behaviour, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(BehaviourTopLevelDeclarationSemantics.class)
                .validateOnSave(
                    Maybe.some(behaviour),
                    this
                );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.NORMAL)
    public void validateAgentNORMAL(final Agent agent) {
        try {
            Model model = EcoreUtil2.getContainerOfType(agent, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(AgentDeclarationSemantics.class).validateOnSave(
                Maybe.some(agent),
                this
            );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.EXPENSIVE)
    public void validateModelEXPENSIVE(final Model model) {
        try {
            this._jadescriptCompilerUtils.setAnnotationReferenceBuilder(
                this._jvmAnnotationReferenceBuilder
            );

            Maybe<String> maybeName = Maybe.some(model.getName());
            boolean _isReservedName =
                ValidationHelper.isReservedName(maybeName);

            if (_isReservedName) {
                this.error("Invalid module name: " + maybeName.orElse("(null)"),
                    model, null, ValidationMessageAcceptor.INSIGNIFICANT_INDEX
                );
            }


            HashMultimap<String, GlobalFunctionOrProcedure> functionsMap =
                HashMultimap.create();

            EList<TopElement> _elements = model.getElements();
            for (final TopElement element : _elements) {
                if ((element instanceof GlobalFunctionOrProcedure)) {
                    functionsMap.put(
                        ((GlobalFunctionOrProcedure) element).getName(),
                        ((GlobalFunctionOrProcedure) element)
                    );
                }
            }


            for (final String k : functionsMap.keySet()) {
                {
                    SemanticsModule module = createSemanticsModule(model);
                    GlobalOperationDeclarationSemantics gms = module.get(
                        GlobalOperationDeclarationSemantics.class);
                    for (final GlobalFunctionOrProcedure v :
                        functionsMap.get(k)) {
                        gms.addMethod(Maybe.some(v));
                    }

                    gms.validateOnRequest(gms.getOriginalMethod(k), this);

                }
            }
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.EXPENSIVE)
    public void validateOntologyEXPENSIVE(final Ontology ontology) {
        try {
            Model model = EcoreUtil2.getContainerOfType(ontology, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(OntologyDeclarationSemantics.class).validateOnRequest(
                Maybe.some(ontology), this
            );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.EXPENSIVE)
    public void validateBehaviourEXPENSIVE(final Behaviour behaviour) {
        try {
            Model model = EcoreUtil2.getContainerOfType(behaviour, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(BehaviourTopLevelDeclarationSemantics.class)
                .validateOnRequest(
                    Maybe.some(behaviour),
                    this
                );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


    @Check(CheckType.EXPENSIVE)
    public void validateAgentEXPENSIVE(final Agent agent) {
        try {
            Model model = EcoreUtil2.getContainerOfType(agent, Model.class);

            SemanticsModule module = createSemanticsModule(model);

            module.get(AgentDeclarationSemantics.class).validateOnRequest(
                Maybe.some(agent),
                this
            );
        } catch (final Throwable _t) {
            if (_t instanceof RuntimeException) {
                final RuntimeException ex = (RuntimeException) _t;
                ex.printStackTrace();
            } else {
                //noinspection DataFlowIssue
                throw Exceptions.sneakyThrow(_t);
            }
        }
    }


}