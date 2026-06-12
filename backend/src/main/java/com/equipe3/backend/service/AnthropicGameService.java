package com.equipe3.backend.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.equipe3.backend.model.ChatMessage;
import com.equipe3.backend.model.Company;
import com.equipe3.backend.model.DecisionRecord;
import com.equipe3.backend.model.EventOutcome;
import com.equipe3.backend.model.GameEvent;
import com.equipe3.backend.model.LexiconEntry;
import com.equipe3.backend.model.Scores;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Wraps the Claude API for the three AI steps of the game loop:
 * <ul>
 *   <li>event generation ({@code prompts/event_generation.md}),</li>
 *   <li>decision scoring ({@code prompts/event_scoring.md}),</li>
 *   <li>the help chat where the player discusses the situation with the AI.</li>
 * </ul>
 *
 * When {@code anthropic.api-key} is blank the service falls back to built-in
 * sample content so the application keeps working without a key.
 */
@Service
public class AnthropicGameService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicGameService.class);

    /** Themes the AI may pick from (Design §4.1), used to fill <theme-list>. */
    private static final String THEME_LIST = String.join(", ",
            "Financier", "Écologie", "Impact de l'IA", "Politique / réglementation",
            "Concurrence agressive", "Gestion des employés", "Croissance & passage à l'échelle",
            "Relation clients", "Partenariats & Négociation", "Expansion internationale",
            "Incident inattendu lié à l'entreprise", "Communication & Médias",
            "Innovation & Développement produit");

    private static final String CHARACTER_LIST = String.join(", ",
            "une investisseuse", "un employé", "une cliente mécontente", "un journaliste",
            "un concurrent rusé", "une partenaire", "un conseiller", "un fournisseur");

    // Jackson has native record support, so a plain mapper deserializes our
    // record DTOs without any extra module.
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final String model;
    private final AnthropicClient client; // null when no API key is configured

    public AnthropicGameService(
            @Value("${anthropic.api-key:}") String apiKey,
            @Value("${anthropic.model:claude-opus-4-8}") String model) {
        this.model = model;
        if (apiKey != null && !apiKey.isBlank()) {
            this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
            log.info("Claude API enabled (model={}).", model);
        } else {
            this.client = null;
            log.warn("ANTHROPIC_API_KEY is not set — the game will use built-in sample events.");
        }
    }

    public boolean isEnabled() {
        return client != null;
    }

    // ------------------------------------------------------------------ events

    /** Generate the next event for the given company. */
    public GameEvent generateEvent(Company company) {
        if (client == null) {
            return fallbackEvent(company);
        }
        String system = """
                Tu es le maître du jeu d'un jeu de rôle sur l'entrepreneuriat appelé « Mon Entreprise »,
                destiné à un enfant de 12 ans. Réponds toujours en français, avec un vocabulaire qu'un
                enfant de 12 ans comprend. Reste centré sur le jeu et respecte les garde-fous.

                <theme-list>%s</theme-list>
                Personnages possibles pour présenter le problème : %s. Tu peux aussi en inventer un.

                %s

                Réponds UNIQUEMENT avec un objet JSON valide (aucun texte autour, pas de ```), au format :
                {
                  "problem": "la description narrative du problème (2-4 phrases)",
                  "solutions": ["solution 1", "solution 2", "solution 3"],
                  "character": "qui présente le problème",
                  "lexicon": [{"term": "terme technique", "definition": "définition simple pour un enfant de 12 ans"}]
                }
                """.formatted(THEME_LIST, CHARACTER_LIST, loadPrompt("prompts/event_generation.md"));

        String user = "Contexte de l'entreprise :\n" + companyContext(company)
                + "\n\nGénère le prochain événement (numéro "
                + (company.getEventNumber() + 1) + " sur " + Company.MAX_EVENTS + ").";

        try {
            String json = extractJson(call(system, user, 2000L));
            GameEvent event = mapper.readValue(json, GameEvent.class);
            if (event.problem() == null || event.solutions().isEmpty()) {
                throw new IllegalStateException("Empty event returned by the model.");
            }
            return event;
        } catch (Exception e) {
            log.error("Event generation failed, using fallback.", e);
            return fallbackEvent(company);
        }
    }

    // ---------------------------------------------------------------- scoring

    /** Score a player's decision and produce its narrative consequence. */
    public EventOutcome scoreDecision(Company company, String problem, String solution) {
        if (client == null) {
            return fallbackOutcome(solution);
        }
        String template = loadPrompt("prompts/event_scoring.md")
                .replace("<context>", companyContext(company))
                .replace("<problem>", problem == null ? "" : problem)
                .replace("<solution>", solution == null ? "" : solution);

        String system = """
                Tu es le maître du jeu d'un jeu de rôle sur l'entrepreneuriat pour un enfant de 12 ans.
                Réponds toujours en français, avec un ton fun et bienveillant.

                %s

                Les indicateurs vont de 0 à 100. Chaque variation (delta) doit être un entier entre -30 et 30,
                cohérent avec la décision et le contexte. Les effets peuvent être contradictoires
                (par exemple +argent mais -écologie).

                Réponds UNIQUEMENT avec un objet JSON valide (aucun texte autour, pas de ```), au format :
                {
                  "narrative": "la conséquence narrative de la décision (2-4 phrases, fun)",
                  "moneyDelta": 0,
                  "ecologyDelta": 0,
                  "ethicsDelta": 0
                }
                """.formatted(template);

        try {
            String json = extractJson(call(system, "Donne la conséquence et les variations.", 1500L));
            return mapper.readValue(json, EventOutcome.class);
        } catch (Exception e) {
            log.error("Decision scoring failed, using fallback.", e);
            return fallbackOutcome(solution);
        }
    }

    // ------------------------------------------------------------------- chat

    /** Free-form help chat: the AI helps the kid think without giving the answer. */
    public String chat(Company company, String playerMessage) {
        if (client == null) {
            return "Bonne question ! Pense aux conséquences de chaque choix sur ton argent 💰, "
                    + "ton écologie 🌱 et ton image 🤝. (Assistant IA hors-ligne pour l'instant.)";
        }
        String system = """
                Tu es l'assistant IA d'un enfant de 12 ans qui dirige une entreprise dans un jeu.
                Aide-le à RÉFLÉCHIR à sa décision sans jamais révéler la « meilleure » solution.
                Réponds en français, de façon courte (2-3 phrases), fun et encourageante.
                """;

        StringBuilder user = new StringBuilder("Contexte de l'entreprise :\n")
                .append(companyContext(company)).append('\n');
        GameEvent event = company.getCurrentEvent();
        if (event != null) {
            user.append("\nProblème actuel : ").append(event.problem())
                .append("\nSolutions proposées : ").append(String.join(" | ", event.solutions()))
                .append('\n');
        }
        List<ChatMessage> chat = company.getChat();
        if (!chat.isEmpty()) {
            user.append("\nDiscussion jusqu'ici :\n");
            for (ChatMessage m : chat) {
                user.append("user".equals(m.author()) ? "Joueur : " : "Assistant : ")
                    .append(m.content()).append('\n');
            }
        }
        user.append("\nLe joueur demande : ").append(playerMessage);

        try {
            return call(system, user.toString(), 800L).trim();
        } catch (Exception e) {
            log.error("Help chat failed, using fallback.", e);
            return "Hmm, réfléchissons ensemble : quel choix protège le mieux ton entreprise sur la durée ? 🤔";
        }
    }

    // --------------------------------------------------------------- internals

    private String call(String system, String user, long maxTokens) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .system(system)
                .addUserMessage(user)
                .build();
        Message response = client.messages().create(params);
        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .collect(Collectors.joining());
    }

    private String companyContext(Company company) {
        Scores s = company.getScores();
        StringBuilder sb = new StringBuilder()
                .append("- Nom : ").append(company.getName()).append('\n')
                .append("- Type : ").append(company.getType() == null ? "non précisé" : company.getType()).append('\n')
                .append("- Avatar du joueur : ").append(company.getCharacter() == null ? "non précisé" : company.getCharacter()).append('\n')
                .append("- Indicateurs (0-100) : 💰 argent ").append(s.getMoney())
                .append(", 🌱 écologie ").append(s.getEcology())
                .append(", 🤝 image ").append(s.getEthics()).append('\n');
        List<DecisionRecord> history = company.getHistory();
        if (history.isEmpty()) {
            sb.append("- Historique des décisions : aucune pour l'instant.\n");
        } else {
            sb.append("- Historique des décisions :\n");
            for (DecisionRecord r : history) {
                sb.append("    • « ").append(r.problem()).append(" » → choix : « ")
                  .append(r.solution()).append(" »\n");
            }
        }
        return sb.toString();
    }

    /** Strips optional ``` fences and isolates the outermost JSON object. */
    private static String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) {
                t = t.substring(firstNewline + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
            t = t.trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return t.substring(start, end + 1);
        }
        return t;
    }

    private String loadPrompt(String resourcePath) {
        try {
            return new ClassPathResource(resourcePath).getContentAsString(StandardCharsets.UTF_8).trim();
        } catch (IOException e) {
            log.error("Could not read prompt template {}", resourcePath, e);
            return "";
        }
    }

    // ------------------------------------------------------------- fallbacks

    private GameEvent fallbackEvent(Company company) {
        int n = company.getEventNumber();
        // A small rotation of offline sample events so the loop is playable.
        List<GameEvent> samples = List.of(
                new GameEvent(
                        "Une grande chaîne de magasins veut vendre tes produits, mais elle demande un prix "
                                + "très bas. Tu gagnerais beaucoup de clients, mais bien moins d'argent par vente.",
                        List.of("Accepter pour gagner en notoriété",
                                "Refuser et garder tes prix",
                                "Négocier un prix au milieu"),
                        "une investisseuse", "",
                        List.of(new LexiconEntry("Notoriété",
                                "À quel point les gens connaissent ton entreprise."))),
                new GameEvent(
                        "Tes emballages en plastique coûtent peu cher, mais polluent. Un fournisseur propose "
                                + "des emballages en carton recyclé, plus chers.",
                        List.of("Passer au carton recyclé",
                                "Garder le plastique pour économiser",
                                "Mélanger les deux pour tester"),
                        "un employé soucieux de l'environnement", "",
                        List.of(new LexiconEntry("Fournisseur",
                                "L'entreprise qui te vend ce dont tu as besoin pour fabriquer tes produits."))),
                new GameEvent(
                        "Un journaliste a remarqué que ton entreprise grandit vite. Il veut écrire un article, "
                                + "mais il pose des questions difficiles sur tes employés.",
                        List.of("Être transparent et tout expliquer",
                                "Refuser l'interview",
                                "Préparer une réponse avec ton équipe"),
                        "un journaliste curieux", "",
                        List.of(new LexiconEntry("Transparence",
                                "Dire la vérité et ne rien cacher d'important."))));
        return samples.get(n % samples.size());
    }

    private EventOutcome fallbackOutcome(String solution) {
        // Deterministic but varied deltas so offline play still moves the gauges.
        int seed = solution == null ? 0 : Math.abs(solution.hashCode());
        int money = (seed % 21) - 8;        // -8..12
        int ecology = ((seed / 7) % 17) - 8; // -8..8
        int ethics = ((seed / 13) % 17) - 6; // -6..10
        String narrative = "Ta décision a des conséquences ! Ton équipe observe les résultats : "
                + "certains indicateurs montent, d'autres baissent. Continue à bien réfléchir 🚀";
        return new EventOutcome(narrative, money, ecology, ethics);
    }
}
