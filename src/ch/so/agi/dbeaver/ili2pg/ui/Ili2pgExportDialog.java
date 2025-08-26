package ch.so.agi.dbeaver.ili2pg.ui;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;

import java.util.List;

public class Ili2pgExportDialog extends TitleAreaDialog {
    private final String schemaName;
    private final List<String> modelNames;

    // model pickers
    private ComboViewer existingModelCombo;
    private ComboViewer exportModelCombo;
    private Label exportModelLabel;

    private Button disableValidationBtn;
    private Button overwriteBtn;

    private Text datasetsText;
    private Text basketsText;
    private Text topicsText;
    private Text outputText;

    private String selectedModel;        // model in schema
    private String selectedExportModel;  // export model (may equal selectedModel)
    private boolean disableValidation;
    private boolean overwrite;
    private String datasets;
    private String baskets;
    private String topics;
    private String outputPath;

    public Ili2pgExportDialog(Shell parentShell, String schemaName, List<String> modelNames) {
        super(parentShell);
        this.schemaName = schemaName;
        this.modelNames = modelNames;
    }

    @Override
    public void create() {
        super.create();
        setTitle("Export schema with ili2pg");
        setMessage("Schema: " + schemaName + " — choose a model and options.");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite area = (Composite) super.createDialogArea(parent);
        Composite container = new Composite(area, SWT.NONE);
        GridLayoutFactory.swtDefaults().numColumns(3).margins(10, 10).spacing(8, 8).applyTo(container);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

        // --- Model in schema (always shown) ---
        new Label(container, SWT.NONE).setText("Model in schema:");
        existingModelCombo = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        existingModelCombo.setContentProvider(ArrayContentProvider.getInstance());
        existingModelCombo.setLabelProvider(new LabelProvider());
        existingModelCombo.setInput(modelNames);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
            .hint(320, SWT.DEFAULT).applyTo(existingModelCombo.getCombo());
        if (!modelNames.isEmpty()) {
            existingModelCombo.setSelection(new StructuredSelection(modelNames.get(0)));
        }
        existingModelCombo.addSelectionChangedListener(e -> validate());

        // --- Export model (shown only if >= 2 models) ---
        exportModelLabel = new Label(container, SWT.NONE);
        exportModelLabel.setText("Export model:");

        exportModelCombo = new ComboViewer(container, SWT.DROP_DOWN | SWT.READ_ONLY);
        exportModelCombo.setContentProvider(ArrayContentProvider.getInstance());
        exportModelCombo.setLabelProvider(new LabelProvider());
        exportModelCombo.setInput(modelNames);
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1)
            .hint(320, SWT.DEFAULT).applyTo(exportModelCombo.getCombo());
        if (!modelNames.isEmpty()) {
            exportModelCombo.setSelection(new StructuredSelection(modelNames.get(0)));
        }
        exportModelCombo.addSelectionChangedListener(e -> validate());

        boolean showExportModel = modelNames.size() >= 2;
        toggleExportModelVisibility(showExportModel);

        // --- Checkboxes (no group) ---
        disableValidationBtn = new Button(container, SWT.CHECK);
        disableValidationBtn.setText("Disable validation");
        GridDataFactory.fillDefaults().span(3, 1).applyTo(disableValidationBtn);
        disableValidationBtn.addListener(SWT.Selection, l -> validate());

//        overwriteBtn = new Button(container, SWT.CHECK);
//        overwriteBtn.setText("Overwrite existing output");
//        GridDataFactory.fillDefaults().span(3, 1).applyTo(overwriteBtn);
//        overwriteBtn.addListener(SWT.Selection, l -> validate());

        // Dataset(s)
        new Label(container, SWT.NONE).setText("Dataset(s):");
        datasetsText = new Text(container, SWT.BORDER);
        datasetsText.setMessage("e.g. dataset1;dataset2");
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(datasetsText);

        // Basket(s)
        new Label(container, SWT.NONE).setText("Basket(s):");
        basketsText = new Text(container, SWT.BORDER);
        basketsText.setMessage("e.g. basketId1;basketId2");
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(basketsText);

