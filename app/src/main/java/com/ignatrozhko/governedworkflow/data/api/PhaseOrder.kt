package com.ignatrozhko.governedworkflow.data.api

/**
 * Orders phase ids of mixed depth (`3.1` for a sub-phase, `3.1.0` for its steps) segment by segment as numbers;
 * plain string order would put `10` before `2`.
 */
val PhaseOrder = Comparator<String> { first, second ->
    val firstParts = first.split('.')
    val secondParts = second.split('.')
    firstParts.zip(secondParts)
        .map { (a, b) -> compareSegments(a, b) }
        .firstOrNull { it != 0 }
        ?: firstParts.size.compareTo(secondParts.size)
}

private fun compareSegments(first: String, second: String): Int {
    val firstNumber = first.toIntOrNull()
    val secondNumber = second.toIntOrNull()
    return if (firstNumber != null && secondNumber != null) firstNumber.compareTo(secondNumber) else first.compareTo(second)
}
