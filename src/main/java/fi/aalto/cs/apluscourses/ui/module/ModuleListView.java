package fi.aalto.cs.apluscourses.ui.module;

import fi.aalto.cs.apluscourses.presentation.module.ModuleListElementViewModel;
import fi.aalto.cs.apluscourses.ui.base.BaseListView;
import fi.aalto.cs.apluscourses.ui.base.ComponentUtil;
import java.awt.font.TextAttribute;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ModuleListView
    extends BaseListView<ModuleListElementViewModel, ModuleListElementView> {

  public ModuleListView() {
    super(new ModuleListElementView());
  }

  @Override
  protected void updateElementView(@NotNull ModuleListElementView elementView,
                                   @NotNull ModuleListElementViewModel element) {
    elementView.nameLabel.setText(element.getName());
    ComponentUtil.setFont(elementView.nameLabel, TextAttribute.WEIGHT, element.getFontWeight());
    elementView.statusLabel.setText("[" + element.getStatus() + "]");
    elementView.basePanel.setToolTipText(element.getUrl());
  }
}
