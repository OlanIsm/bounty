package id.ac.binus.bounty.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import java.util.function.Consumer;
import id.ac.binus.bounty.R;
import id.ac.binus.bounty.models.Challenge;
import id.ac.binus.bounty.utils.ChallengeDisplay;

public class ChallengeAdapter extends ListAdapter<Challenge, ChallengeAdapter.Holder> {
    private final Consumer<Challenge> onOpen;

    public ChallengeAdapter(Consumer<Challenge> onOpen) {
        super(new DiffUtil.ItemCallback<Challenge>() {
            @Override
            public boolean areItemsTheSame(@NonNull Challenge oldItem, @NonNull Challenge newItem) {
                return oldItem.id == newItem.id;
            }
            @Override
            public boolean areContentsTheSame(@NonNull Challenge oldItem, @NonNull Challenge newItem) {
                return oldItem.title.equals(newItem.title) && oldItem.description.equals(newItem.description)
                        && oldItem.creatorName.equals(newItem.creatorName) && oldItem.creatorAvatar.equals(newItem.creatorAvatar)
                        && oldItem.reward == newItem.reward && oldItem.deadline.equals(newItem.deadline)
                        && oldItem.status.equals(newItem.status);
            }
        });
        this.onOpen = onOpen;
    }
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_challenge, parent, false));
    }
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Challenge challenge = getItem(position);
        ChallengeDisplay.bind(holder.itemView, challenge);
        ((TextView) holder.itemView.findViewById(R.id.challenge_description)).setText(challenge.description);
        holder.itemView.setOnClickListener(view -> onOpen.accept(challenge));
        holder.itemView.findViewById(R.id.view_challenge).setOnClickListener(view -> onOpen.accept(challenge));
    }
    static class Holder extends RecyclerView.ViewHolder {
        Holder(View view) { super(view); }
    }
}
