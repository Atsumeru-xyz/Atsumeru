package com.atsumeru.web.enums;

import com.atsumeru.web.util.GUString;

import java.util.ArrayList;
import java.util.HashMap;

public enum Genre {
    ACTION,
    ADULT,
    ADVENTURE,
    COMEDY,
    DOUJINSHI,
    DRAMA,
    ECCHI,
    FANTASY,
    GENDERBENDER,
    HAREM,
    HISTORICAL,
    HORROR,
    JOSEI,
    MAGIC,
    MARTIALARTS,
    MECHA,
    MYSTERY,
    ONESHOT,
    PSYCHOLOGICAL,
    ROMANCE,
    SCHOOLLIFE,
    SCIFI,
    SEINEN,
    SHOUJO,
    SHOUJOAI,
    SHOUNEN,
    SHOUNENAI,
    SLICEOFLIFE,
    SPORTS,
    SUPERNATURAL,
    TRAGEDY,
    YAOI,
    YURI;

    static final String[] ActionArr;
    static final String[] AdultArr;
    static final String[] AdventureArr;
    static final String[] ComedyArr;
    static final String[] DoujinshiArr;
    static final String[] DramaArr;
    static final String[] EcchiArr;
    static final String[] FantasyArr;
    static final String[] GenderBenderArr;
    static final String[] HaremArr;
    static final String[] HistoricalArr;
    static final String[] HorrorArr;
    static final String[] JoseiArr;
    static final String[] MagicArr;
    static final String[] MartialArtsArr;
    static final String[] MechaArr;
    static final String[] MysteryArr;
    static final String[] OneShotArr;
    static final String[] PsychologicalArr;
    static final String[] RomanceArr;
    static final String[] SchoolLifeArr;
    static final String[] SciFiArr;
    static final String[] SeinenArr;
    static final String[] ShoujoArr;
    static final String[] ShoujoAiArr;
    static final String[] ShounenArr;
    static final String[] ShounenAiArr;
    static final String[] SliceOfLifeArr;
    static final String[] SportsArr;
    static final String[] SupernaturalArr;
    static final String[] TragedyArr;
    static final String[] YaoiArr;
    static final String[] YuriArr;
    static HashMap<Genre, String[]> MangaGenreStr;

    public static Genre getGenreFromString(String string) {
        string = string.toLowerCase().trim();
        for (Genre genre : Genre.MangaGenreStr.keySet()) {
            if (isEq(string, Genre.MangaGenreStr.get(genre))) {
                return genre;
            }
        }
        return null;
    }

    static boolean isEq(String value, String[] arr) {
        for (String str : arr) {
            if (str.equals(value)) {
                return true;
            }
        }
        return false;
    }

    public static boolean addGenreFromString(ArrayList<Genre> genres, String genreStr) {
        if (GUString.isEmpty(genreStr)) {
            return false;
        }
        genreStr = genreStr.toLowerCase().trim();
        for (Genre genre : Genre.MangaGenreStr.keySet()) {
            if (isEq(genreStr, Genre.MangaGenreStr.get(genre))) {
                if (!genres.contains(genre)) {
                    genres.add(genre);
                }
                return true;
            }
        }
        return false;
    }

