package it.unipr.ailab.jadescript.ui.launching;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class SelectAgentDialog extends Dialog {

	private final int DIALOG_WIDTH = 500;
	private final int DIALOG_HEIGHT = 650;
	
	
	private final List<String> fullyQualifiedNames;
	private final Consumer<LaunchAgentData> selectedAgentAcceptor;

	private final List<Button> selectAgentButtons = new ArrayList<>();
	private Text agentNameText;
	private Text agentArgsText;
	private Button mainContainerRadio;
	private Text localHostText;
	private Text localPortText;
	private Text platformNameText;
	private Button useGui;
	private Button peripheralContainerRadio;
	private Text mainContainerAddrText;
	private Text mainContainerPortText;
	private Text containerNameText;

	public SelectAgentDialog(Shell parentShell, List<String> fullyQualifiedNames,
			Consumer<LaunchAgentData> selectedAgentAcceptor) {
		super(parentShell);
		this.fullyQualifiedNames = fullyQualifiedNames;
		this.selectedAgentAcceptor = selectedAgentAcceptor;
	}

	@Override
	protected Control createDialogArea(Composite parent) {

		ScrolledComposite scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.BORDER);
		
		Composite container = (Composite) super.createDialogArea(scrolledComposite);
		GridLayout containerLayout = (GridLayout) container.getLayout();
		containerLayout.numColumns = 1;
		containerLayout.verticalSpacing = 9;
		
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);
		scrolledComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
		scrolledComposite.setContent(container);
		
		
		// AGENT CLASS
		Group selectAgentGroup = new Group(container, SWT.BORDER);
		selectAgentGroup.setText("Agent type:");
		selectAgentGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectAgentGroup.setLayout(new RowLayout(SWT.VERTICAL));

		String firstAgentName = "";
		AtomicBoolean neverChanged = new AtomicBoolean(true);

		for (int i = 0; i < fullyQualifiedNames.size(); i++) {
			String agentName = fullyQualifiedNames.get(i);
			Button button = new Button(selectAgentGroup, SWT.RADIO);
			if (i == 0) {
				firstAgentName = LaunchAgentData.extractDefaultName(agentName);

				button.setSelection(true);
			} else {
				button.setSelection(false);
			}

			button.setText(agentName);
			selectAgentButtons.add(button);
		}

		
		// AGENT NAME AND ARGS
		Group agentDetailsGroup = new Group(container, SWT.BORDER);
		agentDetailsGroup.setText("Agent details:");
		agentDetailsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout agentDetailsGroupLayout = new GridLayout();
		agentDetailsGroupLayout.numColumns = 1;
		agentDetailsGroupLayout.verticalSpacing = 5;
		agentDetailsGroup.setLayout(agentDetailsGroupLayout);

		Label agentNameLabel = new Label(agentDetailsGroup, SWT.NONE);
		agentNameLabel.setText("Agent name:");
		agentNameText = new Text(agentDetailsGroup, SWT.BORDER | SWT.SINGLE);
		agentNameText.setText(firstAgentName);
		agentNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		agentNameText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent arg0) {
				neverChanged.set(false);
			}
		});
		selectAgentButtons.forEach(b -> {
			b.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (neverChanged.get() && b.getSelection()) {
						agentNameText.setText(LaunchAgentData.extractDefaultName(b.getText()));
						neverChanged.set(true);
					}
				}
			});
		});
		Label agentArgsLabel = new Label(agentDetailsGroup, SWT.NONE);
		agentArgsLabel.setText("Agent's 'on create' arguments:");
		agentArgsText = new Text(agentDetailsGroup, SWT.BORDER);
		GridData agentArgsTextLayoutData = new GridData(GridData.FILL_HORIZONTAL);
		agentArgsText.setLayoutData(agentArgsTextLayoutData);
