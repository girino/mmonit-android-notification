package org.girino.mmonit.domain

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class MMonitStatusCounts(
    val failed: Int = 0,
    val servicesFailed: Int = 0,
    val servicesFailedOrUnmonitored: Int = 0,
    val inactive: Int = 0,
    val ignored: Int = 0,
    val ok: Int = 0,
)

class InvalidMMonitPayloadException(message: String) : IllegalArgumentException(message)

object MMonitStatusParser {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(
        payload: String,
        checkedAt: Long = System.currentTimeMillis(),
    ): MMonitStatusSnapshot {
        val root = try {
            json.parseToJsonElement(payload) as? JsonObject
                ?: throw InvalidMMonitPayloadException("A resposta não é um objeto JSON.")
        } catch (exception: InvalidMMonitPayloadException) {
            throw exception
        } catch (exception: Exception) {
            throw InvalidMMonitPayloadException("A resposta não contém JSON válido.")
        }

        val statusArray = root["status"] as? JsonArray
            ?: throw InvalidMMonitPayloadException("A resposta não contém a lista de status.")
        if (statusArray.isEmpty()) {
            throw InvalidMMonitPayloadException("A lista de status está vazia.")
        }

        var counts = MMonitStatusCounts()
        var recognizedEntries = 0

        statusArray.forEach { element ->
            val status = element as? JsonObject ?: return@forEach
            val label = (status["label"] as? JsonPrimitive)
                ?.content
                ?.trim()
                ?.lowercase()
                ?: return@forEach
            val count = parseCount(status["data"])

            when (label) {
                "failed" -> {
                    counts = counts.copy(failed = count)
                    recognizedEntries++
                }

                "services failed" -> {
                    counts = counts.copy(servicesFailed = count)
                    recognizedEntries++
                }

                "some services failed/unmonitored" -> {
                    counts = counts.copy(servicesFailedOrUnmonitored = count)
                    recognizedEntries++
                }

                "inactive" -> {
                    counts = counts.copy(inactive = count)
                    recognizedEntries++
                }

                "ignored" -> {
                    counts = counts.copy(ignored = count)
                    recognizedEntries++
                }

                "ok" -> {
                    counts = counts.copy(ok = count)
                    recognizedEntries++
                }
            }
        }

        if (recognizedEntries == 0) {
            throw InvalidMMonitPayloadException("A resposta não contém status reconhecido.")
        }

        val level = when {
            counts.failed > 0 -> MMonitLevel.RED
            counts.servicesFailed > 0 -> MMonitLevel.YELLOW
            counts.servicesFailedOrUnmonitored > 0 -> MMonitLevel.ORANGE
            counts.inactive > 0 || counts.ignored > 0 -> MMonitLevel.GRAY
            else -> MMonitLevel.GREEN
        }

        return MMonitStatusSnapshot(
            level = level,
            detail = detailFor(counts),
            checkedAt = checkedAt,
        )
    }

    private fun parseCount(element: kotlinx.serialization.json.JsonElement?): Int {
        val primitive = element as? JsonPrimitive ?: return 0
        return primitive.content.toIntOrNull()?.coerceAtLeast(0) ?: 0
    }

    private fun detailFor(counts: MMonitStatusCounts): String {
        val details = buildList {
            if (counts.failed > 0) add("${counts.failed} host(s) com falha crítica")
            if (counts.servicesFailed > 0) add("${counts.servicesFailed} host(s) com serviços em falha")
            if (counts.servicesFailedOrUnmonitored > 0) {
                add("${counts.servicesFailedOrUnmonitored} host(s) com serviços em alerta")
            }
            if (counts.inactive > 0) add("${counts.inactive} host(s) inativo(s)")
            if (counts.ignored > 0) add("${counts.ignored} host(s) ignorado(s)")
        }

        return when {
            details.isNotEmpty() -> details.joinToString(separator = "\n")
            counts.ok > 0 -> "${counts.ok} host(s) estão OK"
            else -> "Nenhum alerta ativo"
        }
    }
}