    static {
        ActionArr = new String[] { "action", "acción", "live action", "azione", "ação", "حركة", "боевик" };
        AdultArr = new String[] { "adult", "mature", "erotica", "erotic", "adulte", "adulta", "adulto", "maduro", "maduras", "madura", "orgía", "эротика", "18_плюс", "хентай" };
        AdventureArr = new String[] { "adventure", "aventure", "aventura", "aventuras", "avventura", "مغامرات", "приключения" };
        ComedyArr = new String[] { "comedy", "comédie", "comédia", "comedia", "humor", "commedia", "مضحك", "комедия" };
        DoujinshiArr = new String[] { "doujinshi", "hentai", "dounshinji", "додзинси" };
        DramaArr = new String[] { "drama", "drame", "drama", "drama", "drammatico", "دراما", "драма" };
        EcchiArr = new String[] { "ecchi", "إيتشي", "eichii" };
        FantasyArr = new String[] { "fantasy", "fantaisie", "fantasía", "خيال", "fantastique", "fantasia", "fantasi", "fantasia", "fantasia negra", "fantasy", "фэнтези", "героическое_фэнтези" };
        GenderBenderArr = new String[] { "gender bender", "sexedit", "gender+intriga", "travelo", "transexual", "гендерная_интрига" };
        HaremArr = new String[] { "harem", "harem", "harén", "harém", "гарем" };
        HistoricalArr = new String[] { "historical", "historic", "historique", "histórica", "histórico", "storico", "تاريخي", "история" };
        HorrorArr = new String[] { "horror", "horreur", "رعب", "ужасы" };
        JoseiArr = new String[] { "josei", "dsesay", "جوسيّ", "дзёсэй" };
        MagicArr = new String[] { "magic", "magico", "magia" };
        MartialArtsArr = new String[] { "martial arts", "martial_arts", "martial+arts", "fightskill", "arts martiaux", "artes marciales", "فنون قتالية", "arte marcial", "artes marciais", "samurai", "боевые_искусства", "самурайский_боевик" };
        MechaArr = new String[] { "mecha", "меха" };
        MysteryArr = new String[] { "mystery", "mystère", "misterio", "mistério", "misteri", "غموض", "мистика" };
        OneShotArr = new String[] { "one shot", "one-shot", "فصل واحد" };
        PsychologicalArr = new String[] { "psychological", "psycho", "psicologico", "psicológico", "نفساني", "psychologique", "psicologico", "психология" };
        RomanceArr = new String[] { "romance", "romantica", "romantico", "romántica", "romántico", "romã?ntico", "sentimentale", "رومانسي", "романтика" };
        SchoolLifeArr = new String[] { "school life", "school", "vie scolaire", "escolar", "vida escola", "vida escolar", "scolastico", "حياة مدرسية", "школа" };
        SciFiArr = new String[] { "sci_fi", "sci-fi", "sci fi", "scifi", "fantastic", "science fiction", "ciencia ficción", "رجل آلي", "خيال علمي", "science-fiction", "fantascienza", "ciência ficção", "ficção científica", "научная_фантастика", "фантастика" };
        SeinenArr = new String[] { "seinen", "سيّنين", "сэйнэн", "сэйнен" };
        ShoujoArr = new String[] { "shoujo", "shojo", "shōjo", "شوجو", "shôjo", "сёдзё" };
        ShoujoAiArr = new String[] { "shoujo ai", "shoujo-ai", "shoujo_ai", "shoujoai", "сёдзё-ай" };
        ShounenArr = new String[] { "shounen", "shonen", "shonem", "shōnen", "شونين", "shônen", "сёнэн" };
        ShounenAiArr = new String[] { "shounen ai", "shounen-ai", "shounen_ai", "shounenai" };
        SliceOfLifeArr = new String[] { "slice of life", "slice-of-life", "natural", "routine", "tranche de vie", "vida real", "vida cotidiana", "vita quotidiana", "cotidiano", "حركة حياتية", "شريحة من الحياة", "повседневность" };
        SportsArr = new String[] { "sports", "sport", "deporte", "deportes", "sportivo", "esporte", "esportes", "رياضة", "спорт" };
        SupernaturalArr = new String[] { "supernatural", "surnaturel", "sobrenatural", "sobrenarutal", "sovrannaturale", "soprannaturale", "قوى خارقة", "сверхъестественное" };
        TragedyArr = new String[] { "tragedy", "tragédie", "tragedia", "tragédia", "tragico", "مأسوي", "трагедия" };
        YaoiArr = new String[] { "yaoi", "яой" };
        YuriArr = new String[] { "yuri", "юри" };

        Genre.MangaGenreStr = new HashMap<Genre, String[]>() {{
            put(Genre.ACTION, Genre.ActionArr);
            put(Genre.ADULT, Genre.AdultArr);
            put(Genre.ADVENTURE, Genre.AdventureArr);
            put(Genre.COMEDY, Genre.ComedyArr);
            put(Genre.DOUJINSHI, Genre.DoujinshiArr);
            put(Genre.DRAMA, Genre.DramaArr);
            put(Genre.ECCHI, Genre.EcchiArr);
            put(Genre.FANTASY, Genre.FantasyArr);
            put(Genre.GENDERBENDER, Genre.GenderBenderArr);
            put(Genre.HAREM, Genre.HaremArr);
            put(Genre.HISTORICAL, Genre.HistoricalArr);
            put(Genre.HORROR, Genre.HorrorArr);
            put(Genre.JOSEI, Genre.JoseiArr);
            put(Genre.MAGIC, Genre.MagicArr);
            put(Genre.MARTIALARTS, Genre.MartialArtsArr);
            put(Genre.MECHA, Genre.MechaArr);
            put(Genre.MYSTERY, Genre.MysteryArr);
            put(Genre.ONESHOT, Genre.OneShotArr);
            put(Genre.PSYCHOLOGICAL, Genre.PsychologicalArr);
            put(Genre.ROMANCE, Genre.RomanceArr);
            put(Genre.SCHOOLLIFE, Genre.SchoolLifeArr);
            put(Genre.SCIFI, Genre.SciFiArr);
            put(Genre.SEINEN, Genre.SeinenArr);
            put(Genre.SHOUJO, Genre.ShoujoArr);
            put(Genre.SHOUJOAI, Genre.ShoujoAiArr);
            put(Genre.SHOUNEN, Genre.ShounenArr);
            put(Genre.SHOUNENAI, Genre.ShounenAiArr);
            put(Genre.SLICEOFLIFE, Genre.SliceOfLifeArr);
            put(Genre.SPORTS, Genre.SportsArr);
            put(Genre.SUPERNATURAL, Genre.SupernaturalArr);
            put(Genre.TRAGEDY, Genre.TragedyArr);
            put(Genre.YAOI, Genre.YaoiArr);
            put(Genre.YURI, Genre.YuriArr);
        }};
    }
}