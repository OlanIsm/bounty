package id.ac.binus.bounty.utils;

import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import id.ac.binus.bounty.R;

/** Keep forms clear of system bars and the keyboard without losing screen padding. */
public final class ScreenInsets {
    private ScreenInsets() {}

    public static void apply(AppCompatActivity activity) {
        EdgeToEdge.enable(activity);
        View root = activity.findViewById(R.id.main);
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(left + safe.left, top + safe.top,
                    right + safe.right, bottom + safe.bottom);
            // Root handles safe padding; do not let Material navigation add it a second time.
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }
}
