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
 * Habilidad offline para realizar llamadas o abrir el marcador telefónico hacia contactos o números.
 */
class PhoneCallSkill : StandardRecognizerSkill(
    info = SkillInfo(
        id = "phone_call_skill",
        name = "Llamadas Telefónicas",
        description = "Llama a un contacto de tu agenda o a un número telefónico especificado.",
        neededPermissions = listOf(android.Manifest.permission.READ_CONTACTS)
    ),
    specificity = Specificity.HIGH
) {

    override val patterns: List<Construct> = listOf(
        // "llama a [contacto/número]", "marcar a [contacto]"
        SequenceConstruct(
            WordConstruct(
                "llama", "llamame", "llamar",
                "marca", "marcame", "marcar",
                "comunicate", "comunicar"
            ),
            OptionalConstruct(WordConstruct("a", "al", "con")),
            OptionalConstruct(WordConstruct("el", "la", "mi", "contacto")),
            OptionalConstruct(WordConstruct("numero", "telefono")),
            CapturingConstruct("target")
        ),
        // "haz una llamada a [contacto]"
        SequenceConstruct(
            WordConstruct("haz", "hazme", "hacer", "inicia", "iniciar"),
            OptionalConstruct(WordConstruct("una")),
            WordConstruct("llamada"),
            OptionalConstruct(WordConstruct("a", "al", "con", "para")),
            OptionalConstruct(WordConstruct("el", "la", "mi")),
            CapturingConstruct("target")
        )
    )

    override suspend fun execute(context: SkillContext, input: String, score: SkillScore): SkillOutput {
        val target = score.capturedSlots["target"]?.trim()
            ?: return SkillOutput("¿A quién deseas llamar?", success = false)

        val cleanTarget = target.trim().removeSurrounding("\"", "'")

        // 1. Si el objetivo es un número directo (dígitos y opcionalmente '+')
        val digitCount = cleanTarget.count { it.isDigit() }
        if (digitCount >= 3 && digitCount >= cleanTarget.length / 2) {
            val phoneNumber = cleanTarget.filter { it.isDigit() || it == '+' }
            return launchDialer(context.androidContext, phoneNumber, phoneNumber)
        }

        // 2. Si el objetivo es un nombre, buscar en la libreta de contactos
        val contactMatch = findContactNumber(context.androidContext, cleanTarget)

        return if (contactMatch != null) {
            launchDialer(context.androidContext, contactMatch.number, contactMatch.name)
        } else {
            // Abrir libreta de contactos o marcador si no se encuentra
            try {
                val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.androidContext.startActivity(intent)
                val msg = "No encontré el número de '$cleanTarget'. Abrí tus contactos para buscarlo."
                SkillOutput(speech = msg, displayText = msg, success = true)
            } catch (e: Exception) {
                val msg = "No encontré a '$cleanTarget' en tu agenda de contactos."
                SkillOutput(speech = msg, displayText = msg, success = false)
            }
        }
    }

    private fun launchDialer(context: Context, number: String, displayName: String): SkillOutput {
        return try {
            val uri = Uri.parse("tel:$number")
            // Usar ACTION_DIAL para abrir el marcador con el número listo (compatible sin permisos peligrosos)
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            val msg = "Llamando a $displayName."
            SkillOutput(speech = msg, displayText = msg, success = true)
        } catch (e: Exception) {
            val err = "No se pudo iniciar la llamada a $displayName: ${e.message}"
            SkillOutput(speech = err, displayText = err, success = false)
        }
    }

    private data class ContactResult(val name: String, val number: String)

    private fun findContactNumber(context: Context, contactName: String): ContactResult? {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return null

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$contactName%")

        return try {
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val foundName = if (nameIdx != -1) cursor.getString(nameIdx) else contactName
                    val foundNum = if (numIdx != -1) cursor.getString(numIdx) else null
                    if (foundNum != null) ContactResult(foundName, foundNum) else null
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
