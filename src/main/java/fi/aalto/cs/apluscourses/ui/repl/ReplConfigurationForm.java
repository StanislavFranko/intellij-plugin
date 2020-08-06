package fi.aalto.cs.apluscourses.ui.repl;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;
import static java.util.Objects.requireNonNull;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.presentation.ReplConfigurationFormModel;
import fi.aalto.cs.apluscourses.ui.IconListCellRenderer;
import icons.PluginIcons;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;

public class ReplConfigurationForm extends JPanel {

  public static final String INFOLABEL_TEXT = getText("ui.repl.infoLabel");

  private ReplConfigurationFormModel model;

  private TextFieldWithBrowseButton workingDirectoryField;
  private ComboBox<String> moduleComboBox;
  private JCheckBox dontShowThisWindowCheckBox;
  private JPanel contentPane;
  private JLabel infoTextLabel;

  /**
   * Creates an empty REPL configuration form, bad, bad, form, do not use (but, required later on by
   * autogenerated code).
   */
  public ReplConfigurationForm() {
  }

  /**
   * Creates a workable REPL configuration form with all the bells and whistles required. Binds the
   * corresponding data model {@link ReplConfigurationFormModel} into it.
   */
  public ReplConfigurationForm(@NotNull ReplConfigurationFormModel model) {
    this.model = model;

    dontShowThisWindowCheckBox
        .setSelected(PluginSettings.getInstance().shouldShowReplConfigurationDialog());

    infoTextLabel.setText(INFOLABEL_TEXT);

    addFileChooser(workingDirectoryField, model.getProject());
    workingDirectoryField.setText(model.getModuleWorkingDirectory());

    model.getModuleNames().forEach(moduleName -> moduleComboBox.addItem(moduleName));
    moduleComboBox.setSelectedItem(model.getTargetModuleName());
    moduleComboBox.setEnabled(true);
    moduleComboBox.setRenderer(new IconListCellRenderer<>(PluginIcons.A_PLUS_MODULE));
  }

  private void addFileChooser(
      @NotNull final TextFieldWithBrowseButton textField,
      @NotNull final Project project) {
    final FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(
        false, true, false,
        false, false, false) {
      @Override
      public boolean isFileVisible(VirtualFile file, boolean showHiddenFiles) {
        return super.isFileVisible(file, showHiddenFiles) && file.isDirectory();
      }
    };
    fileChooserDescriptor.setTitle(getText("ui.repl.chooseWorkingDirectory"));
    textField.addBrowseFolderListener(getText("ui.repl.chooseWorkingDirectory"), null,
        project, fileChooserDescriptor);
  }

  /**
   * This method updates the embedded {@link ReplConfigurationFormModel} with the current state of
   * the {@link ReplConfigurationForm} build.
   */
  public void updateModel() {
    model.setTargetModuleName(requireNonNull(moduleComboBox.getSelectedItem()).toString());
    model.setModuleWorkingDirectory(workingDirectoryField.getText());
    boolean chosenState = !dontShowThisWindowCheckBox.isSelected();
    PluginSettings.getInstance().setShowReplConfigurationDialog(chosenState);
  }

  public void cancelReplStart() {
    model.setStartRepl(false);
  }

  public JPanel getContentPane() {
    return contentPane;
  }

  public ReplConfigurationFormModel getModel() {
    return model;
  }

  public void setModel(ReplConfigurationFormModel model) {
    this.model = model;
  }

  public TextFieldWithBrowseButton getWorkingDirectoryField() {
    return workingDirectoryField;
  }

  public ComboBox<String> getModuleComboBox() {
    return moduleComboBox;
  }

  public JCheckBox getDontShowThisWindowCheckBox() {
    return dontShowThisWindowCheckBox;
  }

  public JLabel getInfoTextLabel() {
    return infoTextLabel;
  }
}
