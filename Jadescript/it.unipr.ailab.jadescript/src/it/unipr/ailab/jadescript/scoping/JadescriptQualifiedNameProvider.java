package it.unipr.ailab.jadescript.scoping;

import it.unipr.ailab.jadescript.jadescript.Model;
import it.unipr.ailab.jadescript.jadescript.NamedFeature;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.common.types.JvmIdentifiableElement;
import org.eclipse.xtext.common.types.JvmMember;
import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.xbase.scoping.XbaseQualifiedNameProvider;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("restriction")
public class JadescriptQualifiedNameProvider
    extends XbaseQualifiedNameProvider {


    /**
     * @return the qualified name for the given object, <code>null</code> if
     * this
     * {@link IQualifiedNameProvider} is not responsible or if the given
     * object doesn't have qualified name. Please note that some objects
     * might not have a QualifiedName even when expected to do so by the
     * grammar: this might happen if the code is invalid and this is
     * invoked by the validator.
     */
    @Override
    public @Nullable QualifiedName getFullyQualifiedName(EObject obj) {
        if (obj == null) {
            return null;
        }

        if (obj instanceof NamedFeature) {
            return getFullyQualifiedNameForNamedFeature((NamedFeature) obj);
        }

        // additional subcases to prevent super.getFullyQualifiedName() to throw
        // an IllegalArgumentException when the returned qn is an empty string.
        if (obj instanceof JvmType || obj instanceof JvmMember) {
            final String qualifiedName =
                ((JvmIdentifiableElement) obj).getQualifiedName();
            if (qualifiedName == null || qualifiedName.isBlank()) {
                return null;
            }
        }


        return super.getFullyQualifiedName(obj);
    }


    public @Nullable QualifiedName getFullyQualifiedNameForNamedFeature(
        NamedFeature e
    ) {
        if (e == null || e.getName() == null) {
            return null;
        }
        var model = EcoreUtil2.getContainerOfType(e, Model.class);
        if (
            model != null
                && model.isWithModule()
                && model.getName() != null
        ) {
            var modelFQN = getFullyQualifiedName(model);

            if (modelFQN != null) {
                return modelFQN.append(e.getName());
            }
        }

        return QualifiedName.create(e.getName());

    }

}
