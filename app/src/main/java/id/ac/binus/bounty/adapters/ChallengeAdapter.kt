package id.ac.binus.bounty.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.util.function.Consumer
import id.ac.binus.bounty.R
import id.ac.binus.bounty.models.Challenge
import id.ac.binus.bounty.utils.ChallengeDisplay

class ChallengeAdapter(private val onOpen: Consumer<Challenge>) :
    ListAdapter<Challenge, ChallengeAdapter.Holder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_challenge, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val challenge = getItem(position)
        ChallengeDisplay.bind(holder.itemView, challenge)
        holder.itemView.findViewById<TextView>(R.id.challenge_description).text = challenge.description
        holder.itemView.setOnClickListener { onOpen.accept(challenge) }
        holder.itemView.findViewById<View>(R.id.view_challenge).setOnClickListener { onOpen.accept(challenge) }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view)

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Challenge>() {
            override fun areItemsTheSame(oldItem: Challenge, newItem: Challenge) =
                (oldItem.cloudId ?: oldItem.id.toString()) == (newItem.cloudId ?: newItem.id.toString())

            override fun areContentsTheSame(oldItem: Challenge, newItem: Challenge) =
                oldItem.title == newItem.title && oldItem.description == newItem.description &&
                    oldItem.creatorName == newItem.creatorName && oldItem.creatorAvatar == newItem.creatorAvatar &&
                    oldItem.reward == newItem.reward && oldItem.deadline == newItem.deadline &&
                    oldItem.status == newItem.status
        }
    }
}
