package id.ac.binus.bounty.utils

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import id.ac.binus.bounty.R

class ActionConfirmation : AppCompatDialogFragment() {
    override fun onCreateDialog(state: Bundle?): Dialog {
        val args = requireArguments()
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(args.getString("title"))
            .setMessage(args.getString("message"))
            .setNegativeButton(R.string.cancel_action) { _, _ -> }
            .setPositiveButton(args.getString("action")) { _, _ ->
                parentFragmentManager.setFragmentResult(
                    args.getString("key")!!,
                    Bundle(args.getBundle("data")!!)
                )
            }
            .create()
    }

    companion object {
        @JvmStatic
        fun show(manager: FragmentManager, key: String, title: String, message: String, action: String, data: Bundle) {
            if (manager.isStateSaved || manager.findFragmentByTag(key) != null) return
            val args = Bundle().apply {
                putString("key", key)
                putString("title", title)
                putString("message", message)
                putString("action", action)
                putBundle("data", data)
            }
            ActionConfirmation().apply {
                arguments = args
                showNow(manager, key)
            }
        }
    }
}
