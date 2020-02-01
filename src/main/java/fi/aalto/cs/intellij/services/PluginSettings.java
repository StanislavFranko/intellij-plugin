package fi.aalto.cs.intellij.services;

import com.intellij.notification.Notifications;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.serviceContainer.NonInjectable;
import fi.aalto.cs.intellij.common.Course;
import fi.aalto.cs.intellij.common.MalformedCourseConfigurationFileException;
import fi.aalto.cs.intellij.common.ResourceException;
import fi.aalto.cs.intellij.notifications.CourseConfigurationError;
import fi.aalto.cs.intellij.notifications.Notifier;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PluginSettings {
  private static final Logger logger = LoggerFactory
      .getLogger(PluginSettings.class);

  @NotNull
  private final Course currentlyLoadedCourse;
  @NotNull
  private final Notifier notifier;

  /**
   * Constructs a plugin settings instance with the default course configuration file.
   */
  public PluginSettings() {
    // Replace this with the correct path when testing with a local course configuration file.
    this("o1.json", Notifications.Bus::notify);
  }

  /**
   * Constructs a plugin settings instance with the default course configuration file and the given
   * notifier.
   * @param notifier The notifier used to notify the user of various events.
   */
  @NonInjectable
  public PluginSettings(@NotNull String courseConfigurationFilePath, @NotNull Notifier notifier) {
    this.notifier = notifier;
    Course course;
    try {
      course = Course.fromResource(courseConfigurationFilePath);
    } catch (ResourceException | MalformedCourseConfigurationFileException e) {
      course = Course.createEmptyCourse();
      logger.info("Error occurred while trying to parse a course configuration file", e);
      notifier.notify(new CourseConfigurationError(e), null);
    }
    currentlyLoadedCourse = course;
  }

  @NotNull
  public static PluginSettings getInstance() {
    return ServiceManager.getService(PluginSettings.class);
  }

  @NotNull
  public Course getCurrentlyLoadedCourse() {
    return currentlyLoadedCourse;
  }
}
