package id.ac.binus.bounty.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import id.ac.binus.bounty.R
import id.ac.binus.bounty.utils.ViewMotion

data class LeaderboardItem(
    val rank: Int,
    val userId: String,
    val name: String,
    val avatarUrl: String,
    val count: Int,
    val isCreatorCategory: Boolean,
    val isCurrentUser: Boolean
)

class LeaderboardAdapter : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    private val items = mutableListOf<LeaderboardItem>()

    fun submitList(newItems: List<LeaderboardItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView = itemView.findViewById(R.id.leaderboard_item_card)
        val rankContainer: FrameLayout = itemView.findViewById(R.id.rank_badge_container)
        val rankNumber: TextView = itemView.findViewById(R.id.rank_number)
        val avatar: ImageView = itemView.findViewById(R.id.leaderboard_avatar)
        val userName: TextView = itemView.findViewById(R.id.leaderboard_user_name)
        val badgeYou: TextView = itemView.findViewById(R.id.badge_you)
        val subtitle: TextView = itemView.findViewById(R.id.leaderboard_role_subtitle)
        val scoreBadge: TextView = itemView.findViewById(R.id.leaderboard_score_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_leaderboard, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.rankNumber.text = item.rank.toString()
        holder.userName.text = item.name.ifEmpty { "Pengguna Bounty" }
        holder.badgeYou.visibility = if (item.isCurrentUser) View.VISIBLE else View.GONE

        // Rank badge styling
        when (item.rank) {
            1 -> {
                holder.rankContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                holder.rankNumber.setTextColor(Color.parseColor("#B45309"))
            }
            2 -> {
                holder.rankContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                holder.rankNumber.setTextColor(Color.parseColor("#475569"))
            }
            3 -> {
                holder.rankContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEDD5"))
                holder.rankNumber.setTextColor(Color.parseColor("#C2410C"))
            }
            else -> {
                holder.rankContainer.backgroundTintList = null
                holder.rankNumber.setTextColor(context.getColor(R.color.bounty_text_secondary))
            }
        }

        // Subtitle & score
        if (item.isCreatorCategory) {
            holder.subtitle.text = "Pembuat Tantangan"
            holder.scoreBadge.text = context.getString(R.string.stat_challenges_created, item.count)
        } else {
            holder.subtitle.text = "Penerima Tantangan"
            holder.scoreBadge.text = context.getString(R.string.stat_challenges_received, item.count)
        }

        // Card highlight if current user
        if (item.isCurrentUser) {
            holder.card.strokeColor = context.getColor(R.color.bounty_primary)
            holder.card.strokeWidth = 2
        } else {
            holder.card.strokeColor = context.getColor(R.color.bounty_surface_container)
            holder.card.strokeWidth = 1
        }

        Glide.with(holder.avatar)
            .load(item.avatarUrl)
            .placeholder(R.drawable.ic_avatar)
            .error(R.drawable.ic_avatar)
            .circleCrop()
            .into(holder.avatar)

        ViewMotion.applySpringPress(holder.card)
    }

    override fun getItemCount(): Int = items.size
}
