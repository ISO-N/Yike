package com.kariscode.yike.data.backup

import android.app.Application
import android.net.Uri
import androidx.room.withTransaction
import com.kariscode.yike.core.dispatchers.AppDispatchers
import com.kariscode.yike.core.time.TimeProvider
import com.kariscode.yike.data.local.db.YikeDatabase
import com.kariscode.yike.data.local.db.dao.CardDao
import com.kariscode.yike.data.local.db.dao.DeckDao
import com.kariscode.yike.data.local.db.dao.QuestionDao
import com.kariscode.yike.data.local.db.dao.ReviewRecordDao
import com.kariscode.yike.data.local.db.entity.CardEntity
import com.kariscode.yike.data.local.db.entity.DeckEntity
import com.kariscode.yike.data.local.db.entity.QuestionEntity
import com.kariscode.yike.data.local.db.entity.ReviewRecordEntity
import com.kariscode.yike.data.mapper.RoomMappers
import com.kariscode.yike.domain.model.AppSettings
import com.kariscode.yike.domain.model.Question
import com.kariscode.yike.domain.model.QuestionStatus
import com.kariscode.yike.domain.model.ReviewRecord
import com.kariscode.yike.domain.repository.AppSettingsRepository
import java.io.FileNotFoundException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

/**
 * 备份服务集中处理导出、校验与恢复，是为了让页面只负责触发文件选择而不触碰高风险数据操作细节。
 */
