package id.ac.binus.bounty.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import id.ac.binus.bounty.R
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.utils.ChallengeDisplay
import id.ac.binus.bounty.utils.ViewMotion

class PortfolioChallengeAdapter(
    private val ownerUserId: String,
    private val onOpen: (Challenge) -> Unit
) : ListAdapter<Challenge, PortfolioChallengeAdapter.Holder>(DIFF) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val roleBadge: TextView = view.findViewById(R.id.portfolio_role_badge)
        val statusBadge: TextView = view.findViewById(R.id.portfolio_status_badge)
        val title: TextView = view.findViewById(R.id.portfolio_title)
        val reward: TextView = view.findViewById(R.id.portfolio_reward)
        val deadline: TextView = view.findViewById(R.id.portfolio_deadline)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_portfolio_challenge, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val challenge = getItem(position)
        val ctx = holder.itemView.context

        holder.title.text = challenge.title
        holder.reward.text = ChallengeDisplay.rupiah(ctx, challenge.reward)
        holder.deadline.text = challenge.deadline
        ChallengeDisplay.bindStatus(holder.statusBadge, challenge.status)

        val isCreator = challenge.creatorId == ownerUserId
        if (isCreator) {
            holder.roleBadge.text = ctx.getString(R.string.portfolio_role_creator)
            holder.roleBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.bounty_gold_bg))
            holder.roleBadge.setTextColor(ContextCompat.getColor(ctx, R.color.bounty_gold_text))
        } else {
            holder.roleBadge.text = ctx.getString(R.string.portfolio_role_hunter)
            holder.roleBadge.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.bounty_primary_fixed))
            holder.roleBadge.setTextColor(ContextCompat.getColor(ctx, R.color.bounty_on_primary_fixed))
        }

        ViewMotion.applySpringPress(holder.itemView) { onOpen(challenge) }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Challenge>() {
            override fun areItemsTheSame(a: Challenge, b: Challenge) =
                (a.cloudId ?: a.id.toString()) == (b.cloudId ?: b.id.toString())

            override fun areContentsTheSame(a: Challenge, b: Challenge) =
                a.title == b.title && a.status == b.status && a.reward == b.reward &&
                    a.deadline == b.deadline && a.creatorId == b.creatorId && a.participantId == b.participantId
        }
    }
}
