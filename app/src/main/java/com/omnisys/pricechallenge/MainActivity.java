package com.omnisys.pricechallenge;

import android.app.Activity;
import android.os.Bundle;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * OmniSys Price Challenge-ren pantaila nagusia eta jokoaren logika.
 * Produktuak eta estatistikak lokalean gordetzen ditu; emaitzak zerbitzariarekin
 * sinkronizatzen saiatzen da konexioa dagoenean.
 */
public class MainActivity extends Activity {
    private static final int TOTAL_QUESTIONS = 10;
    private static final int POINTS_PER_CORRECT = 10;

    private final int BG = Color.rgb(5, 14, 28);
    private final int PANEL = Color.rgb(12, 30, 52);
    private final int PANEL_LIGHT = Color.rgb(17, 43, 70);
    private final int CYAN = Color.rgb(0, 220, 240);
    private final int BLUE = Color.rgb(0, 139, 255);
    private final int WHITE = Color.rgb(241, 247, 255);
    private final int MUTED = Color.rgb(158, 181, 207);
    private final int GREEN = Color.rgb(10, 105, 75);
    private final int RED = Color.rgb(116, 37, 52);

    private LocalDatabase database;
    private User currentUser;
    private List<Product> products = new ArrayList<>();
    private List<Product> sessionProducts = new ArrayList<>();
    private Product currentProduct;

    private int questionIndex;
    private int sessionPoints;
    private int correctAnswers;
    private boolean resultSaved = false;
    private int lifetimePoints;
    private int lifetimeQuestions;
    private boolean answered;

    private LinearLayout root;
    private LinearLayout choicesLayout;
    private TextView progressText;
    private TextView scoreText;
    private TextView productNameText;
    private TextView categoryText;
    private TextView feedbackText;
    private ImageView productImage;
    private ProgressBar progressBar;
    private Button advanceButton;

    private final List<Button> choiceButtons = new ArrayList<>();
    private final List<Double> choicePrices = new ArrayList<>();
    private final Random random = new Random();

    // Hasieratu datu-basea, estatistikak eta saioa hasteko pantaila.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new LocalDatabase(this);

        // Lehen abiaraztean, demoko produktuak SQLite-n gordetzen dira.
        if (database.getProductCount() == 0) {
            for (Product p : ProductRepository.getProducts()) {
                database.saveProduct(p);
            }
        }

