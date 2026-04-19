package demo.drive.trash.scheduler

import demo.drive.trash.service.TrashService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class TrashScheduler(private val trashService: TrashService) {

    @Scheduled(cron = "0 0 3 * * *")
    fun purgeExpiredTrash() {
        val threshold = Instant.now().minus(30, ChronoUnit.DAYS)
        trashService.purgeExpired(threshold)
    }
}
