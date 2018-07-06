package org.apereo.cas.audit;

import org.apereo.inspektr.audit.AuditActionContext;
import org.apereo.inspektr.audit.support.JdbcAuditTrailManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Set;

public class CasJdbcAuditTrailManager extends JdbcAuditTrailManager implements GenericAuditTrailManager {
    public CasJdbcAuditTrailManager(final TransactionTemplate transactionTemplate) {
        super(transactionTemplate);
    }

    @Override
    public Set<? extends AuditActionContext> getGenericAuditRecordsSince(final LocalDate localDate) {
        return getAuditRecordsSince(localDate);
    }
}
