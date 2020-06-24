package fi.aalto.cs.apluscourses.intellij.model;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import fi.aalto.cs.apluscourses.intellij.utils.CourseFileManager;
import fi.aalto.cs.apluscourses.model.Component;
import fi.aalto.cs.apluscourses.model.Course;
import fi.aalto.cs.apluscourses.model.Library;
import fi.aalto.cs.apluscourses.model.ModelFactory;
import fi.aalto.cs.apluscourses.model.Module;
import fi.aalto.cs.apluscourses.model.ModuleMetadata;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.CalledWithReadLock;
import org.jetbrains.annotations.NotNull;

public class IntelliJModelFactory implements ModelFactory {

  @NotNull
  private final APlusProject project;

  private final Map<String, ModuleMetadata> modulesMetadata;


  public IntelliJModelFactory(@NotNull Project project) {
    this.project = new APlusProject(project);
    modulesMetadata = CourseFileManager.getInstance().getModulesMetadata();
  }

  @Override
  public Course createCourse(@NotNull String id,
                             @NotNull String name,
                             @NotNull List<Module> modules,
                             @NotNull List<Library> libraries,
                             @NotNull Map<String, String> requiredPlugins,
                             @NotNull Map<String, URL> resourceUrls,
                             @NotNull List<String> autoInstallComponentNames) {

    IntelliJCourse course =
        new IntelliJCourse(id, name, modules, libraries, requiredPlugins, resourceUrls,
            autoInstallComponentNames, project, new CommonLibraryProvider(project));

    Component.InitializationCallback componentInitializationCallback =
        component -> registerComponentToCourse(component, course);
    course.getCommonLibraryProvider().setInitializationCallback(componentInitializationCallback);
    course.getComponents().forEach(componentInitializationCallback::initialize);

    ReadAction.run(() -> addListeners(course));

    course.resolve();

    return course;
  }

  @CalledWithReadLock
  private void addListeners(IntelliJCourse course) {
    project.getMessageBus().connect().subscribe(ProjectTopics.MODULES, new ModuleListener() {
      @Override
      public void moduleRemoved(@NotNull Project project,
                                @NotNull com.intellij.openapi.module.Module projectModule) {
        Optional.of(projectModule.getName())
            .map(course::getComponentIfExists)
            .ifPresent(Component::setUnresolved);
      }
    });
    project.getLibraryTable().addListener(new LibraryTable.Listener() {
      @Override
      public void afterLibraryRemoved(
          @NotNull com.intellij.openapi.roots.libraries.Library library) {
        Optional.ofNullable(library.getName())
            .map(course::getComponentIfExists)
            .ifPresent(Component::setUnresolved);
      }
    });
    project.getMessageBus().connect().subscribe(VirtualFileManager.VFS_CHANGES,
        new BulkFileListener() {
          @Override
          public void after(@NotNull List<? extends VFileEvent> events) {
            for (VFileEvent event : events) {
              if (event instanceof VFileDeleteEvent) {
                Optional.ofNullable(event.getFile())
                    .map(course::getComponentIfExists)
                    .ifPresent(Component::setUnresolved);
              }
            }
          }
        }
    );
  }

  private void registerComponentToCourse(@NotNull Component component, @NotNull Course course) {
    component.onError.addListener(course, Course::resolve);
  }

  @Override
  public Module createModule(@NotNull String name, @NotNull URL url, @NotNull String versionId) {
    ModuleMetadata moduleMetadata = Optional.ofNullable(modulesMetadata.get(name))
        .orElse(new ModuleMetadata(null, null));
    return new IntelliJModule(name, url, versionId,
        moduleMetadata.getModuleId(), moduleMetadata.getDownloadedAt(), project);
  }

  @Override
  public Library createLibrary(@NotNull String name) {
    throw new UnsupportedOperationException(
        "Only common libraries like Scala SDK are currently supported.");
  }
}
