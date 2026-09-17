package org.girino.mmonit.domain

enum class MMonitLevel(
    val title: String,
    val colorArgb: Long,
) {
    GREEN("Tudo OK", 0xFF2E7D32),
    ORANGE("Serviços com alerta", 0xFFEF6C00),
    YELLOW("Serviços com falha", 0xFFF9A825),
    RED("Falha crítica", 0xFFC62828),
    GRAY("Estado inativo ou ignorado", 0xFF757575),
    BLACK("Indisponível", 0xFF212121),
    UNKNOWN("Aguardando consulta", 0xFF78909C),
}

data class MMonitStatusSnapshot(
    val level: MMonitLevel,
    val detail: String,
    val checkedAt: Long,
) {
    companion object {
        fun initial(): MMonitStatusSnapshot = MMonitStatusSnapshot(
            level = MMonitLevel.UNKNOWN,
            detail = "Configure um servidor M/Monit para começar.",
            checkedAt = 0L,
        )

        fun unavailable(detail: String, checkedAt: Long = System.currentTimeMillis()) =
            MMonitStatusSnapshot(
                level = MMonitLevel.BLACK,
                detail = detail,
                checkedAt = checkedAt,
            )
    }
}
