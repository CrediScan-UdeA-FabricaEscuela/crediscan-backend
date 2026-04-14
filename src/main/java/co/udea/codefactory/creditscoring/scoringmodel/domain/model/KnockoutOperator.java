package co.udea.codefactory.creditscoring.scoringmodel.domain.model;

import java.math.BigDecimal;

/**
 * Operadores de comparación para reglas de exclusión automática (knockout).
 * Cada operador evalúa si un valor observado activa la regla respecto al umbral configurado.
 */
public enum KnockoutOperator {

    /** Mayor estricto: valor > umbral */
    GT {
        @Override
        public boolean evaluar(BigDecimal valorObservado, BigDecimal umbral) {
            return valorObservado.compareTo(umbral) > 0;
        }
    },
    /** Menor estricto: valor < umbral */
    LT {
        @Override
        public boolean evaluar(BigDecimal valorObservado, BigDecimal umbral) {
            return valorObservado.compareTo(umbral) < 0;
        }
    },
    /** Mayor o igual: valor >= umbral */
    GTE {
        @Override
        public boolean evaluar(BigDecimal valorObservado, BigDecimal umbral) {
            return valorObservado.compareTo(umbral) >= 0;
        }
    },
    /** Menor o igual: valor <= umbral */
    LTE {
        @Override
        public boolean evaluar(BigDecimal valorObservado, BigDecimal umbral) {
            return valorObservado.compareTo(umbral) <= 0;
        }
    },
    /** Igual: valor == umbral */
    EQ {
        @Override
        public boolean evaluar(BigDecimal valorObservado, BigDecimal umbral) {
            return valorObservado.compareTo(umbral) == 0;
        }
    },
    /** Distinto: valor != umbral */
    NEQ {
        @Override
        public boolean evaluar(BigDecimal valorObservado, BigDecimal umbral) {
            return valorObservado.compareTo(umbral) != 0;
        }
    };

    /**
     * Evalúa si el {@code valorObservado} activa esta regla con respecto al {@code umbral}.
     *
     * @return {@code true} si la condición se cumple (la regla se activa).
     */
    public abstract boolean evaluar(BigDecimal valorObservado, BigDecimal umbral);
}
