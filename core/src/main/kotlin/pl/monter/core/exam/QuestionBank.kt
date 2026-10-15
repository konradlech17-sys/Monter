package pl.monter.core.exam

import pl.monter.core.exam.SepCategory.INSTALL
import pl.monter.core.exam.SepCategory.LAW
import pl.monter.core.exam.SepCategory.MEASURE
import pl.monter.core.exam.SepCategory.RESCUE
import pl.monter.core.exam.SepCategory.SHOCK

/**
 * Bank pytań przygotowujących do egzaminu SEP – grupa 1, eksploatacja (E), urządzenia do 1 kV.
 * To NIE są oficjalne pytania komisji (te nie są publikowane) – to pytania sprawdzające tę samą wiedzę,
 * oparte na normach PN-HD 60364, PN-EN 60445, przepisach BHP i Prawie budowlanym/energetycznym.
 */
object QuestionBank {

    private const val HD41 = "PN-HD 60364-4-41"
    private const val HD6 = "PN-HD 60364-6"
    private const val BHP = "Rozporządzenie w sprawie BHP przy urządzeniach energetycznych"

    val static: List<Question> = listOf(
        // =============================================================== PRZEPISY I ORGANIZACJA PRACY
        q(
            "law.who", LAW, "Kto zgodnie z Prawem energetycznym musi mieć świadectwo kwalifikacyjne?",
            "Osoby zajmujące się eksploatacją sieci, urządzeń i instalacji określonych w przepisach",
            listOf("Każdy właściciel instalacji w domu", "Wyłącznie pracownicy zakładów energetycznych", "Tylko osoby z wykształceniem wyższym elektrycznym"),
            "Obowiązek dotyczy osób, które eksploatują (obsługują, konserwują, naprawiają, montują, mierzą) urządzenia, instalacje i sieci wskazane w przepisach. Kwalifikacje potwierdza świadectwo wydane po egzaminie przed komisją.",
            "Prawo energetyczne, art. 54",
        ),
        q(
            "law.e", LAW, "Co oznacza zakres „E\" na świadectwie kwalifikacyjnym?",
            "Eksploatację – wykonywanie prac przy urządzeniach",
            listOf("Dozór – kierowanie pracami i nadzór", "Energetykę zawodową", "Egzamin z elektrotechniki ogólnej"),
            "„E\" – eksploatacja: wykonywanie prac (obsługa, konserwacja, naprawy, montaż, pomiary). „D\" – dozór: kierowanie czynnościami osób eksploatujących i nadzór nad nimi.",
        ),
        q(
            "law.d", LAW, "Czym zajmuje się osoba ze świadectwem w zakresie „D\" (dozór)?",
            "Kieruje czynnościami osób wykonujących eksploatację i sprawuje nad nimi nadzór",
            listOf("Wyłącznie wykonuje pomiary", "Wyłącznie obsługuje urządzenia", "Projektuje instalacje elektryczne"),
            "Dozór to funkcje kierownicze i nadzorcze – planowanie, organizacja pracy, odpowiedzialność za bezpieczeństwo. Często potrzebne są oba świadectwa: E i D.",
        ),
        q(
            "law.g1", LAW, "Jakie urządzenia obejmuje grupa 1 świadectw kwalifikacyjnych?",
            "Urządzenia, instalacje i sieci elektroenergetyczne",
            listOf("Urządzenia cieplne", "Urządzenia gazowe", "Urządzenia dźwigowe"),
            "Grupa 1 – elektroenergetyczne, grupa 2 – cieplne, grupa 3 – gazowe.",
        ),
        q(
            "law.scope", LAW, "Co należy do zakresu prac eksploatacyjnych?",
            "Obsługa, konserwacja, naprawy, montaż i prace kontrolno-pomiarowe",
            listOf("Wyłącznie obsługa bieżąca", "Projektowanie i kosztorysowanie", "Sprzedaż i dystrybucja energii"),
            "Na egzaminie zwykle zaznacza się, których rodzajów prac dotyczy świadectwo – np. „kontrolno-pomiarowe\" uprawnia do wykonywania i podpisywania pomiarów.",
        ),
        q(
            "law.orders", LAW, "Jakie rodzaje poleceń wykonania pracy przewidują przepisy BHP przy urządzeniach energetycznych?",
            "Pisemne i ustne",
            listOf("Wyłącznie pisemne", "Wyłącznie ustne", "Telefoniczne i mailowe jako jedyne"),
            "Prace w warunkach szczególnego zagrożenia wykonuje się na polecenie – pisemne lub ustne, zależnie od rodzaju pracy i ustaleń w instrukcji organizacji bezpiecznej pracy.",
            BHP,
        ),
        q(
            "law.dopuszczajacy", LAW, "Kto przygotowuje miejsce pracy i dopuszcza zespół do pracy?",
            "Dopuszczający",
            listOf("Poleceniodawca", "Członek zespołu", "Inspektor nadzoru budowlanego"),
            "Dopuszczający sprawdza i przygotowuje miejsce pracy (wyłączenia, zabezpieczenia, sprawdzenie braku napięcia, uziemienia), a potem dopuszcza zespół, przekazując miejsce pracy kierującemu.",
            BHP,
        ),
        q(
            "law.polecajacy", LAW, "Kto wydaje polecenie wykonania pracy?",
            "Poleceniodawca",
            listOf("Dopuszczający", "Kierujący zespołem", "Najstarszy stażem członek zespołu"),
            "Poleceniodawca określa zakres pracy, środki bezpieczeństwa i skład zespołu. Funkcje te są rozdzielone, by kilka osób niezależnie dbało o bezpieczeństwo.",
            BHP,
        ),
        q(
            "law.kierujacy", LAW, "Kto odpowiada za bezpieczne wykonywanie pracy przez zespół na stanowisku?",
            "Kierujący zespołem (pracami)",
            listOf("Poleceniodawca", "Dostawca energii", "Właściciel budynku"),
            "Kierujący zespołem przejmuje miejsce pracy od dopuszczającego, instruuje członków zespołu i pilnuje przestrzegania zasad przez cały czas pracy.",
            BHP,
        ),
        q(
            "law.two", LAW, "Ile osób co najmniej wykonuje prace w warunkach szczególnego zagrożenia?",
            "Co najmniej dwie",
            listOf("Jedna, jeśli ma uprawnienia D", "Co najmniej trzy", "Liczba nie ma znaczenia"),
            "Druga osoba może natychmiast wyłączyć zasilanie i udzielić pomocy w razie porażenia. Przepisy dopuszczają nieliczne wyjątki określone w instrukcji.",
            BHP,
        ),
        q(
            "law.special", LAW, "Która praca jest przykładem pracy w warunkach szczególnego zagrożenia?",
            "Praca w pobliżu nieosłoniętych urządzeń znajdujących się pod napięciem",
            listOf("Wymiana żarówki w wyłączonym obwodzie oświetlenia", "Odczyt licznika energii", "Sprzątanie pomieszczenia biurowego"),
            "Szczególne zagrożenie to m.in. prace pod napięciem, w pobliżu urządzeń pod napięciem, w zbiornikach, na wysokości – wymagają polecenia i dodatkowych środków.",
            BHP,
        ),
        q(
            "law.nocommand", LAW, "Kiedy można przystąpić do pracy bez polecenia?",
            "Gdy trzeba ratować zdrowie lub życie ludzkie",
            listOf("Gdy praca potrwa krócej niż godzinę", "Gdy pracuje doświadczony elektryk", "Nigdy, w żadnej sytuacji"),
            "Ratowanie życia (np. uwolnienie porażonego spod napięcia) nie czeka na formalności – ale trzeba zadbać o własne bezpieczeństwo.",
            BHP,
        ),
        q(
            "law.5years", LAW, "Jak często, zgodnie z Prawem budowlanym, należy sprawdzać instalację elektryczną w budynku (kontrola okresowa)?",
            "Co najmniej raz na 5 lat",
            listOf("Co najmniej raz na 10 lat", "Tylko przy sprzedaży budynku", "Co najmniej raz na 2 lata dla wszystkich budynków"),
            "Kontrola okresowa obejmuje m.in. stan połączeń, zabezpieczeń, środków ochrony przeciwporażeniowej, rezystancję izolacji i uziemienia. Niektóre obiekty mają dodatkowe, częstsze obowiązki.",
            "Prawo budowlane, art. 62",
        ),
        q(
            "law.first", LAW, "Kiedy wykonuje się sprawdzenie odbiorcze nowej instalacji?",
            "Przed oddaniem instalacji do użytkowania",
            listOf("Po roku użytkowania", "Tylko na wniosek ubezpieczyciela", "Przy pierwszej awarii"),
            "Sprawdzenie odbiorcze to oględziny + próby i pomiary wykonywane w trakcie montażu i po jego zakończeniu, zanim użytkownik zacznie korzystać z instalacji.",
            HD6,
        ),
        q(
            "law.inspection", LAW, "Na czym polegają oględziny instalacji?",
            "Na sprawdzeniu wzrokowym stanu instalacji, zwykle przed pomiarami i bez jej rozłączania",
            listOf("Na pomiarze rezystancji izolacji", "Na wymianie zużytych elementów", "Na pomiarze impedancji pętli zwarcia"),
            "Oględziny wykrywają m.in. uszkodzone obudowy, brak osłon, niewłaściwe oznaczenia, dobór zabezpieczeń do przewodów, poprawne kolory żył. Wykonuje się je przed próbami.",
            HD6,
        ),
        q(
            "law.protocol", LAW, "Co powinien zawierać protokół z pomiarów?",
            "Wyniki, datę, użyte przyrządy, ocenę i podpis osoby z uprawnieniami",
            listOf("Wyłącznie podpis właściciela", "Tylko ocenę „dobra\" lub „zła\"", "Wyłącznie zdjęcia rozdzielnicy"),
            "Protokół to dokument – musi pozwalać odtworzyć, co, czym i z jakim wynikiem zmierzono, oraz kto za to odpowiada.",
        ),
        q(
            "law.5rules", LAW, "Która czynność jest PIERWSZA spośród zasad przygotowania bezpiecznego miejsca pracy?",
            "Wyłączyć napięcie i odłączyć urządzenie",
            listOf("Sprawdzić brak napięcia", "Uziemić i zewrzeć", "Osłonić sąsiednie części pod napięciem"),
            "Kolejność: wyłącz → zabezpiecz przed ponownym załączeniem → sprawdź brak napięcia → uziem i zewrzyj → osłoń sąsiednie części pod napięciem.",
            "PN-EN 50110-1",
        ),
        q(
            "law.indicator", LAW, "Co trzeba zrobić ze wskaźnikiem napięcia bezpośrednio przed sprawdzeniem braku napięcia?",
            "Sprawdzić jego działanie na źródle napięcia",
            listOf("Nic, jeśli ma aktualne badanie", "Wymienić baterie", "Uziemić go"),
            "Uszkodzony wskaźnik pokaże „brak napięcia\" także na urządzeniu pod napięciem. Dlatego sprawdza się go przed (a najlepiej też po) pomiarze.",
        ),

        // =============================================================== OCHRONA PRZECIWPORAŻENIOWA
        q(
            "shock.ul50", SHOCK, "Jakie jest dopuszczalne długotrwale napięcie dotykowe prądu przemiennego w warunkach normalnych?",
            "50 V", listOf("25 V", "120 V", "230 V"),
            "50 V AC (120 V DC) w warunkach normalnych. W warunkach o zwiększonym zagrożeniu (wilgoć, łazienki, place budowy) przyjmuje się 25 V AC (60 V DC).",
            HD41,
        ),
        q(
            "shock.ul25", SHOCK, "Jakie dopuszczalne napięcie dotykowe AC przyjmuje się w warunkach zwiększonego zagrożenia (np. wilgoć, plac budowy)?",
            "25 V", listOf("50 V", "12 V", "60 V"),
            "Mokra skóra ma znacznie mniejszą rezystancję, więc ten sam prąd rażenia płynie przy niższym napięciu.",
        ),
        q(
            "shock.ul120", SHOCK, "Jakie jest dopuszczalne długotrwale napięcie dotykowe prądu stałego w warunkach normalnych?",
            "120 V", listOf("50 V", "60 V", "230 V"),
            "Prąd stały jest dla organizmu mniej groźny niż przemienny 50 Hz, stąd wyższa granica.",
        ),
        q(
            "shock.t04", SHOCK, "Jaki jest maksymalny czas samoczynnego wyłączenia w układzie TN dla obwodów odbiorczych do 32 A przy U0 = 230 V?",
            "0,4 s", listOf("5 s", "0,2 s", "1 s"),
            "Obwody odbiorcze (gniazda, odbiorniki ręczne) do 32 A: 0,4 s w TN, 0,2 s w TT. Obwody rozdzielcze i obwody powyżej 32 A: 5 s w TN, 1 s w TT.",
            HD41,
        ),
        q(
            "shock.t5", SHOCK, "Jaki maksymalny czas wyłączenia dopuszcza się w układzie TN dla obwodów rozdzielczych?",
            "5 s", listOf("0,4 s", "0,2 s", "30 s"),
            "Obwody rozdzielcze (np. WLZ) nie zasilają bezpośrednio urządzeń trzymanych w ręku, więc dopuszcza się dłuższy czas.",
            HD41,
        ),
        q(
            "shock.tt02", SHOCK, "Jaki jest maksymalny czas wyłączenia w układzie TT dla obwodów odbiorczych do 32 A przy U0 = 230 V?",
            "0,2 s", listOf("0,4 s", "5 s", "1 s"),
            "W TT prąd zwarcia płynie przez ziemię i jest mniejszy, a napięcie dotykowe może być wyższe – stąd krótszy czas niż w TN.",
            HD41,
        ),
        q(
            "shock.tncond", SHOCK, "Jak brzmi warunek samoczynnego wyłączenia zasilania w układzie TN?",
            "Zs · Ia ≤ U0",
            listOf("Zs · Ia ≥ U0", "RA · IΔn ≤ 50 V", "Zs ≤ U0 · Ia"),
            "Zs – impedancja pętli zwarcia, Ia – prąd zapewniający zadziałanie zabezpieczenia w wymaganym czasie, U0 – napięcie fazowe względem ziemi (230 V).",
            HD41,
        ),
        q(
            "shock.ttcond", SHOCK, "Jaki warunek musi spełniać układ TT chroniony wyłącznikiem RCD?",
            "RA · IΔn ≤ 50 V",
            listOf("Zs · Ia ≤ U0", "RA ≤ 50 Ω niezależnie od RCD", "IΔn ≥ 500 mA"),
            "RA – suma rezystancji uziomu i przewodu ochronnego. Przy prądzie IΔn napięcie na częściach dostępnych nie może przekroczyć 50 V.",
            HD41,
        ),
        q(
            "shock.extra", SHOCK, "Co stanowi ochronę uzupełniającą w instalacjach niskiego napięcia?",
            "Wyłącznik różnicowoprądowy o IΔn ≤ 30 mA",
            listOf("Wyłącznik nadprądowy B16", "Ogranicznik przepięć", "Bezpiecznik topikowy gG"),
            "RCD 30 mA chroni nawet przy dotyku bezpośrednim (gdy zawiodą inne środki), np. przy uszkodzonym przewodzie przedłużacza. Wymagany m.in. dla gniazd do 32 A.",
            HD41,
        ),
        q(
            "shock.basic", SHOCK, "Który środek jest środkiem ochrony podstawowej (przed dotykiem bezpośrednim)?",
            "Izolacja części czynnych",
            listOf("Samoczynne wyłączenie zasilania", "Połączenia wyrównawcze", "Wyłącznik nadprądowy"),
            "Ochrona podstawowa: izolacja, obudowy i osłony, przegrody, przeszkody, umieszczenie poza zasięgiem ręki. Ochrona przy uszkodzeniu: np. samoczynne wyłączenie, izolacja podwójna, separacja.",
            HD41,
        ),
        q(
            "shock.fault", SHOCK, "Który środek jest środkiem ochrony przy uszkodzeniu (przed dotykiem pośrednim)?",
            "Samoczynne wyłączenie zasilania",
            listOf("Izolacja podstawowa przewodu", "Obudowa urządzenia", "Umieszczenie poza zasięgiem ręki"),
            "Gdy izolacja zawiedzie i obudowa znajdzie się pod napięciem, zabezpieczenie (MCB, bezpiecznik, RCD) musi szybko wyłączyć zasilanie.",
            HD41,
        ),
        q(
            "shock.reach", SHOCK, "Jaki jest zasięg ręki w pionie (nad powierzchnią, na której stoi człowiek)?",
            "2,5 m", listOf("1,25 m", "2,0 m", "3,5 m"),
            "Części czynne umieszczone wyżej niż 2,5 m nad stanowiskiem uważa się za poza zasięgiem ręki (w poziomie – 1,25 m).",
        ),
        q(
            "shock.class2", SHOCK, "Jakim symbolem oznacza się urządzenie II klasy ochronności?",
            "Dwa kwadraty jeden w drugim (⧈)",
            listOf("Romb z cyfrą III", "Symbol uziemienia", "Trójkąt z błyskawicą"),
            "Klasa II – izolacja podwójna lub wzmocniona, urządzenie nie ma zacisku PE i nie wolno go do PE podłączać.",
        ),
        q(
            "shock.class1", SHOCK, "Co jest warunkiem ochrony w urządzeniu I klasy ochronności?",
            "Połączenie części przewodzących dostępnych z przewodem ochronnym PE",
            listOf("Zasilanie napięciem SELV", "Izolacja podwójna", "Brak jakichkolwiek części metalowych"),
            "W klasie I obudowa jest uziemiona przez PE – przy uszkodzeniu izolacji płynie prąd zwarciowy i zadziała zabezpieczenie.",
        ),
        q(
            "shock.class3", SHOCK, "Czym charakteryzuje się urządzenie III klasy ochronności?",
            "Jest zasilane bardzo niskim napięciem SELV lub PELV",
            listOf("Ma zacisk PE", "Ma podwójną izolację i zasilanie 230 V", "Jest przeznaczone do sieci IT"),
            "Symbol: romb z cyfrą III. Bezpieczeństwo wynika z niskiego napięcia ze źródła bezpiecznego (np. transformatora bezpieczeństwa).",
        ),
        q(
            "shock.selv", SHOCK, "Czym różni się obwód SELV od PELV?",
            "SELV nie jest uziemiony, PELV ma połączenie z ziemią",
            listOf("SELV ma wyższe napięcie niż PELV", "PELV nie wymaga źródła bezpiecznego", "Niczym – to synonimy"),
            "Oba to bardzo niskie napięcie (≤ 50 V AC / 120 V DC) ze źródła bezpiecznego. PELV dopuszcza uziemienie obwodu (np. ze względów funkcjonalnych).",
        ),
        q(
            "shock.separation", SHOCK, "Na czym polega ochrona przez separację elektryczną?",
            "Na zasilaniu odbiornika z transformatora separacyjnego – obwód nie jest połączony z ziemią",
            listOf("Na podłączeniu obudowy do PE", "Na zastosowaniu RCD 30 mA", "Na zasilaniu napięciem 12 V"),
            "Dotknięcie jednego przewodu obwodu separowanego nie zamyka obwodu przez ciało i ziemię. Zasadniczo z jednego transformatora separacyjnego zasila się jeden odbiornik.",
            HD41,
        ),
        q(
            "shock.rcdA", SHOCK, "Jakie prądy różnicowe wykrywa wyłącznik RCD typu A?",
            "Sinusoidalne przemienne i pulsujące jednokierunkowe",
            listOf("Tylko sinusoidalne przemienne", "Także gładkie prądy stałe", "Tylko prądy o częstotliwości 1 kHz"),
            "AC – tylko sinusoidalne; A – dodatkowo pulsujące; F – dodatkowo mieszane częstotliwości; B – również gładkie prądy stałe (np. falowniki, ładowarki EV).",
        ),
        q(
            "shock.rcdTNC", SHOCK, "Dlaczego wyłącznika RCD nie stosuje się w układzie TN-C?",
            "Bo nie ma osobnego przewodu PE – prąd uszkodzeniowy wraca przez PEN, który przechodzi przez RCD",
            listOf("Bo w TN-C napięcie wynosi 400 V", "Bo RCD działa tylko w układzie IT", "Bo przepisy zakazują RCD w budynkach"),
            "Aby RCD wykrył różnicę prądów, prąd uszkodzeniowy musi wracać drogą omijającą wyłącznik – przewodem PE. W TN-C takiego przewodu nie ma.",
        ),
        q(
            "shock.tncs", SHOCK, "Co obowiązuje po rozdzieleniu przewodu PEN na N i PE w układzie TN-C-S?",
            "Przewodów N i PE nie wolno ponownie ze sobą łączyć",
            listOf("N i PE można łączyć w każdej puszce", "Przewód N należy uziemić w każdym gnieździe", "PE można zastąpić przewodem N"),
            "Ponowne połączenie spowoduje, że prąd roboczy popłynie przewodem ochronnym, a wyłączniki RCD przestaną działać poprawnie.",
        ),
        q(
            "shock.pen10", SHOCK, "Jaki jest minimalny przekrój przewodu PEN z miedzi w instalacji ułożonej na stałe?",
            "10 mm²", listOf("2,5 mm²", "4 mm²", "25 mm²"),
            "PEN: min. 10 mm² Cu lub 16 mm² Al. Przerwa w przewodzie PEN jest bardzo groźna – stąd wymagania wytrzymałości mechanicznej. W PEN nie instaluje się łączników.",
        ),
        q(
            "shock.t_letter", SHOCK, "Co oznacza pierwsza litera „T\" w oznaczeniu układu sieci (np. TN, TT)?",
            "Bezpośrednie połączenie punktu neutralnego źródła z ziemią",
            listOf("Izolację punktu neutralnego od ziemi", "Uziemienie części przewodzących dostępnych", "Układ trójfazowy"),
            "Pierwsza litera opisuje źródło: T – uziemione bezpośrednio, I – izolowane lub uziemione przez impedancję. Druga – części dostępne: N – połączone z punktem neutralnym, T – uziemione niezależnie.",
        ),
        q(
            "shock.it", SHOCK, "Czym charakteryzuje się układ IT?",
            "Punkt neutralny jest izolowany od ziemi lub uziemiony przez dużą impedancję",
            listOf("Ma przewód PEN", "Punkt neutralny jest uziemiony bezpośrednio, a obudowy przez PEN", "Nie ma żadnych uziemień"),
            "Pierwsze zwarcie z ziemią nie powoduje wyłączenia – dlatego IT stosuje się tam, gdzie liczy się ciągłość zasilania (np. sale operacyjne, kopalnie). Wymaga kontroli stanu izolacji.",
        ),
        q(
            "shock.zone1", SHOCK, "Do jakiej wysokości nad podłogą sięga strefa 1 w łazience?",
            "2,25 m", listOf("1,5 m", "2,0 m", "3,0 m"),
            "Strefa 0 – wnętrze wanny/brodzika, strefa 1 – nad nimi do 2,25 m, strefa 2 – pas 0,6 m od strefy 1.",
            "PN-HD 60364-7-701",
        ),
        q(
            "shock.zone2", SHOCK, "Jaką szerokość ma strefa 2 w łazience (mierzoną od granicy strefy 1)?",
            "0,6 m", listOf("0,3 m", "1,0 m", "1,2 m"),
            "W strefach 1 i 2 urządzenia muszą mieć co najmniej IPX4, a w strefie 0 – IPX7.",
            "PN-HD 60364-7-701",
        ),
        q(
            "shock.zone0", SHOCK, "Jaki minimalny stopień ochrony przed wodą musi mieć urządzenie w strefie 0 łazienki?",
            "IPX7", listOf("IPX4", "IPX1", "IP20"),
            "Strefa 0 to wnętrze wanny – urządzenie może się znaleźć pod wodą. Dopuszcza się tylko SELV do 12 V AC (30 V DC) i urządzenia przeznaczone do tej strefy.",
            "PN-HD 60364-7-701",
        ),
        q(
            "shock.gsu", SHOCK, "Co łączy się na głównej szynie uziemiającej (GSU)?",
            "Przewód uziemiający, przewody ochronne oraz połączenia wyrównawcze głównych instalacji (np. wodnej, gazowej)",
            listOf("Wyłącznie przewody neutralne N", "Przewody fazowe wszystkich obwodów", "Wyłącznie ekrany kabli telekomunikacyjnych"),
            "Połączenia wyrównawcze główne wyrównują potencjały metalowych instalacji wchodzących do budynku – nie powstanie groźne napięcie między kranem a obudową pralki.",
        ),

        // =============================================================== POMIARY
        q(
            "meas.riso_v", MEASURE, "Jakim napięciem probierczym mierzy się rezystancję izolacji obwodu 230/400 V?",
            "500 V DC", listOf("250 V DC", "1000 V DC", "230 V AC"),
            "Obwody SELV/PELV: 250 V DC; obwody do 500 V (np. 230/400 V): 500 V DC; powyżej 500 V: 1000 V DC.",
            HD6,
        ),
        q(
            "meas.riso_min", MEASURE, "Jaka jest minimalna wymagana rezystancja izolacji obwodu 230/400 V?",
            "1 MΩ", listOf("0,5 MΩ", "0,25 MΩ", "10 MΩ"),
            "Dla obwodów do 500 V – co najmniej 1 MΩ (przy 500 V DC). Dla SELV/PELV – 0,5 MΩ (przy 250 V DC). W praktyce nowa instalacja ma wartości setek MΩ.",
            HD6,
        ),
        q(
            "meas.riso_selv", MEASURE, "Jaka jest minimalna rezystancja izolacji obwodu SELV/PELV i jakim napięciem się ją mierzy?",
            "0,5 MΩ przy 250 V DC", listOf("1 MΩ przy 500 V DC", "0,25 MΩ przy 100 V DC", "1 MΩ przy 1000 V DC"),
            "Niższe napięcie probiercze, bo obwody SELV pracują przy bardzo niskim napięciu i mają delikatniejszą izolację.",
            HD6,
        ),
        q(
            "meas.riso_prep", MEASURE, "Co trzeba zrobić przed pomiarem rezystancji izolacji obwodu?",
            "Wyłączyć napięcie i odłączyć odbiorniki oraz wrażliwą elektronikę (np. ograniczniki przepięć, ściemniacze)",
            listOf("Nic – mierzy się pod napięciem", "Wkręcić wszystkie żarówki", "Zewrzeć przewód fazowy z PE na stałe"),
            "Napięcie probiercze 500 V DC może uszkodzić elektronikę, a podłączone odbiorniki zafałszują wynik (np. zmierzysz rezystancję grzałki, a nie izolacji).",
        ),
        q(
            "meas.cont", MEASURE, "Jakim prądem co najmniej wykonuje się pomiar ciągłości przewodów ochronnych?",
            "Co najmniej 0,2 A, przy napięciu źródła 4–24 V",
            listOf("1 mA przy 500 V DC", "10 A przy 230 V AC", "0,2 mA przy 1000 V DC"),
            "Pomiar małej rezystancji wymaga odpowiednio dużego prądu – wykryje złe połączenia i przerwane żyły PE.",
            HD6,
        ),
        q(
            "meas.zs_what", MEASURE, "Co mierzy się przy pomiarze impedancji pętli zwarcia w układzie TN?",
            "Impedancję obwodu: przewód fazowy – przewód ochronny – źródło",
            listOf("Rezystancję izolacji między L a N", "Rezystancję uziomu otokowego", "Rezystancję przewodu N"),
            "Pomiar Zs pozwala sprawdzić, czy prąd zwarcia L–PE będzie dość duży, aby zabezpieczenie wyłączyło obwód w wymaganym czasie.",
        ),
        q(
            "meas.rcd300", MEASURE, "W jakim czasie powinien zadziałać wyłącznik RCD ogólnego typu przy prądzie różnicowym równym IΔn?",
            "Nie dłużej niż 300 ms", listOf("Nie dłużej niż 40 ms", "Nie dłużej niż 1 s", "Nie dłużej niż 5 s"),
            "RCD bezzwłoczny: ≤ 300 ms przy IΔn, ≤ 150 ms przy 2·IΔn i ≤ 40 ms przy 5·IΔn. RCD selektywny (S) jest celowo wolniejszy.",
            "PN-EN 61008-1",
        ),
        q(
            "meas.rcd40", MEASURE, "W jakim czasie powinien zadziałać RCD ogólnego typu przy prądzie 5 · IΔn?",
            "Nie dłużej niż 40 ms", listOf("Nie dłużej niż 300 ms", "Nie dłużej niż 0,4 s", "Nie dłużej niż 10 ms"),
            "Przy dużym prądzie różnicowym (np. bezpośredni dotyk) wyłączenie musi nastąpić szybciej, zanim dojdzie do migotania komór serca.",
            "PN-EN 61008-1",
        ),
        q(
            "meas.rcd_trip", MEASURE, "W jakim zakresie powinien mieścić się rzeczywisty prąd zadziałania wyłącznika RCD typu AC?",
            "Od 0,5 · IΔn do 1,0 · IΔn", listOf("Od 1,0 · IΔn do 2,0 · IΔn", "Od 0,1 · IΔn do 0,3 · IΔn", "Dokładnie 1,0 · IΔn"),
            "RCD nie może wyzwalać przy zbyt małych upływach (np. < 0,5 · IΔn), ale musi zadziałać najpóźniej przy IΔn.",
            "PN-EN 61008-1",
        ),
        q(
            "meas.rotation", MEASURE, "Do czego służy pomiar kolejności faz?",
            "Do sprawdzenia kierunku wirowania pola – ważne dla silników i gniazd 400 V",
            listOf("Do pomiaru rezystancji izolacji", "Do sprawdzenia działania RCD", "Do pomiaru mocy biernej"),
            "Zamiana dwóch faz odwraca kierunek obrotów silnika – pompa nie tłoczy, wentylator ssie zamiast wydmuchiwać.",
        ),
        q(
            "meas.earth", MEASURE, "Jak mierzy się rezystancję uziemienia?",
            "Miernikiem metodą techniczną z sondami (np. metoda 3-przewodowa), przy odłączonym zacisku kontrolnym",
            listOf("Omomierzem między przewodem L a N", "Miernikiem izolacji 1000 V", "Wskaźnikiem napięcia"),
            "Prąd pomiarowy płynie przez ziemię między uziomem a sondą prądową, a napięcie mierzy się sondą napięciową umieszczoną w odpowiedniej odległości.",
        ),
        q(
            "meas.order", MEASURE, "Od czego zaczyna się sprawdzanie instalacji przed pomiarami wymagającymi napięcia?",
            "Od oględzin oraz pomiarów bez napięcia (ciągłość PE, rezystancja izolacji)",
            listOf("Od testu RCD pod napięciem", "Od pomiaru impedancji pętli zwarcia", "Od załączenia wszystkich odbiorników"),
            "Najpierw sprawdza się, czy instalację można bezpiecznie załączyć (PE ciągły, izolacja dobra), dopiero potem pomiary pod napięciem.",
            HD6,
        ),

        // =============================================================== INSTALACJE, PRZEWODY, ZABEZPIECZENIA
        q(
            "inst.charB", INSTALL, "Przy jakim prądzie zadziała natychmiast (wyzwalacz elektromagnetyczny) wyłącznik o charakterystyce B?",
            "Od 3 do 5 × In", listOf("Od 5 do 10 × In", "Od 10 do 20 × In", "Od 1,13 do 1,45 × In"),
            "B: 3–5 × In (oświetlenie, gniazda), C: 5–10 × In (silniki, duże prądy rozruchowe), D: 10–20 × In (transformatory, spawarki).",
            "PN-EN 60898-1",
        ),
        q(
            "inst.charC", INSTALL, "Do zabezpieczania jakich odbiorników stosuje się zwykle wyłączniki o charakterystyce C?",
            "Odbiorników o dużym prądzie rozruchu, np. silników",
            listOf("Wyłącznie oświetlenia LED", "Obwodów SELV", "Przewodów PEN"),
            "Charakterystyka C nie wyzwala przy chwilowym prądzie rozruchowym silnika, który bywa 5–7 razy większy od znamionowego.",
        ),
        q(
            "inst.gg", INSTALL, "Czym różni się wkładka topikowa gG od aM?",
            "gG chroni przed przeciążeniem i zwarciem, aM – tylko przed zwarciem (do silników)",
            listOf("gG jest tylko do prądu stałego", "aM chroni przed przeciążeniem, gG nie", "Niczym – to różne nazwy tej samej wkładki"),
            "Wkładki aM (silnikowe) współpracują z zabezpieczeniem przeciążeniowym (np. termikiem), bo same przeciążeń nie wyłączą.",
        ),
        q(
            "inst.selective", INSTALL, "Jaki stosunek prądów znamionowych kolejnych wkładek gG zapewnia zwykle wybiórczość (selektywność)?",
            "Co najmniej 1,6 : 1", listOf("1 : 1", "1,1 : 1", "Wybiórczość nie zależy od prądów znamionowych"),
            "Selektywność oznacza, że przy zwarciu zadziała zabezpieczenie najbliższe miejsca zwarcia, a nie to przed licznikiem.",
        ),
        q(
            "inst.iz", INSTALL, "Jaki warunek musi spełniać zabezpieczenie przeciążeniowe przewodu?",
            "IB ≤ In ≤ Iz oraz I2 ≤ 1,45 · Iz",
            listOf("In ≥ Iz", "IB ≥ Iz", "In = 2 · Iz"),
            "IB – prąd obciążenia, In – prąd znamionowy zabezpieczenia, Iz – obciążalność długotrwała przewodu, I2 – prąd zadziałania zabezpieczenia.",
            "PN-HD 60364-4-43",
        ),
        q(
            "inst.ydyp", INSTALL, "Co oznacza symbol przewodu YDYp 3×2,5?",
            "Przewód miedziany, izolacja i powłoka z PVC, płaski, 3 żyły po 2,5 mm²",
            listOf("Przewód aluminiowy, okrągły", "Kabel ziemny z ekranem", "Linka giętka w gumie"),
            "Y – PVC, D – drut, p – płaski. Typowy przewód do instalacji podtynkowych.",
        ),
        q(
            "inst.lgy", INSTALL, "Co oznacza oznaczenie LgY?",
            "Linka giętka w izolacji z PVC",
            listOf("Drut aluminiowy w gumie", "Kabel ziemny z polietylenu", "Przewód płaski do wtynkowania"),
            "LgY stosuje się m.in. w rozdzielnicach – linka jest elastyczna, ale wymaga końcówek tulejkowych w zaciskach śrubowych.",
        ),
        q(
            "inst.yky", INSTALL, "Do czego przeznaczony jest kabel YKY?",
            "Do układania w ziemi i na zewnątrz – kabel miedziany z izolacją i powłoką PVC",
            listOf("Wyłącznie do instalacji podtynkowych w łazience", "Do zasilania lamp w strefie 0", "To przewód sygnałowy SELV"),
            "Kabel ma grubszą powłokę i jest przeznaczony do trudniejszych warunków niż przewód instalacyjny.",
        ),
        q(
            "inst.al", INSTALL, "Co oznacza litera „A\" na początku lub w oznaczeniu przewodu (np. YAKY)?",
            "Żyły z aluminium", listOf("Ekran antyzakłóceniowy", "Przewód do zastosowań akustycznych", "Izolacja z gumy"),
            "Brak litery A oznacza żyły miedziane. Aluminium wymaga większego przekroju dla tego samego prądu.",
        ),
        q(
            "inst.pen_color", INSTALL, "Jak oznacza się przewód PEN?",
            "Kolorem zielono-żółtym z niebieskimi oznaczeniami na końcach (lub odwrotnie)",
            listOf("Wyłącznie kolorem niebieskim", "Kolorem brązowym", "Kolorem czarnym z białym paskiem"),
            "PEN pełni funkcję PE i N jednocześnie – oznaczenie łączy oba kolory.",
            "PN-EN 60445",
        ),
        q(
            "inst.ip_first", INSTALL, "Co oznacza pierwsza cyfra w kodzie IP (np. IP44)?",
            "Ochronę przed wnikaniem ciał stałych (i dostępem do części niebezpiecznych)",
            listOf("Ochronę przed wodą", "Klasę ochronności", "Odporność na uderzenia"),
            "Pierwsza cyfra 0–6 – ciała stałe (6 = pyłoszczelne), druga 0–8 (9) – woda. Odporność na uderzenia to kod IK.",
            "PN-EN 60529",
        ),
        q(
            "inst.ip44", INSTALL, "Co oznacza stopień ochrony IP44?",
            "Ochronę przed ciałami stałymi > 1 mm i przed bryzgami wody z dowolnego kierunku",
            listOf("Pyłoszczelność i wodoszczelność przy zanurzeniu", "Brak ochrony przed wodą", "Ochronę przed strumieniem wody pod ciśnieniem"),
            "IP44 to typowy wymóg dla urządzeń w łazienkach (strefa 2) i na zewnątrz pod zadaszeniem.",
            "PN-EN 60529",
        ),
        q(
            "inst.ip65", INSTALL, "Co oznacza stopień ochrony IP65?",
            "Urządzenie pyłoszczelne, chronione przed strugą wody",
            listOf("Ochrona przed ciałami > 12 mm, bez ochrony przed wodą", "Ochrona przed długotrwałym zanurzeniem", "Urządzenie przeznaczone wyłącznie do wnętrz suchych"),
            "6 – pyłoszczelne, 5 – struga wody z dyszy. Odpowiednie np. do oświetlenia zewnętrznego.",
            "PN-EN 60529",
        ),
        q(
            "inst.u400", INSTALL, "Dlaczego w sieci 230/400 V napięcie między fazami wynosi 400 V?",
            "Bo napięcie przewodowe jest √3 razy większe od fazowego (230 · √3 ≈ 400)",
            listOf("Bo fazy dodają się arytmetycznie (230 + 170)", "Bo transformator podwaja napięcie", "Bo tak oznacza się umownie, a faktycznie to 230 V"),
            "Napięcia fazowe są przesunięte o 120°, więc ich różnica ma wartość √3 · U0.",
        ),
        q(
            "inst.p3", INSTALL, "Jakim wzorem oblicza się moc czynną odbiornika trójfazowego symetrycznego?",
            "P = √3 · U · I · cos φ", listOf("P = 3 · U · I", "P = U · I · cos φ", "P = √3 · U / I"),
            "U – napięcie przewodowe (400 V), I – prąd przewodowy, cos φ – współczynnik mocy.",
        ),
        q(
            "inst.vdrop", INSTALL, "Jaki spadek napięcia zaleca się nie przekraczać w obwodach oświetleniowych instalacji zasilanej z sieci publicznej?",
            "3 %", listOf("10 %", "1 %", "5 %"),
            "Zalecenia normy: 3 % dla oświetlenia i 5 % dla innych odbiorów (liczone od złącza). Zbyt duży spadek – migotanie światła, gorsza praca silników.",
            "PN-HD 60364-5-52",
        ),
        q(
            "inst.pe_size", INSTALL, "Jaki przekrój powinien mieć przewód ochronny PE dla przewodu fazowego 25 mm² Cu (ten sam materiał)?",
            "16 mm²", listOf("25 mm²", "10 mm²", "6 mm²"),
            "Dla S ≤ 16 mm²: PE = S; dla 16 < S ≤ 35: PE = 16 mm²; dla S > 35: PE = S/2.",
            "PN-HD 60364-5-54",
        ),
        q(
            "inst.spd", INSTALL, "Gdzie w rozdzielnicy montuje się ogranicznik przepięć typu 1+2?",
            "Na początku instalacji – przed wyłącznikiem różnicowoprądowym",
            listOf("Za wyłącznikami nadprądowymi każdego obwodu", "Wyłącznie w gniazdach wtyczkowych", "W przewodzie PEN zamiast połączenia z ziemią"),
            "Prąd udarowy odprowadzany do PE wyzwalałby RCD (a nawet mógł go uszkodzić). SPD łączy się z szyną PE możliwie krótko.",
        ),

        // =============================================================== PIERWSZA POMOC I PPOŻ.
        q(
            "res.first", RESCUE, "Co należy zrobić w pierwszej kolejności, gdy człowiek jest porażony prądem i nadal styka się z urządzeniem?",
            "Uwolnić go spod napięcia – wyłączyć zasilanie, dbając o własne bezpieczeństwo",
            listOf("Od razu rozpocząć uciskanie klatki piersiowej", "Chwycić go gołymi rękami i odciągnąć", "Polać go wodą"),
            "Ratujący nie może stać się drugą ofiarą. Najpierw wyłączenie zasilania (wyłącznik, bezpiecznik), a gdy to niemożliwe – odciągnięcie za pomocą przedmiotu izolacyjnego.",
        ),
        q(
            "res.cpr", RESCUE, "Jaki jest stosunek uciśnięć klatki piersiowej do oddechów ratowniczych u dorosłego?",
            "30 : 2", listOf("15 : 2", "5 : 1", "30 : 5"),
            "Tempo 100–120 uciśnięć na minutę, głębokość 5–6 cm. Jeśli nie możesz lub nie chcesz wykonywać oddechów – prowadź same uciśnięcia.",
            "Wytyczne ERC",
        ),
        q(
            "res.rate", RESCUE, "Z jaką częstotliwością uciska się klatkę piersiową podczas resuscytacji?",
            "100–120 razy na minutę", listOf("60–80 razy na minutę", "150–180 razy na minutę", "30 razy na minutę"),
            "Pomaga rytm piosenki „Stayin' Alive\" – ma ok. 104 uderzenia na minutę.",
            "Wytyczne ERC",
        ),
        q(
            "res.breath", RESCUE, "Jak długo maksymalnie ocenia się oddech u poszkodowanego?",
            "Do 10 sekund", listOf("Do 1 minuty", "Do 2 sekund", "Do 30 sekund"),
            "Patrz, słuchaj, wyczuwaj – nie dłużej niż 10 s. Brak prawidłowego oddechu = wezwij pomoc i zacznij RKO.",
            "Wytyczne ERC",
        ),
        q(
            "res.aed", RESCUE, "Kiedy należy użyć defibrylatora AED?",
            "Jak najszybciej u osoby nieprzytomnej, która nie oddycha prawidłowo",
            listOf("Tylko po 30 minutach resuscytacji", "Wyłącznie przez lekarza", "U każdej osoby z oparzeniem"),
            "AED sam analizuje rytm serca i wydaje polecenia głosowe. Porażenie prądem często wywołuje migotanie komór – właśnie wtedy defibrylacja ratuje życie.",
        ),
        q(
            "res.vf", RESCUE, "Jaki jest najgroźniejszy skutek przepływu prądu przemiennego przez serce?",
            "Migotanie komór serca", listOf("Oparzenie naskórka", "Chwilowe mrowienie", "Skurcz mięśni dłoni"),
            "Migotanie komór oznacza, że serce przestaje pompować krew – bez defibrylacji prowadzi do śmierci w ciągu minut.",
        ),
        q(
            "res.letgo", RESCUE, "Ile w przybliżeniu wynosi prąd samouwolnienia (prąd przemienny 50 Hz), powyżej którego człowiek może nie być w stanie puścić przewodu?",
            "Ok. 10 mA", listOf("Ok. 0,5 mA", "Ok. 1 A", "Ok. 100 mA"),
            "Ok. 0,5–1 mA – próg odczuwania, ok. 10 mA – próg samouwolnienia (skurcz mięśni), kilkadziesiąt mA przy dłuższym czasie – zagrożenie migotaniem komór. Dlatego RCD ma 30 mA.",
        ),
        q(
            "res.doctor", RESCUE, "Czy porażony, który czuje się dobrze, powinien zostać zbadany przez lekarza?",
            "Tak – zawsze, bo zaburzenia rytmu serca mogą wystąpić później",
            listOf("Nie, jeśli nie stracił przytomności", "Tylko gdy ma widoczne oparzenia", "Tylko przy napięciu powyżej 1 kV"),
            "Przepływ prądu może uszkodzić serce i tkanki wewnętrzne bez widocznych objawów.",
        ),
        q(
            "res.extinguisher", RESCUE, "Czym gasi się urządzenia elektryczne pod napięciem do 1 kV?",
            "Gaśnicą proszkową lub śniegową (CO₂)",
            listOf("Wodą z hydrantu", "Gaśnicą pianową", "Kocem nasączonym wodą"),
            "Woda i piana przewodzą prąd. Gaśnica proszkowa lub CO₂ – z zachowaniem odległości (dla urządzeń do 1 kV co najmniej 1 m). Najlepiej najpierw wyłączyć zasilanie.",
        ),
        q(
            "res.distance", RESCUE, "Z jakiej minimalnej odległości gasi się gaśnicą urządzenia pod napięciem do 1 kV?",
            "1 m", listOf("0,1 m", "3 m", "Odległość nie ma znaczenia"),
            "Odległość chroni przed łukiem elektrycznym i przeskokiem. Instrukcja na gaśnicy podaje dopuszczalne napięcie i odległość.",
        ),
        q(
            "res.gloves", RESCUE, "Jak często wykonuje się okresowe badania rękawic dielektrycznych?",
            "Co 6 miesięcy", listOf("Co 5 lat", "Tylko raz – przy zakupie", "Co 10 lat"),
            "Sprzęt ochronny (rękawice, kalosze, półbuty dielektryczne) ma okresowe badania napięciowe. Przed każdym użyciem rękawice sprawdza się też wzrokowo i na szczelność.",
        ),
        q(
            "res.burn", RESCUE, "Jak postępować z oparzeniem elektrycznym?",
            "Schłodzić, zabezpieczyć jałowym opatrunkiem i zapewnić pomoc medyczną",
            listOf("Posmarować tłuszczem", "Przekłuć pęcherze", "Nic – oparzenia elektryczne są zawsze powierzchowne"),
            "Oparzenie elektryczne wygląda niewinnie z zewnątrz, a pod skórą może być rozległe uszkodzenie tkanek (prąd płynie przez wnętrze ciała).",
        ),
    )

    fun byCategory(c: SepCategory) = static.filter { it.category == c }
}
