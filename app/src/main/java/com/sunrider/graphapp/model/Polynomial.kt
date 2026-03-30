package com.sunrider.graphapp.model

import kotlin.math.abs

fun Int.toSuperscript(): String {
    val superscriptDigits = charArrayOf('⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹')
    return this.toString().map { superscriptDigits[it - '0'] }.joinToString("")
}

data class Polynomial(
    val coefficients: Map<Int, Long> = emptyMap()
) {
    companion object {
        fun zero() = Polynomial()
        fun monomial(coefficient: Long, power: Int) =
            if (coefficient == 0L) zero()
            else Polynomial(mapOf(power to coefficient))

        fun kPow(n: Int) = monomial(1, n)

        // k(k-1)(k-2)...(k-n+1) — нисходящий факториал
        fun fallingFactorial(n: Int): Polynomial {
            if (n == 0) return monomial(1, 0)
            var result = Polynomial(mapOf(1 to 1L)) // k
            for (i in 1 until n) {
                // умножаем на (k - i)
                result = result * Polynomial(mapOf(1 to 1L, 0 to -i.toLong()))
            }
            return result
        }
    }

    val isZero get() = coefficients.all { it.value == 0L }
    val degree get() = coefficients.filter { it.value != 0L }.keys.maxOrNull() ?: 0

    operator fun plus(other: Polynomial): Polynomial {
        val result = coefficients.toMutableMap()
        other.coefficients.forEach { (power, coeff) ->
            result[power] = (result[power] ?: 0L) + coeff
        }
        return Polynomial(result.filter { it.value != 0L })
    }

    operator fun minus(other: Polynomial): Polynomial {
        val result = coefficients.toMutableMap()
        other.coefficients.forEach { (power, coeff) ->
            result[power] = (result[power] ?: 0L) - coeff
        }
        return Polynomial(result.filter { it.value != 0L })
    }

    operator fun times(scalar: Long): Polynomial {
        if (scalar == 0L) return zero()
        return Polynomial(coefficients.mapValues { it.value * scalar }.filter { it.value != 0L })
    }

    operator fun times(other: Polynomial): Polynomial {
        if (isZero || other.isZero) return zero()
        val result = mutableMapOf<Int, Long>()
        coefficients.forEach { (p1, c1) ->
            other.coefficients.forEach { (p2, c2) ->
                val power = p1 + p2
                result[power] = (result[power] ?: 0L) + c1 * c2
            }
        }
        return Polynomial(result.filter { it.value != 0L })
    }

    fun evaluate(k: Long): Long {
        var result = 0L
        coefficients.forEach { (power, coeff) ->
            var term = coeff
            repeat(power) { term *= k }
            result += term
        }
        return result
    }

    override fun toString(): String {
        if (isZero) return "0"
        val sorted = coefficients.filter { it.value != 0L }.entries.sortedByDescending { it.key }
        return buildString {
            sorted.forEachIndexed { index, (power, coeff) ->
                val absCoeff = abs(coeff)
                if (index == 0) {
                    if (coeff < 0) append("-")
                } else {
                    append(if (coeff < 0) " - " else " + ")
                }
                when {
                    power == 0 -> append(absCoeff)
                    power == 1 && absCoeff == 1L -> append("x")
                    power == 1 -> append("${absCoeff}x")
                    absCoeff == 1L -> append("x${power.toSuperscript()}")
                    else -> append("${absCoeff}x${power.toSuperscript()}")
                }
            }
        }
    }

    fun toDisplayString(): String {
        if (isZero) return "P(G, x) = 0"
        return "P(G, x) = $this"
    }
}
