package id.ac.binus.bounty.utils

import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import id.ac.binus.bounty.R
import id.ac.binus.bounty.models.Challenge
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.text.NumberFormat
import java.util.Locale

object ChallengeDisplay {
    @JvmStatic
    fun rupiah(context: android.content.Context, amount: Double): String = context.getString(
        R.string.rupiah_amount,
        NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(amount)
    )

    @JvmStatic
    fun bindStatus(badge: TextView, status: String) {
        val colors = when (status) {
            "OPEN" -> R.color.status_open_bg to R.color.status_open_fg
            "ACCEPTED" -> R.color.status_accepted_bg to R.color.status_accepted_fg
            "SUBMITTED", "PENDING" -> R.color.status_submitted_bg to R.color.status_submitted_fg
            "COMPLETED", "APPROVED" -> R.color.status_completed_bg to R.color.status_completed_fg
            "REJECTED" -> R.color.status_rejected_bg to R.color.status_rejected_fg
            else -> R.color.status_neutral_bg to R.color.status_neutral_fg
        }
        badge.background = GradientDrawable().apply {
            cornerRadius = 8 * badge.resources.displayMetrics.density
            setColor(ContextCompat.getColor(badge.context, colors.first))
        }
        badge.setTextColor(ContextCompat.getColor(badge.context, colors.second))
        badge.text = status
    }

    @JvmStatic
    fun bind(root: View, challenge: Challenge) {
        root.findViewById<TextView>(R.id.challenge_title).text = challenge.title
        root.findViewById<TextView>(R.id.challenge_creator).text = challenge.creatorName
        root.findViewById<TextView>(R.id.challenge_reward).text = rupiah(root.context, challenge.reward)
        bindStatus(root.findViewById(R.id.challenge_status), challenge.status)
        val deadline = try {
            LocalDate.parse(challenge.deadline).format(
                DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"))
            )
        } catch (_: DateTimeParseException) {
            challenge.deadline
        }
        root.findViewById<TextView>(R.id.challenge_deadline).text =
            root.context.getString(R.string.challenge_deadline, deadline)
        val avatar = root.findViewById<ImageView>(R.id.challenge_avatar)
        Glide.with(avatar).load(challenge.creatorAvatar).placeholder(R.drawable.ic_avatar)
            .error(R.drawable.ic_avatar).circleCrop().into(avatar)
    }
}
