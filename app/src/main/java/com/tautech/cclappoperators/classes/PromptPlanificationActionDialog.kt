package com.tautech.cclappoperators.classes

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.tautech.cclappoperators.R

class PromptPlanificationActionDialog(val items: Array<String>, val listener: DialogInterface.OnClickListener) : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.i("PromptDialog", "${items.size} items $items")
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setTitle(R.string.select_an_action)
            builder.setItems(items, listener)
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}