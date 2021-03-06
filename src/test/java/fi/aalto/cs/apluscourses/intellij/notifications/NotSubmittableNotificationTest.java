package fi.aalto.cs.apluscourses.intellij.notifications;

import static org.hamcrest.Matchers.containsString;

import org.junit.Assert;
import org.junit.Test;

public class NotSubmittableNotificationTest {

  @Test
  public void testNotSubmittableNotification() {
    NotSubmittableNotification notification = new NotSubmittableNotification();
    Assert.assertEquals("Group ID should be A+", "A+", notification.getGroupId());
    Assert.assertThat("The content mentions the A+ web interface", notification.getContent(),
        containsString("This assignment can only be submitted via a web browser on the "
            + "course website in A+."));
  }

}
