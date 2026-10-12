package com.artagram.platform.common.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * HU-35: cada peticion deja un traceId en el MDC (para que los logs de esa
 * interaccion se puedan seguir juntos) y lo devuelve en la respuesta para que
 * el cliente lo pueda reportar si algo falla.
 */
class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void generaUnTraceIdNuevoCuandoElClienteNoMandaUno() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/feed");
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] traceIdVistoDentro = new String[1];

        filter.doFilter(request, response, (req, res) -> traceIdVistoDentro[0] = MDC.get(TraceContext.TRACE_ID_MDC_KEY));

        assertThat(traceIdVistoDentro[0]).isNotBlank();
        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo(traceIdVistoDentro[0]);
        assertThat(MDC.get(TraceContext.TRACE_ID_MDC_KEY)).isNull();
    }

    @Test
    void reutilizaElTraceIdQueMandaElCliente() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/feed");
        request.addHeader(TraceContext.TRACE_ID_HEADER, "trace-de-prueba-123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(TraceContext.TRACE_ID_HEADER)).isEqualTo("trace-de-prueba-123");
    }
}
