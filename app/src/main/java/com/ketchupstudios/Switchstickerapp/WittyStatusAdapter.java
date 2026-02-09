package com.ketchupstudios.Switchstickerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class WittyStatusAdapter extends RecyclerView.Adapter<WittyStatusAdapter.StatusViewHolder> {

    // Estructura para guardar los datos de cada opción
    public static class StatusItem {
        String title;
        String game;
        public StatusItem(String title, String game) {
            this.title = title;
            this.game = game;
        }
    }

    private List<StatusItem> items;
    private int selectedPosition = -1; // Cuál está seleccionado (-1 es ninguno)

    public WittyStatusAdapter(List<StatusItem> items, String currentTitle) {
        this.items = items;
        // Buscar si ya tenemos uno seleccionado
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).title.equals(currentTitle)) {
                selectedPosition = i;
                break;
            }
        }
    }

    @NonNull
    @Override
    public StatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_witty_status, parent, false);
        return new StatusViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull StatusViewHolder holder, int position) {
        StatusItem item = items.get(position);

        holder.txtTitle.setText(item.title);

        if (item.game == null || item.game.isEmpty()) {
            holder.txtGame.setVisibility(View.GONE);
        } else {
            holder.txtGame.setVisibility(View.VISIBLE);
            holder.txtGame.setText(item.game);
        }

        // Marcar el circulito si es el seleccionado
        holder.radioButton.setChecked(position == selectedPosition);

        // LOGICA NUEVA PARA DESELECCIONAR
        View.OnClickListener listener = v -> {
            if (selectedPosition == holder.getAdapterPosition()) {
                selectedPosition = -1; // Si ya estaba, lo quitamos (Desmarcar)
            } else {
                selectedPosition = holder.getAdapterPosition(); // Si no, lo marcamos
            }
            notifyDataSetChanged(); // Actualizar toda la lista visualmente
        };

        holder.itemView.setOnClickListener(listener);
        holder.radioButton.setOnClickListener(listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // Método para que la Activity sepa cuál elegimos
    public StatusItem getSelectedItem() {
        if (selectedPosition != -1) {
            return items.get(selectedPosition);
        }
        return null;
    }

    static class StatusViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtGame;
        RadioButton radioButton;

        public StatusViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtStatusTitle);
            txtGame = itemView.findViewById(R.id.txtStatusGame);
            radioButton = itemView.findViewById(R.id.radioStatus);
        }
    }
}