        // Topic(s)
        new Label(container, SWT.NONE).setText("Topic(s):");
        topicsText = new Text(container, SWT.BORDER);
        topicsText.setMessage("e.g. Model.TopicA;Model.TopicB");
        GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(topicsText);

        // Output path row
//        new Label(container, SWT.NONE).setText("Output file:");
//        outputText = new Text(container, SWT.BORDER);
//        GridDataFactory.fillDefaults().grab(true, false).applyTo(outputText);
//        outputText.addModifyListener(e -> validate());
//
//        Button browse = new Button(container, SWT.PUSH);
//        browse.setText("Browse…");
//        browse.addSelectionListener(new SelectionAdapter() {
//            @Override public void widgetSelected(SelectionEvent e) {
//                FileDialog fd = new FileDialog(getShell(), SWT.SAVE);
//                fd.setText("Choose export file");
//                fd.setFilterExtensions(new String[] { "*.xtf", "*.*" });
//                String sel = fd.open();
//                if (sel != null) outputText.setText(sel);
//            }
//        });

        return area;
    }

    private void toggleExportModelVisibility(boolean show) {
        // show/hide and collapse space using GridData.exclude
        exportModelLabel.setVisible(show);
        exportModelCombo.getCombo().setVisible(show);

        GridData gd1 = (GridData) exportModelLabel.getLayoutData();
        if (gd1 == null) { gd1 = new GridData(); exportModelLabel.setLayoutData(gd1); }
        gd1.exclude = !show;

        GridData gd2 = (GridData) exportModelCombo.getCombo().getLayoutData();
        if (gd2 == null) { gd2 = new GridData(); exportModelCombo.getCombo().setLayoutData(gd2); }
        gd2.exclude = !show;

        exportModelCombo.getCombo().setEnabled(show);
        exportModelCombo.getCombo().setToolTipText(show ? "" : "Only available when the schema contains two or more models.");

        exportModelLabel.getParent().layout(true, true);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        getShell().setMinimumSize(560, 480);
        validate();
    }

    @Override
    protected boolean isResizable() { return true; }

    private void validate() {
        IStructuredSelection selExisting = existingModelCombo != null
            ? (IStructuredSelection) existingModelCombo.getSelection()
            : StructuredSelection.EMPTY;
        String out = outputText != null ? outputText.getText().trim() : "";

        String error = null;
        if (modelNames.isEmpty()) error = "No models found in this schema.";
        else if (selExisting.isEmpty()) error = "Please select a model.";
        //else if (out.isEmpty()) error = "Please choose an output file.";

        setErrorMessage(error);
        Button ok = getButton(IDialogConstants.OK_ID);
        if (ok != null) ok.setEnabled(error == null);
    }

    @Override
    protected void okPressed() {
        // existing model
        IStructuredSelection selExisting = (IStructuredSelection) existingModelCombo.getSelection();
        this.selectedModel = selExisting.isEmpty() && !modelNames.isEmpty()
            ? modelNames.get(0)
            : (String) selExisting.getFirstElement();

        // export model (if hidden or no selection, default to the existing model)
        boolean exportShown = exportModelCombo.getCombo().getVisible();
        IStructuredSelection selExport = (IStructuredSelection) exportModelCombo.getSelection();
        this.selectedExportModel = (!exportShown || selExport.isEmpty())
            ? this.selectedModel
            : (String) selExport.getFirstElement();

        this.disableValidation = disableValidationBtn.getSelection();
        //this.overwrite         = overwriteBtn.getSelection();
        //this.outputPath        = outputText.getText().trim();
        this.datasets          = datasetsText.getText().isBlank() ? null : datasetsText.getText().trim();
        this.baskets           = basketsText.getText().isBlank() ? null : basketsText.getText().trim();
        this.topics            = topicsText.getText().isBlank() ? null : topicsText.getText().trim();

        super.okPressed();
    }

    // Getters
    public String  getSelectedModel()       { return selectedModel; }
    public String  getSelectedExportModel() { return selectedExportModel; }
    public boolean isDisableValidation()    { return disableValidation; }
    public boolean isOverwrite()            { return overwrite; }
    public String  getOutputPath()          { return outputPath; }
    public String  getDatasets()            { return datasets; }
    public String  getBaskets()             { return baskets; }
    public String  getTopics()              { return topics; }
}
