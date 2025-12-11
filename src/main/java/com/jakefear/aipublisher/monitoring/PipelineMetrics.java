package com.jakefear.aipublisher.monitoring;

import com.jakefear.aipublisher.agent.AgentRole;
import com.jakefear.aipublisher.document.DocumentState;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects and tracks pipeline metrics for monitoring and analysis.
 */
public class PipelineMetrics {

    private final AtomicInteger totalPipelinesStarted = new AtomicInteger(0);
    private final AtomicInteger totalPipelinesCompleted = new AtomicInteger(0);
    private final AtomicInteger totalPipelinesFailed = new AtomicInteger(0);
    private final AtomicInteger totalRevisionCycles = new AtomicInteger(0);
    private final AtomicInteger totalApprovalsRequested = new AtomicInteger(0);
    private final AtomicInteger totalApprovalsGranted = new AtomicInteger(0);
    private final AtomicInteger totalApprovalsRejected = new AtomicInteger(0);

    private final AtomicLong totalProcessingTimeMs = new AtomicLong(0);
    private final AtomicLong minProcessingTimeMs = new AtomicLong(Long.MAX_VALUE);
    private final AtomicLong maxProcessingTimeMs = new AtomicLong(0);

    private final Map<DocumentState, AtomicInteger> failuresByState = new ConcurrentHashMap<>();
    private final Map<AgentRole, AtomicLong> processingTimeByAgent = new ConcurrentHashMap<>();
    private final Map<AgentRole, AtomicInteger> invocationsByAgent = new ConcurrentHashMap<>();

    private final Instant startTime = Instant.now();

    /**
     * Record a pipeline start.
     */
    public void recordPipelineStarted() {
        totalPipelinesStarted.incrementAndGet();
    }

    /**
     * Record a successful pipeline completion.
     */
    public void recordPipelineCompleted(Duration processingTime) {
        totalPipelinesCompleted.incrementAndGet();
        updateProcessingTime(processingTime.toMillis());
    }

