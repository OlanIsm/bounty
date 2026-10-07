package id.ac.binus.bounty.utils;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import id.ac.binus.bounty.R;

/** Native saved dialog state; confirmation sends only the captured action to its owning activity. */
public class ActionConfirmation extends AppCompatDialogFragment {
    public static void show(FragmentManager manager, String key, String title, String message,
            String action, Bundle data) {
        if (manager.isStateSaved() || manager.findFragmentByTag(key) != null) return;
        Bundle args = new Bundle();
        args.putString("key", key); args.putString("title", title);
        args.putString("message", message); args.putString("action", action); args.putBundle("data", data);
        ActionConfirmation dialog = new ActionConfirmation();
        dialog.setArguments(args);
        dialog.showNow(manager, key);
    }
    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        Bundle args = requireArguments();
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(args.getString("title")).setMessage(args.getString("message"))
                .setNegativeButton(R.string.cancel_action, (dialog, which) -> { })
                .setPositiveButton(args.getString("action"), (dialog, which) ->
                        getParentFragmentManager().setFragmentResult(args.getString("key"),
                                new Bundle(args.getBundle("data"))))
                .create();
    }
}
