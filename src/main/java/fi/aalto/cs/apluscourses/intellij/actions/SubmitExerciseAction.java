package fi.aalto.cs.apluscourses.intellij.actions;

import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getAndReplaceText;
import static fi.aalto.cs.apluscourses.utils.PluginResourceBundle.getText;
import static icons.PluginIcons.ACCENT_COLOR;

import com.intellij.history.LocalHistory;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import fi.aalto.cs.apluscourses.intellij.notifications.ExerciseNotSelectedNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingFileNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.MissingModuleNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NetworkErrorNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.NotSubmittableNotification;
import fi.aalto.cs.apluscourses.intellij.notifications.Notifier;
import fi.aalto.cs.apluscourses.intellij.notifications.SubmissionSentNotification;
import fi.aalto.cs.apluscourses.intellij.services.Dialogs;
import fi.aalto.cs.apluscourses.intellij.services.MainViewModelProvider;
import fi.aalto.cs.apluscourses.intellij.services.PluginSettings;
import fi.aalto.cs.apluscourses.intellij.utils.VfsUtil;
import fi.aalto.cs.apluscourses.model.Authentication;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Exercise;
import fi.aalto.cs.apluscourses.model.ExerciseDataSource;
import fi.aalto.cs.apluscourses.model.FileDoesNotExistException;
import fi.aalto.cs.apluscourses.model.FileFinder;
import fi.aalto.cs.apluscourses.model.Group;
import fi.aalto.cs.apluscourses.model.SubmissionHistory;
import fi.aalto.cs.apluscourses.model.SubmissionInfo;
import fi.aalto.cs.apluscourses.model.SubmissionStatusUpdater;
import fi.aalto.cs.apluscourses.model.SubmittableFile;
import fi.aalto.cs.apluscourses.presentation.CourseViewModel;
import fi.aalto.cs.apluscourses.presentation.MainViewModel;
import fi.aalto.cs.apluscourses.presentation.ModuleSelectionViewModel;
import fi.aalto.cs.apluscourses.presentation.base.BaseTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseGroupViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExerciseViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.ExercisesTreeViewModel;
import fi.aalto.cs.apluscourses.presentation.exercise.SubmissionViewModel;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SubmitExerciseAction extends AnAction {

  public static final String ACTION_ID = SubmitExerciseAction.class.getCanonicalName();

  @NotNull
  private final MainViewModelProvider mainViewModelProvider;

  @NotNull
  private final FileFinder fileFinder;

  @NotNull
  private final ModuleSource moduleSource;

  @NotNull
  private final Dialogs dialogs;

  @NotNull
  private final Notifier notifier;

  @NotNull
  private final Tagger tagger;
  private final DocumentSaver documentSaver;

  /**
   * Constructor with reasonable defaults.
   */
  public SubmitExerciseAction() {
    this(
        PluginSettings.getInstance(),
        VfsUtil::findFileInDirectory,
        DefaultModuleSource.INSTANCE,
        Dialogs.DEFAULT,
        Notifications.Bus::notify,
        LocalHistory.getInstance()::putSystemLabel,
        FileDocumentManager.getInstance()::saveAllDocuments
    );
  }

  /**
   * Construct an exercise submission action with the given parameters. This constructor is useful
   * for testing purposes.
   */
  public SubmitExerciseAction(@NotNull MainViewModelProvider mainViewModelProvider,
                              @NotNull FileFinder fileFinder,
                              @NotNull ModuleSource moduleSource,
                              @NotNull Dialogs dialogs,
                              @NotNull Notifier notifier,
                              @NotNull Tagger tagger,
                              @NotNull DocumentSaver documentSaver) {
    this.mainViewModelProvider = mainViewModelProvider;
    this.fileFinder = fileFinder;
    this.moduleSource = moduleSource;
    this.dialogs = dialogs;
    this.notifier = notifier;
    this.tagger = tagger;
    this.documentSaver = documentSaver;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    MainViewModel mainViewModel = mainViewModelProvider.getMainViewModel(project);
    CourseViewModel courseViewModel = mainViewModel.courseViewModel.get();
    ExercisesTreeViewModel exercisesViewModel = mainViewModel.exercisesViewModel.get();
    Authentication authentication = mainViewModel.authentication.get();

    e.getPresentation().setEnabled(project != null && exercisesViewModel != null
        && authentication != null && courseViewModel != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }
    try {
      trySubmit(project);
    } catch (IOException ex) {
      notifyNetworkError(ex, project);
    } catch (FileDoesNotExistException ex) {
      notifier.notify(new MissingFileNotification(ex.getPath(), ex.getName()), project);
    } catch (ModuleMissingException ex) {
      notifier.notify(new MissingModuleNotification(ex.getModuleName()), project);
    }
  }

  private void trySubmit(@NotNull Project project)
      throws IOException, FileDoesNotExistException, ModuleMissingException {
    MainViewModel mainViewModel = mainViewModelProvider.getMainViewModel(project);
    CourseViewModel courseViewModel = mainViewModel.courseViewModel.get();
    ExercisesTreeViewModel exercisesViewModel = mainViewModel.exercisesViewModel.get();
    Authentication authentication = mainViewModel.authentication.get();

    if (courseViewModel == null || exercisesViewModel == null || authentication == null) {
      return;
    }

    BaseTreeViewModel.Selection selection = exercisesViewModel.findSelected();
    ExerciseViewModel selectedExercise = (ExerciseViewModel) selection.getLevel(2);
    ExerciseGroupViewModel selectedExerciseGroup = (ExerciseGroupViewModel) selection.getLevel(1);
    if (selectedExercise == null || selectedExerciseGroup == null) {
      notifier.notify(new ExerciseNotSelectedNotification(), project);
      return;
    }

    Exercise exercise = selectedExercise.getModel();
    Course course = courseViewModel.getModel();
    String language = PluginSettings
        .getInstance()
        .getCourseFileManager(project)
        .getLanguage();
    ExerciseDataSource exerciseDataSource = course.getExerciseDataSource();

    SubmissionInfo submissionInfo = exerciseDataSource.getSubmissionInfo(exercise, authentication);
    if (!submissionInfo.isSubmittable(language)) {
      notifier.notify(new NotSubmittableNotification(), project);
      return;
    }

    Map<String, String> exerciseModules =
        courseViewModel.getModel().getExerciseModules().get(exercise.getId());

    Optional<String> moduleName = Optional
        .ofNullable(exerciseModules)
        .map(self -> self.get(language));

    Module selectedModule;
    if (moduleName.isPresent()) {
      selectedModule = moduleName
          .map(self -> moduleSource.getModule(project, self))
          .orElseThrow(() -> new ModuleMissingException(moduleName.get()));
    } else {
      Module[] modules = moduleSource.getModules(project);

      ModuleSelectionViewModel moduleSelectionViewModel = new ModuleSelectionViewModel(modules);
      if (!dialogs.create(moduleSelectionViewModel, project).showAndGet()) {
        return;
      }
      selectedModule = moduleSelectionViewModel.selectedModule.get();
    }

    if (selectedModule == null) {
      return;
    }

    documentSaver.saveAllDocuments();

    Path modulePath = Paths.get(ModuleUtilCore.getModuleDirPath(selectedModule));
    Map<String, Path> files = new HashMap<>();
    for (SubmittableFile file : submissionInfo.getFiles(language)) {
      files.put(file.getKey(), fileFinder.findFile(modulePath, file.getName()));
    }

    SubmissionHistory history = exerciseDataSource.getSubmissionHistory(exercise, authentication);

    List<Group> groups = new ArrayList<>(exerciseDataSource.getGroups(course, authentication));
    groups.add(0, new Group(-1, Collections
        .singletonList(getText("ui.toolWindow.subTab.exercises.submission.submitAlone"))));

    // Find the group from the available groups that matches the default group ID.
    // A group could be removed, so this way we check that the default group ID is still valid.
    Optional<Long> defaultGroupId = PluginSettings.getInstance().getDefaultGroupId();
    Group defaultGroup = defaultGroupId
        .flatMap(id -> groups
            .stream()
            .filter(group -> group.getId() == id)
            .findFirst())
        .orElse(null);

    SubmissionViewModel submission = new SubmissionViewModel(
        exercise, submissionInfo, history, groups, defaultGroup, files, language);

    if (!dialogs.create(submission, project).showAndGet()) {
      return;
    }

    if (Boolean.TRUE.equals(submission.makeDefaultGroup.get())) {
      PluginSettings.getInstance().setDefaultGroupId(submission.selectedGroup.get().getId());
    } else {
      PluginSettings.getInstance().clearDefaultGroupId();
    }

    String submissionUrl = exerciseDataSource.submit(submission.buildSubmission(), authentication);
    new SubmissionStatusUpdater(
        project, exerciseDataSource, authentication, submissionUrl, selectedExercise.getModel()
    ).start();
    notifier.notify(new SubmissionSentNotification(), project);

    String tag = getAndReplaceText("ui.localHistory.submission.tag",
        selectedExerciseGroup.getPresentableName(),
        submission.getPresentableExerciseName(),
        submission.getCurrentSubmissionNumber());
    addLocalHistoryTag(project, tag);
  }

  private void notifyNetworkError(@NotNull IOException exception, @Nullable Project project) {
    notifier.notify(new NetworkErrorNotification(exception), project);
  }

  private void addLocalHistoryTag(@NotNull Project project, @NotNull String tag) {
    tagger.putSystemLabel(project, tag, ACCENT_COLOR);
  }

  public interface ModuleSource {

    @NotNull
    Module[] getModules(@NotNull Project project);

    @Nullable
    Module getModule(@NotNull Project project, @NotNull String moduleName);
  }

  private static class DefaultModuleSource implements ModuleSource {

    public static final ModuleSource INSTANCE = new DefaultModuleSource();

    @Override
    @NotNull
    public Module[] getModules(@NotNull Project project) {
      return ModuleManager.getInstance(project).getModules();
    }

    @Override
    @Nullable
    public Module getModule(@NotNull Project project, @NotNull String moduleName) {
      return ModuleManager.getInstance(project).findModuleByName(moduleName);
    }
  }

  private static class ModuleMissingException extends Exception {

    @NotNull
    private final String moduleName;

    public ModuleMissingException(@NotNull String moduleName) {
      this.moduleName = moduleName;
    }

    @NotNull
    public String getModuleName() {
      return moduleName;
    }
  }

  @FunctionalInterface
  public interface Tagger {
    void putSystemLabel(@Nullable Project project, @NotNull String tag, int color);
  }

  @FunctionalInterface
  public interface DocumentSaver {
    void saveAllDocuments();
  }
}
