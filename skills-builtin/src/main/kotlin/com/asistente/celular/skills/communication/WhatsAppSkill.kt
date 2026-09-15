package com.asistente.celular.skills.communication

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.asistente.celular.nlu.construct.CapturingConstruct
import com.asistente.celular.nlu.construct.Construct
import com.asistente.celular.nlu.construct.OptionalConstruct
import com.asistente.celular.nlu.construct.SequenceConstruct
import com.asistente.celular.nlu.construct.WordConstruct
import com.asistente.celular.nlu.model.SkillScore
import com.asistente.celular.nlu.model.Specificity
import com.asistente.celular.nlu.skill.SkillContext
import com.asistente.celular.nlu.skill.SkillInfo
import com.asistente.celular.nlu.skill.SkillOutput
import com.asistente.celular.nlu.skill.StandardRecognizerSkill

/**
 * Habilidad offline para redactar y enviar mensajes por WhatsApp.
 */
class WhatsAppSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "whatsapp_skill",
        name = "Mensajería WhatsApp",
        description = "Envía mensajes de texto a través de WhatsApp con destinatario y contenido prellenado.",
        neededPermissions = listOf(android.Manifest.permission.READ_CONTACTS)
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // "envía un whatsapp a [destinatario] diciendo [mensaje]"
        SequenceConstruct(
            OptionalConstruct(
                WordConstruct(
                    "manda", "mandame", "mandar",
                    "envia", "enviame", "enviar",
                    "escribe", "escribeme", "escribir"
                )
            ),
            OptionalConstruct(WordConstruct("un")),
            OptionalConstruct(WordConstruct("mensaje")),
            OptionalConstruct(WordConstruct("de", "por")),
            WordConstruct("whatsapp", "wasap", "guasap"),
            OptionalConstruct(WordConstruct("a", "al", "con", "para")),
            CapturingConstruct("query")
        ),
        // "escribe a [destinatario] por whatsapp [mensaje]"
        SequenceConstruct(
            WordConstruct("escribe", "escribeme", "escribir", "manda", "mandar", "envia", "enviar"),
            OptionalConstruct(WordConstruct("a", "al", "para")),
            CapturingConstruct("contact", stopAtConstruct = WordConstruct("por", "en")),
            WordConstruct("por", "en"),
            WordConstruct("whatsapp", "wasap", "guasap"),
            CapturingConstruct("message")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        var contact = score.capturedSlots["contact"]?.trim()
        var message = score.capturedSlots["message"]?.trim() ?: ""

        val query = score.capturedSlots["query"]?.trim()
        if (contact == null && query != null) {
            val parsed = parseContactAndMessage(query)
            contact = parsed.first
            message = parsed.second
        }

        if (contact.isNullOrBlank()) {
            return SkillOutput("¿A quién deseas enviar el mensaje de WhatsApp?", success = false)
        }

        val cleanContact = contact.trim().trimStart('a', ' ').trim()

        // Buscar si existe número asociado al contacto en la agenda
        val phoneNumber = findContactPhoneNumber(context.androidContext, cleanContact)

        return try {
            if (phoneNumber != null && phoneNumber.isNotBlank()) {
                val digitsOnly = phoneNumber.filter { it.isDigit() }
                val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$digitsOnly&text=${Uri.encode(message)}")
                val intent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.whatsapp")
                }
                context.androidContext.startActivity(intent)
            } else {
                // Si no hay número directo, compartir texto con la app de WhatsApp para seleccionar chat
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, message)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    setPackage("com.whatsapp")
                }
                context.androidContext.startActivity(sendIntent)
            }

            val msg = if (message.isNotBlank()) {
                "Abriendo WhatsApp para enviar mensaje a $cleanContact: \"$message\""
            } else {
                "Abriendo chat de WhatsApp con $cleanContact."
            }
            SkillOutput(speech = msg, displayText = msg, success = true)

        } catch (e: Exception) {
            // Si WhatsApp no está instalado, intentar abrir en navegador web
            try {
                val fallbackUri = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
                val browserIntent = Intent(Intent.ACTION_VIEW, fallbackUri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.androidContext.startActivity(browserIntent)
                val msg = "Abriendo WhatsApp en el navegador."
                SkillOutput(speech = msg, displayText = msg, success = true)
            } catch (ex: Exception) {
                val err = "No se pudo abrir WhatsApp: ${ex.message}"
                SkillOutput(speech = err, displayText = err, success = false)
            }
        }
    }

    private fun parseContactAndMessage(query: String): Pair<String, String> {
        val connectors = listOf(
            " diciendo que ",
            " diciendo ",
            " que diga que ",
            " que diga ",
            " dile que ",
            " diles que ",
            " con el mensaje ",
            " mensaje "
        )

        for (connector in connectors) {
            val idx = query.indexOf(connector, ignoreCase = true)
            if (idx != -1) {
                val contactPart = query.substring(0, idx).trim()
                val messagePart = query.substring(idx + connector.length).trim()
                return Pair(contactPart, messagePart)
            }
        }

        // Si no hay conectores explícitos, separar primera palabra como destinatario si hay varias
        val words = query.split("\\s+".toRegex())
        return if (words.size >= 2) {
            Pair(words.first(), words.drop(1).joinToString(" "))
        } else {
            Pair(query, "")
        }
    }

    private fun findContactPhoneNumber(context: Context, contactName: String): String? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    if (numIdx != -1) cursor.getString(numIdx) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
