package co.udea.codefactory.creditscoring.financialdata.application.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.udea.codefactory.creditscoring.applicant.domain.port.out.ApplicantRepositoryPort;
import co.udea.codefactory.creditscoring.financialdata.application.dto.FinancialDataComparison;
import co.udea.codefactory.creditscoring.financialdata.domain.exception.InvalidFinancialDataException;
import co.udea.codefactory.creditscoring.financialdata.domain.model.FinancialData;
import co.udea.codefactory.creditscoring.financialdata.domain.port.in.GetFinancialHistoryUseCase;
import co.udea.codefactory.creditscoring.financialdata.domain.port.out.FinancialDataRepositoryPort;
import co.udea.codefactory.creditscoring.shared.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class GetFinancialHistoryService implements GetFinancialHistoryUseCase {

    private final FinancialDataRepositoryPort financialDataRepositoryPort;
    private final ApplicantRepositoryPort applicantRepositoryPort;

    public GetFinancialHistoryService(
            FinancialDataRepositoryPort financialDataRepositoryPort,
            ApplicantRepositoryPort applicantRepositoryPort) {
        this.financialDataRepositoryPort = financialDataRepositoryPort;
        this.applicantRepositoryPort = applicantRepositoryPort;
    }

    @Override
    public List<FinancialData> obtenerHistorial(UUID solicitanteId) {
        verificarSolicitanteExiste(solicitanteId);
        return financialDataRepositoryPort.findAllByApplicantId(solicitanteId);
    }

    @Override
    public FinancialDataComparison comparar(UUID solicitanteId, int version1, int version2) {
        if (version1 == version2) {
            throw new InvalidFinancialDataException("Las versiones a comparar deben ser diferentes");
        }
        verificarSolicitanteExiste(solicitanteId);

        // La versión de menor número es la base (más antigua); la de mayor número es la comparada
        int versionBase = Math.min(version1, version2);
        int versionComparada = Math.max(version1, version2);

        FinancialData base = financialDataRepositoryPort
                .findByApplicantIdAndVersion(solicitanteId, versionBase)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialData", "version", versionBase));

        FinancialData comparada = financialDataRepositoryPort
                .findByApplicantIdAndVersion(solicitanteId, versionComparada)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialData", "version", versionComparada));

        return computarComparacion(base, comparada);
    }

    // --------------------------------------------------------------------------
    // Lógica de comparación
    // --------------------------------------------------------------------------

    private FinancialDataComparison computarComparacion(FinancialData base, FinancialData comparada) {
        List<FinancialDataComparison.CampoComparado> camposModificados = new ArrayList<>();

        // Ingresos anuales: mayor es mejor
        agregarCampoDecimal(camposModificados, "ingresos_anuales",
                base.annualIncome(), comparada.annualIncome(), true);

        // Gastos mensuales: menor es mejor
        agregarCampoDecimal(camposModificados, "gastos_mensuales",
                base.monthlyExpenses(), comparada.monthlyExpenses(), false);

        // Deuda actual: menor es mejor
        agregarCampoDecimal(camposModificados, "deuda_actual",
                base.currentDebts(), comparada.currentDebts(), false);

        // Activos: mayor es mejor
        agregarCampoDecimal(camposModificados, "activos",
                base.assetsValue(), comparada.assetsValue(), true);

        // Patrimonio declarado: mayor es mejor
        agregarCampoDecimal(camposModificados, "patrimonio",
                base.declaredPatrimony(), comparada.declaredPatrimony(), true);

        // Moras 12 meses: menor es mejor
        agregarCampoEntero(camposModificados, "moras_12m",
                base.defaultsLast12m(), comparada.defaultsLast12m(), false);

        // Moras 24 meses: menor es mejor
        agregarCampoEntero(camposModificados, "moras_24m",
                base.defaultsLast24m(), comparada.defaultsLast24m(), false);

        // Productos crediticios vigentes: menor es mejor (menor exposición crediticia)
        agregarCampoEntero(camposModificados, "productos_vigentes",
                base.activeCreditProducts(), comparada.activeCreditProducts(), false);

        // Score de bureau: mayor es mejor (solo si al menos una versión tiene dato)
        if (base.externalBureauScore() != null || comparada.externalBureauScore() != null) {
            String valorBase = base.externalBureauScore() != null
                    ? base.externalBureauScore().toString() : "No disponible";
            String valorComp = comparada.externalBureauScore() != null
                    ? comparada.externalBureauScore().toString() : "No disponible";
            String estado = computarEstadoScore(base.externalBureauScore(), comparada.externalBureauScore());
            if (!"SIN_CAMBIO".equals(estado)) {
                camposModificados.add(new FinancialDataComparison.CampoComparado(
                        "score_bureau", valorBase, valorComp, estado));
            }
        }

        String tendencia = calcularTendencia(base, comparada);
        return new FinancialDataComparison(base, comparada, camposModificados, tendencia);
    }

    /**
     * Agrega un campo decimal a la lista si cambió entre versiones.
     * {@code mayorEsMejor = true} significa que un valor más alto es una mejora.
     */
    private void agregarCampoDecimal(List<FinancialDataComparison.CampoComparado> lista,
            String campo, BigDecimal valorBase, BigDecimal valorComp, boolean mayorEsMejor) {
        int diferencia = valorComp.compareTo(valorBase);
        if (diferencia == 0) {
            return; // No hubo cambio; no se incluye en la lista
        }
        String estado = (diferencia > 0) == mayorEsMejor ? "MEJORA" : "DETERIORO";
        lista.add(new FinancialDataComparison.CampoComparado(
                campo, valorBase.toPlainString(), valorComp.toPlainString(), estado));
    }

    /**
     * Agrega un campo entero a la lista si cambió entre versiones.
     * {@code mayorEsMejor = true} significa que un valor más alto es una mejora.
     */
    private void agregarCampoEntero(List<FinancialDataComparison.CampoComparado> lista,
            String campo, int valorBase, int valorComp, boolean mayorEsMejor) {
        int diferencia = Integer.compare(valorComp, valorBase);
        if (diferencia == 0) {
            return;
        }
        String estado = (diferencia > 0) == mayorEsMejor ? "MEJORA" : "DETERIORO";
        lista.add(new FinancialDataComparison.CampoComparado(
                campo, String.valueOf(valorBase), String.valueOf(valorComp), estado));
    }

    /**
     * Computa el estado del score de bureau, manejando casos en que el dato
     * estaba ausente en una o ambas versiones.
     */
    private String computarEstadoScore(Integer scoreBase, Integer scoreComp) {
        if (scoreBase == null && scoreComp == null) {
            return "SIN_CAMBIO";
        }
        if (scoreBase == null) {
            return "MEJORA"; // Ahora tiene score de bureau (antes no disponible)
        }
        if (scoreComp == null) {
            return "DETERIORO"; // Dejó de tener score de bureau
        }
        int diferencia = Integer.compare(scoreComp, scoreBase);
        if (diferencia == 0) {
            return "SIN_CAMBIO";
        }
        return diferencia > 0 ? "MEJORA" : "DETERIORO";
    }

    /**
     * Calcula la tendencia general de acuerdo con RN1 de HU-005:
     * se considera MEJORA si el ratio deuda/ingreso disminuye Y el score de bureau aumenta.
     * Cuando el score de bureau no está disponible en ambas versiones, se usa solo el ratio.
     */
    private String calcularTendencia(FinancialData base, FinancialData comparada) {
        int comparacionRatio = comparada.debtToIncomeRatio().compareTo(base.debtToIncomeRatio());

        if (base.externalBureauScore() != null && comparada.externalBureauScore() != null) {
            boolean ratioMejoro = comparacionRatio < 0;
            boolean scoreMejoro = comparada.externalBureauScore() > base.externalBureauScore();

            if (ratioMejoro && scoreMejoro) {
                return "MEJORA";
            }
            if (!ratioMejoro && !scoreMejoro && comparacionRatio != 0) {
                return "DETERIORO";
            }
            return "ESTABLE";
        }

        // Sin score de bureau en ambas versiones: tendencia basada solo en el ratio de deuda
        if (comparacionRatio < 0) {
            return "MEJORA";
        }
        if (comparacionRatio > 0) {
            return "DETERIORO";
        }
        return "ESTABLE";
    }

    private void verificarSolicitanteExiste(UUID solicitanteId) {
        if (applicantRepositoryPort.findById(solicitanteId).isEmpty()) {
            throw new ResourceNotFoundException("Applicant", "id", solicitanteId);
        }
    }
}
