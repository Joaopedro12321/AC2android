package com.example.ac2;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.ViewHolder> {

    private List<Movie> filmes;
    private List<Movie> shown;
    private String currentFilter = "Todos";

    public interface OnItemClickListener { void onItemClick(Movie movie); }
    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public MovieAdapter(List<Movie> filmes) {
        this.filmes = (filmes != null) ? filmes : new ArrayList<Movie>();
        this.shown = new ArrayList<>();
        filterByGenre(currentFilter);
    }

    public void updateData(List<Movie> novos) {
        this.filmes = (novos != null) ? novos : new ArrayList<Movie>();
        filterByGenre(currentFilter); // reaplica filtro ao atualizar
    }

    private String norm(String s) { return s == null ? "" : s.trim().toLowerCase(); }

    public void filterByGenre(String genero) {
        currentFilter = (genero == null ? "Todos" : genero);
        String g = norm(currentFilter);

        shown.clear();
        if (g.isEmpty() || g.equals("todos")) {
            shown.addAll(filmes);
        } else {
            for (Movie m : filmes) if (norm(m.genero).equals(g)) shown.add(m);
        }
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_simplw, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int pos) {
        Movie m = shown.get(pos);

        String tituloAno = (m.titulo != null ? m.titulo : "");
        if (m.ano > 0) tituloAno += " (" + m.ano + ")";
        h.txt1.setText(tituloAno);

        String info = "Nota: " + m.nota + "/5";
        if (m.genero != null && !m.genero.isEmpty()) info += " • " + m.genero;
        h.txt2.setText(info);

        // Clique curto: editar
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(m); });

        // Clique longo: perguntar e excluir
        h.itemView.setOnLongClickListener(v -> {
            Context ctx = v.getContext();
            new AlertDialog.Builder(ctx)
                    .setTitle("Excluir")
                    .setMessage("Excluir este filme?")
                    .setPositiveButton("Sim", (DialogInterface d, int which) -> deletarFilme(m.id, h.getAdapterPosition(), v))
                    .setNegativeButton("Não", null)
                    .show();
            return true;
        });
    }

    private void deletarFilme(String idDocumento, int position, View view) {
        if (idDocumento == null || idDocumento.isEmpty()) {
            Toast.makeText(view.getContext(), "ID inválido.", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("movies")
                .document(idDocumento)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // remove de ambas as listas
                    Movie removed = shown.remove(position);
                    for (int i = 0; i < filmes.size(); i++) {
                        if (filmes.get(i).id != null && filmes.get(i).id.equals(removed.id)) {
                            filmes.remove(i);
                            break;
                        }
                    }
                    notifyItemRemoved(position);
                    Toast.makeText(view.getContext(), "Filme deletado!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(view.getContext(), "Erro ao deletar: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public int getItemCount() { return shown != null ? shown.size() : 0; }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt1, txt2;
        public ViewHolder(View itemView) {
            super(itemView);
            txt1 = itemView.findViewById(R.id.text1);
            txt2 = itemView.findViewById(R.id.text2);
        }
    }
}
