package id.ac.binus.bounty.utils;

import android.view.View;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import id.ac.binus.bounty.R;
import id.ac.binus.bounty.models.Challenge;

/** Shared card/detail presentation; reward is always labelled as a demo. */
public final class ChallengeDisplay {
    private ChallengeDisplay() { }
    public static String rupiah(Context context, double amount) {
        return context.getString(R.string.rupiah_amount,
                NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount));
    }
    public static void bind(View root, Challenge challenge) {
        ((TextView) root.findViewById(R.id.challenge_title)).setText(challenge.title);
        ((TextView) root.findViewById(R.id.challenge_creator)).setText(challenge.creatorName);
        ((TextView) root.findViewById(R.id.challenge_reward)).setText(
                rupiah(root.getContext(), challenge.reward));
        ((TextView) root.findViewById(R.id.challenge_status)).setText(challenge.status);
        String deadline = challenge.deadline;
        try {
            deadline = LocalDate.parse(deadline).format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID")));
        } catch (DateTimeParseException ignored) {
            // Keep non-ISO legacy/mock dates readable rather than hiding them.
        }
        ((TextView) root.findViewById(R.id.challenge_deadline)).setText(
                root.getContext().getString(R.string.challenge_deadline, deadline));
        ImageView avatar = root.findViewById(R.id.challenge_avatar);
        Glide.with(avatar).load(challenge.creatorAvatar).placeholder(R.drawable.ic_avatar)
                .error(R.drawable.ic_avatar).circleCrop().into(avatar);
    }
}
