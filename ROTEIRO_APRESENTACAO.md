# 🎤 Roteiro Geral de Apresentação (15 a 20 Minutos)
**Tema:** Monitorização de Atividade Física em Android — Sensores Físicos de Passos, GPS e Histórico Agregado  
**Aplicação:** Healthspan  

---

## ⏱️ Distribuição de Tempo da Apresentação

| Secção | Descrição | Tempo |
| :--- | :--- | :--- |
| **1. Enquadramento do Tema** | Contexto mHealth, relevância e abordagem híbrida. | 2–3 min |
| **2. Conceitos Fundamentais** | SensorManager, Step Counter/Detector, GPS, Permissões e Ciclo de Vida. | 3–4 min |
| **3. Comparação de Código (Java vs. Kotlin)** | Implementação prática do listener de sensores e cálculos nas duas linguagens. | 4–5 min |
| **4. Demonstração Prática (Healthspan)** | Navegação ao vivo: Ecrã Principal (Home) e Aba Dedicada de Passos (Semana/Mês). | 3–4 min |
| **5. Vantagens, Limitações e Erros Frequentes** | Pontos fortes, bateria, falsos positivos e boas práticas de engenharia. | 2–3 min |
| **6. Questões para Discussão** | Perguntas de debate com a turma e respostas de apoio. | 2 min |

---

## 1. Enquadramento do Tema (2–3 Minutos)

### 🗣️ Guião de Fala:
> *"Boa tarde a todos. Hoje vou apresentar como podemos utilizar os **sensores físicos de movimento (hardware)** e a **localização GPS** no ecossistema Android para calcular, monitorizar e analisar a atividade física dos utilizadores — focando-nos na contagem de passos diários, semanais e mensais.*
>
> *No mercado atual de aplicações móveis de saúde (mHealth), a experiência do utilizador exige dados precisos sem comprometer a autonomia da bateria. Durante esta apresentação, vamos abordar os conceitos fundamentais da framework de sensores do Android, comparar código em Java e Kotlin, demonstrar a aplicação **Healthspan** a funcionar em tempo real e discutir as principais vantagens e erros comuns no desenvolvimento móvel."*

### 📌 Pontos-Chave:
* **Crescimento mHealth:** O smartphone como coprocessador de saúde contínuo.
* **Eficiência Energética:** Processar o movimento no hardware sem acordar constantemente o CPU principal.
* **Abordagem Híbrida:** Integração de sensores de hardware nativos com a API **Health Connect** da Google.

---

## 2. Conceitos Fundamentais (3–4 Minutos)

### A) Framework de Sensores do Android (`SensorManager`)
O sistema operativo Android disponibiliza a classe `SensorManager` para comunicar com o hardware. Existem dois sensores dedicados a passos:

1. **`Sensor.TYPE_STEP_COUNTER` (Contador de Passos):**
   * **Funcionamento:** Devolve o número total acumulado de passos desde o último reinício (*boot*) do telemóvel.
   * **Conceito de Baseline:** Como o contador não reinicia à meia-noite, a aplicação calcula a linha de base diária:
     $$\text{Passos de Hoje} = \text{Valor Atual do Sensor} - \text{Valor Registado no Início do Dia}$$
2. **`Sensor.TYPE_STEP_DETECTOR` (Detetor de Passos):**
   * **Funcionamento:** Dispara um evento individual (`value[0] = 1.0`) exatamente a cada passo dado em tempo real.

### B) Permissões de Privacidade no Android Moderno (API 29+)
* **`android.permission.ACTIVITY_RECOGNITION`**: Obrigatória no Android 10+ para aceder a sensores de movimento humano.
* **`android.permission.ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION`**: Permite aceder ao GPS para medir distância exata percorrida.

### C) Gestão Eficiente do Ciclo de Vida
* Registo do `SensorEventListener` no `onResume()` e cancelamento no `onPause()` / `onStop()` para prevenir o esgotamento da bateria quando a aplicação está em segundo plano.

---

## 3. Comparação de Código: Java vs. Kotlin (4–5 Minutos)

### A) Leitura e Registo do Sensor Físico de Passos

#### ☕ Código em Java:
```java
public class StepSensorManagerJava implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private long liveStepsToday = 0;
    private float baselineSteps = -1;

    public StepSensorManagerJava(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        }
    }

    public void startListening() {
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    public void stopListening() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            float totalSteps = event.values[0];
            if (baselineSteps < 0) {
                baselineSteps = totalSteps - liveStepsToday;
            }
            liveStepsToday = (long) Math.max(0, totalSteps - baselineSteps);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
```

