package pro.wata.sdk.acquiring;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import pro.wata.sdk.http.HttpTransport;
import pro.wata.sdk.model.CreateLinkRequest;
import pro.wata.sdk.model.LinkSearchQuery;
import pro.wata.sdk.model.PageResult;
import pro.wata.sdk.model.PaymentLink;

/** Платёжные ссылки: {@code /api/h2h/links}. */
public final class LinksClient {

    private final HttpTransport transport;
    private final JavaType pageType;

    LinksClient(HttpTransport transport, ObjectMapper mapper) {
        this.transport = transport;
        this.pageType = mapper.getTypeFactory().constructParametricType(PageResult.class, PaymentLink.class);
    }

    /**
     * Создаёт платёжную ссылку. Изменяющий запрос — по умолчанию не
     * повторяется при сбое, чтобы повтор не создал вторую ссылку.
     */
    public PaymentLink create(CreateLinkRequest request) {
        return transport.post("/links", request, PaymentLink.class, false);
    }

    public PageResult<PaymentLink> search(LinkSearchQuery query) {
        return transport.get("/links", query.toQueryParams(), pageType);
    }

    public PaymentLink get(String linkId) {
        return transport.get("/links/" + linkId, null, PaymentLink.class);
    }
}
