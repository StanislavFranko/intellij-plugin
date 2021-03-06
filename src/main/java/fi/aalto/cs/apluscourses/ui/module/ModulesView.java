package fi.aalto.cs.apluscourses.ui.module;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.ui.GuiObject;
import javax.swing.JPanel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModulesView {
  @GuiObject
  public ModuleListView moduleListView;
  @GuiObject
  public JPanel toolbarContainer;
  @GuiObject
  private JPanel basePanel;

  /**
   * A view that holds the content of the Modules tool window.
   */
  public ModulesView() {
    // Avoid this instance getting GC'ed before its UI components.
    //
    // Here we add a (strong) reference from a UI component to this object, thus ensuring that this
    // object lives at least as long as that UI component.
    //
    // This makes it possible to use this object as a weakly referred observer for changes that
    // require UI updates.
    //
    // If UI components are GC'ed, this object can also go.
    //
    // It depends on the implementation of IntelliJ's GUI designer whether this "hack"
    // needed (I don't know if these objects of bound classes are strongly referred to from UI or
    // not), but it's better to play it safe.
    //
    // We use class name as a unique key for the property.
    basePanel.putClientProperty(ModulesView.class.getName(), this);
  }

  @NotNull
  public JPanel getBasePanel() {
    return basePanel;
  }

  /**
   * Update this modules view with the given view model (which may be null).
   */
  public void viewModelChanged(@Nullable CourseViewModel course) {
    ApplicationManager.getApplication().invokeLater(
        () -> moduleListView.setModel(course == null ? null : course.getModules()),
        ModalityState.any()
    );
  }
}