//		GC gc = new GC(agentArgsText);
//		try
//		{
//		    gc.setFont(agentArgsText.getFont());
//		    FontMetrics fm = gc.getFontMetrics();
//		    agentArgsTextLayoutData.heightHint = 5 * fm.getHeight();
//		}
//		finally
//		{
//		    gc.dispose();
//		}

		// CONTAINER
		Group containerGroup = new Group(container, SWT.BORDER);
		containerGroup.setText("Container:");
		containerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout containerGroupLayout = new GridLayout();
		containerGroupLayout.numColumns = 1;
		containerGroupLayout.verticalSpacing = 9;
		containerGroup.setLayout(containerGroupLayout);

		mainContainerRadio = new Button(containerGroup, SWT.RADIO);
		mainContainerRadio.setText("Run in new main container");

		Group mainContainerGroup = new Group(containerGroup, SWT.BORDER);
		mainContainerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout mainContainerGroupLayout = new GridLayout();
		mainContainerGroupLayout.numColumns = 1;
		mainContainerGroupLayout.verticalSpacing = 5;
		mainContainerGroup.setLayout(mainContainerGroupLayout);

		Label localHostLabel = new Label(mainContainerGroup, SWT.NONE);
		localHostLabel.setText("Main container address:");
		localHostText = new Text(mainContainerGroup, SWT.BORDER | SWT.SINGLE);
		localHostText.setText("localhost");
		localHostText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label localPortLabel = new Label(mainContainerGroup, SWT.NONE);
		localPortLabel.setText("Main container port:");
		localPortText = new Text(mainContainerGroup, SWT.BORDER | SWT.SINGLE);
		localPortText.setText("1099");
		Label platformNameLabel = new Label(mainContainerGroup, SWT.NONE);
		platformNameLabel.setText("Platform name:");
		platformNameText = new Text(mainContainerGroup, SWT.BORDER | SWT.SINGLE);
		platformNameText.setMessage("[assign automatically]");
		platformNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		useGui = new Button(mainContainerGroup, SWT.CHECK);
		useGui.setText("Start RMA GUI agent");
		useGui.setEnabled(false);

		peripheralContainerRadio = new Button(containerGroup, SWT.RADIO);
		peripheralContainerRadio.setText("Run in new peripheral container");

		Group peripheralContainerGroup = new Group(containerGroup, SWT.BORDER);
		peripheralContainerGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		GridLayout peripheralContainerGroupLayout = new GridLayout();
		peripheralContainerGroupLayout.numColumns = 1;
		peripheralContainerGroupLayout.verticalSpacing = 5;
		peripheralContainerGroup.setLayout(peripheralContainerGroupLayout);

		Label mainContainerAddrLabel = new Label(peripheralContainerGroup, SWT.NONE);
		mainContainerAddrLabel.setText("Main container address:");
		mainContainerAddrText = new Text(peripheralContainerGroup, SWT.BORDER | SWT.SINGLE);
		mainContainerAddrText.setText("localhost");
		mainContainerAddrText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		Label mainContainerPortLabel = new Label(peripheralContainerGroup, SWT.NONE);
		mainContainerPortLabel.setText("Main container port:");
		mainContainerPortText = new Text(peripheralContainerGroup, SWT.BORDER | SWT.SINGLE);
		mainContainerPortText.setText("1099");
		Label containerNameLabel = new Label(peripheralContainerGroup, SWT.NONE);
		containerNameLabel.setText("Container name:");
		containerNameText = new Text(peripheralContainerGroup, SWT.BORDER | SWT.SINGLE);
		containerNameText.setMessage("[assign automatically]");
		containerNameText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		mainContainerRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				peripheralContainerGroup.setEnabled(false);
				recursiveSetEnabled(peripheralContainerGroup, false);
				mainContainerGroup.setEnabled(true);
				recursiveSetEnabled(mainContainerGroup, true);
			}
		});

		peripheralContainerRadio.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				peripheralContainerGroup.setEnabled(true);
				recursiveSetEnabled(peripheralContainerGroup, true);
				mainContainerGroup.setEnabled(false);
				recursiveSetEnabled(mainContainerGroup, false);
			}
		});

		peripheralContainerRadio.setSelection(true);
		recursiveSetEnabled(peripheralContainerGroup, true);
		mainContainerRadio.setSelection(false);
		recursiveSetEnabled(mainContainerGroup, false);
		
		scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		container.layout();

		return scrolledComposite;
	}

	private static void recursiveSetEnabled(Control ctrl, boolean enabled) {
		if (ctrl instanceof Composite) {
			Composite comp = (Composite) ctrl;
			for (Control c : comp.getChildren())
				recursiveSetEnabled(c, enabled);
		} else {
			ctrl.setEnabled(enabled);
		}
	}

	private static String defaultize(String input, String defaultVal) {
		if (input == null || input.isEmpty()) {
			return defaultVal;
		} else {
			return input;
		}
	}

	@Override
	protected void okPressed() {
		String agentClass = selectAgentButtons.stream().filter(b -> b.getSelection()).map(b -> b.getText()).findFirst()
				.orElse(selectAgentButtons.get(0).getText());

		// TODO update fill-in of LaunchAgentData

		LaunchAgentData data = new LaunchAgentData(agentClass, defaultize(agentNameText.getText(), null),
				defaultize(agentArgsText.getText(), null),
				peripheralContainerRadio.getSelection()
						? new LaunchAgentData.SecondaryContainerData(
								defaultize(mainContainerAddrText.getText(), null),
								defaultize(mainContainerPortText.getText(), null),
								defaultize(containerNameText.getText(), null))
						: new LaunchAgentData.MainContainerData(//
								defaultize(localHostText.getText(), null), //
								defaultize(localPortText.getText(), null), //
								defaultize(platformNameText.getText(), null), useGui.getSelection()));

		selectedAgentAcceptor.accept(data);

		super.okPressed();
	}

	@Override
	protected void setShellStyle(int arg0) {
		super.setShellStyle(SWT.TITLE | SWT.CLOSE);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Run Jadescript Agent");
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

}