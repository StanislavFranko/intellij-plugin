package fi.aalto.cs.apluscourses.ui.repl;

import static org.junit.Assert.assertEquals;

import javax.swing.JList;
import org.junit.Test;

public class ModuleComboBoxListRendererTest {

  @Test
  public void testGetListCellRendererComponent() {
    //  given
    ModuleComboBoxListRenderer renderer = new ModuleComboBoxListRenderer();

    //  when
    renderer.customize(new JList(), "",
        0, true, true);

    //  then
    assertEquals("The correct icon for rendering Modules in ComboBoxList is set",
        ModuleComboBoxListRenderer.iconPath, renderer.getIcon().toString());
  }
}