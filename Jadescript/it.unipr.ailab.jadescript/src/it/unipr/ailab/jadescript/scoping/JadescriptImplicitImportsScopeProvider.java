package it.unipr.ailab.jadescript.scoping;

import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.scoping.impl.ImportNormalizer;
import org.eclipse.xtext.xbase.scoping.XImportSectionNamespaceScopeProvider;

import java.util.ArrayList;
import java.util.List;

public class JadescriptImplicitImportsScopeProvider
    extends XImportSectionNamespaceScopeProvider {

    @Override
    protected List<ImportNormalizer> getImplicitImports(boolean ignoreCase) {
        List<ImportNormalizer> result = new ArrayList<>(
            super.getImplicitImports(ignoreCase)
        );
        result.add(doCreateImportNormalizer(
            QualifiedName.create("jade", "core", "AID"),
            false,
            false
        ));
        return result;
    }

}
