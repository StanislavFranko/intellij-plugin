package fi.aalto.cs.intellij.ui.list;

import com.intellij.ui.components.JBList;
import fi.aalto.cs.intellij.presentation.common.BaseListModel;
import fi.aalto.cs.intellij.presentation.common.ListElementModel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;

public abstract class BaseListView<E extends ListElementModel, V>
    extends JBList<E> {

  private final ConcurrentMap<E, V> views = new ConcurrentHashMap<>();

  public BaseListView(BaseListModel<E> listModel) {
    super(listModel);
    setSelectionModel(listModel.getSelectionModel());
    installCellRenderer(this::getRendererForElement);
  }

  @NotNull
  protected abstract V createElementView(E element);

  protected abstract void updateElementView(V view, E element);

  protected abstract JComponent renderElementView(V view);

  @NotNull
  private JComponent getRendererForElement(E element) {
    V view = views.computeIfAbsent(element, this::createElementView);
    if (element.checkIfChanged()) {
      updateElementView(view, element);
    }
    return renderElementView(view);
  }
}
