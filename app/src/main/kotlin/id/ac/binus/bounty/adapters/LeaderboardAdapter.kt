package id.ac.binus.bounty.adapters

import android.content.res.ColorStateList
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
        val cardShine: View = itemView.findViewById(R.id.leaderboard_card_shine)
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

        // Subtitle & score text
        if (item.isCreatorCategory) {
            holder.subtitle.text = "Pembuat Tantangan"
            holder.scoreBadge.text = context.getString(R.string.stat_challenges_created, item.count)
        } else {
            holder.subtitle.text = "Penerima Tantangan"
            holder.scoreBadge.text = context.getString(R.string.stat_challenges_received, item.count)
        }

        // Rank styling: Top 1 = Kuning, Top 2 = Ijo, Top 3 = Biru, sisanya = Putih
        // Plus shiny animation on Top 1, 2, 3
        when (item.rank) {
            1 -> {
                // Top 1: Full Yellow / Gold Card
                holder.card.setCardBackgroundColor(Color.parseColor("#FEF08A"))
                holder.card.strokeColor = if (item.isCurrentUser) context.getColor(R.color.bounty_primary) else Color.parseColor("#EAB308")
                holder.card.strokeWidth = 2
                holder.userName.setTextColor(Color.parseColor("#713F12"))
                holder.subtitle.setTextColor(Color.parseColor("#854D0E"))
                holder.rankContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF9C3"))
                holder.rankNumber.setTextColor(Color.parseColor("#713F12"))
                holder.scoreBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FDE047"))
                holder.scoreBadge.setTextColor(Color.parseColor("#713F12"))
                holder.cardShine.visibility = View.VISIBLE
                ViewMotion.startShimmerSweep(holder.cardShine, holder.card, 3600)
            }
            2 -> {
                // Top 2: Full Green / Emerald Card
                holder.card.setCardBackgroundColor(Color.parseColor("#DCFCE7"))
                holder.card.strokeColor = if (item.isCurrentUser) context.getColor(R.color.bounty_primary) else Color.parseColor("#22C55E")
                holder.card.strokeWidth = 2
                holder.userName.setTextColor(Color.parseColor("#14532D"))
                holder.subtitle.setTextColor(Color.parseColor("#166534"))
                holder.rankContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F0FDF4"))
                holder.rankNumber.setTextColor(Color.parseColor("#14532D"))
                holder.scoreBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BBF7D0"))
                holder.scoreBadge.setTextColor(Color.parseColor("#14532D"))
                holder.cardShine.visibility = View.VISIBLE
                ViewMotion.startShimmerSweep(holder.cardShine, holder.card, 4000)
            }
            3 -> {
                // Top 3: Full Blue / Azure Card
                holder.card.setCardBackgroundColor(Color.parseColor("#DBEAFE"))
                holder.card.strokeColor = if (item.isCurrentUser) context.getColor(R.color.bounty_primary) else Color.parseColor("#3B82F6")
                holder.card.strokeWidth = 2
                holder.userName.setTextColor(Color.parseColor("#1E3A8A"))
                holder.subtitle.setTextColor(Color.parseColor("#1D4ED8"))
                holder.rankContainer.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EFF6FF"))
                holder.rankNumber.setTextColor(Color.parseColor("#1E3A8A"))
                holder.scoreBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#BFDBFE"))
                holder.scoreBadge.setTextColor(Color.parseColor("#1E3A8A"))
                holder.cardShine.visibility = View.VISIBLE
                ViewMotion.startShimmerSweep(holder.cardShine, holder.card, 4400)
            }
            else -> {
                // Rank 4+: Pure White Card
                holder.card.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                holder.card.strokeColor = if (item.isCurrentUser) context.getColor(R.color.bounty_primary) else context.getColor(R.color.bounty_surface_container)
                holder.card.strokeWidth = if (item.isCurrentUser) 2 else 1
                holder.userName.setTextColor(context.getColor(R.color.bounty_text))
                holder.subtitle.setTextColor(context.getColor(R.color.bounty_text_secondary))
                holder.rankContainer.backgroundTintList = null
                holder.rankNumber.setTextColor(context.getColor(R.color.bounty_text_secondary))
                holder.scoreBadge.backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.bounty_surface_container_low))
                holder.scoreBadge.setTextColor(context.getColor(R.color.bounty_primary))
                holder.cardShine.visibility = View.GONE
            }
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
