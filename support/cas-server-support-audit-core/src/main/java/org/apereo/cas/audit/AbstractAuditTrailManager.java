package org.apereo.cas.audit;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apereo.inspektr.audit.AuditActionContext;

import java.time.LocalDate;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This is {@link AbstractAuditTrailManager}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public abstract class AbstractAuditTrailManager implements GenericAuditTrailManager {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    protected boolean asynchronous;

    @Override
    public void record(final AuditActionContext audit) {
        if (this.asynchronous) {
            this.executorService.execute(() -> saveAuditRecord(audit));
        } else {
            saveAuditRecord(audit);
        }
    }

    public abstract Set<? extends AuditActionContext> getGenericAuditRecordsSince(LocalDate localDate);

    @Override
    public Set<AuditActionContext> getAuditRecordsSince(final LocalDate localDate) {
        return getGenericAuditRecordsSince(localDate).stream().map(it -> (AuditActionContext) it).collect(Collectors.toSet());
    }

    protected abstract void saveAuditRecord(final AuditActionContext audit);
}
