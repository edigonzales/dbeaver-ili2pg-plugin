package ch.so.agi.dbeaver.ili2pg.ui;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

import java.util.List;

public class Ili2pgImportDialog extends TitleAreaDialog {

  // incoming list of models (required for the combo)
  private final List<String> modelNames;

  // UI
  private ComboViewer modelCombo;     // top: model picker
  private Text transferFileText;      // local .xtf/.xml/.itf
  private Text externalFileText;      // ilidata:...
  private Button disableValidationBtn;
  private Text datasetText;
  private Text basketsText;
  private Text topicsText;

  // results
  private String selectedModel;
  private String transferFilePath;
  private String externalTransferRef;
  private boolean disableValidation;
  private String dataset;
  private String baskets;
  private String topics;

  public Ili2pgImportDialog(Shell parentShell, List<String> modelNames) {
    super(parentShell);
    this.modelNames = modelNames;
  }

  @Override
  public void create() {
    super.create();
    setTitle("Import data with ili2pg");
    setMessage("Select a model, provide a local transfer file (.xtf/.xml/.itf) or an ilidata reference, and choose options.");
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(3).margins(10, 10).spacing(8, 8).applyTo(container);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

    // --- Model (top position) ---
    new Label(container, SWT.NONE).setText("Model:");
    modelCombo = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY);
    modelCombo.setContentProvider(ArrayContentProvider.getInstance());
    modelCombo.setLabelProvider(new LabelProvider());
    modelCombo.setInput(modelNames);
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
        .hint(320, SWT.DEFAULT).applyTo(modelCombo.getCombo());
    if (!modelNames.isEmpty()) {
      modelCombo.setSelection(new StructuredSelection(modelNames.get(0)));
    }
    modelCombo.addSelectionChangedListener(e -> validateInputs());

    // --- Local transfer file ---
    new Label(container, SWT.NONE).setText("Transfer file:");
    transferFileText = new Text(container, SWT.BORDER);
    transferFileText.setMessage("Select .xtf, .xml or .itf…");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(transferFileText);

    Button browse = new Button(container, SWT.PUSH);
    browse.setText("Browse…");
    browse.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
        fd.setText("Select Interlis transfer file");
        fd.setFilterExtensions(new String[] { "*.xtf;*.xml;*.itf", "*.xtf", "*.xml", "*.itf", "*.*" });
        String sel = fd.open();
        if (sel != null) transferFileText.setText(sel);
      }
    });

    // --- External transfer (ilidata) ---
    new Label(container, SWT.NONE).setText("External transfer (ilidata):");
    externalFileText = new Text(container, SWT.BORDER);
    externalFileText.setMessage("ilidata:my-dataset-or-resource");
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(externalFileText);

    // --- Checkbox ---
    disableValidationBtn = new Button(container, SWT.CHECK);
    disableValidationBtn.setText("Disable validation");
    GridDataFactory.fillDefaults().span(3, 1).applyTo(disableValidationBtn);

    // --- Optional filters ---
    new Label(container, SWT.NONE).setText("Dataset:");
    datasetText = new Text(container, SWT.BORDER);
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(datasetText);

    new Label(container, SWT.NONE).setText("Basket(s):");
    basketsText = new Text(container, SWT.BORDER);
    basketsText.setMessage("e.g. basketId1, basketId2");
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(basketsText);

    new Label(container, SWT.NONE).setText("Topic(s):");
    topicsText = new Text(container, SWT.BORDER);
    topicsText.setMessage("e.g. Model.TopicA, Model.TopicB");
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(topicsText);

    // Live validation
    ModifyListener mod = e -> validateInputs();
    transferFileText.addModifyListener(mod);
    externalFileText.addModifyListener(mod);
    datasetText.addModifyListener(mod);
    basketsText.addModifyListener(mod);
    topicsText.addModifyListener(mod);
    disableValidationBtn.addListener(SWT.Selection, e -> validateInputs());

    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    getShell().setMinimumSize(560, 360);
    validateInputs();
  }

  @Override
  protected boolean isResizable() { return true; }

  private void validateInputs() {
    String local = transferFileText != null ? transferFileText.getText().trim() : "";
    String ext   = externalFileText != null ? externalFileText.getText().trim()   : "";
    IStructuredSelection sel = modelCombo != null
        ? (IStructuredSelection) modelCombo.getSelection()
        : StructuredSelection.EMPTY;

    String error = null;

    // model required
    if (modelNames == null || modelNames.isEmpty()) {
      error = "No models available.";
    } else if (sel.isEmpty()) {
      error = "Please select a model.";
    // require at least one of local or ilidata
    } else if (local.isEmpty() && ext.isEmpty()) {
      error = "Provide either a local transfer file or an ilidata reference.";
    } else if (!local.isEmpty() && !hasValidTransferExt(local)) {
      error = "Local file must end with .xtf, .xml, or .itf.";
    } else if (!ext.isEmpty() && !ext.startsWith("ilidata:")) {
      error = "External reference must start with \"ilidata:\".";
    }

    setErrorMessage(error);
    Button ok = getButton(IDialogConstants.OK_ID);
    if (ok != null) ok.setEnabled(error == null);
  }

  private boolean hasValidTransferExt(String path) {
    String p = path.toLowerCase();
    return p.endsWith(".xtf") || p.endsWith(".xml") || p.endsWith(".itf");
  }

  @Override
  protected void okPressed() {
    IStructuredSelection sel = (IStructuredSelection) modelCombo.getSelection();
    this.selectedModel        = (String) sel.getFirstElement();
    this.transferFilePath     = transferFileText.getText().isBlank() ? null : transferFileText.getText().trim();
    this.externalTransferRef  = externalFileText.getText().isBlank() ? null : externalFileText.getText().trim();
    this.disableValidation    = disableValidationBtn.getSelection();
    
    this.dataset           = datasetText.getText().isBlank() ? null : datasetText.getText().trim();
    this.baskets           = basketsText.getText().isBlank() ? null : basketsText.getText().trim();
    this.topics            = topicsText.getText().isBlank() ? null : topicsText.getText().trim();

    
    super.okPressed();
  }

  // Getters for your import job
  public String  getSelectedModel()       { return selectedModel; }
  public String  getTransferFilePath()    { return transferFilePath; }
  public String  getExternalTransferRef() { return externalTransferRef; }
  public boolean isDisableValidation()    { return disableValidation; }
  public String  getDataset()             { return dataset; }
  public String  getBaskets()             { return baskets; }
  public String  getTopics()              { return topics; }
}
