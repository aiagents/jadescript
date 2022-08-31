package it.unipr.ailab.jadescript.ui.wizards;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.xtend2.lib.StringConcatenation;

public class NewOntologyWizard extends NewAbstractJadescriptFileWizard {

	private Text ontologyNameText;
	private Text moduleText;

	@Override
	public String getWizardTitle() {
		return "New Jadescript Ontology File";
	}

	@Override
	public String getWizardDescription() {
		return "Creates a new Jadescript Ontology declaration in a *.jade file.";
	}

	@Override
	public void fillInControls(Composite container, Runnable refresher) {
		Label label = new Label(container, SWT.NULL);
		label.setText("&Name:");

		ontologyNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		ontologyNameText.setLayoutData(gd);
		ontologyNameText.addModifyListener(e -> refresher.run());

		new Label(container, SWT.LEAD);

		label = new Label(container, SWT.NULL);
		label.setText("&Module:");
		moduleText = new Text(container, SWT.BORDER | SWT.SINGLE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		moduleText.setLayoutData(gd);
		moduleText.addModifyListener(e -> refresher.run());

		if (selection instanceof ITreeSelection) {
			for (Object element : ((ITreeSelection) selection).toList()) {
				if (element instanceof IPackageFragment) {
					String packageName = ((IPackageFragment) element).getElementName();
					moduleText.setText(packageName);
					break;
				}
			}
		}

	}

	@Override
	public boolean validate(Consumer<String> statusAcceptor) {
		String agentName = getOntologyName();
		String moduleName = getModuleName();
		if (agentName.length() == 0) {
			statusAcceptor.accept("Ontology name must be specified");
			return true;
		}
		if (!agentName.matches("[A-Za-z][A-Za-z0-9_]*")) {
			statusAcceptor.accept("Ontology name must be valid");
			return true;
		}
		if (!moduleName.isEmpty() && (!moduleName.matches("[A-Za-z][A-Za-z0-9_\\.]*") || moduleName.endsWith("."))) {
			statusAcceptor.accept("Invalid module name");
			return true;
		}
		return false;
	}

	@Override
	public String finalizeFileName() {
		return getOntologyName() + ".jade";
	}

	public String getModuleName() {
		return moduleText.getText();
	}

	public String getOntologyName() {
		return ontologyNameText.getText();
	}

	@Override
	public void finalizeContent(StringConcatenation builder, Map<String, Object> data) {
		String moduleName = (String) data.get("module");
		String ontologyName = (String) data.get("ontology");

		if (!moduleName.isEmpty()) {
			builder.append("module ");
			builder.append(moduleName);
		}
		builder.newLineIfNotEmpty();
		builder.newLine();
		builder.append("ontology ");
		builder.append(ontologyName);
		builder.newLineIfNotEmpty();
		builder.append("    ");
		builder.append("concept my");
		builder.append(ontologyName);
		builder.append("concept");
		builder.newLine();
		builder.append("    ");
		builder.newLine();

	}
	
	@Override
	public Map<String, Object> extractContentData() {
		Map<String, Object> m = new HashMap<>();
		String moduleName = getModuleName();
		String ontologyName = getOntologyName();
		m.put("module", moduleName);
		m.put("ontology", ontologyName);
		return m;
	}

}