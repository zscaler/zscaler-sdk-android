package com.zscaler.sdk.demoapp.view

import android.content.Context
import androidx.appcompat.app.AlertDialog

object ZdkDialog {
    fun showMessageDialog(context: Context, message: String) {
        // Create a new AlertDialog builder
        val builder = AlertDialog.Builder(context)

        // Set the message for the dialog
        builder.setMessage(message)

        // Add a button to the dialog
        builder.setPositiveButton("OK") { dialog, which ->
            // Dismiss the dialog when the button is clicked
            dialog.dismiss()
        }

        // Create and show the dialog
        val dialog = builder.create()
        dialog.show()
    }
}