#### 🟢 Código em Kotlin (Implementado na App `Healthspan`):
* **Ficheiro:** [`StepSensorManager.kt`](file:///C:/Users/rdcto/AndroidStudioProjects/healthspan/app/src/main/java/com/pdm/healthspan/sensor/StepSensorManager.kt)

```kotlin
class StepSensorManager(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    private val _liveStepsToday = MutableStateFlow(0L)
    val liveStepsToday = _liveStepsToday.asStateFlow()

    private var initialCounterBaseline = -1f

    fun startListening() {
        stepCounterSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSinceReboot = event.values[0]
            if (initialCounterBaseline < 0f) {
                initialCounterBaseline = totalSinceReboot - _liveStepsToday.value
            }
            val currentSteps = (totalSinceReboot - initialCounterBaseline).coerceAtLeast(0f).toLong()
            _liveStepsToday.value = currentSteps
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
```

---

### B) Estimativa de Distância (Passada / Geometria)

#### ☕ Em Java:
```java
public double calculateDistanceKm(long steps, double strideLengthMeters) {
    return (steps * strideLengthMeters) / 1000.0;
}
```

#### 🟢 Em Kotlin:
```kotlin
fun calculateDistanceKm(steps: Long, strideLengthMeters: Float = 0.75f): String {
    val distance = (steps * strideLengthMeters) / 1000f
    return String.format(java.util.Locale.getDefault(), "%.2f", distance)
}
```

---

## 4. Demonstração Prática da Aplicação Healthspan (3–4 Minutos)

### 📲 Passos para a Demonstração ao Vivo:

1. **Ecrã Principal (`Home.kt`):**
   * **Mostrar:** O cartão de destaque `HomeStepCard` no topo.
   * **Destacar:** A contagem de passos de hoje (ex: `7.450 / 10.000 passos`), a barra de progresso em tempo real, a estimativa de distância em km e calorias (kcal).
   * **Mostrar:** O badge do estado do sensor ("Sensor Físico Ativo" vs "Health Connect").

2. **Navegação e Aba de Passos (`StepsScreen.kt`):**
   * **Mostrar:** A navegação inferior (`NavigationSuiteScaffold`) com o novo destino **Passos**.
   * **Aba "Semana":** Apresentar o gráfico de barras dos últimos 7 dias, a média diária da semana, o total semanal e a listagem diária.
   * **Aba "Mês":** Apresentar a evolução dos últimos 30 dias obtida com a agrupação por data (`groupBy { LocalDate }`) no `MainViewModel`, destacando os dias em que a meta diária foi atingida.

---

## 5. Vantagens, Limitações e Erros Frequentes (2–3 Minutos)

### 🟢 Vantagens
1. **Processamento em Hardware:** Baixo consumo energético com `TYPE_STEP_COUNTER`.
2. **Arquitetura Reativa:** Fluxo reativo contínuo com `StateFlow` e `Jetpack Compose`.
3. **Resiliência:** Sincronização e fallback com a API Health Connect.

### ⚠️ Limitações
1. **Falsos Positivos:** Vibrações em transportes (carro, autocarro, comboio) podem registar passos incorretos.
2. **Reinício do Dispositivo:** O reinício zera o contador nativo, exigindo cálculo de linha de base (*baseline*).

### 🛑 Erros Frequentes no Desenvolvimento

| Erro Frequente | Consequência | Solução Aplicada no Projeto |
| :--- | :--- | :--- |
| **Falta de `unregisterListener`** | Drenagem rápida da bateria do dispositivo. | Cancelamento no método `onPause()` da [`MainActivity`](file:///C:/Users/rdcto/AndroidStudioProjects/healthspan/app/src/main/java/com/pdm/healthspan/MainActivity.kt#L57). |
| **Falta da permissão `ACTIVITY_RECOGNITION`** | Exceção de segurança ou sensor mudo no Android 10+. | Permissão adicionada ao [`AndroidManifest.xml`](file:///C:/Users/rdcto/AndroidStudioProjects/healthspan/app/src/main/AndroidManifest.xml#L9). |
| **Ignorar o reinício do dispositivo** | Contagem negativa ou inconsistente. | Controlo de *baseline* com persistência local em `SharedPreferences`. |
| **Tamanho de passada fixo** | Imprecisão no cálculo de distância para utilizadores de diferentes alturas. | Parametrização configurável por passada média (0,75m). |

---

## 6. Questões para Discussão com a Turma (2 Minutos)

### ❓ Questão 1 (Arquitetura e Bateria):
> *"Tendo em conta a autonomia da bateria nos dispositivos móveis, em que cenários de uma aplicação de fitness é preferível utilizar o **Sensor Físico de Passos (Pedómetro)** versus a **Localização Contínua via GPS**?"*
>
> **Resposta de Apoio:** O sensor de passos físico deve ser utilizado para monitorização contínua ao longo de todo o dia (gasto energético mínimo). O GPS deve ser ativado exclusivamente durante sessões de exercício ao ar livre (corrida/ciclismo) onde o trajeto e o mapa preciso sejam indispensáveis.

### ❓ Questão 2 (Privacidade e Ecossistemas):
> *"Com o avanço do Android 14 e do **Health Connect**, acham que as aplicações de saúde devem aceder diretamente aos sensores físicos de hardware ou depender unicamente de repositórios agregados do sistema?"*
>
> **Resposta de Apoio:** A abordagem híbrida é a mais recomendada: o hardware direto proporciona contagem reativa e imediata em tempo real, enquanto o Health Connect garante um histórico de longo prazo (30+ dias) consolidado com relógios inteligentes e outras aplicações.
