package fi.aalto.cs.apluscourses.presentation.exercise;

import fi.aalto.cs.apluscourses.model.ExerciseGroup;
import fi.aalto.cs.apluscourses.presentation.base.BaseViewModel;
import fi.aalto.cs.apluscourses.presentation.base.SelectableNodeViewModel;
import fi.aalto.cs.apluscourses.presentation.base.TreeViewModel;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExercisesTreeViewModel extends BaseViewModel<List<ExerciseGroup>>
    implements TreeViewModel {

  @NotNull
  private final List<ExerciseGroupViewModel> groupViewModels;

  /**
   * Construct an exercises tree view model from the given exercise groups.
   */
  public ExercisesTreeViewModel(@NotNull List<ExerciseGroup> exerciseGroups) {
    super(exerciseGroups);
    this.groupViewModels = exerciseGroups
        .stream()
        .map(ExerciseGroupViewModel::new)
        .collect(Collectors.toList());
  }

  /**
   * Returns the exercise from this tree that is selected, or null if no exercise is selected.
   */
  @Nullable
  public ExerciseViewModel getSelectedExercise() {
    return getGroupViewModels().stream()
        .flatMap(ExerciseGroupViewModel::streamChildren)
        .filter(SelectableNodeViewModel::isSelected)
        .map(ExerciseViewModel.class::cast)
        .findFirst()
        .orElse(null);
  }

  /**
   * Returns the submission from this tree that is selected, or null if no submission is selected.
   */
  @Nullable
  public SubmissionResultViewModel getSelectedSubmission() {
    return getGroupViewModels().stream()
        .flatMap(ExerciseGroupViewModel::streamChildren)
        .flatMap(SelectableNodeViewModel::streamChildren)
        .filter(SelectableNodeViewModel::isSelected)
        .map(SubmissionResultViewModel.class::cast)
        .findFirst()
        .orElse(null);
  }

  @NotNull
  public List<ExerciseGroupViewModel> getGroupViewModels() {
    return groupViewModels;
  }

  @Override
  public @NotNull List<ExerciseGroupViewModel> getChildren() {
    return getGroupViewModels();
  }
}
