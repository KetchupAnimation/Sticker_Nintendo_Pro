package com.ketchupstudios.Switchstickerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import android.widget.EditText;
import android.graphics.Color;

public class GameSearchActivity extends AppCompatActivity {

    private RecyclerView rvResults;
    private GameSearchAdapter adapter;

    // Base de datos completa (se descarga en segundo plano)
    private List<GameItem> allGames = new ArrayList<>();

    // Lo que se ve en pantalla
    private List<GameItem> displayedGames = new ArrayList<>();

    private ProgressBar progressBar;
    private static final String URL_TITLE_DB = "https://raw.githubusercontent.com/julesontheroad/titledb/master/titles.US.en.json";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- INICIO DEL TRUCO PARA PANTALLA COMPLETA ---
        // Esto elimina los límites de la ventana para que el banner se dibuje
        // DETRÁS de la barra de estado (donde está la hora y la batería).
        getWindow().setFlags(
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        );
        // -----------------------------------------------

        setContentView(R.layout.activity_search);

        SearchView searchView = findViewById(R.id.searchView);
        // --- CORRECCIÓN DE VISIBILIDAD DE TEXTO ---
        // Buscamos el "EditText" interno del buscador para cambiarle el color a la fuerza
        EditText txtSearch = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        if (txtSearch != null) {
            txtSearch.setTextColor(Color.BLACK);       // El texto que escribes (Ej: "Mario")
            txtSearch.setHintTextColor(Color.DKGRAY);  // El texto de ayuda ("Search Zelda...")
        }
        // ------------------------------------------
        rvResults = findViewById(R.id.rvSearchResults);
        ImageView imgBanner = findViewById(R.id.imgSearchBanner);
        progressBar = findViewById(R.id.progressBarMain);

        // Banner
        String myBannerUrl = "https://raw.githubusercontent.com/KetchupAnimation/StickerApp-repo/main/banner_13.png";
        Glide.with(this).load(myBannerUrl).centerCrop().into(imgBanner);

        // Importante: Asegúrate de importar androidx.recyclerview.widget.GridLayoutManager;
        rvResults.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));

        // Configurar adaptador
        adapter = new GameSearchAdapter(this, displayedGames, game -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("GAME_ID", game.id);
            resultIntent.putExtra("GAME_NAME", game.name);
            resultIntent.putExtra("GAME_BANNER", game.bannerUrl);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
        rvResults.setAdapter(adapter);

        // 1. CARGAR LISTA VIP INMEDIATAMENTE (Sin esperar internet)
        cargarListaVipManual();

        // 2. Descargar el resto de juegos en segundo plano para el buscador
        descargarTitleDB();

        // Configuración del buscador
        searchView.setIconified(false);
        searchView.setQueryHint("Search (Zelda, Mario, etc)...");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { filtrar(query); return false; }
            @Override
            public boolean onQueryTextChange(String newText) { filtrar(newText); return false; }
        });

        searchView.setOnCloseListener(() -> { finish(); return false; });
    }

    // --- AQUÍ DEFINIMOS TUS FAVORITOS MANUALMENTE ---
    // Esto asegura que SIEMPRE salgan estos, en este orden exacto.
    private void cargarListaVipManual() {
        List<GameItem> vipList = new ArrayList<>();

        // Agregamos los juegos uno por uno (ID, Nombre, Banner vacío por ahora)
        vipList.add(new GameItem("0100C2500FC20000", "Splatoon 3", "")); // ¡NUEVO!
        vipList.add(new GameItem("0100152000022000", "Mario Kart 8 Deluxe", ""));
        vipList.add(new GameItem("01006F8002326000", "Animal Crossing: New Horizons", ""));
        vipList.add(new GameItem("01007EF00011E000", "The Legend of Zelda: Breath of the Wild", ""));
        vipList.add(new GameItem("0100F2300032E000", "The Legend of Zelda: Tears of the Kingdom", ""));
        vipList.add(new GameItem("0100E95004038000", "Super Mario Odyssey", ""));
        vipList.add(new GameItem("01006A800E174000", "Super Smash Bros. Ultimate", ""));
        vipList.add(new GameItem("0100ABF008968000", "Pokémon Sword", ""));
        vipList.add(new GameItem("01008DB008C2C000", "Pokémon Shield", ""));
        vipList.add(new GameItem("010021C011EB6000", "Pokémon Scarlet", ""));
        vipList.add(new GameItem("01008F6011EBA000", "Pokémon Violet", ""));
        vipList.add(new GameItem("0100F8F00C622000", "Splatoon 2", ""));
        vipList.add(new GameItem("0100000000010000", "Super Mario Bros. Wonder", ""));
        vipList.add(new GameItem("01004D300C5AE000", "Kirby and the Forgotten Land", ""));
        vipList.add(new GameItem("010031D010FA6000", "Super Mario Party", ""));
        vipList.add(new GameItem("01006E1014420000", "Mario Party Superstars", ""));
        vipList.add(new GameItem("0100B04011742000", "Minecraft", ""));
        vipList.add(new GameItem("010065A016334000", "Among Us", ""));
        vipList.add(new GameItem("0100N1N000000000", "Nintendo Switch Sports", ""));

        // Actualizamos la pantalla de inmediato
        adapter.updateList(vipList);

        // Guardamos copia en allGames por si el usuario borra la búsqueda
        allGames.clear();
        allGames.addAll(vipList);
    }

    private void descargarTitleDB() {
        new Thread(() -> {
            try {
                URL url = new URL(URL_TITLE_DB);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                JSONObject jsonMap = new JSONObject(response.toString());
                List<GameItem> loadedGames = new ArrayList<>();
                Iterator<String> keys = jsonMap.keys();

                while(keys.hasNext()) {
                    String gameId = keys.next();
                    JSONObject gameData = jsonMap.getJSONObject(gameId);

                    // CORRECCIÓN: Solo validamos que tenga nombre.
                    // No filtramos por bannerUrl aquí porque ese dato suele faltar en el JSON,
                    // pero la imagen sí puede existir en la API de nlib.cc.
                    if (gameData.has("name")) {
                        String name = gameData.getString("name");
                        // El banner lo dejamos vacío o lo leemos, pero NO filtramos por él.
                        String banner = gameData.optString("bannerUrl", "");

                        loadedGames.add(new GameItem(gameId, name, banner));
                    }
                }

                // Ordenar alfabéticamente
                Collections.sort(loadedGames, (g1, g2) -> g1.name.compareToIgnoreCase(g2.name));

                // Guardar la lista completa en memoria
                List<GameItem> finalLoadedGames = new ArrayList<>(loadedGames);

                runOnUiThread(() -> {
                    allGames.clear();
                    allGames.addAll(finalLoadedGames);
                    progressBar.setVisibility(View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void filtrar(String texto) {
        String query = texto.toLowerCase().trim();
        List<GameItem> filtrados = new ArrayList<>();

        if (query.isEmpty()) {
            cargarListaVipManual();
            return;
        }

        for (GameItem g : allGames) {
            if (g.name != null && g.name.toLowerCase().contains(query)) {
                filtrados.add(g);
            }
        }

        adapter.updateList(filtrados);
    }
}