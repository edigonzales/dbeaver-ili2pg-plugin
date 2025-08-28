package ch.so.agi.dbeaver.ili2pg.ui;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;

public class Ili2pgImportSchemaDialog extends TitleAreaDialog {

  private Text iniPathText;
  private Text ilidataText;
  private Text schemaNameText;

  private String iniPath;
  private String ilidataRef;
  private String targetSchema;

  public Ili2pgImportSchemaDialog(Shell parentShell) {
    super(parentShell);
  }

  @Override
  public void create() {
    super.create();
    setTitle("Import schema with ili2pg");
    setMessage("Provide either an INI file or an ilidata reference, and a target schema name.");
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Composite container = new Composite(area, SWT.NONE);
    GridLayoutFactory.swtDefaults().numColumns(3).margins(10, 10).spacing(8, 8).applyTo(container);
    GridDataFactory.fillDefaults().grab(true, true).applyTo(container);

    // INI file
    new Label(container, SWT.NONE).setText("INI file:");
    iniPathText = new Text(container, SWT.BORDER);
    iniPathText.setMessage("Choose ili2pg .ini file…");
    GridDataFactory.fillDefaults().grab(true, false).applyTo(iniPathText);

    Button browse = new Button(container, SWT.PUSH);
    browse.setText("Browse…");
    browse.addSelectionListener(new SelectionAdapter() {
      @Override public void widgetSelected(SelectionEvent e) {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
        fd.setText("Select ili2pg INI file");
        fd.setFilterExtensions(new String[] { "*.ini", "*.*" });
        String sel = fd.open();
        if (sel != null) iniPathText.setText(sel);
      }
    });

    // ilidata
    new Label(container, SWT.NONE).setText("ilidata reference:");
    ilidataText = new Text(container, SWT.BORDER);
    ilidataText.setMessage("ilidata:my-dataset-or-resource");
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(ilidataText);

    // Target schema
    new Label(container, SWT.NONE).setText("Target schema name:");
    schemaNameText = new Text(container, SWT.BORDER);
    schemaNameText.setMessage("e.g. my_schema");
    GridDataFactory.fillDefaults().grab(true, false).span(2, 1).applyTo(schemaNameText);

    ModifyListener mod = e -> validate();
    iniPathText.addModifyListener(mod);
    ilidataText.addModifyListener(mod);
    schemaNameText.addModifyListener(mod);

    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    getShell().setMinimumSize(560, 300);
    validate();
  }

  @Override
  protected boolean isResizable() { return true; }

  private void validate() {
    String ini = iniPathText != null ? iniPathText.getText().trim() : "";
    String ili = ilidataText != null ? ilidataText.getText().trim() : "";
    String sch = schemaNameText != null ? schemaNameText.getText().trim() : "";

    String error = null;

    // schema name required
    if (sch.isEmpty()) {
      error = "Please enter a target schema name.";
    } else if (!sch.matches("[A-Za-z_][A-Za-z0-9_]*")) {
      error = "Schema name must start with a letter or underscore and contain only letters, digits, or underscores.";
    } else if (ini.isEmpty() && ili.isEmpty()) {
      // at least one of INI or ilidata
      error = "Choose either an INI file or an ilidata reference.";
    } else if (!ini.isEmpty() && !ini.toLowerCase().endsWith(".ini")) {
      error = "The selected file must have the .ini extension.";
    } else if (!ili.isEmpty() && !ili.startsWith("ilidata:")) {
      error = "The ilidata reference must start with \"ilidata:\".";
    }

    setErrorMessage(error);
    Button ok = getButton(IDialogConstants.OK_ID);
    if (ok != null) ok.setEnabled(error == null);
  }

  @Override
  protected void okPressed() {
    this.iniPath      = iniPathText.getText().isBlank() ? null : iniPathText.getText().trim();
    this.ilidataRef   = ilidataText.getText().isBlank() ? null : ilidataText.getText().trim();
    this.targetSchema = schemaNameText.getText().trim();    
    super.okPressed();
  }

  // Getters
  public String getIniPath()      { return iniPath; }
  public String getIliDataRef()   { return ilidataRef; }
  public String getTargetSchema() { return targetSchema; }
}
