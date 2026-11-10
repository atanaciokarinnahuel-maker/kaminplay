package com.karin.kaminplay.core

import com.karin.kaminplay.core.Genre.CIENCIA_FICCION
import com.karin.kaminplay.core.Genre.CINE_NEGRO
import com.karin.kaminplay.core.Genre.COMEDIA
import com.karin.kaminplay.core.Genre.DRAMA
import com.karin.kaminplay.core.Genre.TERROR

/**
 * Starter catalogue: films in the US public domain, used until the TMDB catalogue is wired in.
 * Status varies by country and by print (restorations and new scores can carry their own rights).
 */
object SeedCatalog {
    val films: List<Film> = listOf(
        Film("viaje-a-la-luna-1902", "Viaje a la Luna", "Le Voyage dans la Lune", 1902, "Georges Méliès", 13, "Francia", CIENCIA_FICCION, Motif.LUNA,
            "Un grupo de astrónomos se dispara hacia la Luna dentro de un proyectil y descubre que no está deshabitada."),
        Film("caligari-1920", "El gabinete del doctor Caligari", "Das Cabinet des Dr. Caligari", 1920, "Robert Wiene", 76, "Alemania", TERROR, Motif.ESCALERA,
            "Un feriante exhibe a un sonámbulo que predice muertes en un pueblo de calles torcidas y sombras pintadas."),
        Film("el-chico-1921", "El chico", "The Kid", 1921, "Charles Chaplin", 68, "EE. UU.", COMEDIA, Motif.VENTANA,
            "Un vagabundo encuentra a un bebé abandonado y lo cría como puede hasta que intentan separarlos."),
        Film("nosferatu-1922", "Nosferatu", "Nosferatu, eine Symphonie des Grauens", 1922, "F. W. Murnau", 94, "Alemania", TERROR, Motif.ESCALERA,
            "Un agente inmobiliario viaja a los Cárpatos para cerrar una venta con un conde que no soporta la luz del día."),
        Film("el-hombre-mosca-1923", "El hombre mosca", "Safety Last!", 1923, "Fred C. Newmeyer y Sam Taylor", 70, "EE. UU.", COMEDIA, Motif.RELOJ,
            "Un dependiente que finge haber triunfado acaba escalando la fachada de unos grandes almacenes."),
        Film("sherlock-jr-1924", "El moderno Sherlock Holmes", "Sherlock Jr.", 1924, "Buster Keaton", 45, "EE. UU.", COMEDIA, Motif.OJO,
            "Un proyeccionista que sueña con ser detective se queda dormido y entra en la película que está pasando."),
        Film("potemkin-1925", "El acorazado Potemkin", "Bronenosets Potiomkin", 1925, "Serguéi Eisenstein", 75, "URSS", DRAMA, Motif.ONDAS,
            "La tripulación de un buque de guerra se amotina en 1905 y la ciudad de Odesa sale a apoyarla."),
        Film("fantasma-opera-1925", "El fantasma de la ópera", "The Phantom of the Opera", 1925, "Rupert Julian", 93, "EE. UU.", TERROR, Motif.OJO,
            "Una figura enmascarada vive bajo la Ópera de París y decide convertir a una joven cantante en estrella."),
        Film("la-general-1926", "El maquinista de La General", "The General", 1926, "Buster Keaton y Clyde Bruckman", 79, "EE. UU.", COMEDIA, Motif.RIEL,
            "Un maquinista persigue en solitario a los espías que le han robado la locomotora."),
        Film("metropolis-1927", "Metrópolis", "Metropolis", 1927, "Fritz Lang", 148, "Alemania", CIENCIA_FICCION, Motif.CIUDAD,
            "En una ciudad dividida entre torres y subsuelo, el hijo del gobernante baja a ver cómo viven los obreros."),
        Film("heroe-del-rio-1928", "El héroe del río", "Steamboat Bill, Jr.", 1928, "Charles Reisner", 70, "EE. UU.", COMEDIA, Motif.ONDAS,
            "El hijo delicado de un capitán de vapor intenta ayudar a su padre justo cuando llega un ciclón."),
        Film("luna-nueva-1940", "Luna nueva", "His Girl Friday", 1940, "Howard Hawks", 92, "EE. UU.", COMEDIA, Motif.VENTANA,
            "Un director de periódico usa una gran exclusiva para impedir que su mejor reportera, y exmujer, deje el oficio."),
        Film("juan-nadie-1941", "Juan Nadie", "Meet John Doe", 1941, "Frank Capra", 122, "EE. UU.", DRAMA, Motif.CIUDAD,
            "Una periodista inventa a un hombre corriente indignado y el periódico contrata a un vagabundo para encarnarlo."),
        Film("detour-1945", "El desvío", "Detour", 1945, "Edgar G. Ulmer", 68, "EE. UU.", CINE_NEGRO, Motif.RIEL,
            "Un pianista hace autostop hacia California y cada decisión que toma en la carretera lo hunde un poco más."),
        Film("perversidad-1945", "Perversidad", "Scarlet Street", 1945, "Fritz Lang", 102, "EE. UU.", CINE_NEGRO, Motif.PERSIANA,
            "Un cajero que pinta los domingos se enamora de una mujer que solo ve en él una fuente de dinero."),
        Film("el-extrano-1946", "El extraño", "The Stranger", 1946, "Orson Welles", 95, "EE. UU.", CINE_NEGRO, Motif.RELOJ,
            "Un investigador sigue el rastro de un criminal de guerra hasta un pueblo tranquilo de Connecticut."),
        Film("con-las-horas-contadas-1950", "Con las horas contadas", "D.O.A.", 1950, "Rudolph Maté", 83, "EE. UU.", CINE_NEGRO, Motif.PERSIANA,
            "Un contable descubre que lo han envenenado y dedica el tiempo que le queda a encontrar a su asesino."),
        Film("mansion-horrores-1959", "La mansión de los horrores", "House on Haunted Hill", 1959, "William Castle", 75, "EE. UU.", TERROR, Motif.VENTANA,
            "Un millonario ofrece dinero a cinco invitados si aguantan una noche entera en una casa con mala fama."),
        Film("pequena-tienda-1960", "La pequeña tienda de los horrores", "The Little Shop of Horrors", 1960, "Roger Corman", 72, "EE. UU.", COMEDIA, Motif.OJO,
            "El empleado torpe de una floristería cultiva una planta que crece muy deprisa y tiene un apetito concreto."),
        Film("carnaval-almas-1962", "El carnaval de las almas", "Carnival of Souls", 1962, "Herk Harvey", 78, "EE. UU.", TERROR, Motif.ONDAS,
            "Tras sobrevivir a un accidente, una organista se muda de ciudad y empieza a ver a un hombre que nadie más ve."),
        Film("ultimo-hombre-1964", "El último hombre sobre la Tierra", "The Last Man on Earth", 1964, "Ubaldo Ragona y Sidney Salkow", 86, "Italia / EE. UU.", CIENCIA_FICCION, Motif.LUNA,
            "El único superviviente de una plaga pasa los días buscando provisiones y las noches encerrado en casa."),
        Film("muertos-vivientes-1968", "La noche de los muertos vivientes", "Night of the Living Dead", 1968, "George A. Romero", 96, "EE. UU.", TERROR, Motif.VENTANA,
            "Siete desconocidos se atrincheran en una granja mientras los muertos se levantan en los alrededores."),
    )

    private val byId: Map<String, Film> = films.associateBy { it.id }

    fun byId(id: String?): Film? = if (id == null) null else byId[id]

    fun similarTo(film: Film): List<Film> = films.filter { it.genre == film.genre && it.id != film.id }
}