        products = database.getProducts();
        int[] stats = database.getStats();
        lifetimePoints = stats[0];
        lifetimeQuestions = stats[1];
        showLogin();
    }

    // Erabiltzaile-izena eskatu eta erabiltzailea berreskuratu edo sortu.
    private void showLogin() {

        root = createRoot();
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(18), dp(35), dp(18), dp(25));

        scroll.addView(content);
        root.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1f)
        );

        TextView logo = makeText(
                "O M N I S Y S",
                15,
                CYAN,
                true
        );
        logo.setGravity(Gravity.CENTER);
        logo.setLetterSpacing(0.25f);
        content.addView(logo);

        TextView title = makeText(
                "PRICE CHALLENGE",
                27,
                WHITE,
                true
        );
        title.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams titleLp =
                new LinearLayout.LayoutParams(-1, -2);
        titleLp.topMargin = dp(12);
        content.addView(title, titleLp);

        TextView subtitle = makeText(
                "Ongi etorri!",
                18,
                CYAN,
                true
        );
        subtitle.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams subLp =
                new LinearLayout.LayoutParams(-1, -2);
        subLp.topMargin = dp(28);
        content.addView(subtitle, subLp);

        TextView instructions = makeText(
                "Idatzi zure erabiltzaile-izena jolasten hasteko.",
                14,
                MUTED,
                false
        );
        instructions.setGravity(Gravity.CENTER);
        instructions.setPadding(0, dp(8), 0, dp(20));
        content.addView(instructions);

        LinearLayout loginCard = makePanel();
        loginCard.setOrientation(LinearLayout.VERTICAL);
        loginCard.setPadding(dp(16), dp(18), dp(16), dp(18));

        content.addView(loginCard);

        loginCard.addView(
                makeText("ERABILTZAILE-IZENA", 12, CYAN, true)
        );

        EditText usernameInput = new EditText(this);
        usernameInput.setSingleLine(true);
        usernameInput.setTextSize(16);
        usernameInput.setTextColor(WHITE);
        usernameInput.setHintTextColor(MUTED);
        usernameInput.setHint("Adib.: Imanol");
        usernameInput.setPadding(
                dp(12), dp(8), dp(12), dp(8)
        );
        usernameInput.setBackground(
                makeBackground(PANEL_LIGHT, 12, CYAN)
        );

        LinearLayout.LayoutParams inputLp =
                new LinearLayout.LayoutParams(-1, dp(54));
        inputLp.topMargin = dp(10);
        loginCard.addView(usernameInput, inputLp);

        TextView errorText = makeText(
                "",
                12,
                Color.rgb(255, 100, 120),
                true
        );
        errorText.setPadding(0, dp(8), 0, 0);
        loginCard.addView(errorText);

        Button loginButton = makeButton(
                "SARTU JOLASERA",
                BLUE
        );

        LinearLayout.LayoutParams buttonLp =
                new LinearLayout.LayoutParams(-1, dp(54));
        buttonLp.topMargin = dp(18);
        content.addView(loginButton, buttonLp);

        loginButton.setOnClickListener(v -> {

            String username =
                    usernameInput.getText().toString().trim();

            if (username.isEmpty()) {
                errorText.setText(
                        "Idatzi erabiltzaile-izena jarraitzeko."
                );
                usernameInput.requestFocus();
                return;
            }

            if (username.length() > 40) {
                errorText.setText(
                        "Erabiltzaile-izenak 40 karaktere baino gutxiago izan behar ditu."
                );
                return;
            }

            User user = database.getOrCreateUser(username);

            if (user == null) {
                errorText.setText(
                        "Ezin izan da erabiltzailea gorde."
                );
                return;
            }

            currentUser = user;
            showHome();
        });

        TextView offline = makeText(
                "● OFFLINE  ·  PASAHITZIK GABE",
                11,
                CYAN,
                true
        );
        offline.setGravity(Gravity.CENTER);
        offline.setPadding(0, dp(25), 0, 0);
        content.addView(offline);
    }

    // Hasierako menua, estatistikak eta nabigazio-botoiak erakutsi.
    private void showHome() {
        int[] stats = database.getStats();
        lifetimePoints = stats[0];
        lifetimeQuestions = stats[1];

        root = createRoot();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(26), dp(22), dp(20));
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        TextView logo = makeText("O M N I S Y S", 14, CYAN, true);
        logo.setLetterSpacing(0.22f);
        content.addView(logo);

        TextView title = makeText("PRICE CHALLENGE", 29, WHITE, true);
        title.setPadding(0, dp(8), 0, dp(4));
        content.addView(title);

        TextView subtitle = makeText("Zenbat balio du produktuak?", 16, MUTED, false);
        content.addView(subtitle);

        if (currentUser != null) {

            TextView welcome = makeText(
                    "JOKALARIA: " + currentUser.username,
                    13,
                    Color.rgb(255, 165, 0), //Jokalari  mezua laranjaz ikusteko
                    true
            );

            welcome.setPadding(0, dp(12), 0, 0);
            content.addView(welcome);
        }

        LinearLayout info = makePanel();
        info.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(-1, -2);
        infoLp.topMargin = dp(24);
        content.addView(info, infoLp);
        info.addView(makeText("NOLA JOLASTU", 12, CYAN, true));
        TextView instructions = makeText(
                "10 galdera izango dituzu. Galdera bakoitzean produktu baten benetako prezioa aukeratu lau aukeren artean. Erantzun zuzen bakoitzak 10 puntu ematen ditu.",
                15, WHITE, false);
        instructions.setPadding(0, dp(10), 0, 0);
        info.addView(instructions);

        LinearLayout statsPanel = makePanel();

        LinearLayout.LayoutParams statsLp =
                new LinearLayout.LayoutParams(-1, -2);

        statsLp.topMargin = dp(14);
        content.addView(statsPanel, statsLp);

        statsPanel.addView(
                makeText("ESTATISTIKAK", 12, CYAN, true)
        );

        TextView statsContent = makeText(
                "PUNTUAK GUZTIRA: " + lifetimePoints
                        + "\nErantzundako GALDERAK: " + lifetimeQuestions,
                14, WHITE, true
        );

        statsContent.setPadding(0, dp(10), 0, 0);
        statsPanel.addView(statsContent);

        Button start = makeButton("10 GALDERAKO PARTIDA HASI", BLUE);
        LinearLayout.LayoutParams startLp = new LinearLayout.LayoutParams(-1, dp(56));
        startLp.topMargin = dp(22);
        content.addView(start, startLp);
        start.setOnClickListener(v -> startGame());

        Button changeUser = makeButton(
                "ERABILTZAILEA ALDATU",
                PANEL_LIGHT
        );
        Button serverTestButton = makeButton(
                "ZERBITZARIA PROBATU", PANEL_LIGHT
        );

        LinearLayout.LayoutParams serverTestLp =
                new LinearLayout.LayoutParams(-1, dp(50));

        serverTestLp.topMargin = dp(10);

        content.addView(serverTestButton, serverTestLp);

        serverTestButton.setOnClickListener(
                v -> testRankingServer()
        );
        //IRTEN botoia eta bere funtzioa
        Button irtenButton = makeButton(
                "IRTEN", Color.rgb(190, 35, 55)
        );

        LinearLayout.LayoutParams irtenLp =
                new LinearLayout.LayoutParams(-1, dp(48));

        irtenLp.topMargin = dp(10);
        content.addView(irtenButton, irtenLp);

        irtenButton.setOnClickListener(v -> {
            finishAffinity();
        });


        // ERABILTZAILEA ALDATU botoia
        LinearLayout.LayoutParams changeUserLp =
                new LinearLayout.LayoutParams(-1, dp(48));

        changeUserLp.topMargin = dp(10);
        content.addView(changeUser, changeUserLp);

        changeUser.setOnClickListener(v -> showLogin());

        TextView offline = makeText("● OFFLINE  ·  KATALOGOA GAILUAN GORDETA", 10, CYAN, true);
        offline.setGravity(Gravity.CENTER);
        offline.setPadding(0, dp(18), 0, 0);
        content.addView(offline);
    }

    // Partida berria prestatu eta lehen galdera erakutsi.
    private void startGame() {
        products = database.getProducts();
        if (products.isEmpty()) {
            for (Product p : ProductRepository.getProducts()) database.saveProduct(p);
            products = database.getProducts();
        }

        if (products.isEmpty()) {
            showMessageScreen("Ez dago produkturik", "Lehenik produktuak gehitu behar dira.");
            return;
        }

        sessionProducts = buildSessionProducts();
        questionIndex = 0;
        sessionPoints = 0;
        correctAnswers = 0;
        resultSaved = false;
        answered = false;
        showGameLayout();
        displayQuestion();
    }

    private List<Product> buildSessionProducts() {
        List<Product> pool = new ArrayList<>(products);
        Collections.shuffle(pool, random);
        List<Product> session = new ArrayList<>();
        while (session.size() < TOTAL_QUESTIONS) {
            for (Product p : pool) {
                if (session.size() >= TOTAL_QUESTIONS) break;
                session.add(p);
            }
            Collections.shuffle(pool, random);
        }
        return session;
    }

    private void showGameLayout() {
        root = createRoot();
        setContentView(root);

        // Goiko marka eta puntuazioa.
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(header);

        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        header.addView(titleColumn, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView logo = makeText("O M N I S Y S", 11, CYAN, true);
        logo.setLetterSpacing(0.18f);
        titleColumn.addView(logo);
        TextView title = makeText("PRICE CHALLENGE", 21, WHITE, true);
        title.setPadding(0, dp(3), 0, 0);
        titleColumn.addView(title);

        LinearLayout scoreCard = makePanel();
        scoreCard.setPadding(dp(12), dp(8), dp(12), dp(8));
        scoreCard.setGravity(Gravity.CENTER);
        header.addView(scoreCard);
        scoreText = makeText("0 / 100\nPUNTUAK", 12, CYAN, true);
        scoreText.setGravity(Gravity.CENTER);
        scoreCard.addView(scoreText);

        // Galdera kopurua eta aurrerapen barra.
        LinearLayout progressCard = makePanel();
        progressCard.setOrientation(LinearLayout.VERTICAL);
        progressCard.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams progressLp = new LinearLayout.LayoutParams(-1, -2);
        progressLp.topMargin = dp(10);
        root.addView(progressCard, progressLp);

        progressText = makeText("GALDERA 1 / 10", 12, WHITE, true);
        progressCard.addView(progressText);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(TOTAL_QUESTIONS);
        progressBar.setProgress(1);
        progressBar.setProgressTintList(ColorStateList.valueOf(CYAN));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.rgb(33, 57, 82)));
        LinearLayout.LayoutParams barLp = new LinearLayout.LayoutParams(-1, dp(5));
        barLp.topMargin = dp(8);
        progressCard.addView(progressBar, barLp);

        // Erdiko edukia bakarrik mugitzen da; beheko botoia beti ikusgai dago.
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams scrollLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        scrollLp.topMargin = dp(8);
        root.addView(scroll, scrollLp);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);

        LinearLayout productCard = makePanel();
        productCard.setPadding(dp(12), dp(12), dp(12), dp(12));
        content.addView(productCard);

        TextView productLabel = makeText("◇  PRODUKTUA", 12, CYAN, true);
        productCard.addView(productLabel);

        productImage = new ImageView(this);
        productImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        productImage.setAdjustViewBounds(true);
        productImage.setPadding(dp(3), dp(3), dp(3), dp(3));
        LinearLayout.LayoutParams imageLp = new LinearLayout.LayoutParams(-1, dp(176));
        imageLp.topMargin = dp(5);
        imageLp.bottomMargin = dp(4);
        productCard.addView(productImage, imageLp);

        productNameText = makeText("", 19, WHITE, true);
        productNameText.setGravity(Gravity.CENTER);
        productCard.addView(productNameText);

        categoryText = makeText("", 12, MUTED, false);
        categoryText.setGravity(Gravity.CENTER);
        categoryText.setPadding(0, dp(4), 0, dp(12));
        productCard.addView(categoryText);

        TextView chooseTitle = makeText("Aukeratu prezio zuzena:", 13, WHITE, true);
        chooseTitle.setGravity(Gravity.CENTER);
        productCard.addView(chooseTitle);

        choicesLayout = new LinearLayout(this);
        choicesLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams gridLp = new LinearLayout.LayoutParams(-1, -2);
        gridLp.topMargin = dp(9);
        productCard.addView(choicesLayout, gridLp);

        // Feedback-ak altuera erreserbatua du, diseinua ez dadin jauzika mugitu.
        feedbackText = makeText("", 12, CYAN, true);
        feedbackText.setGravity(Gravity.CENTER);
        feedbackText.setMinHeight(dp(28));
        feedbackText.setPadding(0, dp(7), 0, 0);
        productCard.addView(feedbackText);

        advanceButton = makeButton("AUKERATU PREZIO BAT  →", Color.rgb(26, 61, 94));
        advanceButton.setEnabled(false);
        LinearLayout.LayoutParams advanceLp = new LinearLayout.LayoutParams(-1, dp(52));
        advanceLp.topMargin = dp(8);
        advanceLp.bottomMargin = dp(3);
        root.addView(advanceButton, advanceLp);
        advanceButton.setOnClickListener(v -> advance());
    }

    private void displayQuestion() {
        if (questionIndex >= TOTAL_QUESTIONS) {
            showResults();
            return;
        }

        currentProduct = sessionProducts.get(questionIndex);
        answered = false;
        choiceButtons.clear();
        choicePrices.clear();
        choicesLayout.removeAllViews();

        progressText.setText("GALDERA " + (questionIndex + 1) + " / " + TOTAL_QUESTIONS);
        progressBar.setProgress(questionIndex + 1);
        scoreText.setText(sessionPoints + " / 100\nPUNTUAK");
        productNameText.setText(currentProduct.name);
        categoryText.setText("Kategoria: " + currentProduct.category);
        feedbackText.setText("");

        // Irudi osoa erakutsi: FIT_CENTER-k proportzioak gordetzen ditu eta ez du mozten.
        int imageId = getResources().getIdentifier(
                "product_" + currentProduct.imageName, "drawable", getPackageName());
        if (imageId != 0) {
            productImage.setImageResource(imageId);
        } else {
            productImage.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        List<Double> options = buildPriceOptions(currentProduct.price);
        // Bi erantzun goian eta bi behean: 2 x 2 sareta.
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(54));
            if (rowIndex > 0) rowLp.topMargin = dp(7);
            choicesLayout.addView(row, rowLp);

            for (int col = 0; col < 2; col++) {
                int optionIndex = rowIndex * 2 + col;
                double price = options.get(optionIndex);
                Button b = makeButton(formatPrice(price), PANEL_LIGHT);
                b.setTextSize(14);
                b.setMinWidth(0);
                b.setPadding(dp(4), dp(4), dp(4), dp(4));
                LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(0, -1, 1f);
                if (col == 0) btnLp.rightMargin = dp(4);
                else btnLp.leftMargin = dp(4);
                row.addView(b, btnLp);
                choiceButtons.add(b);
                choicePrices.add(price);
                b.setOnClickListener(v -> choosePrice(b, price));
            }
        }

        advanceButton.setEnabled(false);
        advanceButton.setText("AUKERATU PREZIO BAT  →");
        advanceButton.setBackground(makeBackground(Color.rgb(26, 61, 94), 15, Color.rgb(53, 95, 137)));
    }

    // Prezio zuzena eta hiru aukera oker sortu; aukerak nahastu.
    private List<Double> buildPriceOptions(double correctPrice) {
        List<Double> options = new ArrayList<>();
        options.add(correctPrice);
        double[] factors = {0.60, 0.72, 0.84, 1.16, 1.28, 1.40, 1.55, 0.48, 0.91, 1.09};
        int attempts = 0;
        while (options.size() < 4 && attempts < 100) {
            double factor = factors[random.nextInt(factors.length)];
            double candidate = Math.max(0.5, Math.round(correctPrice * factor * 100.0) / 100.0);
            boolean duplicate = false;
            for (double current : options) {
                if (Math.abs(current - candidate) < 0.005) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) options.add(candidate);
            attempts++;
        }
        double factor = 1.13;
        while (options.size() < 4) {
            options.add(Math.round(correctPrice * factor * 100.0) / 100.0);
            factor += 0.12;
        }
        Collections.shuffle(options, random);
        return options;
    }

    // Erantzuna egiaztatu, puntuazioa eguneratu eta botoiak blokeatu.
    private void choosePrice(Button selected, double chosenPrice) {
        if (answered) return;
        answered = true;

        boolean correct = Math.abs(chosenPrice - currentProduct.price) < 0.005;
        if (correct) {
            correctAnswers++;
            sessionPoints += POINTS_PER_CORRECT;
            lifetimePoints += POINTS_PER_CORRECT;
            feedbackText.setText("ZUZENA!  +" + POINTS_PER_CORRECT + " PUNTU");
            feedbackText.setTextColor(CYAN);
        } else {
            feedbackText.setText("EZ DA ZUZENA. Prezioa: " + formatPrice(currentProduct.price));
            feedbackText.setTextColor(Color.rgb(255, 181, 130));
        }
        lifetimeQuestions++;

        for (int i = 0; i < choiceButtons.size(); i++) {
            Button b = choiceButtons.get(i);
            double price = choicePrices.get(i);
            b.setEnabled(false);
            if (Math.abs(price - currentProduct.price) < 0.005) {
                b.setBackground(makeBackground(GREEN, 14, CYAN));
            } else if (b == selected && !correct) {
                b.setBackground(makeBackground(RED, 14, Color.rgb(255, 80, 108)));
            }
        }

        database.saveStats(lifetimePoints, lifetimeQuestions);
        scoreText.setText(sessionPoints + " / 100\nPUNTUAK");

        advanceButton.setEnabled(true);
        advanceButton.setBackground(makeBackground(BLUE, 15, CYAN));
        if (questionIndex == TOTAL_QUESTIONS - 1) {
            advanceButton.setText("EMAITZAK IKUSI  →");
        } else {
            advanceButton.setText("HURRENGO PRODUKTUA  →");
        }
    }

    // Hurrengo galderara joan edo emaitzen pantaila ireki.
    private void advance() {
        if (!answered) return;
        if (questionIndex == TOTAL_QUESTIONS - 1) {
            showResults();
        } else {
            questionIndex++;
            displayQuestion();
        }
    }

    // Emaitza behin gorde, sailkapena erakutsi eta berriro jokatzeko aukerak eman.
    private void showResults() {

        // Emaitza behin bakarrik gordetzen da partida bakoitzeko.

        // Emaitza lokalean gorde eta zerbitzarira bidali.
        if (!resultSaved && currentUser != null) {
            long playedAt = System.currentTimeMillis();

            String resultId = database.saveGameResult(
                    currentUser.id,
                    sessionPoints,
                    correctAnswers,
                    TOTAL_QUESTIONS
            );

            resultSaved = true;

            RankingApi.saveResult(
                    resultId,
                    currentUser.username,
                    sessionPoints,
                    correctAnswers,
                    TOTAL_QUESTIONS,
                    playedAt,
                    new RankingApi.Callback() {
                        @Override
                        public void onSuccess(org.json.JSONObject response) {
                            database.markResultSynced(resultId);

                            android.util.Log.d(
                                    "OmniSysRanking",
                                    "Emaitza zerbitzarian gorde da: " + response
                            );
                        }

                        @Override
                        public void onError(String message) {
                            android.util.Log.e(
                                    "OmniSysRanking",
                                    "Ezin izan da emaitza bidali: " + message
                            );
                        }
                    }
            );
        }

        root = createRoot();
        setContentView(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(18), dp(20), dp(18), dp(20));

        scroll.addView(content);
        root.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1f)
        );

        TextView logo = makeText(
                "O M N I S Y S", 13, CYAN, true
        );
        logo.setGravity(Gravity.CENTER);
        content.addView(logo);

        TextView title = makeText(
                "PARTIDA AMAITUTA!", 25, WHITE, true
        );
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(18), 0, dp(6));
        content.addView(title);

        TextView subtitle = makeText(
                "Hona hemen zure emaitzak", 14, MUTED, false
        );
        subtitle.setGravity(Gravity.CENTER);
        content.addView(subtitle);

        // Partidaren emaitzen txartela.
        LinearLayout resultCard = makePanel();
        resultCard.setGravity(Gravity.CENTER);
        resultCard.setPadding(
                dp(20), dp(24), dp(20), dp(24)
        );

        LinearLayout.LayoutParams resultLp =
                new LinearLayout.LayoutParams(-1, -2);
        resultLp.topMargin = dp(22);
        content.addView(resultCard, resultLp);

        TextView score = makeText(
                sessionPoints + " / 100", 40, CYAN, true
        );
        score.setGravity(Gravity.CENTER);
        resultCard.addView(score);

        TextView pointsLabel = makeText(
                "PUNTU", 12, WHITE, true
        );
        pointsLabel.setGravity(Gravity.CENTER);
        pointsLabel.setPadding(0, dp(2), 0, dp(18));
        resultCard.addView(pointsLabel);

        TextView correct = makeText(
                "ERANTZUN ZUZENAK     "
                        + correctAnswers + " / " + TOTAL_QUESTIONS,
                15, WHITE, true
        );
        correct.setGravity(Gravity.CENTER);
        resultCard.addView(correct);

        TextView wrong = makeText(
                "ERANTZUN OKERRAK     "
                        + (TOTAL_QUESTIONS - correctAnswers)
                        + " / " + TOTAL_QUESTIONS,
                14, MUTED, false
        );
        wrong.setGravity(Gravity.CENTER);
        wrong.setPadding(0, dp(10), 0, 0);
        resultCard.addView(wrong);

        TextView encouragement = makeText(
                resultMessage(), 15, CYAN, true
        );
        encouragement.setGravity(Gravity.CENTER);
        encouragement.setPadding(
                dp(10), dp(18), dp(10), dp(12)
        );
        content.addView(encouragement);

        // TOP 5 ranking-a datu-basetik irakurtzen da.
        LinearLayout rankingCard = makePanel();
        rankingCard.setPadding(
                dp(14), dp(14), dp(14), dp(14)
        );

        LinearLayout.LayoutParams rankingLp =
                new LinearLayout.LayoutParams(-1, -2);
        rankingLp.topMargin = dp(10);
        content.addView(rankingCard, rankingLp);

        TextView rankingTitle = makeText(
                "🏆  TOP 5 JOKALARIAK", 16, CYAN, true
        );
        rankingTitle.setPadding(0, 0, 0, dp(8));
        rankingCard.addView(rankingTitle);

        // Hasieran tokiko sailkapena erakutsi; zerbitzariak erantzuten badu, eguneratu.
        showLocalRanking(rankingCard);
        loadServerRanking(rankingCard);

        // Botoiak.
        Button replay = makeButton(
                "BERRIRO JOLASTU", BLUE
        );

        LinearLayout.LayoutParams replayLp =
                new LinearLayout.LayoutParams(-1, dp(54));
        replayLp.topMargin = dp(14);
        content.addView(replay, replayLp);

        replay.setOnClickListener(v -> startGame());

        Button home = makeButton(
                "HASIERARA ITZULI", PANEL_LIGHT
        );

        LinearLayout.LayoutParams homeLp =
                new LinearLayout.LayoutParams(-1, dp(52));
        homeLp.topMargin = dp(9);
        content.addView(home, homeLp);

        home.setOnClickListener(v -> showHome());
    }



    private String resultMessage() {
        if (correctAnswers >= 9) return "Bikain! Prezioak oso ondo ezagutzen dituzu.";
        if (correctAnswers >= 6) return "Oso ondo! Jarraitu horrela.";
        if (correctAnswers >= 3) return "Ondo saiatu zara. Beste partida bat egin!";
        return "Praktikatzen jarraitu eta hobetu zure marka!";
    }

    private void showMessageScreen(String title, String message) {
        root = createRoot();
        setContentView(root);
        TextView t = makeText(title, 24, WHITE, true);
        root.addView(t);
        TextView m = makeText(message, 15, MUTED, false);
        root.addView(m);
        Button home = makeButton("HASIERARA ITZULI", BLUE);
        root.addView(home);
        home.setOnClickListener(v -> showHome());
    }

    // Pantaila guztiek erabiltzen duten atzeko edukiontzia sortu.
    private LinearLayout createRoot() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(14), dp(7), dp(14), dp(7));
        layout.setBackgroundColor(BG);
        return layout;
    }

    private LinearLayout makePanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(12), dp(12), dp(12));
        panel.setBackground(makeBackground(PANEL, 18, Color.rgb(0, 132, 190)));
        return panel;
    }

    private TextView makeText(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button makeButton(String label, int color) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(WHITE);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(makeBackground(color, 14, Color.rgb(0, 165, 220)));
        button.setPadding(dp(6), dp(4), dp(6), dp(4));
        return button;
    }

    private GradientDrawable makeBackground(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private String formatPrice(double amount) {
        // Odoo-ko demo-datuak USDtan ezarri dira; diru-unitatea alda daiteke gero enpresaren monetara.
        return NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
    // Zerbitzariaren konexioa eskuz probatzeko botoiaren ekintza.
    private void testRankingServer() {
        RankingApi.checkServer(new RankingApi.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Zerbitzaria")
                        .setMessage("Konexioa ondo dago!\n\n" + response)
                        .setPositiveButton("ADOS", null)
                        .show();
            }

            @Override
            public void onError(String message) {
                new android.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("Konexio-errorea")
                        .setMessage(message)
                        .setPositiveButton("ADOS", null)
                        .show();
            }
        });
    }


    // Zerbitzariko TOP 5a kargatu; errorean tokiko sailkapena mantendu.
    private void loadServerRanking(LinearLayout rankingCard) {
        RankingApi.getTopFive(new RankingApi.Callback() {
            @Override
            public void onSuccess(org.json.JSONObject response) {
                org.json.JSONArray entries = response.optJSONArray("entries");

                if (entries == null) {
                    // Erantzuna espero bezalakoa ez bada, tokiko sailkapena mantendu.
                    return;
                }

                // Izenburua lehen elementua da; kendu aurreko errenkadak.
                if (rankingCard.getChildCount() > 1) {
                    rankingCard.removeViews(
                            1, rankingCard.getChildCount() - 1
                    );
                }

                showRankingEntries(rankingCard, entries);
            }

            @Override
            public void onError(String message) {
                // Zerbitzaria erabilgarri ez badago, tokiko TOP 5a ikusgai geratzen da.
                android.util.Log.w(
                        "OmniSysRanking",
                        "Tokiko TOP 5a erabiliko da: " + message
                );
            }
        });
    }


    // Konexiorik gabe gordetako emaitzak bidali eta sinkronizatuta markatu.
    private void syncPendingResults() {
        org.json.JSONArray pending = database.getPendingResults();

        for (int i = 0; i < pending.length(); i++) {
            org.json.JSONObject item = pending.optJSONObject(i);
            if (item == null) {
                continue;
            }

            String resultId = item.optString("resultId", "");
            String username = item.optString("username", "");

            int score = item.optInt("score", 0);
            int correctAnswers = item.optInt("correctAnswers", 0);
            int totalQuestions = item.optInt("totalQuestions", 10);
            long playedAt = item.optLong("playedAt", System.currentTimeMillis());

            if (resultId.isEmpty() || username.isEmpty()) {
                continue;
            }

            RankingApi.saveResult(
                    resultId,
                    username,
                    score,
                    correctAnswers,
                    totalQuestions,
                    playedAt,
                    new RankingApi.Callback() {
                        @Override
                        public void onSuccess(org.json.JSONObject response) {
                            database.markResultSynced(resultId);
                            android.util.Log.d(
                                    "OmniSysRanking",
                                    "Sinkronizatuta: " + username
                            );
                        }

                        @Override
                        public void onError(String message) {
                            android.util.Log.e(
                                    "OmniSysRanking",
                                    "Sinkronizazioa atzeratuta: " + message
                            );
                        }
                    }
            );
        }
    }


    private void showLocalRanking(LinearLayout rankingCard) {
        List<LeaderboardEntry> topFive = database.getTopFive();

        if (topFive.isEmpty()) {
            TextView empty = makeText(
                    "Oraindik ez dago emaitzarik.",
                    13, MUTED, false
            );
            rankingCard.addView(empty);
            return;
        }

        for (int i = 0; i < topFive.size(); i++) {
            LeaderboardEntry entry = topFive.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(10), dp(10), dp(10), dp(10));

            int borderColor = Color.rgb(44, 76, 106);
            if (currentUser != null && entry.userId == currentUser.id) {
                borderColor = CYAN;
            }

            row.setBackground(
                    makeBackground(PANEL_LIGHT, 12, borderColor)
            );

            LinearLayout.LayoutParams rowLp =
                    new LinearLayout.LayoutParams(-1, -2);
            rowLp.topMargin = dp(6);
            rankingCard.addView(row, rowLp);

            TextView position = makeText(
                    String.valueOf(i + 1), 15, CYAN, true
            );
            position.setGravity(Gravity.CENTER);
            row.addView(position,
                    new LinearLayout.LayoutParams(dp(28), -2));

            TextView username = makeText(
                    entry.username, 14, WHITE, true
            );
            LinearLayout.LayoutParams usernameLp =
                    new LinearLayout.LayoutParams(0, -2, 1f);
            usernameLp.leftMargin = dp(8);
            row.addView(username, usernameLp);

            TextView points = makeText(
                    entry.bestScore + " p", 14, CYAN, true
            );
            row.addView(points);
        }
    }


    private void showRankingEntries(
            LinearLayout rankingCard,
            org.json.JSONArray entries
    ) {
        if (entries.length() == 0) {
            TextView empty = makeText(
                    "Oraindik ez dago emaitzarik.",
                    13, MUTED, false
            );
            rankingCard.addView(empty);
            return;
        }

        for (int i = 0; i < entries.length(); i++) {
            try {
                org.json.JSONObject entry = entries.getJSONObject(i);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(10), dp(10), dp(10), dp(10));

                row.setBackground(
                        makeBackground(
                                PANEL_LIGHT, 12, Color.rgb(44, 76, 106)
                        )
                );

                LinearLayout.LayoutParams rowLp =
                        new LinearLayout.LayoutParams(-1, -2);
                rowLp.topMargin = dp(6);
                rankingCard.addView(row, rowLp);

                TextView position = makeText(
                        String.valueOf(entry.optInt("position", i + 1)),
                        15, CYAN, true
                );
                position.setGravity(Gravity.CENTER);
                row.addView(
                        position,
                        new LinearLayout.LayoutParams(dp(28), -2)
                );

                TextView username = makeText(
                        entry.optString("username", "Jokalaria"),
                        14, WHITE, true
                );
                LinearLayout.LayoutParams usernameLp =
                        new LinearLayout.LayoutParams(0, -2, 1f);
                usernameLp.leftMargin = dp(8);
                row.addView(username, usernameLp);

                TextView points = makeText(
                        entry.optInt("score", 0) + " p",
                        14, CYAN, true
                );
                row.addView(points);

            } catch (org.json.JSONException e) {
                android.util.Log.e(
                        "OmniSysRanking",
                        "Ezin izan da sailkapeneko errenkada irakurri",
                        e
                );
            }
        }
    }
    // Aplikaziora itzultzean, bidali oraindik sinkronizatu gabeko emaitzak.
    @Override
    protected void onResume() {
        super.onResume();

        if (database != null) {
            syncPendingResults();
        }
    }
}

