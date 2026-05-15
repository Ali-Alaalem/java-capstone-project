package com.project4.TaskManager.repositories;

import com.project4.TaskManager.models.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReminderRepository extends JpaRepository<Reminder, Long> {
    List<Reminder> findByTaskId(Long taskId);
    List<Reminder> findByRemindAtBeforeAndSentFalse(LocalDateTime now); // this is to fetch all the reminders whose time has passed but haven't been sent yet (if these two conditions are there then send the email).
}