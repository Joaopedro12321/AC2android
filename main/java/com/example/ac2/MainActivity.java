package com.example.ac2;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText edtTitulo, edtDiretor, edtAno;
    private RatingBar ratingBar;
    private Spinner spnGenero, spnFiltroGenero;
    private CheckBox chkCinema;
    private Button btnSalvar;
    private RecyclerView rvFilmes;

    private MovieAdapter adapter;

    private FirebaseFirestore db;
    private CollectionReference moviesRef;
    private ListenerRegistration listenerRegistration;

    private String editingId = null; // null = novo, não nulo = atualizar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Catálogo de Filmes");
        setContentView(R.layout.activity_main);

        // Views
        edtTitulo = findViewById(R.id.edtTitulo);
        edtDiretor = findViewById(R.id.edtDiretor);
        edtAno = findViewById(R.id.edtAno);
        ratingBar = findViewById(R.id.ratingBar);
        spnGenero = findViewById(R.id.spnGenero);
        spnFiltroGenero = findViewById(R.id.spnFiltroGenero);
        chkCinema = findViewById(R.id.chkCinema);
        btnSalvar = findViewById(R.id.btnSalvar);
        rvFilmes = findViewById(R.id.rvFilmes);

        // Lista
        rvFilmes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MovieAdapter(new ArrayList<>());
        adapter.setOnItemClickListener(this::carregarParaEdicao);
        rvFilmes.setAdapter(adapter);

        // Firestore
        db = FirebaseFirestore.getInstance();
        moviesRef = db.collection("movies");

        // Listener em tempo real
        listenerRegistration = moviesRef
                .orderBy("titulo")
                .addSnapshotListener(this, (snap, e) -> {
                    if (e != null) {
                        Toast.makeText(this, "Erro listener: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        return;
                    }
                    if (snap == null) return;

                    List<Movie> lista = new ArrayList<>();
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Movie m = doc.toObject(Movie.class);
                        if (m != null) { m.id = doc.getId(); lista.add(m); }
                    }
                    adapter.updateData(lista);
                    aplicarFiltroAtual(); // reaplica o filtro do spinner
                });

        // Salvar/Atualizar
        btnSalvar.setOnClickListener(v -> salvarOuAtualizar());

        // Filtro por gênero
        spnFiltroGenero.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                aplicarFiltroAtual();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerRegistration != null) listenerRegistration.remove();
    }

    private void aplicarFiltroAtual() {
        String genero = spnFiltroGenero.getSelectedItem() != null
                ? spnFiltroGenero.getSelectedItem().toString()
                : "Todos";
        adapter.filterByGenre(genero);
    }

    // Clique curto carrega para edição:
    private void carregarParaEdicao(Movie m) {
        editingId = m.id;
        edtTitulo.setText(m.titulo);
        edtDiretor.setText(m.diretor);
        edtAno.setText(m.ano > 0 ? String.valueOf(m.ano) : "");
        ratingBar.setRating(m.nota);
        chkCinema.setChecked(m.viuNoCinema);

        if (m.genero != null) {
            for (int i = 0; i < spnGenero.getCount(); i++) {
                if (m.genero.equals(spnGenero.getItemAtPosition(i).toString())) {
                    spnGenero.setSelection(i);
                    break;
                }
            }
        }
        Toast.makeText(this, "Editando: " + (m.titulo != null ? m.titulo : ""), Toast.LENGTH_SHORT).show();
    }

    // Salva novo ou atualiza existente (se editingId != null)
    private void salvarOuAtualizar() {
        String titulo = edtTitulo.getText().toString().trim();
        String diretor = edtDiretor.getText().toString().trim();
        int ano = 0;
        try { ano = Integer.parseInt(edtAno.getText().toString().trim()); } catch (Exception ignored) { }
        int nota = (int) ratingBar.getRating();
        String genero = spnGenero.getSelectedItem() != null ? spnGenero.getSelectedItem().toString() : "Outro";
        boolean viuNoCinema = chkCinema.isChecked();

        if (titulo.isEmpty()) {
            Toast.makeText(this, "Informe o título.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (editingId == null) {
            // novo
            String id = moviesRef.document().getId();
            Movie m = new Movie(id, titulo, diretor, ano, nota, genero, viuNoCinema);
            moviesRef.document(id).set(m)
                    .addOnSuccessListener(aVoid -> { Toast.makeText(this, "Salvo!", Toast.LENGTH_SHORT).show(); limparCampos(); })
                    .addOnFailureListener(ex -> Toast.makeText(this, "Erro ao salvar: " + ex.getMessage(), Toast.LENGTH_LONG).show());
        } else {
            // atualizar existente
            Movie m = new Movie(editingId, titulo, diretor, ano, nota, genero, viuNoCinema);
            moviesRef.document(editingId).set(m)
                    .addOnSuccessListener(aVoid -> { Toast.makeText(this, "Atualizado!", Toast.LENGTH_SHORT).show(); limparCampos(); })
                    .addOnFailureListener(ex -> Toast.makeText(this, "Erro ao atualizar: " + ex.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void limparCampos() {
        editingId = null;
        edtTitulo.setText("");
        edtDiretor.setText("");
        edtAno.setText("");
        ratingBar.setRating(0);
        chkCinema.setChecked(false);
        if (spnGenero.getCount() > 0) spnGenero.setSelection(0);
        edtTitulo.requestFocus();
    }
}
