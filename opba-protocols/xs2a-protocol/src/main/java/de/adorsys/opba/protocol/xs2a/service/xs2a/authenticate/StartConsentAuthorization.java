package de.adorsys.opba.protocol.xs2a.service.xs2a.authenticate;

import de.adorsys.opba.protocol.api.common.CurrentBankProfile;
import de.adorsys.opba.protocol.bpmnshared.dto.DtoMapper;
import de.adorsys.opba.protocol.bpmnshared.service.context.ContextUtil;
import de.adorsys.opba.protocol.bpmnshared.service.exec.ValidatedExecution;
import de.adorsys.opba.protocol.xs2a.context.Xs2aContext;
import de.adorsys.opba.protocol.xs2a.service.dto.ValidatedPathHeaders;
import de.adorsys.opba.protocol.xs2a.service.mapper.PathHeadersMapperTemplate;
import de.adorsys.opba.protocol.xs2a.service.xs2a.dto.Xs2aInitialConsentParameters;
import de.adorsys.opba.protocol.xs2a.service.xs2a.dto.Xs2aStandardHeaders;
import de.adorsys.opba.protocol.xs2a.service.xs2a.validation.Xs2aValidator;
import de.adorsys.xs2a.adapter.api.AccountInformationService;
import de.adorsys.xs2a.adapter.api.Response;
import de.adorsys.xs2a.adapter.api.model.StartScaprocessResponse;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static de.adorsys.opba.protocol.xs2a.constant.GlobalConst.CONTEXT;
import static de.adorsys.xs2a.adapter.api.ResponseHeaders.ASPSP_SCA_APPROACH;

/**
 * Initiates the consent authorization. Optionally may provide preferred ASPSP approach.
 */
@Service("xs2aStartConsentAuthorization")
@RequiredArgsConstructor
public class StartConsentAuthorization extends ValidatedExecution<Xs2aContext> {

    private final Extractor extractor;
    private final Xs2aValidator validator;
    private final AccountInformationService ais;
    private final TppRedirectPreferredResolver tppRedirectPreferredResolver;

    @Override
    protected void doValidate(DelegateExecution execution, Xs2aContext context) {
        validator.validate(execution, context, this.getClass(), extractor.forValidation(context));
    }

    @Override
    protected void doRealExecution(DelegateExecution execution, Xs2aContext context) {
        CurrentBankProfile config = context.aspspProfile();

        ValidatedPathHeaders<Xs2aInitialConsentParameters, Xs2aStandardHeaders> params =
                extractor.forExecution(context);

        params.getHeaders().setTppRedirectPreferred(tppRedirectPreferredResolver.isRedirectApproachPreferred(config));

        Response<StartScaprocessResponse> scaStart = ais.startConsentAuthorisation(
                params.getPath().getConsentId(),
                params.getHeaders().toHeaders(),
                params.getPath().toParameters()
        );

        String aspspSelectedApproach = scaStart.getHeaders().getHeader(ASPSP_SCA_APPROACH);
        context.setAspspScaApproach(null == aspspSelectedApproach ? config.getPreferredApproach().name() : aspspSelectedApproach);
        context.setAuthorizationId(scaStart.getBody().getAuthorisationId());
        context.setStartScaProcessResponse(scaStart.getBody());

        execution.setVariable(CONTEXT, context);
    }

    @Override
    protected void doMockedExecution(DelegateExecution execution, Xs2aContext context) {
        CurrentBankProfile config = context.aspspProfile();

        ContextUtil.getAndUpdateContext(execution, (Xs2aContext ctx) -> {
            ctx.setAspspScaApproach(config.getPreferredApproach().name());
            ctx.setAuthorizationId(UUID.randomUUID().toString());
        });
    }

    @Service
    public static class Extractor extends PathHeadersMapperTemplate<
            Xs2aContext,
            Xs2aInitialConsentParameters,
            Xs2aStandardHeaders> {

        public Extractor(
                DtoMapper<Xs2aContext, Xs2aStandardHeaders> toHeaders,
                DtoMapper<Xs2aContext, Xs2aInitialConsentParameters> toParameters) {
            super(toHeaders, toParameters);
        }
    }
}
