package com.seii.legal.data.mysql;

import com.seii.legal.common.enums.RoundType;
import com.seii.legal.common.enums.SubmissionStatus;
import com.seii.legal.data.entity.Submission;
import com.seii.legal.data.entity.TaskRound;
import com.seii.legal.data.repository.AnnotationRepository;
import com.seii.legal.data.repository.DataConflictException;
import com.seii.legal.data.repository.TaskRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "LEGAL_TEST_JDBC_URL", matches = ".+")
class RoundSubmissionTest {
    @Configuration
    @EnableTransactionManagement
    static class Transactions {}

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 12, 0);
    private static AnnotationConfigApplicationContext context;
    private static AnnotationRepository annotations;
    private static TaskRepository rounds;
    private static TransactionTemplate transaction;
    private final String task = UUID.randomUUID().toString();

    @BeforeAll
    static void setup() {
        DataSource source = new DriverManagerDataSource(System.getenv("LEGAL_TEST_JDBC_URL"),
                System.getenv("LEGAL_TEST_DB_USER"), System.getenv("LEGAL_TEST_DB_PASSWORD"));
        new ResourceDatabasePopulator(new ClassPathResource("db/mysql/V001__rounds_and_submissions.sql")).execute(source);
        var manager = new DataSourceTransactionManager(source);
        context = new AnnotationConfigApplicationContext();
        context.registerBean(JdbcTemplate.class, () -> new JdbcTemplate(source));
        context.registerBean(DataSourceTransactionManager.class, () -> manager);
        context.register(Transactions.class, JdbcTaskRepository.class, JdbcAnnotationRepository.class);
        context.refresh();
        rounds = context.getBean(TaskRepository.class);
        annotations = context.getBean(AnnotationRepository.class);
        transaction = new TransactionTemplate(manager);
    }

    @AfterAll
    static void closeContext() {
        if (context != null) context.close();
    }

    private String open(int sequence) {
        return rounds.saveRound(new TaskRound(null, task, "doc", RoundType.CUTTING,
                sequence, "OPEN", NOW, null));
    }

    private Submission submission(String round, String actor, String content) {
        return new Submission(null, task, "doc", RoundType.CUTTING, round, actor, NOW,
                "v1.2", content, SubmissionStatus.SUBMITTED, null);
    }

    @Test
    void reopenedRoundKeepsOldResultsAndAllowsFreshSubmission() {
        String first = open(1);
        assertThrows(DataConflictException.class, () -> open(2));
        Submission original = submission(first, task, "第一版𠮷");
        String id = annotations.saveSubmission(original, "first");
        annotations.updateSubmissionStatus(id, SubmissionStatus.LOCKED, null, "裁定完成");
        rounds.closeRound(first, NOW);
        assertTrue(rounds.findCurrentRound(task, "doc", RoundType.CUTTING).isEmpty());
        assertThrows(DataConflictException.class, () ->
                annotations.updateSubmissionStatus(id, SubmissionStatus.RETURNED, "修改", "退回"));
        String second = open(2);
        assertEquals(id, annotations.saveSubmission(original, "first"));
        String next = annotations.saveSubmission(submission(second, task, "第二版"), "second");
        assertEquals(List.of(next), annotations.findSubmissions(task, "doc", RoundType.CUTTING, null)
                .stream().map(Submission::submissionId).toList());
        assertEquals("第一版𠮷", annotations.findSubmissionsByRound(first, task).get(0).content());
        assertEquals(List.of(first, second), rounds.findRounds(task, "doc").stream().map(TaskRound::roundId).toList());
    }

    @Test
    void distinguishesReplayFromChangedContentAndRepeatSubmission() {
        String round = open(1);
        String id = annotations.saveSubmission(submission(round, task, "A"), "key");
        assertThrows(DataConflictException.class, () -> annotations.saveSubmission(submission(round, task, "B"), "key"));
        var duplicate = assertThrows(DataConflictException.class,
                () -> annotations.saveSubmission(submission(round, task, "A"), "new"));
        assertEquals("本轮已经提交", duplicate.getMessage());
        annotations.updateSubmissionStatus(id, SubmissionStatus.RETURNED, "修正切分", "退回");
        String revised = annotations.saveSubmission(submission(round, task, "B"), "new");
        assertNotEquals(id, revised);
        assertEquals(revised, annotations.findSubmissions(task, "doc", RoundType.CUTTING, task).get(0).submissionId());
    }

    @Test
    void sameRequestIdIsIndependentForDifferentAnnotators() {
        String round = open(1);
        var first = annotations.saveSubmission(submission(round, task + "-a", "A"), "shared");
        var second = annotations.saveSubmission(submission(round, task + "-b", "B"), "shared");
        assertNotEquals(first, second);
        assertEquals(2, annotations.findSubmissions(task, "doc", RoundType.CUTTING, null).size());
        assertEquals(first, annotations.findSubmissionsByRound(round, task + "-a").get(0).submissionId());
    }

    @Test
    void callerRollbackRemovesRecordAndAllowsRetry() {
        String round = open(1);
        Submission body = submission(round, task, "A");
        transaction.executeWithoutResult(tx -> {
            annotations.saveSubmission(body, "retry");
            rounds.closeRound(round, NOW);
            tx.setRollbackOnly();
        });
        assertTrue(annotations.findSubmissionsByRound(round, null).isEmpty());
        assertEquals(round, rounds.findCurrentRound(task, "doc", RoundType.CUTTING).orElseThrow().roundId());
        assertNotNull(annotations.saveSubmission(body, "retry"));
    }

    @Test
    void concurrentIdenticalRequestsReturnOneReceipt() throws Exception {
        String round = open(1);
        Submission body = submission(round, task, "并发");
        var start = new CountDownLatch(1);
        var workers = Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<String> request = () -> {
                start.await();
                return annotations.saveSubmission(body, "shared");
            };
            Future<String> a = workers.submit(request);
            Future<String> b = workers.submit(request);
            start.countDown();
            assertEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(1, annotations.findSubmissionsByRound(round, null).size());
        } finally {
            workers.shutdownNow();
        }
    }
}