class BackupService(
    private val application: Application,
    private val database: YikeDatabase,
    private val deckDao: DeckDao,
    private val cardDao: CardDao,
    private val questionDao: QuestionDao,
    private val reviewRecordDao: ReviewRecordDao,
    private val appSettingsRepository: AppSettingsRepository,
    private val backupValidator: BackupValidator,
    private val timeProvider: TimeProvider,
    private val dispatchers: AppDispatchers
) {
    /**
     * 导出时即便数据为空也要写出合法备份文件，
     * 这样用户才能在“先备份配置、后逐步录入内容”的场景下获得稳定结果。
     */
    suspend fun exportToUri(uri: Uri) = withContext(dispatchers.io) {
        val exportedAt = timeProvider.nowEpochMillis()
        val document = buildBackupDocument(exportedAtEpochMillis = exportedAt)
        val jsonString = BackupJson.json.encodeToString(document)
        application.contentResolver.openOutputStream(uri)?.use { outputStream ->
            outputStream.write(jsonString.toByteArray())
            outputStream.flush()
        } ?: throw FileNotFoundException("无法创建备份文件")
        appSettingsRepository.setBackupLastAt(exportedAt)
    }

    /**
     * 恢复前先做解析与校验，再执行全量覆盖；
     * 这样能把“文件非法”和“写库失败”两类风险明确分层，便于给用户稳定反馈。
     */
    suspend fun restoreFromUri(uri: Uri) = withContext(dispatchers.io) {
        val jsonString = application.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: throw FileNotFoundException("无法读取备份文件")
        val document = BackupJson.json.decodeFromString<BackupDocument>(jsonString)
        backupValidator.validate(document).getOrThrow()
        restoreDocument(document)
    }

    /**
     * 导出文件名单独提供生成方法，是为了把命名规范固定下来并便于页面直接复用。
     */
    fun createSuggestedFileName(nowEpochMillis: Long = timeProvider.nowEpochMillis()): String {
        val stamp = BackupJson.formatEpochMillis(nowEpochMillis)
            .replace("-", "")
            .replace(":", "")
            .replace("T", "-")
            .substringBefore("+")
        return "yike-backup-$stamp.json"
    }

    /**
     * 构建备份文档时读取完整数据快照，可确保序列化结果与当下本地状态严格一致。
     */
    private suspend fun buildBackupDocument(exportedAtEpochMillis: Long): BackupDocument {
        val settings = appSettingsRepository.observeSettings().first()
        val decks = deckDao.listAll()
        val cards = cardDao.listAll()
        val questions = questionDao.listAll()
        val reviewRecords = reviewRecordDao.listAll()

        return BackupDocument(
            app = BackupAppInfo(
                name = "忆刻",
                backupVersion = BackupConstants.BACKUP_VERSION,
                exportedAt = BackupJson.formatEpochMillis(exportedAtEpochMillis)
            ),
            settings = BackupSettings(
                dailyReminderEnabled = settings.dailyReminderEnabled,
                dailyReminderTime = settings.toBackupReminderTime(),
                schemaVersion = settings.schemaVersion,
                backupLastAt = settings.backupLastAt?.let(BackupJson::formatEpochMillis)
            ),
            decks = decks.map { deck -> deck.toBackup() },
            cards = cards.map { card -> card.toBackup() },
            questions = questions.map { question -> question.toBackup() },
            reviewRecords = reviewRecords.map { reviewRecord -> reviewRecord.toBackup() }
        )
    }

    /**
     * 为了做到“恢复失败时当前数据不被修改”，这里先保留旧快照并在设置写入失败时执行补偿恢复。
     */
    private suspend fun restoreDocument(document: BackupDocument) {
        val previousSettings = appSettingsRepository.observeSettings().first()
        val previousDecks = deckDao.listAll()
        val previousCards = cardDao.listAll()
        val previousQuestions = questionDao.listAll()
        val previousReviewRecords = reviewRecordDao.listAll()

        try {
            database.withTransaction {
                reviewRecordDao.clearAll()
                questionDao.clearAll()
                cardDao.clearAll()
                deckDao.clearAll()

                deckDao.upsertAll(document.decks.map { deck -> deck.toEntity() })
                cardDao.upsertAll(document.cards.map { card -> card.toEntity() })
                questionDao.upsertAll(document.questions.map { question -> question.toEntity() })
                reviewRecordDao.insertAll(document.reviewRecords.map { record -> record.toEntity() })
            }

            writeSettingsFromBackup(document.settings)
        } catch (throwable: Throwable) {
            database.withTransaction {
                reviewRecordDao.clearAll()
                questionDao.clearAll()
                cardDao.clearAll()
                deckDao.clearAll()

                deckDao.upsertAll(previousDecks)
                cardDao.upsertAll(previousCards)
                questionDao.upsertAll(previousQuestions)
                reviewRecordDao.insertAll(previousReviewRecords)
            }
            restorePreviousSettings(previousSettings)
            throw IllegalStateException("恢复失败，当前数据未被修改", throwable)
        }
    }

    /**
     * 设置写入单独封装，是为了让恢复成功后的提醒配置和版本信息与备份内容保持一致。
     */
    private suspend fun writeSettingsFromBackup(settings: BackupSettings) {
        val (hour, minute) = BackupReminderTimeCodec.parse(settings.dailyReminderTime)
        persistSettings(
            dailyReminderEnabled = settings.dailyReminderEnabled,
            dailyReminderHour = hour,
            dailyReminderMinute = minute,
            schemaVersion = settings.schemaVersion,
            backupLastAt = settings.backupLastAt?.let(BackupJson::parseEpochMillis)
        )
    }

    /**
     * 发生补偿回滚时要把旧设置一并恢复，
     * 否则即使数据库回滚成功，提醒配置仍可能与当前数据脱节。
     */
    private suspend fun restorePreviousSettings(settings: AppSettings) {
        persistSettings(
            dailyReminderEnabled = settings.dailyReminderEnabled,
            dailyReminderHour = settings.dailyReminderHour,
            dailyReminderMinute = settings.dailyReminderMinute,
            schemaVersion = settings.schemaVersion,
            backupLastAt = settings.backupLastAt
        )
    }

    /**
     * 设置字段统一经由同一写入顺序落库，是为了避免恢复成功路径和补偿回滚路径逐步偏离。
     */
    private suspend fun persistSettings(
        dailyReminderEnabled: Boolean,
        dailyReminderHour: Int,
        dailyReminderMinute: Int,
        schemaVersion: Int,
        backupLastAt: Long?
    ) {
        appSettingsRepository.setDailyReminderEnabled(dailyReminderEnabled)
        appSettingsRepository.setDailyReminderTime(dailyReminderHour, dailyReminderMinute)
        appSettingsRepository.setSchemaVersion(schemaVersion)
        appSettingsRepository.setBackupLastAt(backupLastAt)
    }

    /**
     * 统一把设置映射为固定 `HH:mm` 文本，是为了让备份文件结构稳定且便于人工阅读。
     */
    private fun AppSettings.toBackupReminderTime(): String =
        BackupReminderTimeCodec.format(
            hour = dailyReminderHour,
            minute = dailyReminderMinute
        )

    /**
     * Deck 到备份模型的映射收敛到单点后，字段调整时就不必同时追踪导出主流程里的长内联表达式。
     */
    private fun DeckEntity.toBackup(): BackupDeck = BackupDeck(
        id = id,
        name = name,
        description = description,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = BackupJson.formatEpochMillis(createdAt),
        updatedAt = BackupJson.formatEpochMillis(updatedAt)
    )

    /**
     * Card 的备份映射独立出来，是为了让层级字段与时间字段的导出规则保持易读且可复用。
     */
    private fun CardEntity.toBackup(): BackupCard = BackupCard(
        id = id,
        deckId = deckId,
        title = title,
        description = description,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = BackupJson.formatEpochMillis(createdAt),
        updatedAt = BackupJson.formatEpochMillis(updatedAt)
    )

    /**
     * Question 映射集中在单点后，可以把状态与时间字段的转换规则固定住，
     * 避免导出路径未来增删字段时遗漏调度相关数据。
     */
    private fun QuestionEntity.toBackup(): BackupQuestion {
        val domainQuestion = RoomMappers.run { toDomain() }
        return domainQuestion.toBackup()
    }

    /**
     * ReviewRecord 的备份映射抽出来后，导出主流程只保留“读哪些表”的骨架，更容易检查事务语义。
     */
    private fun ReviewRecordEntity.toBackup(): BackupReviewRecord {
        val domainRecord = RoomMappers.run { toDomain() }
        return domainRecord.toBackup()
    }

    /**
     * 领域问题转换成备份模型，是为了让状态枚举与字符串字段的边界集中在一处维护。
     */
    private fun Question.toBackup(): BackupQuestion = BackupQuestion(
        id = id,
        cardId = cardId,
        prompt = prompt,
        answer = answer,
        tags = tags,
        status = when (status) {
            QuestionStatus.ACTIVE -> QuestionEntity.STATUS_ACTIVE
            QuestionStatus.ARCHIVED -> QuestionEntity.STATUS_ARCHIVED
        },
        stageIndex = stageIndex,
        dueAt = BackupJson.formatEpochMillis(dueAt),
        lastReviewedAt = lastReviewedAt?.let(BackupJson::formatEpochMillis),
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        createdAt = BackupJson.formatEpochMillis(createdAt),
        updatedAt = BackupJson.formatEpochMillis(updatedAt)
    )

    /**
     * 领域复习记录转换为备份模型时保留原始评分链路，
     * 这样恢复后仍能重建用户真实的复习历史。
     */
    private fun ReviewRecord.toBackup(): BackupReviewRecord = BackupReviewRecord(
        id = id,
        questionId = questionId,
        rating = rating.name,
        oldStageIndex = oldStageIndex,
        newStageIndex = newStageIndex,
        oldDueAt = BackupJson.formatEpochMillis(oldDueAt),
        newDueAt = BackupJson.formatEpochMillis(newDueAt),
        reviewedAt = BackupJson.formatEpochMillis(reviewedAt),
        responseTimeMs = responseTimeMs,
        note = note
    )

    /**
     * 备份模型恢复成 DeckEntity 的规则集中后，导入主流程就能更清楚地表达层级恢复顺序。
     */
    private fun BackupDeck.toEntity(): DeckEntity = DeckEntity(
        id = id,
        name = name,
        description = description,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = BackupJson.parseEpochMillis(createdAt),
        updatedAt = BackupJson.parseEpochMillis(updatedAt)
    )

    /**
     * Card 备份恢复映射抽成扩展，是为了把时间解析与层级字段恢复放在一起维护。
     */
    private fun BackupCard.toEntity(): CardEntity = CardEntity(
        id = id,
        deckId = deckId,
        title = title,
        description = description,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = BackupJson.parseEpochMillis(createdAt),
        updatedAt = BackupJson.parseEpochMillis(updatedAt)
    )

    /**
     * Question 恢复映射集中到单点后，状态字符串与领域模型之间的边界就不会散落在事务主流程里。
     */
    private fun BackupQuestion.toEntity(): QuestionEntity = RoomMappers.run {
        Question(
            id = this@toEntity.id,
            cardId = this@toEntity.cardId,
            prompt = this@toEntity.prompt,
            answer = this@toEntity.answer,
            tags = this@toEntity.tags,
            status = if (this@toEntity.status == QuestionEntity.STATUS_ARCHIVED) {
                QuestionStatus.ARCHIVED
            } else {
                QuestionStatus.ACTIVE
            },
            stageIndex = this@toEntity.stageIndex,
            dueAt = BackupJson.parseEpochMillis(this@toEntity.dueAt),
            lastReviewedAt = this@toEntity.lastReviewedAt?.let(BackupJson::parseEpochMillis),
            reviewCount = this@toEntity.reviewCount,
            lapseCount = this@toEntity.lapseCount,
            createdAt = BackupJson.parseEpochMillis(this@toEntity.createdAt),
            updatedAt = BackupJson.parseEpochMillis(this@toEntity.updatedAt)
        ).toEntity()
    }

    /**
     * ReviewRecord 恢复映射独立出来，是为了让枚举解析与时间解析的风险点更容易被单独检查。
     */
    private fun BackupReviewRecord.toEntity(): ReviewRecordEntity = RoomMappers.run {
        ReviewRecord(
            id = this@toEntity.id,
            questionId = this@toEntity.questionId,
            rating = enumValueOf(this@toEntity.rating),
            oldStageIndex = this@toEntity.oldStageIndex,
            newStageIndex = this@toEntity.newStageIndex,
            oldDueAt = BackupJson.parseEpochMillis(this@toEntity.oldDueAt),
            newDueAt = BackupJson.parseEpochMillis(this@toEntity.newDueAt),
            reviewedAt = BackupJson.parseEpochMillis(this@toEntity.reviewedAt),
            responseTimeMs = this@toEntity.responseTimeMs,
            note = this@toEntity.note
        ).toEntity()
    }

}
