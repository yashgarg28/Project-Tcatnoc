package com.example.tempcontacts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

object ContactExporter {

    // This will be set by the caller (e.g., ContactDetailScreen)
    var contactsPermissionLauncher: ActivityResultLauncher<Array<String>>? = null

    /**
     * Export a contact to device's phone book
     * Requests permissions if needed, then opens phone book
     */
    fun exportToPhoneBook(context: Context, contact: Contact) {
        if (hasContactsPermissions(context)) {
            // Permissions already granted, proceed with export
            performExport(context, contact)
        } else {
            // Permissions not granted, request them
            if (contactsPermissionLauncher != null) {
                contactsPermissionLauncher!!.launch(
                    arrayOf(
                        Manifest.permission.READ_CONTACTS,
                        Manifest.permission.WRITE_CONTACTS
                    )
                )
            } else {
                Toast.makeText(
                    context,
                    "Contact permissions required. Please enable in settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Perform the actual export to phone book
     */
    fun performExport(context: Context, contact: Contact) {
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                type = ContactsContract.Contacts.CONTENT_TYPE

                // Pre-fill contact data
                putExtra(ContactsContract.Intents.Insert.NAME, contact.name)
                putExtra(ContactsContract.Intents.Insert.PHONE, contact.phone)

                // Add email if available
                if (contact.email.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.EMAIL, contact.email)
                }

                // Add address if available
                if (contact.address.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.POSTAL, contact.address)
                }

                // Add notes if available
                if (contact.notes.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.NOTES, contact.notes)
                }
            }

            context.startActivity(intent)
            Toast.makeText(
                context,
                "Opening phone book. Please save the contact.",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Check if both READ_CONTACTS and WRITE_CONTACTS permissions are granted
     */
    fun hasContactsPermissions(context: Context): Boolean {
        val readContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        val writeContactsPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED

        return readContactsPerm && writeContactsPerm
    }

    /**
     * Check if a contact already exists in phone book
     */
    fun checkIfContactExists(context: Context, phoneNumber: String): Boolean {
        return try {
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
            val selectionArgs = arrayOf(phoneNumber.replace(Regex("[^0-9]"), ""))

            context.contentResolver.query(
                uri, projection, selection, selectionArgs, null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
