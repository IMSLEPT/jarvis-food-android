package com.jarvis.food.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.jarvis.food.data.*
import com.jarvis.food.engine.*
import com.jarvis.food.net.OpenFoodFacts
import kotlinx.coroutines.delay
import java.time.LocalDate

internal object ManualMealDrafts { val recipes = mutableStateMapOf<String, Recipe>() }

@Composable
fun ManualMealScreen(date: LocalDate, slot: Int) {
    val s by Store.state.collectAsState()
    val planned = Store.plannedMeals(s, date).firstOrNull { it.slotIndex == slot }
    val saved = Store.dayPlan(s, date)?.slots?.firstOrNull { it.slot == slot }
    if (planned == null || saved == null) { Column { TopBar("Pasto personalizzato", { Nav.back() }); Text("Pasto non trovato") }; return }
    val draftKey = "$date#$slot"
    val initial = saved.manualRecipe ?: Recipe("manual:$draftKey", "Il mio pasto", 1, emptyList(), meal = planned.solved.template.meal)
    var recipe by remember(draftKey) { mutableStateOf(ManualMealDrafts.recipes[draftKey] ?: initial) }
    fun change(next: Recipe) { recipe = next; ManualMealDrafts.recipes[draftKey] = next }
    var picking by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var online by remember { mutableStateOf<List<Food>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<Food?>(null) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var amount by remember { mutableStateOf("100") }
    var proposal by remember { mutableStateOf<Pair<List<Ingredient>, Macro>?>(null) }
    val target = planned.solved.target
    val actual = ManualMeals.total(recipe.ingredients)
    val complete = ManualMeals.complete(recipe.ingredients)
    val eaten = saved.eatenEntryIds.isNotEmpty()
    val dayTarget = Store.energyFor(s, date).macro
    val recorded = s.diary.filter { it.date == date.toString() }.fold(Macro.ZERO) { a, e -> a + Macro(e.food.kcal, e.food.protein, e.food.carbs, e.food.fat) * (e.grams/100) }
    val preview = s.copy(plan=s.plan.map { dp -> if(dp.date!=date.toString()) dp else dp.copy(slots=dp.slots.map { ps -> if(ps.slot==slot && !eaten) ps.copy(recipeId=recipe.id,manualRecipe=recipe) else ps }) })
    val remainingPlan = Store.plannedMeals(preview, date).filter { it.slotIndex != slot && Store.dayPlan(s, date)?.slots?.firstOrNull { ps -> ps.slot == it.slotIndex }?.eatenEntryIds?.isEmpty() == true }.fold(Macro.ZERO) { a, m -> a + m.solved.total }
    val predicted = recorded + remainingPlan + if (eaten) Macro.ZERO else actual
    val available = dayTarget - recorded - remainingPlan
    val dayFitTarget = Macro(available.kcal.coerceAtLeast(0.0),available.protein.coerceAtLeast(0.0),available.carbs.coerceAtLeast(0.0),available.fat.coerceAtLeast(0.0))
    val local = remember(query, s.customFoods, s.favorites) {
        val terms = FoodDb.normalize(query).split(' ').filter { it.isNotEmpty() }
        ((s.customFoods + s.favorites).filter { f -> terms.all { it in FoodDb.normalize(f.name+" "+f.brand) } } + Db.db.search(query).map { it.toFood() }).distinctBy { it.id }.take(35)
    }
    LaunchedEffect(query, picking) {
        online=emptyList(); loading=false; searchError=null
        if (!picking || query.trim().length<3) return@LaunchedEffect
        delay(650); loading=true
        try { online=OpenFoodFacts.search(query).take(15) }
        catch (_: Exception) { searchError="Ricerca online non disponibile; puoi usare catalogo e alimenti personali." }
        finally { loading=false }
    }
    Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding()) {
        TopBar("Pasto personalizzato", { Nav.back() })
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${planned.slotName} · $date", style=T.Label)
            if (eaten) Text("Questo pasto è già registrato. Le quantità restano consultabili; modifica il diario per correggere ciò che hai mangiato.", style=T.Small)
            JField(recipe.name, { change(recipe.copy(name=it)) }, "Nome del pasto")
            MacroComparison(if(complete || recipe.ingredients.isEmpty()) "Questo pasto" else "Questo pasto · dati parziali", actual, target)
            if(!complete && recipe.ingredients.isNotEmpty()) Text("Mancano calorie o macro per alcuni alimenti: i totali sono parziali e la proposta di quantità resta sospesa. Completa i dati di etichetta.", style=T.Small)
            MacroComparison("Giornata prevista", predicted, dayTarget)
            Text("La previsione somma diario e pasti ancora pianificati, sostituendo questo pasto una sola volta. Non conferma i pasti come mangiati.", style=T.Small)
            recipe.ingredients.forEachIndexed { index, item ->
                JCard {
                    Text(item.food.name, style=T.Label)
                    Text("${amountText(item.grams)} ${if(item.food.liquid) "ml" else "g"} · ${(item.food.kcal*item.grams/100).r0()} kcal", style=T.Small)
                    Row {
                        TextButton(enabled=!eaten, onClick={ editing=index; selected=item.food; amount=amountText(item.grams) }) { Text("Quantità") }
                        TextButton(enabled=!eaten, onClick={ change(recipe.copy(ingredients=recipe.ingredients.filterIndexed { i,_ -> i!=index })) }) { Text("Rimuovi") }
                    }
                }
            }
            if (!eaten) GhostButton("Cerca e aggiungi alimenti", Modifier.fillMaxWidth().testTag("manual-add-food")) { picking=true }
            if (!eaten) GhostButton("Crea alimento personalizzato", Modifier.fillMaxWidth()) { ManualMealDrafts.recipes[draftKey]=recipe; Nav.go(Screen.CustomFood(null, planned.diaryMeal)) }
            if (!eaten && complete) GhostButton("Suggerisci quantità per il pasto", Modifier.fillMaxWidth().testTag("manual-adjust")) { if(!eaten && recipe.ingredients.isNotEmpty()) proposal=ManualMeals.adjust(recipe.ingredients,target) to target }
            if (!eaten && complete) GhostButton("Suggerisci quantità per la giornata", Modifier.fillMaxWidth().testTag("manual-adjust-day")) {
                proposal=ManualMeals.adjust(recipe.ingredients,dayFitTarget) to dayFitTarget
            }
            Text("Il confronto della giornata considera anche gli altri pasti ancora pianificati. I loro grammi possono essere ricalcolati dal piano quando cambi questo pasto.", style=T.Small)
            Text("La proposta varia solo le quantità inserite, entro metà e doppio. Controlla le porzioni prima di applicarla: alcuni obiettivi richiedono alimenti diversi. Non è garantito un incastro perfetto dei macro.", style=T.Small)
            if (saved.manualRecipe != null && !eaten) PrimaryButton("Segna mangiato", Modifier.fillMaxWidth().testTag("manual-eat"), enabled=recipe.ingredients.isNotEmpty()) {
                Store.setManualMeal(date,slot,recipe)
                Store.plannedMeals(Store.current,date).firstOrNull { it.slotIndex==slot }?.let { Store.eatPlanned(date,it) }
                ManualMealDrafts.recipes.remove(draftKey); Nav.back()
            }
            if (saved.manualRecipe!=null && !eaten) GhostButton("Trova un'altra ricetta", Modifier.fillMaxWidth()) { Nav.go(Screen.Alternatives(date,slot)) }
        }
        Row(Modifier.padding(16.dp), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            GhostButton("Annulla",Modifier.weight(1f)) { ManualMealDrafts.recipes.remove(draftKey); Nav.back() }
            PrimaryButton("Salva nel piano",Modifier.weight(2f).testTag("manual-save"),enabled=!eaten && recipe.name.isNotBlank() && recipe.ingredients.isNotEmpty()) {
                Store.setManualMeal(date,slot,recipe); ManualMealDrafts.recipes.remove(draftKey)
                Nav.backTo { it !is Screen.ManualMeal && it !is Screen.Alternatives && it !is Screen.RecipeView }
            }
        }
    }
    if(picking) AlertDialog(onDismissRequest={picking=false},title={Text("Aggiungi al pasto",style=T.Title2)},text={
        Column(Modifier.verticalScroll(rememberScrollState())) {
            JField(query,{query=it},"Cerca alimento o marca")
            Text("Catalogo, preferiti, alimenti personali e prodotti online. Nulla viene aggiunto al diario.",style=T.Small)
            if(loading) Text("Cerco prodotti online…",style=T.Small)
            searchError?.let { Text(it,style=T.Small) }
            (local+online).distinctBy { it.id }.filter { f -> f.tags.none { it in s.dietPrefs.exclude } }.take(45).forEach { food ->
                TextButton(onClick={editing=null; selected=food; amount=amountText(food.servingGrams?:100.0); picking=false}) {
                    Text("${food.name}${food.brand.takeIf { it.isNotBlank() }?.let { " · $it" }?:""}")
                }
            }
        }
    },confirmButton={TextButton(onClick={picking=false}){Text("Chiudi")}},containerColor=C.Surface)
    selected?.let { food -> AlertDialog(onDismissRequest={selected=null}, title={Text(food.name,style=T.Title2)},text={
        Column {
            JField(amount,{amount=it.filter { c -> c.isDigit()||c=='.'||c==',' }.take(8)},"Quantità",Modifier.testTag("manual-quantity"),number=true,suffix=if(food.liquid)"ml" else "g")
            Text("Valori per 100 ${if(food.liquid)"ml" else "g"}: ${food.kcal.r0()} kcal · P ${food.protein.r1()} · C ${food.carbs.r1()} · G ${food.fat.r1()}",style=T.Small)
        }
    },confirmButton={TextButton(enabled=!eaten && (parseNum(amount)?.let { it.isFinite()&&it>0 }==true),onClick={
        val item=Ingredient(food,parseNum(amount)!!)
        val next=recipe.ingredients.toMutableList()
        val index=editing
        if(index!=null&&index in next.indices) next[index]=item else next.add(item)
        change(recipe.copy(ingredients=next)); selected=null
    }){Text("Conferma quantità")}},dismissButton={TextButton(onClick={selected=null}){Text("Annulla")}},containerColor=C.Surface) }
    proposal?.let { (items, goal) -> AlertDialog(onDismissRequest={proposal=null},title={Text("Quantità suggerite",style=T.Title2)},text={
        Column(Modifier.verticalScroll(rememberScrollState())) {
            MacroComparison("Proposta",ManualMeals.total(items),goal)
            items.forEachIndexed { i,it -> Text("${it.food.name}: ${amountText(recipe.ingredients[i].grams)} → ${amountText(it.grams)} ${if(it.food.liquid)"ml" else "g"}",style=T.Small) }
        }
    },confirmButton={TextButton(onClick={change(recipe.copy(ingredients=items)); proposal=null}){Text("Applica proposta")}},dismissButton={TextButton(onClick={proposal=null}){Text("Mantieni le mie quantità")}},containerColor=C.Surface) }
}

@Composable
private fun MacroComparison(title:String, actual:Macro, target:Macro) {
    JCard {
        Text(title,style=T.Label)
        Text("${actual.kcal.r0()} / ${target.kcal.r0()} kcal",style=T.Title2)
        Text("P ${actual.protein.r1()} / ${target.protein.r1()} g · C ${actual.carbs.r1()} / ${target.carbs.r1()} g · G ${actual.fat.r1()} / ${target.fat.r1()} g",style=T.Small)
        Text("Scarto: ${(actual.kcal-target.kcal).r0()} kcal · P ${(actual.protein-target.protein).r1()} · C ${(actual.carbs-target.carbs).r1()} · G ${(actual.fat-target.fat).r1()}",style=T.Small)
    }
}
