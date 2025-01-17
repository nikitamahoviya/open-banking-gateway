package de.adorsys.opba.protocol.xs2a.service.xs2a.ais;

import de.adorsys.opba.protocol.bpmnshared.dto.DtoMapper;
import de.adorsys.opba.protocol.bpmnshared.dto.messages.ProcessResponse;
import de.adorsys.opba.protocol.bpmnshared.service.exec.ValidatedExecution;
import de.adorsys.opba.protocol.xs2a.context.Xs2aContext;
import de.adorsys.opba.protocol.xs2a.context.ais.Xs2aAisContext;
import de.adorsys.opba.protocol.xs2a.service.dto.ValidatedQueryHeaders;
import de.adorsys.opba.protocol.xs2a.service.mapper.QueryHeadersMapperTemplate;
import de.adorsys.opba.protocol.xs2a.service.xs2a.dto.Xs2aWithBalanceParameters;
import de.adorsys.opba.protocol.xs2a.service.xs2a.dto.Xs2aWithConsentIdHeaders;
import de.adorsys.opba.protocol.xs2a.service.xs2a.validation.Xs2aValidator;
import de.adorsys.xs2a.adapter.api.AccountInformationService;
import de.adorsys.xs2a.adapter.api.Response;
import de.adorsys.xs2a.adapter.api.model.AccountList;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * Calls ASPSP XS2A API to list the accounts using already existing consent.
 * The result with account list is published as {@link ProcessResponse} to the event bus.
 */
@Service("xs2aAccountListing")
@RequiredArgsConstructor
public class Xs2aAccountListingService extends ValidatedExecution<Xs2aAisContext> {

    private final ApplicationEventPublisher eventPublisher;
    private final Extractor extractor;
    private final Xs2aValidator validator;
    private final AccountInformationService ais;
    private final Xs2aConsentErrorHandler handler;

    @Override
    protected void doValidate(DelegateExecution execution, Xs2aAisContext context) {
        validator.validate(execution, context, this.getClass(), extractor.forValidation(context));
    }

    @Override
    protected void doRealExecution(DelegateExecution execution, Xs2aAisContext context) {
        ValidatedQueryHeaders<Xs2aWithBalanceParameters, Xs2aWithConsentIdHeaders> params = extractor.forExecution(context);
        handler.tryActionOrHandleConsentErrors(execution, eventPublisher, () -> {
            Response<AccountList> accounts = ais.getAccountList(
                params.getHeaders().toHeaders(),
                params.getQuery().toParameters()
            );
            eventPublisher.publishEvent(
                new ProcessResponse(execution.getRootProcessInstanceId(), execution.getId(), accounts.getBody())
            );
        });
    }

    @Service
    public static class Extractor extends QueryHeadersMapperTemplate<
        Xs2aAisContext,
        Xs2aWithBalanceParameters,
        Xs2aWithConsentIdHeaders> {

        public Extractor(
            DtoMapper<Xs2aContext, Xs2aWithConsentIdHeaders> toHeaders,
            DtoMapper<Xs2aAisContext, Xs2aWithBalanceParameters> toQuery) {
            super(toHeaders, toQuery);
        }
    }
}
