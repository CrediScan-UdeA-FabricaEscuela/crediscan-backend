package co.udea.codefactory.creditscoring.applicant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Tests de integración para HU-003 — Listado general de solicitantes con filtros.
 * Cubre los ACs 01 a 18: paginación, filtros, ordenamiento, exportación CSV y autorización.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ListApplicantsIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void limpiarDatos() {
        jdbcTemplate.update("DELETE FROM applicant_edit_audit");
        jdbcTemplate.update("DELETE FROM applicant");
    }

    // --- Método auxiliar para registrar solicitantes ---

    private void registrar(String identificacion, String nombre, String tipoEmpleo,
                            int ingresos, int antiguedad) throws Exception {
        String payload = """
                {
                  "nombre": "%s",
                  "identificacion": "%s",
                  "fecha_nacimiento": "1990-05-15",
                  "ingresos_mensuales": %d,
                  "tipo_empleo": "%s",
                  "antiguedad_laboral": %d
                }
                """.formatted(nombre, identificacion, ingresos, tipoEmpleo, antiguedad);
        mockMvc.perform(post("/api/v1/solicitantes")
                        .with(user("analyst").roles("ANALYST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated());
    }

    // ===== AC-01: Paginación por defecto (20 registros) =====

    @Test
    void list_paginacionPorDefecto_retorna20RegistrosYTotalElements() throws Exception {
        for (int i = 1; i <= 25; i++) {
            registrar("10172" + String.format("%05d", i), "Solicitante " + i, "Empleado", 3500000, 36);
        }

        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.page.totalElements").value(25))
                .andExpect(jsonPath("$.page.size").value(20))
                .andExpect(jsonPath("$.page.number").value(0));
    }

    @Test
    void list_segundaPagina_retornaRegistrosRestantes() throws Exception {
        for (int i = 1; i <= 25; i++) {
            registrar("20172" + String.format("%05d", i), "Paginado " + i, "Empleado", 3500000, 36);
        }

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("page", "1")
                        .param("size", "20")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.page.number").value(1));
    }

    // ===== AC-02: Filtro por rango de ingresos =====

    @Test
    void list_filtroIngresosMensualesRango_retoraSoloRegistrosDentroDelRango() throws Exception {
        registrar("3001", "Pobre", "Empleado", 1000000, 12);
        registrar("3002", "Medio", "Empleado", 4000000, 24);
        registrar("3003", "Rico", "Empleado", 9000000, 36);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("ingresos_min", "3000000")
                        .param("ingresos_max", "5000000")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Medio"));
    }

    @Test
    void list_filtroSoloIngresosMin_retoraSolicitantesConIngresosSuperiores() throws Exception {
        registrar("3011", "Bajo", "Empleado", 2000000, 12);
        registrar("3012", "Alto", "Empleado", 7000000, 24);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("ingresos_min", "5000000")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Alto"));
    }

    // ===== AC-03: Filtro por tipo de empleo =====

    @Test
    void list_filtroTipoEmpleo_retoraSoloElTipoEspecificado() throws Exception {
        registrar("4001", "Empleado Juan", "Empleado", 3500000, 24);
        registrar("4002", "Independiente Maria", "Independiente", 3500000, 24);
        registrar("4003", "Pensionado Pedro", "Pensionado", 3500000, 24);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("tipo_empleo", "Independiente")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Independiente Maria"));
    }

    // ===== AC-04: Filtro por antigüedad laboral =====

    @Test
    void list_filtroAntiguedadRango_retoraSoloRegistrosDentroDelRango() throws Exception {
        registrar("5001", "Junior", "Empleado", 3500000, 6);
        registrar("5002", "SemiSenior", "Empleado", 3500000, 36);
        registrar("5003", "Senior", "Empleado", 3500000, 120);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("antiguedad_min", "24")
                        .param("antiguedad_max", "60")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("SemiSenior"));
    }

    // ===== AC-05: Filtro por fecha de registro =====

    @Test
    void list_filtroFechaRegistro_retoraSoloRegistrosDentroDelRango() throws Exception {
        registrar("6001", "Antiguo", "Empleado", 3500000, 24);
        // Se actualiza directamente en DB porque created_at es manejado por la aplicación
        jdbcTemplate.update("UPDATE applicant SET created_at = '2024-01-15T00:00:00Z' WHERE name = 'Antiguo'");

        registrar("6002", "Reciente", "Empleado", 3500000, 24);
        jdbcTemplate.update("UPDATE applicant SET created_at = '2025-06-15T00:00:00Z' WHERE name = 'Reciente'");

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("fecha_registro_desde", "2025-01-01")
                        .param("fecha_registro_hasta", "2025-12-31")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Reciente"));
    }

    // ===== AC-06: Filtros combinados (AND) =====

    @Test
    void list_filtrosCombinados_aplicaLogicaAND() throws Exception {
        // Solo "A" cumple todas las condiciones: Empleado + ingresos altos + antigüedad alta
        registrar("7001", "A Cumple Todo", "Empleado", 6000000, 48);
        registrar("7002", "B Mal Tipo", "Independiente", 6000000, 48);
        registrar("7003", "C Bajo Ingreso", "Empleado", 1500000, 48);
        registrar("7004", "D Poca Antiguedad", "Empleado", 6000000, 5);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("tipo_empleo", "Empleado")
                        .param("ingresos_min", "4000000")
                        .param("antiguedad_min", "24")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("A Cumple Todo"));
    }

    // ===== AC-07/AC-08: Ordenamiento ascendente y descendente =====

    @Test
    void list_ordenamientoPorIngresosAscendente_retornaListaOrdenadaDeMenorAMayor() throws Exception {
        registrar("8001", "Bajo Ingreso", "Empleado", 2000000, 12);
        registrar("8002", "Ingreso Medio", "Empleado", 5000000, 12);
        registrar("8003", "Alto Ingreso", "Empleado", 9000000, 12);

        MvcResult resultado = mockMvc.perform(get("/api/v1/solicitantes")
                        .param("sort", "monthlyIncome,asc")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn();

        // Verifica que "Bajo Ingreso" aparece antes que "Ingreso Medio" en el JSON
        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo.indexOf("Bajo Ingreso")).isLessThan(cuerpo.indexOf("Ingreso Medio"));
        assertThat(cuerpo.indexOf("Ingreso Medio")).isLessThan(cuerpo.indexOf("Alto Ingreso"));
    }

    @Test
    void list_ordenamientoPorIngresosDescendente_retornaListaOrdenadaDeMayorAMenor() throws Exception {
        registrar("8011", "Primero", "Empleado", 9000000, 12);
        registrar("8012", "Segundo", "Empleado", 5000000, 12);
        registrar("8013", "Tercero", "Empleado", 2000000, 12);

        MvcResult resultado = mockMvc.perform(get("/api/v1/solicitantes")
                        .param("sort", "monthlyIncome,desc")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn();

        String cuerpo = resultado.getResponse().getContentAsString();
        assertThat(cuerpo.indexOf("Primero")).isLessThan(cuerpo.indexOf("Segundo"));
        assertThat(cuerpo.indexOf("Segundo")).isLessThan(cuerpo.indexOf("Tercero"));
    }

    // ===== AC-09: Total de registros en la respuesta =====

    @Test
    void list_totalElementosRefleja_ElTotalFiltrado_NoElTamanoDePagina() throws Exception {
        for (int i = 1; i <= 12; i++) {
            registrar("900" + i, "Empleado " + i, "Empleado", 4000000, 24);
        }
        registrar("9100", "Independiente", "Independiente", 4000000, 24);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("tipo_empleo", "Empleado")
                        .param("size", "5")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(12))
                .andExpect(jsonPath("$.content.length()").value(5));
    }

    // ===== AC-10: Filtros vacíos retorna todos los registros =====

    @Test
    void list_sinFiltros_retornaTodosLosRegistros() throws Exception {
        registrar("10001", "Alpha", "Empleado", 3000000, 12);
        registrar("10002", "Beta", "Independiente", 5000000, 36);
        registrar("10003", "Gamma", "Pensionado", 7000000, 120);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(3));
    }

    // ===== AC-11: Exportación CSV con filtros aplicados =====

    @Test
    void export_conFiltroTipoEmpleo_retornaCSVConSoloEseTipo() throws Exception {
        registrar("11001", "Empleado Filtrado", "Empleado", 4000000, 24);
        registrar("11002", "Independiente Excluido", "Independiente", 4000000, 24);

        MvcResult resultado = mockMvc.perform(get("/api/v1/solicitantes/export")
                        .param("tipo_empleo", "Empleado")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("solicitantes.csv")))
                .andReturn();

        String csv = resultado.getResponse().getContentAsString();
        // El encabezado CSV debe estar presente
        assertThat(csv).contains("nombre");
        assertThat(csv).contains("Empleado Filtrado");
        // El solicitante excluido por el filtro no debe aparecer
        assertThat(csv).doesNotContain("Independiente Excluido");
    }

    // ===== AC-12: Exportación CSV sin filtros =====

    @Test
    void export_sinFiltros_retornaTodosLosSolicitantesEnCSV() throws Exception {
        registrar("12001", "Alfa", "Empleado", 3000000, 12);
        registrar("12002", "Beta", "Independiente", 5000000, 36);

        MvcResult resultado = mockMvc.perform(get("/api/v1/solicitantes/export")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andReturn();

        String csv = resultado.getResponse().getContentAsString();
        assertThat(csv).contains("Alfa");
        assertThat(csv).contains("Beta");
    }

    // ===== AC-13: ANALYST puede acceder al listado y exportación =====

    @Test
    void list_comoAnalyst_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk());
    }

    @Test
    void export_comoAnalyst_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/export")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk());
    }

    // ===== AC-14: CREDIT_SUPERVISOR puede acceder al listado y exportación =====

    @Test
    void list_comoCreditSupervisor_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("cs").roles("CREDIT_SUPERVISOR")))
                .andExpect(status().isOk());
    }

    @Test
    void export_comoCreditSupervisor_retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/export")
                        .with(user("cs").roles("CREDIT_SUPERVISOR")))
                .andExpect(status().isOk());
    }

    // ===== AC-15: Sin autenticación → 401 =====

    @Test
    void list_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void export_sinAutenticacion_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/export"))
                .andExpect(status().isUnauthorized());
    }

    // ===== AC-16: Roles sin acceso al export → 403 =====

    @Test
    void export_comoAdmin_retorna403() throws Exception {
        // ADMIN puede listar pero NO exportar (restricción más estricta en el export)
        mockMvc.perform(get("/api/v1/solicitantes/export")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void export_comoRiskManager_retorna403() throws Exception {
        mockMvc.perform(get("/api/v1/solicitantes/export")
                        .with(user("rm").roles("RISK_MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void list_comoAdmin_retorna200_compatibilidadHU002() throws Exception {
        // ADMIN conserva acceso al listado (compatibilidad con HU-002)
        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void list_comoRiskManager_retorna200_compatibilidadHU002() throws Exception {
        // RISK_MANAGER conserva acceso al listado (compatibilidad con HU-002)
        mockMvc.perform(get("/api/v1/solicitantes")
                        .with(user("rm").roles("RISK_MANAGER")))
                .andExpect(status().isOk());
    }

    // ===== AC-17: La paginación no carga todos los registros en memoria =====

    @Test
    void list_conMuchosRegistros_retornaSoloPaginaSolicitada() throws Exception {
        for (int i = 1; i <= 30; i++) {
            registrar("17" + String.format("%05d", i), "Muchos " + i, "Empleado", 3500000, 24);
        }

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("page", "0")
                        .param("size", "10")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.page.totalElements").value(30))
                .andExpect(jsonPath("$.page.totalPages").value(greaterThanOrEqualTo(3)));
    }

    // ===== AC-18: Parámetro q combinado con otros filtros =====

    @Test
    void list_qMasFiltroIngresos_combinaAmbosCriterios() throws Exception {
        // Carlos Rico cumple: nombre contiene "Carlos" Y ingresos >= 5000000
        registrar("18001", "Carlos Rico", "Empleado", 7000000, 48);
        // Carlos Pobre: nombre contiene "Carlos" pero NO cumple el filtro de ingresos
        registrar("18002", "Carlos Pobre", "Empleado", 1000000, 12);
        // Pedro Rico: cumple ingresos pero nombre NO contiene "Carlos"
        registrar("18003", "Pedro Rico", "Empleado", 7000000, 48);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "Carlos")
                        .param("ingresos_min", "5000000")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Carlos Rico"));
    }

    @Test
    void list_soloQ_buscaPorNombrePreservandoCompatibilidadHU002() throws Exception {
        registrar("18011", "Maria Lopez", "Empleado", 3500000, 24);
        registrar("18012", "Pedro Ramirez", "Empleado", 3500000, 24);

        mockMvc.perform(get("/api/v1/solicitantes")
                        .param("q", "maria")
                        .with(user("analyst").roles("ANALYST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].nombre").value("Maria Lopez"));
    }
}