    /**
     * Record a pipeline failure.
     */
    public void recordPipelineFailed(DocumentState failedAt) {
        totalPipelinesFailed.incrementAndGet();
        failuresByState.computeIfAbsent(failedAt, k -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * Record a revision cycle.
     */
    public void recordRevisionCycle() {
        totalRevisionCycles.incrementAndGet();
    }

    /**
     * Record an approval request.
     */
    public void recordApprovalRequested() {
        totalApprovalsRequested.incrementAndGet();
    }

    /**
     * Record an approval granted.
     */
    public void recordApprovalGranted() {
        totalApprovalsGranted.incrementAndGet();
    }

    /**
     * Record an approval rejection.
     */
    public void recordApprovalRejected() {
        totalApprovalsRejected.incrementAndGet();
    }

    /**
     * Record agent processing time.
     */
    public void recordAgentProcessing(AgentRole role, Duration processingTime) {
        processingTimeByAgent.computeIfAbsent(role, k -> new AtomicLong(0))
                .addAndGet(processingTime.toMillis());
        invocationsByAgent.computeIfAbsent(role, k -> new AtomicInteger(0))
                .incrementAndGet();
    }

    private void updateProcessingTime(long millis) {
        totalProcessingTimeMs.addAndGet(millis);
        minProcessingTimeMs.updateAndGet(current -> Math.min(current, millis));
        maxProcessingTimeMs.updateAndGet(current -> Math.max(current, millis));
    }

    // Getters for metrics

    public int getTotalPipelinesStarted() {
        return totalPipelinesStarted.get();
    }

    public int getTotalPipelinesCompleted() {
        return totalPipelinesCompleted.get();
    }

    public int getTotalPipelinesFailed() {
        return totalPipelinesFailed.get();
    }

    public int getTotalRevisionCycles() {
        return totalRevisionCycles.get();
    }

    public int getTotalApprovalsRequested() {
        return totalApprovalsRequested.get();
    }

    public int getTotalApprovalsGranted() {
        return totalApprovalsGranted.get();
    }

    public int getTotalApprovalsRejected() {
        return totalApprovalsRejected.get();
    }

    public long getTotalProcessingTimeMs() {
        return totalProcessingTimeMs.get();
    }

    public long getMinProcessingTimeMs() {
        long min = minProcessingTimeMs.get();
        return min == Long.MAX_VALUE ? 0 : min;
    }

    public long getMaxProcessingTimeMs() {
        return maxProcessingTimeMs.get();
    }

    public double getAverageProcessingTimeMs() {
        int completed = totalPipelinesCompleted.get();
        if (completed == 0) {
            return 0.0;
        }
        return (double) totalProcessingTimeMs.get() / completed;
    }

    public double getSuccessRate() {
        int total = totalPipelinesStarted.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalPipelinesCompleted.get() / total;
    }

    public Map<DocumentState, Integer> getFailuresByState() {
        Map<DocumentState, Integer> result = new ConcurrentHashMap<>();
        failuresByState.forEach((state, count) -> result.put(state, count.get()));
        return result;
    }

    public long getAgentTotalProcessingTimeMs(AgentRole role) {
        AtomicLong time = processingTimeByAgent.get(role);
        return time != null ? time.get() : 0;
    }

    public int getAgentInvocationCount(AgentRole role) {
        AtomicInteger count = invocationsByAgent.get(role);
        return count != null ? count.get() : 0;
    }

    public double getAgentAverageProcessingTimeMs(AgentRole role) {
        int count = getAgentInvocationCount(role);
        if (count == 0) {
            return 0.0;
        }
        return (double) getAgentTotalProcessingTimeMs(role) / count;
    }

    public Duration getUptime() {
        return Duration.between(startTime, Instant.now());
    }

    /**
     * Generate a summary report of all metrics.
     */
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Pipeline Metrics Report ===\n\n");

        sb.append("Pipeline Statistics:\n");
        sb.append(String.format("  Total Started: %d\n", getTotalPipelinesStarted()));
        sb.append(String.format("  Total Completed: %d\n", getTotalPipelinesCompleted()));
        sb.append(String.format("  Total Failed: %d\n", getTotalPipelinesFailed()));
        sb.append(String.format("  Success Rate: %.1f%%\n", getSuccessRate() * 100));
        sb.append(String.format("  Total Revisions: %d\n", getTotalRevisionCycles()));
        sb.append("\n");

        sb.append("Processing Time:\n");
        sb.append(String.format("  Total: %d ms\n", getTotalProcessingTimeMs()));
        sb.append(String.format("  Average: %.1f ms\n", getAverageProcessingTimeMs()));
        sb.append(String.format("  Min: %d ms\n", getMinProcessingTimeMs()));
        sb.append(String.format("  Max: %d ms\n", getMaxProcessingTimeMs()));
        sb.append("\n");

        sb.append("Approval Statistics:\n");
        sb.append(String.format("  Requested: %d\n", getTotalApprovalsRequested()));
        sb.append(String.format("  Granted: %d\n", getTotalApprovalsGranted()));
        sb.append(String.format("  Rejected: %d\n", getTotalApprovalsRejected()));
        sb.append("\n");

        if (!failuresByState.isEmpty()) {
            sb.append("Failures by State:\n");
            getFailuresByState().forEach((state, count) ->
                    sb.append(String.format("  %s: %d\n", state, count)));
            sb.append("\n");
        }

        sb.append("Agent Performance:\n");
        for (AgentRole role : AgentRole.values()) {
            int count = getAgentInvocationCount(role);
            if (count > 0) {
                sb.append(String.format("  %s: %d invocations, avg %.1f ms\n",
                        role, count, getAgentAverageProcessingTimeMs(role)));
            }
        }
        sb.append("\n");

        sb.append(String.format("Uptime: %s\n", formatDuration(getUptime())));

        return sb.toString();
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Reset all metrics (useful for testing).
     */
    public void reset() {
        totalPipelinesStarted.set(0);
        totalPipelinesCompleted.set(0);
        totalPipelinesFailed.set(0);
        totalRevisionCycles.set(0);
        totalApprovalsRequested.set(0);
        totalApprovalsGranted.set(0);
        totalApprovalsRejected.set(0);
        totalProcessingTimeMs.set(0);
        minProcessingTimeMs.set(Long.MAX_VALUE);
        maxProcessingTimeMs.set(0);
        failuresByState.clear();
        processingTimeByAgent.clear();
        invocationsByAgent.clear();
    }
}
