package app.metria.cloud

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.LocalDate

class OnlineAnswer internal constructor(
    val text: String, val language: OnlineLanguage, val start: LocalDate, val end: LocalDate,
)

/** Fixed bilingual copy only. No model prose or values can enter a displayed answer. */
internal object OnlineAnswerRenderer {
    fun render(snapshot: OnlineAssistanceRequest, selection: OnlineResponseSelection): OnlineAnswer? {
        val spanish = snapshot.language == OnlineLanguage.Spanish
        fun copy(english: String, es: String) = if (spanish) es else english
        fun number(value: Double, scale: Int): String = BigDecimal.valueOf(value)
            .setScale(scale, RoundingMode.HALF_UP).toPlainString().let { if (spanish) it.replace('.', ',') else it }
        val question = Normalizer.normalize(snapshot.question.lowercase(java.util.Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        fun mentions(pattern: String) = Regex("\\b(?:$pattern)\\b").containsMatchIn(question)
        // Conservative routing, not a comprehensive medical-intent classifier. All paths use fixed safe copy.
        val health = mentions("medical|diagnos\\w*|medicat\\w*|medicine|prescri\\w*|pregnan\\w*|purging|vomit\\w*|anorexi\\w*|bulimi\\w*|emergency|suicid\\w*|chest pain|medic\\w*|embaraz\\w*|purg\\w*|urgencia\\w*|dolor de pecho")
        val wantsWeight = mentions("weight|weigh\\w*|kilograms?|kg|peso|pesaje\\w*|kilos?")
        val wantsNutrition = mentions("nutrition|calories|protein|meals?|intake|nutricion|calorias|proteina\\w*|comidas?|ingesta")
        val wantsBalanced = mentions("balanced|balancead\\w*|equilibrad\\w*") && mentions("meals?|plate|comidas?|plato")
        val blocks = mutableListOf(copy("Online · Nebius\nPeriod: ${snapshot.start} to ${snapshot.end}.",
            "Online · Nebius\nPeriodo: ${snapshot.start} a ${snapshot.end}."))
        if (health || selection.guidance == OnlineGuidance.professional_support) {
            blocks += copy("Metria cannot provide medical advice. Consider a qualified health professional or local emergency services for urgent symptoms.",
                "Metria no puede ofrecer consejo médico. Consulta a un profesional sanitario cualificado o a los servicios de emergencia locales si tienes síntomas urgentes.")
        } else if (!wantsWeight && !wantsNutrition && !wantsBalanced) {
            blocks += copy("Online assistance currently supports recorded weight and nutrition summaries, and general balanced-meal information. Please ask a question in that scope.",
                "La asistencia online admite resúmenes de peso y nutrición registrados e información general sobre comidas equilibradas. Haz una pregunta dentro de ese ámbito.")
        } else {
            if (wantsWeight) {
                val w = snapshot.weight
                if (w == null) blocks += copy("No weight summary was shared. Include it in a new preview to ask about your records.",
                    "No se compartió ningún resumen de peso. Inclúyelo en una nueva vista previa para preguntar por tus registros.")
                else {
                    if (OnlineSummarySection.weight !in selection.sections) return null
                    blocks += if (w.count == 0) copy("No weight measurements are recorded in the shared period.",
                        "No hay mediciones de peso registradas en el periodo compartido.")
                    else buildString {
                        append(copy("Recorded measurements: ${w.count}. First: ${number(w.firstKg!!, 2)} kg (${w.firstDate}); latest: ${number(w.latestKg!!, 2)} kg (${w.latestDate}).",
                            "Mediciones registradas: ${w.count}. Primera: ${number(w.firstKg, 2)} kg (${w.firstDate}); última: ${number(w.latestKg, 2)} kg (${w.latestDate})."))
                        append(if (w.changeKg != null) copy(" Recorded change: ${number(w.changeKg, 2)} kg.",
                            " Cambio registrado: ${number(w.changeKg, 2)} kg.") else copy(" One measurement does not establish a change.",
                            " Una medición no permite establecer un cambio."))
                    }
                }
            }
            if (wantsNutrition) {
                val n = snapshot.nutrition
                if (n == null) blocks += copy("No nutrition summary was shared. Include it in a new preview to ask about your records.",
                    "No se compartió ningún resumen de nutrición. Inclúyelo en una nueva vista previa para preguntar por tus registros.")
                else {
                    if (OnlineSummarySection.nutrition !in selection.sections) return null
                    blocks += copy("Recorded meals: ${n.recordedMeals}. Complete estimates: ${n.completeMeals}; partial: ${n.partialMeals}; without estimates: ${n.missingMeals}.",
                        "Comidas registradas: ${n.recordedMeals}. Estimaciones completas: ${n.completeMeals}; parciales: ${n.partialMeals}; sin estimación: ${n.missingMeals}.")
                    if (n.completeMeals > 0) blocks += copy("Complete recorded estimates: ${number(n.completeCalories!!, 0)} kcal and ${number(n.completeProtein!!, 1)} g protein.",
                        "Estimaciones completas registradas: ${number(n.completeCalories, 0)} kcal y ${number(n.completeProtein, 1)} g de proteína.")
                    if (n.partialMeals > 0) blocks += copy("Partial recorded estimates: ${number(n.partialCalories!!, 0)} kcal and ${number(n.partialProtein!!, 1)} g protein; these meals have incomplete coverage.",
                        "Estimaciones parciales registradas: ${number(n.partialCalories, 0)} kcal y ${number(n.partialProtein, 1)} g de proteína; estas comidas tienen cobertura incompleta.")
                    blocks += copy("These are recorded estimates, not your total intake. Missing estimates are unknown, not zero.",
                        "Son estimaciones registradas, no tu ingesta total. Las estimaciones ausentes son desconocidas, no cero.")
                }
            }
            if (wantsBalanced) {
                if (selection.guidance != OnlineGuidance.balanced_meal_information) return null
                blocks += copy("For general meal planning, you can combine vegetables or fruit, a protein source, and a grain or other starchy food. This is general information, not a personalized diet or assessment of your records.",
                    "Para planificar una comida, puedes combinar verduras o fruta, una fuente de proteína y cereales u otro alimento con almidón. Es información general, no una dieta personalizada ni una evaluación de tus registros.")
            }
        }
        return OnlineAnswer(blocks.joinToString("\n\n"), snapshot.language, snapshot.start, snapshot.end)
    }
}
